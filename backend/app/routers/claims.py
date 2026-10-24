from uuid import UUID

import psycopg
from fastapi import APIRouter, Depends

from app.core.deps import get_active_user_id
from app.db import get_conn
from app.schemas.claims import (
    CancelResponse,
    ClaimCreate,
    ClaimCreatedResponse,
    DecisionRequest,
    DecisionResponse,
    QuestionsResponse,
)
from app.services.claims import cancel_claim, decide_claim, get_questions, submit_claim

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

@router.post("/claims/{claim_id}/decision", response_model=DecisionResponse)
def decision(
    claim_id: UUID,
    body: DecisionRequest,
    user_id: str = Depends(get_active_user_id),
    conn: psycopg.Connection = Depends(get_conn),
):
    return decide_claim(conn, user_id, claim_id, body)


@router.post("/claims/{claim_id}/cancel", response_model=CancelResponse)
def cancel(
    claim_id: UUID,
    user_id: str = Depends(get_active_user_id),
    conn: psycopg.Connection = Depends(get_conn),
):
    return cancel_claim(conn, user_id, claim_id)