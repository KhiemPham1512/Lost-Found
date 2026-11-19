package com.lostfound.app.core.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Lỗi đã được dịch sang câu tiếng Việt dễ hiểu để hiển thị thẳng lên màn hình. */
class ApiException(
    val status: Int,
    override val message: String
) : Exception(message)

data class HttpResult(val status: Int, val body: String) {
    val isSuccessful: Boolean get() = status in 200..299
}

object Http {
    val JSON = "application/json; charset=utf-8".toMediaType()

    /** Một OkHttpClient dùng chung cho cả app (Coil cũng có client riêng của nó). */
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        // Render gói miễn phí có thể "ngủ", lần gọi đầu mất tới ~50 giây
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun jsonBody(json: JSONObject): RequestBody = json.toString().toRequestBody(JSON)
    fun jsonBody(json: JSONArray): RequestBody = json.toString().toRequestBody(JSON)

    /** Gửi request trên luồng IO. Lỗi mạng được đổi thành ApiException(0, ...). */
    suspend fun execute(request: Request): HttpResult = withContext(Dispatchers.IO) {
        try {
            client.newCall(request).execute().use { response ->
                HttpResult(response.code, response.body?.string().orEmpty())
            }
        } catch (e: IOException) {
            // Ghi nguyên nhân thật vào Logcat (lọc theo "LostFoundHttp") để dễ tìm lỗi
            Log.w("LostFoundHttp", "Lỗi mạng khi gọi ${request.method} ${request.url}", e)
            throw ApiException(0, "Không kết nối được máy chủ. Kiểm tra mạng rồi thử lại.")
        }
    }

    /** Ném ApiException nếu status không phải 2xx. */
    fun HttpResult.orThrow(): HttpResult {
        if (!isSuccessful) throw ApiException(status, errorMessage(status, body))
        return this
    }

    /**
     * Rút câu báo lỗi từ phản hồi của FastAPI ({"detail": ...}) hoặc Supabase
     * ({"msg"}, {"message"}, {"error_description"}).
     */
    fun errorMessage(status: Int, body: String): String {
        val raw = try {
            val json = JSONObject(body)
            when (val detail = json.opt("detail")) {
                is String -> detail
                is JSONArray -> {
                    // Lỗi kiểm tra dữ liệu của FastAPI/Pydantic: [{"loc": [...], "msg": "..."}]
                    val first = detail.optJSONObject(0)
                    first?.optString("msg").orEmpty().removePrefix("Value error, ")
                }
                else -> json.optString("msg").ifBlank {
                    json.optString("message").ifBlank {
                        json.optString("error_description").ifBlank { json.optString("error") }
                    }
                }
            }
        } catch (_: Exception) {
            ""
        }
        return translate(status, raw)
    }

    private fun translate(status: Int, raw: String): String {
        val lower = raw.lowercase()
        return when {
            "invalid login credentials" in lower -> "Email hoặc mật khẩu không đúng"
            "banned" in lower -> "Tài khoản đã bị khóa"
            "email not confirmed" in lower -> "Email chưa được xác nhận"
            "token has expired" in lower || "otp_expired" in lower ||
                ("invalid" in lower && "otp" in lower) -> "Mã xác nhận sai hoặc đã hết hạn"
            "rate limit" in lower || "security purposes" in lower ->
                "Bạn thao tác quá nhanh, vui lòng thử lại sau ít phút"
            "should be different" in lower -> "Mật khẩu mới phải khác mật khẩu cũ"
            "jwt expired" in lower -> "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại"
            raw.isNotBlank() -> raw
            status == 401 -> "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại"
            status == 403 -> "Bạn không có quyền thực hiện thao tác này"
            status == 404 -> "Không tìm thấy dữ liệu"
            status == 429 -> "Bạn thao tác quá nhiều lần, vui lòng thử lại sau"
            status >= 500 -> "Máy chủ đang gặp sự cố, vui lòng thử lại sau"
            else -> "Có lỗi xảy ra (mã $status)"
        }
    }
}

// ---- Tiện ích đọc JSON an toàn (org.json trả "null" dạng chuỗi nếu dùng optString) ----

fun JSONObject.str(name: String): String? =
    if (isNull(name)) null else optString(name).takeIf { has(name) }

fun JSONObject.dbl(name: String): Double? =
    if (!has(name) || isNull(name)) null else optDouble(name).takeUnless { it.isNaN() }

fun JSONObject.lng(name: String): Long? =
    if (!has(name) || isNull(name)) null else optLong(name)

inline fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
    (0 until length()).map { transform(getJSONObject(it)) }
