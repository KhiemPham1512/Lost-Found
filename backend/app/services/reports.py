import psycopg
from fastapi import HTTPException

from app.schemas.reports import ReportCreate

MAX_REPORTS_PER_DAY = 10

# Tên cột do CODE quyết định (không bao giờ lấy từ dữ liệu người dùng) nên ghép vào câu SQL là an toàn
_TARGET_COLUMN = {
    "POST": "target_post_id",
    "USER": "target_user_id",
    "CONVERSATION": "target_conversation_id",
}


def create_report(conn: psycopg.Connection, user_id: str, body: ReportCreate) -> dict:
    # 1) Chống báo cáo hàng loạt
    (count,) = conn.execute(
        "select count(*) from public.reports where reporter_id = %s and created_at > now() - interval '24 hours'",
        (user_id,),
    ).fetchone()
    if count >= MAX_REPORTS_PER_DAY:
        raise HTTPException(status_code=429, detail=f"Bạn đã gửi tối đa {MAX_REPORTS_PER_DAY} báo cáo trong 24 giờ")

    # 2) Đối tượng bị báo cáo phải có thật và hợp lệ
    target_id = body.target_id
    if body.target_type == "POST":
        row = conn.execute(
            "select author_id from public.posts where id = %s and status <> 'DELETED'", (target_id,)
        ).fetchone()
        if row is None:
            raise HTTPException(status_code=404, detail="Không tìm thấy bài đăng")
        if str(row[0]) == user_id:
            raise HTTPException(status_code=400, detail="Bạn không thể báo cáo bài của chính mình")
    elif body.target_type == "USER":
        if str(target_id) == user_id:
            raise HTTPException(status_code=400, detail="Bạn không thể báo cáo chính mình")
        if conn.execute("select 1 from public.profiles where id = %s", (target_id,)).fetchone() is None:
            raise HTTPException(status_code=404, detail="Không tìm thấy người dùng")
    else:  # CONVERSATION: chỉ người trong cuộc mới báo cáo được cuộc trò chuyện của mình
        row = conn.execute(
            "select 1 from public.conversations where id = %s and (post_author_id = %s or claimant_id = %s)",
            (target_id, user_id, user_id),
        ).fetchone()
        if row is None:
            raise HTTPException(status_code=404, detail="Không tìm thấy cuộc trò chuyện")

    column = _TARGET_COLUMN[body.target_type]

    # 3) Không báo cáo trùng khi báo cáo cũ còn đang chờ xử lý
    (dup,) = conn.execute(
        f"select count(*) from public.reports where reporter_id = %s and {column} = %s "
        "and status in ('PENDING', 'REVIEWING')",
        (user_id, target_id),
    ).fetchone()
    if dup:
        raise HTTPException(status_code=409, detail="Bạn đã báo cáo mục này, báo cáo đang chờ xử lý")

    report_id, report_no, status = conn.execute(
        f"insert into public.reports (reporter_id, {column}, reason, description) "
        "values (%s, %s, %s, %s) returning id, report_no, status",
        (user_id, target_id, body.reason, body.description or None),
    ).fetchone()
    return {"report_id": str(report_id), "code": f"R-{report_no}", "status": status}