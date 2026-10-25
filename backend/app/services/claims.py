from uuid import UUID

import psycopg
from fastapi import HTTPException

from app.schemas.claims import ClaimCreate, DecisionRequest

MAX_ATTEMPTS = 2  # số lần bị Fail hoặc tự hủy tối đa trên một bài


def _check_can_claim(conn: psycopg.Connection, user_id: str, post_id: UUID) -> int:
    """Kiểm tra người này có được phép xác minh bài này không. Trả về số lượt còn lại."""
    post = conn.execute(
        "select author_id, status from public.posts where id = %s", (post_id,)
    ).fetchone()
    if post is None:
        raise HTTPException(status_code=404, detail="Không tìm thấy bài đăng")
    author_id, status = post
    if str(author_id) == user_id:
        raise HTTPException(status_code=403, detail="Đây là bài của bạn")
    if status != "ACTIVE":
        raise HTTPException(status_code=409, detail="Bài đăng không còn hoạt động")

    (active,) = conn.execute(
        "select count(*) from public.claims "
        "where post_id = %s and claimant_id = %s and status in ('SUBMITTED', 'PASSED')",
        (post_id, user_id),
    ).fetchone()
    if active:
        raise HTTPException(status_code=409, detail="Bạn đã có yêu cầu xác minh đang xử lý cho bài này")

    (used,) = conn.execute(
        "select count(*) from public.claims "
        "where post_id = %s and claimant_id = %s and status in ('FAILED', 'CANCELLED')",
        (post_id, user_id),
    ).fetchone()
    left = MAX_ATTEMPTS - used
    if left <= 0:
        raise HTTPException(status_code=429, detail="Bạn đã hết lượt thử cho bài này")
    return left


def get_questions(conn: psycopg.Connection, user_id: str, post_id: UUID) -> dict:
    left = _check_can_claim(conn, user_id, post_id)
    rows = conn.execute(
        "select id, position, question_text from public.verification_questions "
        "where post_id = %s order by position",
        (post_id,),
    ).fetchall()
    return {
        "post_id": str(post_id),
        "questions": [{"id": str(r[0]), "position": r[1], "question_text": r[2]} for r in rows],
        "attempts_left": left,
    }


def submit_claim(conn: psycopg.Connection, user_id: str, post_id: UUID, body: ClaimCreate) -> dict:
    _check_can_claim(conn, user_id, post_id)

    # Phải trả lời ĐỦ và ĐÚNG các câu hỏi của bài: không thiếu, không thừa, không trùng
    rows = conn.execute(
        "select id from public.verification_questions where post_id = %s", (post_id,)
    ).fetchall()
    expected = {r[0] for r in rows}
    given = [a.question_id for a in body.answers]
    if len(set(given)) != len(given) or set(given) != expected:
        raise HTTPException(status_code=422, detail="Phải trả lời đủ và đúng các câu hỏi của bài")

    try:
        (claim_id,) = conn.execute(
            "insert into public.claims (post_id, claimant_id) values (%s, %s) returning id",
            (post_id, user_id),
        ).fetchone()
        with conn.cursor() as cur:
            cur.executemany(
                "insert into public.claim_answers (claim_id, question_id, answer_text) values (%s, %s, %s)",
                [(claim_id, a.question_id, a.answer_text) for a in body.answers],
            )
    except psycopg.errors.UniqueViolation:
        # hai yêu cầu gửi cùng lúc: chỉ một cái được vào
        raise HTTPException(status_code=409, detail="Bạn đã có yêu cầu xác minh đang xử lý cho bài này")
    except psycopg.errors.RaiseException:
        # luật nghiệp vụ trong database (bài không hoạt động, tự claim bài mình...)
        raise HTTPException(status_code=409, detail="Không thể gửi yêu cầu cho bài này")

    return {"claim_id": str(claim_id), "status": "SUBMITTED"}

