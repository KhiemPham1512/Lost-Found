from typing import Literal
from uuid import UUID

import psycopg
from fastapi import APIRouter, Depends, Query

from app.core.deps import get_admin_user_id
from app.db import get_conn
from app.schemas.admin import (
    ReportListResponse,
    ReportStatus,
    ReportTargetType,
    ResolveRequest,
    ResolveResponse,
    ReviewResponse,
)
from app.services.admin_reports import (
    get_report_detail,
    list_reports,
    resolve_report,
    review_report,
)

router = APIRouter(prefix="/admin", tags=["admin"])


@router.get("/reports", response_model=ReportListResponse)
def reports_list(
    status: ReportStatus | None = None,
    type: ReportTargetType | None = None,
    q: str | None = Query(default=None, max_length=100),
    limit: int = Query(default=20, ge=1, le=100),
    offset: int = Query(default=0, ge=0),
    _admin_id: str = Depends(get_admin_user_id),
    conn: psycopg.Connection = Depends(get_conn),
):
    return list_reports(conn, status, type, q, limit, offset)


@router.get("/reports/{report_id}")
def report_detail(
    report_id: UUID,
    _admin_id: str = Depends(get_admin_user_id),
    conn: psycopg.Connection = Depends(get_conn),
):
    return get_report_detail(conn, report_id)


@router.post("/reports/{report_id}/review", response_model=ReviewResponse)
def report_review(
    report_id: UUID,
    admin_id: str = Depends(get_admin_user_id),
    conn: psycopg.Connection = Depends(get_conn),
):
    return review_report(conn, admin_id, report_id)


@router.post("/reports/{report_id}/resolve", response_model=ResolveResponse)
def report_resolve(
    report_id: UUID,
    body: ResolveRequest,
    admin_id: str = Depends(get_admin_user_id),
    conn: psycopg.Connection = Depends(get_conn),
):
    return resolve_report(conn, admin_id, report_id, body)
