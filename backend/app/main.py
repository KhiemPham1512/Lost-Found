from fastapi import FastAPI
from fastapi import Depends

from app.core.config import settings  # nếu .env thiếu thì báo lỗi ngay
from app.core.security import get_current_user_id

app = FastAPI(title="Lost & Found API")


@app.get("/health")
def health():
    return {"status": "ok"}

@app.get("/me")
def me(user_id: str = Depends(get_current_user_id)):
    return {"user_id": user_id}