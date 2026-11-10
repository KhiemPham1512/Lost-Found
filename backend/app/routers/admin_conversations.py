from uuid import UUID

import psycopg
from fastapi import APIRouter, Depends, Path, Query, Response

from app.core.deps import get_admin_user_id
from app.db import get_conn
from app.schemas.admin import (
    AdminMessagesResponse,
    AdminImageResponse,
    LockedConversationResponse,
    ModerationReason,
)
from app.services.admin_conversations import (
    lock_conversation,
    read_messages,
    read_image,
)

router = APIRouter(prefix="/admin/conversations", tags=["admin"])


@router.get("/{conversation_id}/messages", response_model=AdminMessagesResponse)
def messages(
    conversation_id: UUID,
    limit: int = Query(100, ge=1, le=100),
    before_id: int | None = Query(None, ge=1),
    admin_id: str = Depends(get_admin_user_id),
    conn: psycopg.Connection = Depends(get_conn),
):
    return read_messages(conn, admin_id, conversation_id, limit, before_id)


@router.get(
    "/{conversation_id}/messages/{message_id}/image", response_model=AdminImageResponse
)
def image(
    conversation_id: UUID,
    response: Response,
    message_id: int = Path(ge=1),
    admin_id: str = Depends(get_admin_user_id),
    conn: psycopg.Connection = Depends(get_conn),
):
    response.headers["Cache-Control"] = "no-store"
    response.headers["Referrer-Policy"] = "no-referrer"
    return read_image(conn, admin_id, conversation_id, message_id)


@router.post("/{conversation_id}/lock", response_model=LockedConversationResponse)
def lock(
    conversation_id: UUID,
    body: ModerationReason,
    admin_id: str = Depends(get_admin_user_id),
    conn: psycopg.Connection = Depends(get_conn),
):
    return lock_conversation(conn, admin_id, conversation_id, body.reason)
