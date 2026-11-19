from uuid import UUID
import re
from urllib.parse import parse_qs, urlsplit

import httpx
import psycopg
from fastapi import HTTPException
from psycopg.rows import dict_row

from app.services.moderation import log_action
from app.core.config import settings


def _require_reported(conn: psycopg.Connection, conversation_id: UUID):
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


def read_messages(
    conn: psycopg.Connection,
    admin_id: str,
    conversation_id: UUID,
    limit: int,
    before_id: int | None,
):
    _require_reported(conn, conversation_id)
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


def read_image(
    conn: psycopg.Connection, admin_id: str, conversation_id: UUID, message_id: int
):
    _require_reported(conn, conversation_id)
    row = conn.execute(
        "select sender_id,image_path from public.messages where id=%s and conversation_id=%s and type='IMAGE'",
        (message_id, conversation_id),
    ).fetchone()
    if row is None:
        raise HTTPException(404, "Không tìm thấy ảnh trong hội thoại")
    sender_id, path = row
    if (
        sender_id is None
        or not isinstance(path, str)
        or not re.fullmatch(
            rf"{conversation_id}/{sender_id}/[A-Za-z0-9_-]{{1,80}}\.(?:jpg|jpeg|png|webp)",
            path,
        )
    ):
        raise HTTPException(422, "Đường dẫn ảnh không hợp lệ")
    if (
        conn.execute(
            "select 1 from storage.buckets where id='chat-images' and not public"
        ).fetchone()
        is None
    ):
        raise HTTPException(503, "Chưa cấu hình kho ảnh chat riêng tư")
    if (
        conn.execute(
            "select 1 from storage.objects where bucket_id='chat-images' and name=%s",
            (path,),
        ).fetchone()
        is None
    ):
        raise HTTPException(404, "Ảnh không còn tồn tại")
    signed_path = f"/object/sign/chat-images/{path}"
    try:
        with httpx.Client(timeout=10) as client:
            response = client.post(
                f"{settings.supabase_url.rstrip('/')}/storage/v1{signed_path}",
                headers={"apikey": settings.supabase_secret_key},
                json={"expiresIn": 60},
            )
            response.raise_for_status()
            signed = response.json().get("signedURL")
        if not isinstance(signed, str):
            raise ValueError("Invalid signing response")
        parts = urlsplit(signed)
        token = parse_qs(parts.query).get("token", [])
        if (
            parts.scheme
            or parts.netloc
            or parts.fragment
            or parts.path != signed_path
            or len(token) != 1
            or not token[0]
        ):
            raise ValueError("Invalid signed URL")
    except (httpx.HTTPError, ValueError, AttributeError):
        raise HTTPException(502, "Chưa mở được ảnh. Vui lòng thử lại.") from None
    log_action(
        conn,
        admin_id,
        "VIEW_CONVERSATION",
        "CONVERSATION",
        conversation_id,
        note=f"Xem ảnh tin nhắn {message_id}",
    )
    return {
        "url": f"{settings.supabase_url.rstrip('/')}/storage/v1{signed}",
        "expires_in": 60,
    }


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
