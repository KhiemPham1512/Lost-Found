from datetime import datetime, timedelta, timezone
from typing import Literal

from pydantic import BaseModel, ConfigDict, Field, field_validator, model_validator


class PostCreate(BaseModel):
    # Tự cắt khoảng trắng đầu/cuối của mọi chuỗi trước khi kiểm tra độ dài
    model_config = ConfigDict(str_strip_whitespace=True)

    type: Literal["LOST", "FOUND"]
    title: str = Field(min_length=3, max_length=120)
    description: str | None = Field(default=None, max_length=2000)
    category_id: int = Field(gt=0)
    location_text: str = Field(min_length=2, max_length=200)
    latitude: float | None = Field(default=None, ge=-90, le=90)
    longitude: float | None = Field(default=None, ge=-180, le=180)
    event_time: datetime
    tags: list[str] = Field(default_factory=list, max_length=10)
    image_paths: list[str] = Field(default_factory=list, max_length=5)
    questions: list[str] = Field(min_length=1, max_length=3)

    @field_validator("event_time")
    @classmethod
    def event_time_ok(cls, v: datetime) -> datetime:
        if v.tzinfo is None:
            raise ValueError("event_time phải có múi giờ, ví dụ 2026-10-23T08:30:00+07:00")
        if v > datetime.now(timezone.utc) + timedelta(minutes=5):
            raise ValueError("event_time không được ở tương lai")
        return v

    @field_validator("tags")
    @classmethod
    def normalize_tags(cls, v: list[str]) -> list[str]:
        out: list[str] = []
        for t in v:
            t = " ".join(t.lower().split())  # viết thường, gộp khoảng trắng
            if not 1 <= len(t) <= 30:
                raise ValueError("Mỗi tag dài 1–30 ký tự")
            if t not in out:
                out.append(t)
        return out

    @field_validator("questions")
    @classmethod
    def check_questions(cls, v: list[str]) -> list[str]:
        for q in v:
            if not 3 <= len(q) <= 300:
                raise ValueError("Mỗi câu hỏi dài 3–300 ký tự")
        return v

    @model_validator(mode="after")
    def coords_together(self):
        if (self.latitude is None) != (self.longitude is None):
            raise ValueError("latitude và longitude phải cùng có hoặc cùng không có")
        return self


class PostCreatedResponse(BaseModel):
    id: str
    status: str
    expires_at: datetime