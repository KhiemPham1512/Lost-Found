import psycopg
from fastapi import APIRouter, Depends

from app.core.deps import get_active_user_id
from app.db import get_conn
from app.schemas.posts import PostCreate, PostCreatedResponse
from app.services.posts import create_post

router = APIRouter(prefix="/posts", tags=["posts"])


@router.post("", response_model=PostCreatedResponse, status_code=201)
def create(
    body: PostCreate,
    user_id: str = Depends(get_active_user_id),
    conn: psycopg.Connection = Depends(get_conn),
):
    return create_post(conn, user_id, body)