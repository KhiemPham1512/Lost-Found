package com.lostfound.app.data.remote

import com.lostfound.app.data.auth.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

data class ApiResponse(
    val statusCode: Int,
    val body: String
) {
    val isSuccessful: Boolean
        get() = statusCode in 200..299
}

class ApiClient {

    companion object {
        private const val BASE_URL =
            "https://lost-found-api-ph8v.onrender.com"
    }

    suspend fun get(path: String): Result<ApiResponse> {
        return request(
            path = path,
            method = "GET"
        )
    }

    suspend fun post(
        path: String,
        jsonBody: String
    ): Result<ApiResponse> {
        return request(
            path = path,
            method = "POST",
            jsonBody = jsonBody
        )
    }

    private suspend fun request(
        path: String,
        method: String,
        jsonBody: String? = null
    ): Result<ApiResponse> = withContext(Dispatchers.IO) {

        var connection: HttpURLConnection? = null

        try {
            val token = SessionManager.getAccessToken()
                ?: throw Exception("Bạn chưa đăng nhập")

            require(path.startsWith("/") && !path.startsWith("//")) {
                "API path không hợp lệ"
            }

            val endpoint = "$BASE_URL$path"

            connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 20000
                readTimeout = 30000

                setRequestProperty("Accept", "application/json")
                setRequestProperty(
                    "Authorization",
                    "Bearer $token"
                )

                if (jsonBody != null) {
                    doOutput = true
                    setRequestProperty(
                        "Content-Type",
                        "application/json"
                    )
                }
            }

            if (jsonBody != null) {
                connection.outputStream.use { output ->
                    output.write(jsonBody.toByteArray(Charsets.UTF_8))
                }
            }

            val statusCode = connection.responseCode

            val responseBody = (
                    if (statusCode in 200..299) {
                        connection.inputStream
                    } else {
                        connection.errorStream
                    }
                    )?.bufferedReader()?.use { it.readText() }.orEmpty()

            Result.success(
                ApiResponse(
                    statusCode = statusCode,
                    body = responseBody
                )
            )

        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }
}
