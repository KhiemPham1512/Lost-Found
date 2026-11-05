from uuid import UUID

import psycopg
from fastapi import APIRouter, Depends

from app.core.deps import get_active_user_id
from app.db import get_conn
from app.schemas.posts import (
    PostCreate,
    PostCreatedResponse,
    PostStatusChange,
    PostStatusResponse,
)
from app.services.posts import change_post_status, create_post

router = APIRouter(prefix="/posts", tags=["posts"])


@router.post("", response_model=PostCreatedResponse, status_code=201)
def create(
    body: PostCreate,
    user_id: str = Depends(get_active_user_id),
    conn: psycopg.Connection = Depends(get_conn),
):
    return create_post(conn, user_id, body)


@router.patch("/{post_id}/status", response_model=PostStatusResponse)
def change_status(
    post_id: UUID,
    body: PostStatusChange,
    user_id: str = Depends(get_active_user_id),
    conn: psycopg.Connection = Depends(get_conn),
):
    return change_post_status(conn, user_id, post_id, body.status)
