package com.lostfound.app.core.supabase

import com.lostfound.app.core.AppConfig
import com.lostfound.app.core.network.Http
import com.lostfound.app.core.network.Http.orThrow
import com.lostfound.app.core.session.SessionStore
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * Đọc dữ liệu thẳng từ Supabase (PostgREST + Storage) bằng token của người dùng,
 * nên RLS trong database tự giới hạn những gì được thấy.
 *
 * Quy tắc của nhóm: ĐỌC đi thẳng Supabase, GHI nghiệp vụ đi qua FastAPI.
 * Ngoại lệ được phép ghi trực tiếp: sửa hồ sơ (display_name, bio, avatar_url) và upload ảnh.
 */
object SupabaseRest {

    private suspend fun authed(url: String): Request.Builder {
        AppConfig.requireConfigured()
        val token = SessionStore.requireToken()
        return Request.Builder()
            .url(url)
            .header("apikey", AppConfig.supabaseKey)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
    }

    private fun restUrl(path: String, params: List<Pair<String, String>>): String {
        val builder = "${AppConfig.supabaseUrl}/rest/v1/$path".toHttpUrl().newBuilder()
        params.forEach { (k, v) -> builder.addQueryParameter(k, v) }
        return builder.build().toString()
    }

    /** GET /rest/v1/<table>?select=...&<bộ lọc PostgREST> */
    suspend fun select(table: String, params: List<Pair<String, String>>): JSONArray {
        val result = Http.execute(authed(restUrl(table, params)).get().build()).orThrow()
        return JSONArray(result.body.ifBlank { "[]" })
    }

    /** Gọi hàm SQL (RPC). Trả về chuỗi JSON thô (mảng hoặc giá trị). */
    suspend fun rpc(function: String, args: JSONObject): String {
        val result = Http.execute(
            authed(restUrl("rpc/$function", emptyList())).post(Http.jsonBody(args)).build()
        ).orThrow()
        return result.body
    }

    /** PATCH /rest/v1/<table>?<bộ lọc> với các cột cần sửa. */
    suspend fun update(table: String, filters: List<Pair<String, String>>, values: JSONObject) {
        Http.execute(
            authed(restUrl(table, filters))
                .header("Prefer", "return=minimal")
                .patch(Http.jsonBody(values))
                .build()
        ).orThrow()
    }

    /** Upload một file lên bucket post-images. [path] phải bắt đầu bằng ID của chính người dùng. */
    suspend fun uploadImage(path: String, bytes: ByteArray, contentType: String = "image/jpeg") {
        val url = "${AppConfig.supabaseUrl}/storage/v1/object/${AppConfig.POST_IMAGES_BUCKET}/$path"
        Http.execute(
            authed(url)
                .header("x-upsert", "false")
                .post(bytes.toRequestBody(contentType.toMediaType()))
                .build()
        ).orThrow()
    }

    /** Xóa các file của chính mình (dọn ảnh đã upload khi đăng bài thất bại). Bỏ qua lỗi. */
    suspend fun removeImages(paths: List<String>) {
        if (paths.isEmpty()) return
        try {
            val url = "${AppConfig.supabaseUrl}/storage/v1/object/${AppConfig.POST_IMAGES_BUCKET}"
            val body = JSONObject().put("prefixes", JSONArray(paths))
            Http.execute(authed(url).delete(Http.jsonBody(body)).build())
        } catch (_: Exception) {
        }
    }

    /** Tạo giá trị cho bộ lọc in.(...) của PostgREST, có đặt chuỗi trong ngoặc kép. */
    fun inList(values: Collection<String>): String =
        values.joinToString(prefix = "in.(", postfix = ")", separator = ",") {
            "\"" + it.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
        }
}
