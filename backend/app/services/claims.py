from uuid import UUID

import psycopg
from fastapi import HTTPException

from app.schemas.claims import ClaimCreate

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