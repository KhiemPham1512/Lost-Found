from fastapi import FastAPI

from app.core.config import settings  # nếu .env thiếu thì báo lỗi ngay

app = FastAPI(title="Lost & Found API")


@app.get("/health")
def health():
    return {"status": "ok"}