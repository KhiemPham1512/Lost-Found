package com.lostfound.app.data.repository

import com.lostfound.app.core.api.BackendApi
import com.lostfound.app.core.network.mapObjects
import com.lostfound.app.core.network.str

data class ReviewAnswer(val question: String, val answer: String)

data class ReviewClaim(
    val id: String,
    val username: String,
    val displayName: String?,
    val avatarUrl: String?,
    val createdAt: String,
    val status: String,
    val answers: List<ReviewAnswer>
)

data class ClaimPage(val total: Int, val items: List<ReviewClaim>)

object ClaimRepository {
    const val PAGE_SIZE = 20

    suspend fun decide(claimId: String, decision: String) {
        require(decision == "PASS" || decision == "FAIL")
        BackendApi.decideClaim(claimId, decision)
    }

    suspend fun list(postId: String, offset: Int = 0): ClaimPage {
        val json = BackendApi.readClaims(postId, PAGE_SIZE, offset)
        return ClaimPage(json.getInt("total"), json.getJSONArray("items").mapObjects { item ->
            val user = item.getJSONObject("claimant")
            ReviewClaim(
                id = item.getString("claim_id"),
                username = user.getString("username"),
                displayName = user.str("display_name"),
                avatarUrl = user.str("avatar_url"),
                createdAt = item.getString("created_at"),
                status = item.getString("status"),
                answers = item.getJSONArray("answers").mapObjects { it }
                    .sortedBy { it.getInt("position") }.map {
                        ReviewAnswer(it.getString("question_text"), it.getString("answer_text"))
                    }
            )
        })
    }
}
