from uuid import UUID

import psycopg
from fastapi import APIRouter, Depends

from app.core.deps import get_admin_user_id
from app.db import get_conn
from app.schemas.admin import (
    ModeratedPostResponse,
    ModeratedUserResponse,
    ModerationReason,
)
from app.services import moderation

router = APIRouter(prefix="/admin", tags=["admin"])


@router.post("/posts/{post_id}/remove", response_model=ModeratedPostResponse)
def remove(
    post_id: UUID,
    body: ModerationReason,
    admin_id: str = Depends(get_admin_user_id),
    conn: psycopg.Connection = Depends(get_conn),
):
    moderation.remove_post(conn, admin_id, post_id, body.reason)
    return {"id": str(post_id), "status": "REMOVED"}


@router.post("/posts/{post_id}/restore", response_model=ModeratedPostResponse)
def restore(
    post_id: UUID,
    admin_id: str = Depends(get_admin_user_id),
    conn: psycopg.Connection = Depends(get_conn),
):
    return moderation.restore_post(conn, admin_id, post_id)


@router.post("/users/{user_id}/ban", response_model=ModeratedUserResponse)
def ban(
    user_id: UUID,
    body: ModerationReason,
    admin_id: str = Depends(get_admin_user_id),
    conn: psycopg.Connection = Depends(get_conn),
):
    moderation.ban_user(conn, admin_id, user_id, body.reason)
    blocked = moderation.set_auth_ban(user_id, True)
    return {"id": str(user_id), "is_banned": True, "login_blocked": blocked}


@router.post("/users/{user_id}/unban", response_model=ModeratedUserResponse)
def unban(
    user_id: UUID,
    body: ModerationReason,
    admin_id: str = Depends(get_admin_user_id),
    conn: psycopg.Connection = Depends(get_conn),
):
    return moderation.unban_user(conn, admin_id, user_id, body.reason)
