import psycopg
from fastapi import APIRouter, Depends

from app.core.deps import get_active_user_id
from app.db import get_conn
from app.schemas.me import PhoneRequest, PhoneSavedResponse
from app.services.profile import save_phone

router = APIRouter(prefix="/me", tags=["me"])


@router.put("/phone", response_model=PhoneSavedResponse)
def update_phone(
    body: PhoneRequest,
    user_id: str = Depends(get_active_user_id),
    conn: psycopg.Connection = Depends(get_conn),
):
    save_phone(conn, user_id, body.phone)
    return {"phone_saved": True}