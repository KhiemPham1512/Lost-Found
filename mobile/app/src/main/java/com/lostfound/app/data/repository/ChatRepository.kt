package com.lostfound.app.data.repository

import com.lostfound.app.core.network.mapObjects
import com.lostfound.app.core.network.str
import com.lostfound.app.core.session.SessionStore
import com.lostfound.app.core.supabase.SupabaseRest
import org.json.JSONObject
import java.time.Instant
import java.util.UUID

data class Conversation(
    val id: String,
    val claimId: String,
    val peerName: String,
    val peerUsername: String?,
    val peerAvatar: String?,
    val postId: String?,
    val postTitle: String?,
    val locked: Boolean,
    val lastMessageAt: String?
)

data class ChatMessage(
    val id: Long,
    val senderId: String?,
    val type: String,
    val content: String?,
    val createdAt: String,
    val readAt: String?,
    val imagePath: String? = null
)

data class MessageHistory(
    val items: List<ChatMessage> = emptyList(),
    val hasOlder: Boolean = false,
    val newlyLoaded: List<ChatMessage> = emptyList()
)

object ChatRepository {
    const val PAGE_SIZE = 20
    private const val SELECT = "id,claim_id,post_author_id,claimant_id,is_locked,last_message_at,created_at," +
        "claims(post_id,posts(title))," +
        "author:profiles!conversations_post_author_id_fkey(username,display_name,avatar_url)," +
        "claimant:profiles!conversations_claimant_id_fkey(username,display_name,avatar_url)"

    suspend fun conversations(offset: Int = 0): List<Conversation> = SupabaseRest.select(
        "conversations", listOf("select" to SELECT,
            "order" to "last_message_at.desc.nullslast,id.desc", "limit" to "$PAGE_SIZE", "offset" to "$offset")
    ).mapObjects { conversation(it) }

    suspend fun get(id: String): Conversation = SupabaseRest.select(
        "conversations", listOf("select" to SELECT, "id" to "eq.$id", "limit" to "1")
    ).mapObjects { conversation(it) }.singleOrNull() ?: error("Không xem được hội thoại này")

    suspend fun messages(id: String, before: Long? = null): List<ChatMessage> = SupabaseRest.select(
        "messages", buildList {
            add("select" to "id,sender_id,type,content,created_at,read_at,image_path")
            add("conversation_id" to "eq.$id")
            add("order" to "id.desc")
            add("limit" to "50")
            if (before != null) add("id" to "lt.$before")
        }
    ).mapObjects { message(it) }

    suspend fun history(id: String, current: MessageHistory = MessageHistory(), before: Long? = null): MessageHistory {
        val page = messages(id, before)
        val overlaps = page.any { fresh -> current.items.any { it.id == fresh.id } }
        // Không gộp hai cửa sổ rời nhau: con trỏ phải còn đi qua được đoạn tin ở giữa.
        if (before == null && !overlaps) return MessageHistory(page, page.size == 50, page)
        val items = (page + current.items).distinctBy { it.id }.sortedByDescending { it.id }
        val older = if (before != null || page.size < 50) page.size == 50 else current.hasOlder
        return MessageHistory(items, older, page)
    }

    suspend fun send(id: String, text: String): ChatMessage {
        // shortcut: chưa có idempotency; sau lỗi không rõ kết quả phải đọc lịch sử trước khi gửi lại.
        val content = text.trim()
        require(content.codePointCount(0, content.length) in 1..2000) { "Tin nhắn cần có 1–2000 ký tự" }
        val sender = SessionStore.requireUserId()
        check(!get(id).locked) { "Hội thoại đã khóa, chỉ được đọc" }
        return message(SupabaseRest.insert("messages", JSONObject()
            .put("conversation_id", id).put("sender_id", sender).put("type", "TEXT").put("content", content)))
    }

    suspend fun markRead(id: String, loaded: List<ChatMessage>) {
        val uid = SessionStore.requireUserId()
        val ids = loaded.filter { it.senderId != uid && it.readAt == null }.map { it.id.toString() }
        if (ids.isEmpty()) return
        SupabaseRest.update("messages", listOf(
            "conversation_id" to "eq.$id", "id" to SupabaseRest.inList(ids), "read_at" to "is.null",
            "or" to "(sender_id.is.null,sender_id.neq.$uid)"
        ), JSONObject().put("read_at", Instant.now().toString()))
    }

    suspend fun sendImage(id: String, jpeg: ByteArray): ChatMessage {
        require(jpeg.size in 3..5_242_880 && jpeg[0] == 0xff.toByte() &&
            jpeg[1] == 0xd8.toByte() && jpeg[2] == 0xff.toByte()) { "Ảnh JPEG phải nhỏ hơn hoặc bằng 5 MiB" }
        val sender = SessionStore.requireUserId()
        check(!get(id).locked) { "Hội thoại đã khóa, chỉ được đọc" }
        val path = "$id/$sender/${UUID.randomUUID()}.jpg"
        check(SessionStore.requireUserId() == sender) { "Phiên đăng nhập đã thay đổi" }
        SupabaseRest.uploadChatImage(path, jpeg)
        check(SessionStore.requireUserId() == sender) { "Phiên đăng nhập đã thay đổi" }
        // shortcut: bucket không cho DELETE; giữ upload mồ côi và ảnh có thể đã gửi khi mất phản hồi, cần cleanup phía server sau.
        return message(SupabaseRest.insert("messages", JSONObject()
            .put("conversation_id", id).put("sender_id", sender).put("type", "IMAGE").put("image_path", path)))
    }

    suspend fun imageRequest(id: String, image: ChatMessage): okhttp3.Request {
        val path = image.imagePath.orEmpty()
        val parts = path.split('/')
        require(image.type == "IMAGE" && image.senderId != null && path.length <= 200 && parts.size == 3 &&
            parts[0] == id && parts[1] == image.senderId &&
            Regex("[A-Za-z0-9_-]{1,80}\\.(jpg|jpeg|png|webp)").matches(parts[2]) &&
            UUID.fromString(id).toString() == id && UUID.fromString(image.senderId).toString() == image.senderId
        ) { "Đường dẫn ảnh hội thoại không hợp lệ" }
        return SupabaseRest.chatImageRequest(path)
    }

    private fun message(row: JSONObject) = ChatMessage(
        row.getLong("id"), row.str("sender_id"), row.getString("type"), row.str("content"),
        row.getString("created_at"), row.str("read_at"), row.str("image_path")
    )

    private fun conversation(row: JSONObject): Conversation {
        val uid = SessionStore.requireUserId()
        require(uid == row.getString("post_author_id") || uid == row.getString("claimant_id"))
        val peer = row.optJSONObject(if (uid == row.getString("post_author_id")) "claimant" else "author")
        val claim = row.optJSONObject("claims")
        return Conversation(
            row.getString("id"), row.getString("claim_id"),
            peer?.str("display_name")?.takeIf { it.isNotBlank() } ?: peer?.str("username") ?: "Người tham gia",
            peer?.str("username"), peer?.str("avatar_url"), claim?.str("post_id"),
            claim?.optJSONObject("posts")?.str("title"), row.getBoolean("is_locked"), row.str("last_message_at")
        )
    }
}
