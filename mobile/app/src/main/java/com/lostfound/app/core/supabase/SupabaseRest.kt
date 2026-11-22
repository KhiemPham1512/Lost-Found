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
 * Ngoại lệ: hồ sơ được phép sửa, tin nhắn/đánh dấu đã đọc và upload ảnh.
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

    /** Chèn một hàng, không tự phát lại khi mất phản hồi; cần kiểm tra dữ liệu trước khi thử lại. */
    suspend fun insert(table: String, values: JSONObject): JSONObject {
        val result = Http.execute(
            authed(restUrl(table, emptyList())).header("Prefer", "return=representation")
                .post(Http.oneShotJsonBody(values)).build()
        ).orThrow()
        return JSONArray(result.body).getJSONObject(0)
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

    /** Ảnh hội thoại chỉ qua bucket private, không phát lại upload hoặc ghi đè object. */
    suspend fun uploadChatImage(path: String, jpeg: ByteArray) {
        val content = jpeg.toRequestBody("image/jpeg".toMediaType())
        val once = object : okhttp3.RequestBody() {
            override fun isOneShot() = true
            override fun contentType() = content.contentType()
            override fun contentLength() = content.contentLength()
            override fun writeTo(sink: okio.BufferedSink) = content.writeTo(sink)
        }
        Http.execute(authed("${AppConfig.supabaseUrl}/storage/v1/object/chat-images/$path")
            .header("x-upsert", "false").header("Cache-Control", "max-age=0")
            .post(once).build()).orThrow()
    }

    suspend fun chatImageRequest(path: String): Request =
        authed("${AppConfig.supabaseUrl}/storage/v1/object/authenticated/chat-images/$path")
            .header("Cache-Control", "no-store").get().build()

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
