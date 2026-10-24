from uuid import UUID

import psycopg
from fastapi import APIRouter, Depends

from app.core.deps import get_active_user_id
from app.db import get_conn
from app.schemas.claims import ClaimCreate, ClaimCreatedResponse, QuestionsResponse
from app.services.claims import get_questions, submit_claim

router = APIRouter(tags=["claims"])


@router.get("/posts/{post_id}/questions", response_model=QuestionsResponse)
def read_questions(
    post_id: UUID,
    user_id: str = Depends(get_active_user_id),
    conn: psycopg.Connection = Depends(get_conn),
):
    return get_questions(conn, user_id, post_id)


@router.post("/posts/{post_id}/claims", response_model=ClaimCreatedResponse, status_code=201)
def create_claim(
    post_id: UUID,
    body: ClaimCreate,
    user_id: str = Depends(get_active_user_id),
    conn: psycopg.Connection = Depends(get_conn),
):
    return submit_claim(conn, user_id, post_id, body)