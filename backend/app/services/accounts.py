import logging
import re
import secrets

import httpx
from fastapi import HTTPException

from app.core.config import settings

logger = logging.getLogger("uvicorn.error")

# Khóa secret chỉ gửi qua header "apikey" (không gửi qua Authorization: Bearer)
_HEADERS = {"apikey": settings.supabase_secret_key}
_TIMEOUT = 10.0
_MAX_HANDLE_TRIES = 5


def make_handle(email: str, attempt: int) -> str:
    """Tạo handle công khai từ email. Luật của database: chữ thường, số, gạch dưới, dài 4-20 ký tự."""
    local = email.split("@")[0].lower()
    base = re.sub(r"[^a-z0-9_]", "", local)
    if len(base) < 3:
        base = base + "user"
    base = base[:15]
    if attempt == 0 and len(base) >= 4:
        return base  # thử tên gọn nhất trước
    return f"{base}{secrets.randbelow(10000):04d}"


def _rollback(client: httpx.Client, user_id: str) -> None:
    """Xóa user vừa tạo ở Auth nếu bước tạo hồ sơ thất bại, để không để lại tài khoản 'mồ côi'."""
    try:
        client.delete(f"/auth/v1/admin/users/{user_id}")
    except httpx.HTTPError:
        logger.error("Không xóa được user mồ côi %s, hãy xóa tay trong dashboard", user_id)


def register_user(email: str, password: str, display_name: str | None) -> dict:
    with httpx.Client(base_url=settings.supabase_url, headers=_HEADERS, timeout=_TIMEOUT) as client:
        # 1) Tạo user ở Supabase Auth (Supabase tự hash mật khẩu)
        r = client.post(
            "/auth/v1/admin/users",
            json={"email": email, "password": password, "email_confirm": True},
        )
        if r.status_code not in (200, 201):
            text = r.text.lower()
            if "email_exists" in text or "already" in text:
                raise HTTPException(status_code=409, detail="Email này đã được đăng ký")
            if "weak_password" in text:
                raise HTTPException(status_code=400, detail="Mật khẩu quá yếu")
            logger.error("Auth admin create_user lỗi: %s %s", r.status_code, r.text)
            raise HTTPException(status_code=502, detail="Không tạo được tài khoản, thử lại sau")
        user_id = r.json()["id"]

        # 2) Tạo hồ sơ công khai (profiles). Nếu handle trùng thì thử handle khác.
        for attempt in range(_MAX_HANDLE_TRIES):
            handle = make_handle(email, attempt)
            payload = {"id": user_id, "username": handle}
            if display_name:
                payload["display_name"] = display_name
            p = client.post(
                "/rest/v1/profiles",
                headers={"Prefer": "return=minimal"},
                json=payload,
            )
            if p.status_code == 201:
                return {"user_id": user_id, "email": email, "username": handle}
            if p.status_code != 409:
                logger.error("Tạo profile lỗi: %s %s", p.status_code, p.text)
                break

        _rollback(client, user_id)
        raise HTTPException(status_code=500, detail="Không tạo được hồ sơ, thử lại sau")