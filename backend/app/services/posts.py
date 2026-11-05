import re
from uuid import UUID

import psycopg
from fastapi import HTTPException

from app.db import require_row
from app.schemas.posts import PostCreate

MAX_POSTS_PER_DAY = 10
_IMAGE_NAME = r"[A-Za-z0-9_.-]+\.(?:jpg|jpeg|png|webp)"


def change_post_status(
    conn: psycopg.Connection, user_id: str, post_id: UUID, status: str
) -> dict:
    row = conn.execute(
        "select author_id, status, expires_at > now() from public.posts where id = %s for update",
        (post_id,),
    ).fetchone()
    if row is None:
        raise HTTPException(404, "Không tìm thấy bài đăng")
    author_id, previous, unexpired = row
    if str(author_id) != user_id:
        raise HTTPException(403, "Chỉ chủ bài được thay đổi trạng thái")
    allowed = {
        "ARCHIVED": ("ACTIVE",),
        "ACTIVE": ("ARCHIVED",),
        "DELETED": ("ACTIVE", "ARCHIVED"),
    }
    if previous not in allowed[status] or (status == "ACTIVE" and not unexpired):
        raise HTTPException(409, "Trạng thái hiện tại không cho phép chuyển")
    conn.execute("update public.posts set status = %s where id = %s", (status, post_id))
    if status in ("ARCHIVED", "DELETED"):
        conn.execute(
            "update public.claims set status='CLOSED' where post_id=%s and status in ('SUBMITTED','PASSED')",
            (post_id,),
        )
    return {"id": str(post_id), "status": status}


def create_post(conn: psycopg.Connection, user_id: str, body: PostCreate) -> dict:
    # 1) Chống spam: giới hạn số bài mỗi 24 giờ
    (count,) = require_row(
        conn.execute(
            "select count(*) from public.posts where author_id = %s and created_at > now() - interval '24 hours'",
            (user_id,),
        ).fetchone()
    )
    if count >= MAX_POSTS_PER_DAY:
        raise HTTPException(
            status_code=429,
            detail=f"Bạn đã đăng tối đa {MAX_POSTS_PER_DAY} bài trong 24 giờ",
        )

    # 2) Kiểm tra ảnh: đúng thư mục của chính mình, đúng đuôi file, không trùng, và đã thật sự được tải lên
    paths = body.image_paths
    pattern = re.compile(rf"{re.escape(user_id)}/{_IMAGE_NAME}", re.IGNORECASE)
    if len(set(paths)) != len(paths) or not all(pattern.fullmatch(p) for p in paths):
        raise HTTPException(status_code=400, detail="Đường dẫn ảnh không hợp lệ")
    if paths:
        found = conn.execute(
            "select name from storage.objects where bucket_id = 'post-images' and name = any(%s::text[])",
            (paths,),
        ).fetchall()
        if len(found) != len(paths):
            raise HTTPException(status_code=400, detail="Có ảnh chưa được tải lên")

    # 3) Ghi bài + ảnh + câu hỏi + tag. Tất cả nằm trong MỘT transaction (do get_conn quản lý):
    #    lỗi ở bất kỳ bước nào thì toàn bộ bị hủy, không bao giờ có bài "nửa vời".
    try:
        post_id, status, expires_at = require_row(
            conn.execute(
                """
            insert into public.posts
              (author_id, type, title, description, category_id,
               location_text, latitude, longitude, event_time)
            values (%s, %s, %s, %s, %s, %s, %s, %s, %s)
            returning id, status, expires_at
            """,
                (
                    user_id,
                    body.type,
                    body.title,
                    body.description or None,
                    body.category_id,
                    body.location_text,
                    body.latitude,
                    body.longitude,
                    body.event_time,
                ),
            ).fetchone()
        )

        with conn.cursor() as cur:
            cur.executemany(
                "insert into public.post_images (post_id, storage_path, sort_order) values (%s, %s, %s)",
                [(post_id, p, i) for i, p in enumerate(paths)],
            )
            cur.executemany(
                "insert into public.verification_questions (post_id, question_text, position) values (%s, %s, %s)",
                [(post_id, q, i) for i, q in enumerate(body.questions, start=1)],
            )

        if body.tags:
            conn.execute(
                "insert into public.tags (name) select unnest(%s::text[]) on conflict (name) do nothing",
                (body.tags,),
            )
            conn.execute(
                "insert into public.post_tags (post_id, tag_id) "
                "select %s, id from public.tags where name = any(%s::text[])",
                (post_id, body.tags),
            )
    except psycopg.errors.ForeignKeyViolation:
        raise HTTPException(status_code=422, detail="Danh mục không tồn tại")
    except psycopg.errors.CheckViolation:
        raise HTTPException(status_code=422, detail="Dữ liệu không hợp lệ")

    return {"id": str(post_id), "status": status, "expires_at": expires_at}
