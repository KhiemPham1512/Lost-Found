from datetime import datetime
from typing import Literal
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field


class QuestionOut(BaseModel):
    id: str
    position: int
    question_text: str


class QuestionsResponse(BaseModel):
    post_id: str
    questions: list[QuestionOut]
    attempts_left: int


class AnswerIn(BaseModel):
    model_config = ConfigDict(str_strip_whitespace=True)

    question_id: UUID
    answer_text: str = Field(min_length=1, max_length=500)


class ClaimCreate(BaseModel):
    answers: list[AnswerIn] = Field(default_factory=list, max_length=3)


class ClaimCreatedResponse(BaseModel):
    claim_id: str
    status: str


class ClaimantOut(BaseModel):
    id: UUID
    username: str
    display_name: str | None
    avatar_url: str | None


class ClaimAnswerOut(BaseModel):
    question_id: UUID
    position: int
    question_text: str
    answer_text: str


class ClaimListItem(BaseModel):
    claim_id: UUID
    claimant: ClaimantOut
    status: str
    created_at: datetime
    answers: list[ClaimAnswerOut]


class ClaimsResponse(BaseModel):
    total: int
    items: list[ClaimListItem]


class DecisionRequest(BaseModel):
    model_config = ConfigDict(str_strip_whitespace=True)

    decision: Literal["PASS", "FAIL"]
    note: str | None = Field(default=None, max_length=500)


class DecisionResponse(BaseModel):
    claim_id: str
    status: str
    conversation_id: str | None = None


class CancelResponse(BaseModel):
    claim_id: str
    status: str


class ConfirmResponse(BaseModel):
    claim_id: str
    status: str
    author_confirmed_at: datetime | None = None
    claimant_confirmed_at: datetime | None = None
