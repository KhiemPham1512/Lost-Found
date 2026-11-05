from typing import Literal
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field


class ReportCreate(BaseModel):
    model_config = ConfigDict(str_strip_whitespace=True)

    target_type: Literal["POST", "USER", "CONVERSATION"]
    target_id: UUID
    reason: Literal["SPAM", "SCAM", "FALSE_INFO", "MONEY_REQUEST", "VIOLATION", "OTHER"]
    description: str | None = Field(default=None, max_length=1000)


class ReportCreatedResponse(BaseModel):
    report_id: str
    code: str  # mã dễ đọc, ví dụ R-1001
    status: str