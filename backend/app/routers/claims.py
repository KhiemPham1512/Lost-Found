from uuid import UUID

import psycopg
from fastapi import APIRouter, Depends, Query

from app.core.deps import get_active_user_id
from app.db import get_conn
from app.schemas.claims import (
    CancelResponse,
    ClaimCreate,
    ClaimCreatedResponse,
    ClaimsResponse,
    ConfirmResponse,
    DecisionRequest,
    DecisionResponse,
    QuestionsResponse,
)
from app.services.claims import (
    cancel_claim,
    confirm_handover,
    decide_claim,
    get_questions,
    list_claims,
    submit_claim,
)

router = APIRouter(tags=["claims"])


@router.get("/posts/{post_id}/claims", response_model=ClaimsResponse)
def read_claims(
    post_id: UUID,
    limit: int = Query(20, ge=1, le=100),
    offset: int = Query(0, ge=0),
    user_id: str = Depends(get_active_user_id),
    conn: psycopg.Connection = Depends(get_conn),
):
    return list_claims(conn, user_id, post_id, limit, offset)


@router.get("/posts/{post_id}/questions", response_model=QuestionsResponse)
def read_questions(
    post_id: UUID,
    user_id: str = Depends(get_active_user_id),
    conn: psycopg.Connection = Depends(get_conn),
):
    return get_questions(conn, user_id, post_id)


@router.post(
    "/posts/{post_id}/claims", response_model=ClaimCreatedResponse, status_code=201
)
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


@router.post("/claims/{claim_id}/confirm", response_model=ConfirmResponse)
def confirm(
    claim_id: UUID,
    user_id: str = Depends(get_active_user_id),
    conn: psycopg.Connection = Depends(get_conn),
):
    return confirm_handover(conn, user_id, claim_id)
