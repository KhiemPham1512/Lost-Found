package com.lostfound.app.ui.navigation

import android.net.Uri
import com.lostfound.app.data.model.ReportReason
import com.lostfound.app.data.model.ReportTarget

/**
 * Tên route của toàn app. CHỈ B sửa file này và AppNavHost (quy tắc vàng của kế hoạch).
 * C muốn thêm màn hình thì gửi tên route cho B.
 */
object Routes {
    // Khởi động & tài khoản
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"

    // 4 tab của thanh điều hướng dưới (nút + ở giữa mở CREATE_POST)
    const val HOME = "home"
    const val MY_POSTS = "my_posts"
    const val MESSAGES = "messages"
    const val PROFILE = "profile"

    // Màn của B
    const val SEARCH = "search"
    const val CREATE_POST = "create_post"
    const val EDIT_PROFILE = "edit_profile"
    const val CHANGE_PASSWORD = "change_password"
    const val SENT_REPORTS = "sent_reports"
    const val REPORT = "report/{target}/{targetId}?reason={reason}&title={title}"

    // Màn của C (B chỉ gắn vào điều hướng)
    const val DETAIL = "detail/{postId}"
    const val VERIFY = "verify/{postId}"
    const val CLAIM_REVIEW = "claim_review/{postId}"
    const val CHAT = "chat"
    const val HANDOVER = "handover"
    const val NOTIFICATIONS = "notifications"

    val AUTH_ROUTES = setOf(SPLASH, LOGIN, REGISTER, FORGOT_PASSWORD)
    val TAB_ROUTES = setOf(HOME, MY_POSTS, MESSAGES, PROFILE)

    fun detail(postId: String) = "detail/$postId"
    fun verify(postId: String) = "verify/$postId"
    fun claimReview(postId: String) = "claim_review/$postId"

    fun report(
        target: ReportTarget,
        targetId: String,
        reason: ReportReason? = null,
        title: String? = null
    ): String {
        val base = "report/${target.code}/$targetId"
        val query = buildList {
            if (reason != null) add("reason=${reason.code}")
            if (!title.isNullOrBlank()) add("title=${Uri.encode(title)}")
        }
        return if (query.isEmpty()) base else base + "?" + query.joinToString("&")
    }
}
