from uuid import UUID

import psycopg
from fastapi import HTTPException
from psycopg.rows import dict_row

from app.services.moderation import log_action


def read_messages(
    conn: psycopg.Connection,
    admin_id: str,
    conversation_id: UUID,
    limit: int,
    before_id: int | None,
):
    # Only a report explicitly targeting this conversation permits inspection.
    if (
        conn.execute(
            "select 1 from public.conversations where id=%s", (conversation_id,)
        ).fetchone()
        is None
    ):
        raise HTTPException(404, "Không tìm thấy hội thoại")
    if (
        conn.execute(
            "select 1 from public.reports where target_conversation_id=%s",
            (conversation_id,),
        ).fetchone()
        is None
    ):
        raise HTTPException(403, "Chỉ được xem hội thoại bị báo cáo")
    with conn.cursor(row_factory=dict_row) as cur:
        cur.execute(
            """select id,sender_id,type,content,image_path,created_at,read_at
            from public.messages where conversation_id=%s and (%s::bigint is null or id < %s)
            order by id desc limit %s""",
            (conversation_id, before_id, before_id, limit),
        )
        messages = cur.fetchall()
    log_action(conn, admin_id, "VIEW_CONVERSATION", "CONVERSATION", conversation_id)
    return {"conversation_id": str(conversation_id), "messages": messages}


def lock_conversation(
    conn: psycopg.Connection, admin_id: str, conversation_id: UUID, reason: str
):
    row = conn.execute(
        "select is_locked from public.conversations where id=%s for update",
        (conversation_id,),
    ).fetchone()
    if row is None:
        raise HTTPException(404, "Không tìm thấy hội thoại")
    if row[0]:
        raise HTTPException(409, "Hội thoại đã bị khóa")
    conn.execute(
        "update public.conversations set is_locked=true where id=%s", (conversation_id,)
    )
    log_action(
        conn,
        admin_id,
        "LOCK_CONVERSATION",
        "CONVERSATION",
        conversation_id,
        note=reason,
    )
    return {"id": str(conversation_id), "is_locked": True}
