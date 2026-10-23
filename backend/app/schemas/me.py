import re

from pydantic import BaseModel, field_validator


class PhoneRequest(BaseModel):
    phone: str

    @field_validator("phone")
    @classmethod
    def valid_phone(cls, v: str) -> str:
        v = re.sub(r"[\s.\-]", "", v)  # bỏ khoảng trắng, dấu chấm, gạch ngang
        if not re.fullmatch(r"(0|\+84)[0-9]{9,10}", v):
            raise ValueError("Số điện thoại không hợp lệ (ví dụ 0901234567)")
        return v


class PhoneSavedResponse(BaseModel):
    phone_saved: bool