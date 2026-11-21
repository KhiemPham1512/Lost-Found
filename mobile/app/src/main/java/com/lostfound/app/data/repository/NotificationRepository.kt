package com.lostfound.app.data.repository

import com.lostfound.app.core.network.mapObjects
import com.lostfound.app.core.network.str
import com.lostfound.app.core.session.SessionStore
import com.lostfound.app.core.supabase.SupabaseRest
import org.json.JSONObject
import java.time.Instant

data class NotificationCursor(val createdAt: String, val id: String)

sealed interface NotificationDestination {
    data class Post(val id: String) : NotificationDestination
    data class Claims(val postId: String) : NotificationDestination
    data class Chat(val id: String) : NotificationDestination
    data class Handover(val id: String) : NotificationDestination
    data object Reports : NotificationDestination
}

data class AppNotification(
    val id: String,
    val type: String,
    val title: String,
    val body: String?,
    val postId: String?,
    val claimId: String?,
    val readAt: String?,
    val createdAt: String
) {
    val cursor: NotificationCursor get() = NotificationCursor(createdAt, id)
}

object NotificationRepository {
    const val PAGE_SIZE = 20

    suspend fun markRead(id: String) {
        SupabaseRest.update("notifications", listOf(
            "user_id" to "eq.${SessionStore.requireUserId()}", "id" to "eq.$id", "read_at" to "is.null"
        ), JSONObject().put("read_at", Instant.now().toString()))
    }

    suspend fun destination(notification: AppNotification): NotificationDestination? = when (notification.type) {
        "CLAIM_RECEIVED" -> notification.postId?.let { NotificationDestination.Claims(it) }
        "CLAIM_PASSED", "HANDOVER_WAITING", "POST_DONE" -> {
            if (notification.claimId == null) notification.postId?.let { NotificationDestination.Post(it) }
            else {
                val id = SupabaseRest.select("conversations", listOf("select" to "id",
                    "claim_id" to "eq.${notification.claimId}", "limit" to "1"))
                    .mapObjects { it.getString("id") }.singleOrNull()
                    ?: error("Không còn xem được hội thoại của thông báo này")
                if (notification.type == "CLAIM_PASSED") NotificationDestination.Chat(id) else NotificationDestination.Handover(id)
            }
        }
        "REPORT_UPDATED" -> NotificationDestination.Reports
        "CLAIM_FAILED", "POST_REMOVED", "POST_EXPIRED", "SYSTEM" -> notification.postId?.let { NotificationDestination.Post(it) }
        else -> null
    }

    suspend fun markAll(through: NotificationCursor) {
        SupabaseRest.update("notifications", listOf(
            "user_id" to "eq.${SessionStore.requireUserId()}", "read_at" to "is.null",
            "or" to "(created_at.lt.${through.createdAt},and(created_at.eq.${through.createdAt},id.lte.${through.id}))"
        ), JSONObject().put("read_at", Instant.now().toString()))
    }

    suspend fun list(before: NotificationCursor? = null): List<AppNotification> {
        val uid = SessionStore.requireUserId()
        return SupabaseRest.select("notifications", buildList {
            add("select" to "id,user_id,type,title,body,post_id,claim_id,read_at,created_at")
            add("user_id" to "eq.$uid")
            add("order" to "created_at.desc,id.desc")
            add("limit" to "$PAGE_SIZE")
            if (before != null) add("or" to "(created_at.lt.${before.createdAt},and(created_at.eq.${before.createdAt},id.lt.${before.id}))")
        }).mapObjects { row ->
            check(row.getString("user_id") == uid) { "Thông báo không thuộc tài khoản hiện tại" }
            AppNotification(row.getString("id"), row.getString("type"), row.getString("title"),
                row.str("body"), row.str("post_id"), row.str("claim_id"), row.str("read_at"), row.getString("created_at"))
        }
    }
}
