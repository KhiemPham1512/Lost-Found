from fastapi import Depends, FastAPI

from app.core.config import settings  # noqa: F401  (thiếu cấu hình thì báo lỗi ngay)
from app.core.security import get_current_user_id
from app.routers import auth

app = FastAPI(title="Lost & Found API")
app.include_router(auth.router)


@app.get("/health")
def health():
    return {"status": "ok"}


@app.get("/me")
def me(user_id: str = Depends(get_current_user_id)):
    return {"user_id": user_id}