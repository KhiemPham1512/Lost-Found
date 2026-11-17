package com.lostfound.app.data.model

/** Danh mục đồ vật (bảng categories). */
data class Category(
    val id: Int,
    val code: String,
    val name: String
)

/** Một bài đăng kèm ảnh, tag, người đăng (ghép từ nhiều bảng). */
data class Post(
    val id: String,
    val authorId: String,
    val type: String,              // LOST | FOUND
    val title: String,
    val description: String?,
    val categoryId: Int,
    val locationText: String,
    val latitude: Double?,
    val longitude: Double?,
    val eventTime: String,
    val status: String,            // ACTIVE | RESOLVED | EXPIRED | ARCHIVED | REMOVED | DELETED
    val createdAt: String,
    val expiresAt: String?,
    val resolvedAt: String?,
    val removedReason: String?,
    val imagePaths: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val authorName: String? = null,
    val authorUsername: String? = null,
    val authorAvatarUrl: String? = null,
    val categoryName: String? = null
) {
    val isFound: Boolean get() = type == "FOUND"
    val coverPath: String? get() = imagePaths.firstOrNull()
}

/** Hồ sơ công khai (bảng profiles). Không có email, không có số điện thoại. */
data class Profile(
    val id: String,
    val username: String,
    val displayName: String?,
    val avatarUrl: String?,
    val bio: String?,
    val role: String,
    val isBanned: Boolean,
    val createdAt: String?
) {
    val shownName: String get() = displayName?.takeIf { it.isNotBlank() } ?: username
}

/** Ba con số ở khối "Cần xử lý" trên Home (hàm my_todo_counts). */
data class TodoCounts(
    val pendingVerifications: Int = 0,
    val unreadMessages: Int = 0,
    val awaitingConfirmation: Int = 0
)

enum class ReportReason(val code: String, val label: String) {
    SPAM("SPAM", "Spam"),
    SCAM("SCAM", "Lừa đảo"),
    FALSE_INFO("FALSE_INFO", "Thông tin sai"),
    MONEY_REQUEST("MONEY_REQUEST", "Đòi tiền"),
    VIOLATION("VIOLATION", "Vi phạm"),
    OTHER("OTHER", "Khác");

    companion object {
        fun fromCode(code: String?) = entries.firstOrNull { it.code == code }
    }
}

/** Đối tượng bị báo cáo, đúng giá trị target_type của POST /reports. */
enum class ReportTarget(val code: String, val label: String) {
    POST("POST", "bài đăng"),
    USER("USER", "tài khoản"),
    CONVERSATION("CONVERSATION", "cuộc trò chuyện");

    companion object {
        fun fromCode(code: String?) = entries.firstOrNull { it.code == code }
    }
}

/** Một báo cáo mình đã gửi (màn "Báo cáo đã gửi"). */
data class SentReport(
    val id: String,
    val code: String,              // R-1042
    val reason: ReportReason?,
    val description: String?,
    val status: String,            // PENDING | REVIEWING | RESOLVED | DISMISSED
    val resolutionAction: String?, // REMOVE_POST | BAN_USER | KEEP | DISMISS
    val createdAt: String,
    val target: ReportTarget?,
    val targetTitle: String?,
    val targetPostId: String? = null
) {
    val statusLabel: String
        get() = when (status) {
            "PENDING" -> "Chờ xử lý"
            "REVIEWING" -> "Đang xem xét"
            "RESOLVED" -> "Đã xử lý"
            "DISMISSED" -> "Không vi phạm"
            else -> status
        }

    val resultLabel: String?
        get() = when (resolutionAction) {
            "REMOVE_POST" -> "Đã gỡ bài"
            "BAN_USER" -> "Đã khóa tài khoản"
            "KEEP" -> "Giữ nguyên"
            "DISMISS" -> "Không vi phạm"
            else -> null
        }
}
