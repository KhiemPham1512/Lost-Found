from fastapi import APIRouter

from app.schemas.auth import RegisterRequest, RegisterResponse
from app.services.accounts import register_user

router = APIRouter(prefix="/auth", tags=["auth"])


@router.post("/register", response_model=RegisterResponse, status_code=201)
def register(body: RegisterRequest):
    return register_user(str(body.email).lower(), body.password, body.display_name)