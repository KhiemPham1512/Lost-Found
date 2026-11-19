from datetime import date as Date
from datetime import datetime
from typing import Literal
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field

ReportStatus = Literal["PENDING", "REVIEWING", "RESOLVED", "DISMISSED"]
ReportTargetType = Literal["POST", "USER", "CONVERSATION"]


class ModerationReason(BaseModel):
    model_config = ConfigDict(str_strip_whitespace=True)
    reason: str = Field(min_length=3, max_length=500)


class UserRef(BaseModel):
    id: str
    username: str


class ReportListItem(BaseModel):
    id: str
    code: str  # R-1042
    target_type: ReportTargetType
    target_id: str
    target_label: str  # tiêu đề bài, handle người dùng, hoặc "a ↔ b" với hội thoại
    reason: str
    status: ReportStatus
    reporter: UserRef
    created_at: datetime
    same_target_count: int  # tổng số báo cáo về cùng đối tượng (kể cả báo cáo này)


class ReportListResponse(BaseModel):
    items: list[ReportListItem]
    total: int


class ReviewResponse(BaseModel):
    report_id: str
    code: str
    status: str
    affected_reports: int


class ResolveRequest(BaseModel):
    model_config = ConfigDict(str_strip_whitespace=True)

    action: Literal["REMOVE_POST", "BAN_USER", "KEEP", "DISMISS"]
    # Ghi chú bắt buộc; được lưu vào nhật ký. Với REMOVE_POST, nội dung này còn gửi cho chủ bài làm lý do gỡ
    note: str = Field(min_length=3, max_length=500)
    # Chỉ cần khi action = BAN_USER và báo cáo nhắm vào một cuộc hội thoại (admin chọn khóa ai trong hai người)
    user_id: UUID | None = None


class ResolveResponse(BaseModel):
    report_id: str
    code: str
    status: str
    resolution_action: str
    affected_reports: int
    banned_user_id: str | None = None
    login_blocked: bool | None = None


PostStatus = Literal["ACTIVE", "ARCHIVED", "DELETED", "REMOVED", "EXPIRED", "RESOLVED"]


class AdminPost(BaseModel):
    id: UUID
    type: Literal["LOST", "FOUND"]
    title: str
    status: PostStatus
    author_id: UUID
    author_username: str
    category_name: str
    created_at: datetime
    expires_at: datetime
    report_count: int


class AdminPostsResponse(BaseModel):
    total: int
    items: list[AdminPost]


class AdminPostDetail(AdminPost):
    description: str | None
    location_text: str | None
    event_time: datetime | None
    tags: list[str]
    image_paths: list[str]


class AdminUser(BaseModel):
    id: UUID
    username: str
    display_name: str | None
    role: Literal["USER", "ADMIN"]
    is_banned: bool
    created_at: datetime
    joined_month: str
    phone_masked: str | None
    post_count: int
    report_count: int


class AdminUsersResponse(BaseModel):
    total: int
    items: list[AdminUser]


class AdminUserDetail(AdminUser):
    avatar_url: str | None
    bio: str | None


class AdminAction(BaseModel):
    id: int
    admin_id: UUID
    admin_username: str
    action: str
    target_type: Literal["POST", "USER", "REPORT", "CONVERSATION"]
    target_id: UUID
    target_label: str
    note: str | None
    created_at: datetime
    report_id: UUID | None
    report_code: str | None


class AdminActionsResponse(BaseModel):
    total: int
    items: list[AdminAction]


class PostCounts(BaseModel):
    LOST: int
    FOUND: int


class DailyPostCounts(PostCounts):
    date: Date


class UserCounts(BaseModel):
    total: int
    new_this_week: int


class RecentReport(BaseModel):
    id: UUID
    code: str
    reason: str
    status: ReportStatus
    created_at: datetime
    target_type: ReportTargetType
    target_label: str


class AdminStatsResponse(BaseModel):
    pending_reports: int
    pending_delta: int
    active_posts: PostCounts
    users: UserCounts
    completed_this_week: int
    completion_rate: float
    posts_by_day: list[DailyPostCounts]
    recent_reports: list[RecentReport]


class AdminMessage(BaseModel):
    id: int
    sender_id: UUID | None
    type: Literal["TEXT", "IMAGE", "SYSTEM"]
    content: str | None
    image_path: str | None
    created_at: datetime
    read_at: datetime | None


class AdminMessagesResponse(BaseModel):
    conversation_id: UUID
    messages: list[AdminMessage]


class ModeratedPostResponse(BaseModel):
    id: UUID
    status: PostStatus


class ModeratedUserResponse(BaseModel):
    id: UUID
    is_banned: bool
    login_blocked: bool


class LockedConversationResponse(BaseModel):
    id: UUID
    is_locked: bool


class AdminImageResponse(BaseModel):
    url: str
    expires_in: int
