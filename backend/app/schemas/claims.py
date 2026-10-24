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
    answers: list[AnswerIn] = Field(min_length=1, max_length=3)


class ClaimCreatedResponse(BaseModel):
    claim_id: str
    status: str