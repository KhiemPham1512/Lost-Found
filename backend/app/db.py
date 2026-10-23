from collections.abc import Iterator

import psycopg
from fastapi import HTTPException
from psycopg_pool import ConnectionPool

from app.core.config import settings

_pool: ConnectionPool | None = None


def open_pool() -> None:
    """Mở nhóm kết nối khi server khởi động (gọi trong lifespan của main.py)."""
    global _pool
    if not settings.database_url:
        return  # chưa cấu hình DATABASE_URL: các endpoint cần database sẽ trả 503
    _pool = ConnectionPool(
        settings.database_url,
        min_size=1,
        max_size=5,
        # prepare_threshold=None: tắt "prepared statement" cho an toàn với mọi kiểu pooler
        kwargs={"prepare_threshold": None},
        open=True,
    )


def close_pool() -> None:
    if _pool is not None:
        _pool.close()


def get_conn() -> Iterator[psycopg.Connection]:
    """Mỗi request một kết nối. Thoát bình thường thì COMMIT, có lỗi thì ROLLBACK."""
    if _pool is None:
        raise HTTPException(status_code=503, detail="Chưa cấu hình database")
    with _pool.connection() as conn:
        yield conn