def _load_claim_for_update(conn: psycopg.Connection, claim_id: UUID) -> tuple:
    """Lấy claim và KHÓA dòng đó đến hết transaction, để hai người không xử lý cùng lúc."""
    row = conn.execute(
        "select post_id, post_author_id, claimant_id, status "
        "from public.claims where id = %s for update",
        (claim_id,),
    ).fetchone()
    if row is None:
        raise HTTPException(status_code=404, detail="Không tìm thấy yêu cầu xác minh")
    return row


def decide_claim(conn: psycopg.Connection, user_id: str, claim_id: UUID, body: DecisionRequest) -> dict:
    post_id, author_id, _claimant_id, status = _load_claim_for_update(conn, claim_id)
    if str(author_id) != user_id:
        raise HTTPException(status_code=403, detail="Chỉ chủ bài mới được quyết định")
    if status != "SUBMITTED":
        raise HTTPException(status_code=409, detail="Yêu cầu này đã được xử lý")
    (post_status,) = conn.execute(
        "select status from public.posts where id = %s", (post_id,)
    ).fetchone()
    if post_status != "ACTIVE":
        raise HTTPException(status_code=409, detail="Bài đăng không còn hoạt động")

    new_status = "PASSED" if body.decision == "PASS" else "FAILED"
    # Trigger trong database lo phần còn lại: PASS thì tự tạo cuộc trò chuyện, tin nhắn hệ thống và thông báo
    conn.execute(
        "update public.claims set status = %s, decision_note = %s where id = %s",
        (new_status, body.note or None, claim_id),
    )
    conversation_id = None
    if new_status == "PASSED":
        row = conn.execute(
            "select id from public.conversations where claim_id = %s", (claim_id,)
        ).fetchone()
        conversation_id = str(row[0]) if row else None
    return {"claim_id": str(claim_id), "status": new_status, "conversation_id": conversation_id}


def cancel_claim(conn: psycopg.Connection, user_id: str, claim_id: UUID) -> dict:
    _post_id, _author_id, claimant_id, status = _load_claim_for_update(conn, claim_id)
    if str(claimant_id) != user_id:
        raise HTTPException(status_code=403, detail="Chỉ người gửi yêu cầu mới được hủy")
    if status not in ("SUBMITTED", "PASSED"):
        raise HTTPException(status_code=409, detail="Yêu cầu này không thể hủy")
    conn.execute("update public.claims set status = 'CANCELLED' where id = %s", (claim_id,))
    return {"claim_id": str(claim_id), "status": "CANCELLED"}

def confirm_handover(conn: psycopg.Connection, user_id: str, claim_id: UUID) -> dict:
    """Mỗi bên xác nhận phần của mình. Khi đủ cả hai, trigger trong database tự chuyển sang DONE."""
    post_id, author_id, claimant_id, status = _load_claim_for_update(conn, claim_id)
    is_author = str(author_id) == user_id
    if not is_author and str(claimant_id) != user_id:
        raise HTTPException(status_code=403, detail="Bạn không tham gia yêu cầu này")

    if status == "PASSED":
        (post_status,) = conn.execute(
            "select status from public.posts where id = %s", (post_id,)
        ).fetchone()
        if post_status != "ACTIVE":
            raise HTTPException(status_code=409, detail="Bài đăng không còn hoạt động")
        column = "author_confirmed_at" if is_author else "claimant_confirmed_at"
        # coalesce: đã xác nhận rồi thì giữ nguyên thời điểm cũ (gọi nhiều lần không gây lỗi)
        conn.execute(
            f"update public.claims set {column} = coalesce({column}, now()) where id = %s",
            (claim_id,),
        )
    elif status != "COMPLETED":
        raise HTTPException(status_code=409, detail="Yêu cầu này chưa ở bước trao trả")

    new_status, author_at, claimant_at = conn.execute(
        "select status, author_confirmed_at, claimant_confirmed_at from public.claims where id = %s",
        (claim_id,),
    ).fetchone()
    return {
        "claim_id": str(claim_id),
        "status": new_status,
        "author_confirmed_at": author_at,
        "claimant_confirmed_at": claimant_at,
    }