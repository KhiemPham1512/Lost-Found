from contextlib import asynccontextmanager

from fastapi import Depends, FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.core.config import settings  # noqa: F401  (thiếu cấu hình thì báo lỗi ngay)
from app.core.security import get_current_user_id
from app.db import close_pool, open_pool
from app.routers import (
    admin_catalog,
    admin_conversations,
    admin_moderation,
    admin_reports,
    auth,
    claims,
    me,
    posts,
    reports,
)


@asynccontextmanager
async def lifespan(app: FastAPI):
    open_pool()  # chạy khi server khởi động
    yield
    close_pool()  # chạy khi server tắt


app = FastAPI(
    title="Lost & Found API",
    lifespan=lifespan,
    swagger_ui_parameters={
        "persistAuthorization": True
    },  # nhớ token sau khi tải lại trang /docs
)
app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        origin.strip().rstrip("/")
        for origin in settings.allowed_origins.split(",")
        if origin.strip()
    ],
    allow_credentials=False,
    allow_methods=["GET", "POST", "PUT", "PATCH", "OPTIONS"],
    allow_headers=["Authorization", "Content-Type"],
)
app.include_router(auth.router)
app.include_router(me.router)
app.include_router(posts.router)
app.include_router(claims.router)
app.include_router(reports.router)
app.include_router(admin_reports.router)
app.include_router(admin_catalog.router)
app.include_router(admin_conversations.router)
app.include_router(admin_moderation.router)


@app.get("/health")
def health():
    return {"status": "ok"}


@app.get("/me")
def read_me(user_id: str = Depends(get_current_user_id)):
    return {"user_id": user_id}
