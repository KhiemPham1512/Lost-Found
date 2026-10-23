from contextlib import asynccontextmanager

from fastapi import Depends, FastAPI

from app.core.config import settings  # noqa: F401  (thiếu cấu hình thì báo lỗi ngay)
from app.core.security import get_current_user_id
from app.db import close_pool, open_pool
from app.routers import auth, me


@asynccontextmanager
async def lifespan(app: FastAPI):
    open_pool()      # chạy khi server khởi động
    yield
    close_pool()     # chạy khi server tắt


app = FastAPI(title="Lost & Found API", lifespan=lifespan)
app.include_router(auth.router)
app.include_router(me.router)


@app.get("/health")
def health():
    return {"status": "ok"}


@app.get("/me")
def read_me(user_id: str = Depends(get_current_user_id)):
    return {"user_id": user_id}