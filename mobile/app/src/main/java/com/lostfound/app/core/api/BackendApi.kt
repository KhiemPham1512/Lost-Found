package com.lostfound.app.core.api

import com.lostfound.app.core.AppConfig
import com.lostfound.app.core.network.Http
import com.lostfound.app.core.network.Http.orThrow
import com.lostfound.app.core.session.SessionStore
import okhttp3.Request
import org.json.JSONObject

/**
 * Gọi FastAPI (hợp đồng ở docs/api.md). Mọi lời gọi có token gắn header
 * Authorization: Bearer <access token của Supabase>.
 */
object BackendApi {

    private suspend fun send(
        method: String,
        path: String,
        body: JSONObject? = null,
        authenticated: Boolean = true,
        oneShot: Boolean = false
    ): JSONObject {
        require(path.startsWith("/")) { "API path không hợp lệ" }
        val builder = Request.Builder()
            .url("${AppConfig.apiBaseUrl}$path")
            .header("Accept", "application/json")
        if (authenticated) {
            builder.header("Authorization", "Bearer ${SessionStore.requireToken()}")
        }
        val requestBody = body?.let { json ->
            if (oneShot) Http.oneShotJsonBody(json) else Http.jsonBody(json)
        }
        builder.method(method, requestBody ?: if (method == "GET") null else Http.jsonBody(JSONObject()))
        val result = Http.execute(builder.build()).orThrow()
        return if (result.body.isBlank()) JSONObject() else JSONObject(result.body)
    }

    /** POST /auth/register — không cần token. */
    suspend fun register(email: String, password: String, displayName: String?): JSONObject {
        val body = JSONObject()
            .put("email", email.trim())
            .put("password", password)
        if (!displayName.isNullOrBlank()) body.put("display_name", displayName.trim())
        return send("POST", "/auth/register", body, authenticated = false)
    }

    /** PUT /me/phone */
    suspend fun savePhone(phone: String) {
        send("PUT", "/me/phone", JSONObject().put("phone", phone.trim()))
    }

    /** POST /posts → {id, status, expires_at} */
    suspend fun createPost(body: JSONObject): JSONObject = send("POST", "/posts", body)

    /** GET /posts/{id}/claims — danh sách dành riêng cho chủ bài. */
    suspend fun readClaims(postId: String, limit: Int, offset: Int): JSONObject =
        send("GET", "/posts/$postId/claims?limit=$limit&offset=$offset")

    suspend fun decideClaim(claimId: String, decision: String): JSONObject =
        send("POST", "/claims/$claimId/decision", JSONObject().put("decision", decision), oneShot = true)

    /** Server xác định bên xác nhận từ token, không nhận vai trò hay trạng thái từ client. */
    suspend fun confirmClaim(claimId: String): JSONObject =
        send("POST", "/claims/$claimId/confirm", JSONObject(), oneShot = true)

    /** PATCH /posts/{id}/status với ARCHIVED, ACTIVE hoặc DELETED. */
    suspend fun changePostStatus(postId: String, status: String): JSONObject =
        send("PATCH", "/posts/$postId/status", JSONObject().put("status", status))

    /** POST /reports → {report_id, code, status} */
    suspend fun createReport(
        targetType: String,
        targetId: String,
        reason: String,
        description: String?
    ): JSONObject {
        val body = JSONObject()
            .put("target_type", targetType)
            .put("target_id", targetId)
            .put("reason", reason)
        if (!description.isNullOrBlank()) body.put("description", description.trim())
        return send("POST", "/reports", body)
    }
}
