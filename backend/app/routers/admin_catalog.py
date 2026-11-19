from typing import Literal
from uuid import UUID

import psycopg
from fastapi import APIRouter, Depends, Query

from app.core.deps import get_admin_user_id
from app.db import get_conn
from app.schemas.admin import (
    AdminActionsResponse,
    AdminPostsResponse,
    AdminPostDetail,
    AdminStatsResponse,
    AdminUsersResponse,
    AdminUserDetail,
    PostStatus,
)
from app.services import admin_catalog

router = APIRouter(
    prefix="/admin", tags=["admin"], dependencies=[Depends(get_admin_user_id)]
)


@router.get("/posts", response_model=AdminPostsResponse)
def posts(
    type: Literal["LOST", "FOUND"] | None = None,
    status: PostStatus | None = None,
    q: str | None = Query(None, max_length=100),
    limit: int = Query(20, ge=1, le=100),
    offset: int = Query(0, ge=0),
    conn: psycopg.Connection = Depends(get_conn),
):
    return admin_catalog.list_posts(conn, type, status, q, limit, offset)


@router.get("/posts/{post_id}", response_model=AdminPostDetail)
def post_detail(post_id: UUID, conn: psycopg.Connection = Depends(get_conn)):
    return admin_catalog.get_post(conn, post_id)


@router.get("/users", response_model=AdminUsersResponse)
def users(
    status: Literal["ACTIVE", "BANNED", "REPORTED"] | None = None,
    q: str | None = Query(None, max_length=100),
    limit: int = Query(20, ge=1, le=100),
    offset: int = Query(0, ge=0),
    conn: psycopg.Connection = Depends(get_conn),
):
    return admin_catalog.list_users(conn, status, q, limit, offset)


@router.get("/users/{user_id}", response_model=AdminUserDetail)
def user_detail(user_id: UUID, conn: psycopg.Connection = Depends(get_conn)):
    return admin_catalog.get_user(conn, user_id)


@router.get("/actions", response_model=AdminActionsResponse)
def actions(
    target_type: Literal["POST", "USER", "REPORT", "CONVERSATION"] | None = None,
    range: Literal["today", "7d", "30d"] | None = None,
    q: str | None = Query(None, max_length=100),
    limit: int = Query(20, ge=1, le=100),
    offset: int = Query(0, ge=0),
    conn: psycopg.Connection = Depends(get_conn),
):
    return admin_catalog.list_actions(conn, target_type, range, q, limit, offset)


@router.get("/stats", response_model=AdminStatsResponse)
def stats(conn: psycopg.Connection = Depends(get_conn)):
    return admin_catalog.get_stats(conn)
