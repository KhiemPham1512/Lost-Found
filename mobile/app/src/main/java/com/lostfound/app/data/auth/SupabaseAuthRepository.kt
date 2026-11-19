package com.lostfound.app.data.auth

import com.lostfound.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val email: String?
)

class SupabaseAuthRepository {

    suspend fun signIn(
        email: String,
        password: String
    ): Result<AuthSession> = withContext(Dispatchers.IO) {

        var connection: HttpURLConnection? = null

        try {
            require(BuildConfig.SUPABASE_URL.isNotBlank()) {
                "Chưa cấu hình Supabase URL"
            }

            require(BuildConfig.SUPABASE_PUBLISHABLE_KEY.isNotBlank()) {
                "Chưa cấu hình Supabase Publishable Key"
            }

            val endpoint =
                "${BuildConfig.SUPABASE_URL.trimEnd('/')}/auth/v1/token?grant_type=password"

            connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 15000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty(
                    "apikey",
                    BuildConfig.SUPABASE_PUBLISHABLE_KEY
                )
            }

            val requestBody = JSONObject().apply {
                put("email", email.trim())
                put("password", password)
            }.toString()

            connection.outputStream.use { output ->
                output.write(requestBody.toByteArray(Charsets.UTF_8))
            }

            val responseCode = connection.responseCode

            val responseText = (
                    if (responseCode in 200..299) {
                        connection.inputStream
                    } else {
                        connection.errorStream
                    }
                    )?.bufferedReader()?.use { it.readText() }.orEmpty()

            val json = JSONObject(responseText)

            if (responseCode !in 200..299) {
                val message = json.optString(
                    "msg",
                    json.optString(
                        "message",
                        json.optString("error_description", "Đăng nhập thất bại")
                    )
                )
                return@withContext Result.failure(Exception(message))
            }

            val user = json.getJSONObject("user")

            Result.success(
                AuthSession(
                    accessToken = json.getString("access_token"),
                    refreshToken = json.getString("refresh_token"),
                    userId = user.getString("id"),
                    email = user.optString("email").takeIf { it.isNotBlank() }
                )
            )

        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }
}
