package com.lostfound.app.core.supabase

import com.lostfound.app.core.AppConfig
import com.lostfound.app.core.network.Http
import com.lostfound.app.core.network.Http.orThrow
import com.lostfound.app.core.session.Session
import okhttp3.Request
import org.json.JSONObject

/**
 * Gọi Supabase Auth (GoTrue) qua REST. Chỉ dùng publishable key.
 *
 * Đăng ký KHÔNG làm ở đây: tài khoản chỉ được tạo qua FastAPI POST /auth/register.
 */
object SupabaseAuth {

    private fun url(path: String) = "${AppConfig.supabaseUrl}/auth/v1$path"

    private fun request(path: String, accessToken: String? = null) =
        Request.Builder()
            .url(url(path))
            .header("apikey", AppConfig.supabaseKey)
            .header("Accept", "application/json")
            .apply { if (accessToken != null) header("Authorization", "Bearer $accessToken") }

    /** Đăng nhập bằng email + mật khẩu. */
    suspend fun signIn(email: String, password: String): Session {
        AppConfig.requireConfigured()
        val body = JSONObject()
            .put("email", email.trim())
            .put("password", password)
        val result = Http.execute(
            request("/token?grant_type=password").post(Http.jsonBody(body)).build()
        ).orThrow()
        return parseSession(JSONObject(result.body))
    }

    /** Đổi refresh token lấy access token mới. */
    suspend fun refresh(refreshToken: String): Session {
        val body = JSONObject().put("refresh_token", refreshToken)
        val result = Http.execute(
            request("/token?grant_type=refresh_token").post(Http.jsonBody(body)).build()
        ).orThrow()
        return parseSession(JSONObject(result.body))
    }

    /** Thu hồi phiên trên server. Lỗi mạng được bỏ qua vì phiên trên máy vẫn bị xóa. */
    suspend fun signOut(accessToken: String) {
        try {
            Http.execute(
                request("/logout", accessToken).post(Http.jsonBody(JSONObject())).build()
            )
        } catch (_: Exception) {
        }
    }

    /**
     * Quên mật khẩu, bước 1: Supabase gửi email chứa mã.
     * Mẫu email "Reset Password" trong Supabase phải có {{ .Token }} để người dùng nhận mã số.
     */
    suspend fun sendRecoveryCode(email: String) {
        AppConfig.requireConfigured()
        val body = JSONObject().put("email", email.trim())
        Http.execute(request("/recover").post(Http.jsonBody(body)).build()).orThrow()
    }

    /** Quên mật khẩu, bước 2: đổi mã trong email lấy một phiên đăng nhập tạm. */
    suspend fun verifyRecoveryCode(email: String, code: String): Session {
        val body = JSONObject()
            .put("type", "recovery")
            .put("email", email.trim())
            .put("token", code.trim())
        val result = Http.execute(request("/verify").post(Http.jsonBody(body)).build()).orThrow()
        return parseSession(JSONObject(result.body))
    }

    /** Đặt mật khẩu mới cho người đang đăng nhập (dùng cho cả quên và đổi mật khẩu). */
    suspend fun updatePassword(accessToken: String, newPassword: String) {
        val body = JSONObject().put("password", newPassword)
        Http.execute(request("/user", accessToken).put(Http.jsonBody(body)).build()).orThrow()
    }

    private fun parseSession(json: JSONObject): Session {
        val user = json.getJSONObject("user")
        val now = System.currentTimeMillis() / 1000
        val expiresAt = json.optLong("expires_at", 0L).takeIf { it > 0 }
            ?: (now + json.optLong("expires_in", 3600L))
        return Session(
            accessToken = json.getString("access_token"),
            refreshToken = json.getString("refresh_token"),
            userId = user.getString("id"),
            email = user.optString("email").takeIf { it.isNotBlank() },
            expiresAt = expiresAt
        )
    }
}
