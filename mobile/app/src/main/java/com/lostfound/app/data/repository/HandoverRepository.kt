package com.lostfound.app.data.repository

import com.lostfound.app.core.api.BackendApi
import com.lostfound.app.core.network.mapObjects
import com.lostfound.app.core.network.str
import com.lostfound.app.core.session.SessionStore
import com.lostfound.app.core.supabase.SupabaseRest

data class HandoverState(
    val conversation: Conversation,
    val status: String,
    val authorConfirmedAt: String?,
    val claimantConfirmedAt: String?,
    val isAuthor: Boolean
) {
    val ownConfirmedAt: String? get() = if (isAuthor) authorConfirmedAt else claimantConfirmedAt
    val peerConfirmedAt: String? get() = if (isAuthor) claimantConfirmedAt else authorConfirmedAt
}

data class HandoverConfirmation(val status: String, val authorConfirmedAt: String?, val claimantConfirmedAt: String?)

object HandoverRepository {
    suspend fun confirm(claimId: String): HandoverConfirmation {
        val json = BackendApi.confirmClaim(claimId)
        check(json.getString("claim_id") == claimId) { "Phản hồi không khớp yêu cầu trao trả" }
        return HandoverConfirmation(json.getString("status"), json.str("author_confirmed_at"), json.str("claimant_confirmed_at"))
    }

    suspend fun load(conversationId: String): HandoverState {
        val chat = ChatRepository.get(conversationId)
        val row = SupabaseRest.select("claims", listOf(
            "select" to "id,status,post_author_id,claimant_id,author_confirmed_at,claimant_confirmed_at",
            "id" to "eq.${chat.claimId}", "limit" to "1"
        )).mapObjects { it }.singleOrNull() ?: error("Không xem được yêu cầu trao trả này")
        val uid = SessionStore.requireUserId()
        require(uid == row.getString("post_author_id") || uid == row.getString("claimant_id"))
        return HandoverState(chat, row.getString("status"), row.str("author_confirmed_at"),
            row.str("claimant_confirmed_at"), uid == row.getString("post_author_id"))
    }
}
