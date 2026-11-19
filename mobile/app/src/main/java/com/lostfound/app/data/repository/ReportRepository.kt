package com.lostfound.app.data.repository

import com.lostfound.app.core.api.BackendApi
import com.lostfound.app.core.network.mapObjects
import com.lostfound.app.core.network.str
import com.lostfound.app.core.session.SessionStore
import com.lostfound.app.core.supabase.SupabaseRest
import com.lostfound.app.data.model.ReportReason
import com.lostfound.app.data.model.ReportTarget
import com.lostfound.app.data.model.SentReport

object ReportRepository {

    /** POST /reports. Trả về mã dễ đọc, ví dụ "R-1042". */
    suspend fun send(
        target: ReportTarget,
        targetId: String,
        reason: ReportReason,
        description: String?
    ): String {
        val result = BackendApi.createReport(target.code, targetId, reason.code, description)
        return result.optString("code")
    }

    /** Báo cáo mình đã gửi (RLS chỉ trả báo cáo của chính mình), kèm tên đối tượng. */
    suspend fun mySentReports(): List<SentReport> {
        val uid = SessionStore.requireUserId()
        val rows = SupabaseRest.select(
            "reports",
            listOf(
                "select" to "id,report_no,reason,description,status,resolution_action,created_at," +
                    "target_post_id,target_user_id,target_conversation_id",
                "reporter_id" to "eq.$uid",
                "order" to "created_at.desc",
                "limit" to "100"
            )
        )
        val items = rows.mapObjects { it }

        // Lấy tiêu đề bài và handle người bị báo cáo để hiển thị cho dễ hiểu
        val postIds = items.mapNotNull { it.str("target_post_id") }.distinct()
        val userIds = items.mapNotNull { it.str("target_user_id") }.distinct()
        val postTitles = if (postIds.isEmpty()) emptyMap() else runCatching {
            SupabaseRest.select(
                "posts",
                listOf("select" to "id,title", "id" to SupabaseRest.inList(postIds))
            ).mapObjects { it.optString("id") to it.optString("title") }.toMap()
        }.getOrDefault(emptyMap())
        val handles = if (userIds.isEmpty()) emptyMap() else runCatching {
            SupabaseRest.select(
                "profiles",
                listOf("select" to "id,username", "id" to SupabaseRest.inList(userIds))
            ).mapObjects { it.optString("id") to "@" + it.optString("username") }.toMap()
        }.getOrDefault(emptyMap())

        return items.map { o ->
            val postId = o.str("target_post_id")
            val userId = o.str("target_user_id")
            val target = when {
                postId != null -> ReportTarget.POST
                userId != null -> ReportTarget.USER
                o.str("target_conversation_id") != null -> ReportTarget.CONVERSATION
                else -> null
            }
            SentReport(
                id = o.getString("id"),
                code = "R-" + o.optLong("report_no"),
                reason = ReportReason.fromCode(o.str("reason")),
                description = o.str("description"),
                status = o.optString("status"),
                resolutionAction = o.str("resolution_action"),
                createdAt = o.optString("created_at"),
                target = target,
                targetTitle = when (target) {
                    ReportTarget.POST -> postTitles[postId]
                    ReportTarget.USER -> handles[userId]
                    else -> null
                },
                targetPostId = postId
            )
        }
    }
}
