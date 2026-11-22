package com.lostfound.app.data.repository

import com.lostfound.app.core.network.mapObjects
import com.lostfound.app.core.network.str
import com.lostfound.app.core.session.SessionStore
import com.lostfound.app.core.supabase.SupabaseRest
import org.json.JSONObject
import com.lostfound.app.data.remote.ClaimAnswer
import com.lostfound.app.data.remote.ClaimCreatedResponse
import com.lostfound.app.core.api.BackendApi
import org.json.JSONArray

data class OwnClaim(val id: String, val postId: String, val status: String, val postTitle: String?) {
    // Đây chỉ là quyền hỏi lại API; API vẫn quyết định bài/lượt có cho gửi yêu cầu mới không.
    val canCheckAttempts: Boolean get() = status in setOf("FAILED", "CANCELLED", "CLOSED")
}

object OwnClaimRepository {
    suspend fun conversationId(claim: OwnClaim): String {
        require(claim.status in setOf("PASSED", "COMPLETED")) { "Yêu cầu chưa được duyệt" }
        val uid = SessionStore.requireUserId()
        return SupabaseRest.select("conversations", listOf(
            "select" to "id,claim_id,claimant_id", "claim_id" to "eq.${claim.id}",
            "claimant_id" to "eq.$uid", "limit" to "1"
        )).mapObjects {
            check(SessionStore.userId == uid) { "Phiên đăng nhập đã thay đổi" }
            require(it.getString("claim_id") == claim.id && it.getString("claimant_id") == uid)
            it.getString("id")
        }.singleOrNull() ?: error("Chưa xem được hội thoại. Hãy tải lại trạng thái rồi thử lại.")
    }
    suspend fun submit(postId: String, answers: List<ClaimAnswer>): ClaimCreatedResponse {
        val body = JSONArray(answers.map { JSONObject().put("question_id", it.questionId).put("answer_text", it.answerText.trim()) })
        val result = BackendApi.submitClaim(postId, body)
        check(result.getString("status") == "SUBMITTED") { "Chưa xác nhận được yêu cầu đã gửi" }
        return ClaimCreatedResponse(result.getString("claim_id"), result.getString("status"))
    }
    private const val SELECT = "id,post_id,claimant_id,status,posts(title)"

    suspend fun latest(postId: String): OwnClaim? = SupabaseRest.select("claims", listOf(
        "select" to SELECT, "post_id" to "eq.$postId", "claimant_id" to "eq.${SessionStore.requireUserId()}",
        "order" to "created_at.desc,id.desc", "limit" to "1"
    )).mapObjects { parse(it, postId) }.singleOrNull()

    private fun parse(row: JSONObject, postId: String): OwnClaim {
        require(row.getString("claimant_id") == SessionStore.requireUserId() && row.getString("post_id") == postId)
        return OwnClaim(row.getString("id"), postId, row.getString("status"), row.optJSONObject("posts")?.str("title"))
    }
}
