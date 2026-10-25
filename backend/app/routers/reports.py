import psycopg
from fastapi import APIRouter, Depends

from app.core.deps import get_active_user_id
from app.db import get_conn
from app.schemas.reports import ReportCreate, ReportCreatedResponse
from app.services.reports import create_report

router = APIRouter(prefix="/reports", tags=["reports"])


@router.post("", response_model=ReportCreatedResponse, status_code=201)
def create(
    body: ReportCreate,
    user_id: str = Depends(get_active_user_id),
    conn: psycopg.Connection = Depends(get_conn),
):
    return create_report(conn, user_id, body)