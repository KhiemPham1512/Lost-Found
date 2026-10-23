import psycopg
from fastapi import Depends, HTTPException

from app.core.security import get_current_user_id
from app.db import get_conn


def get_active_user_id(
    user_id: str = Depends(get_current_user_id),
    conn: psycopg.Connection = Depends(get_conn),
) -> str:
    """Kiểm tra token VÀ kiểm tra tài khoản còn tồn tại, chưa bị khóa."""
    row = conn.execute(
        "select is_banned from public.profiles where id = %s", (user_id,)
    ).fetchone()
    if row is None:
        raise HTTPException(status_code=403, detail="Tài khoản chưa có hồ sơ")
    if row[0]:
        raise HTTPException(status_code=403, detail="Tài khoản đã bị khóa")
    return user_id