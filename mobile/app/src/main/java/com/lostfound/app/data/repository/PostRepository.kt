package com.lostfound.app.data.repository

import android.content.Context
import android.net.Uri
import com.lostfound.app.core.api.BackendApi
import com.lostfound.app.core.image.ImageCompressor
import com.lostfound.app.core.network.dbl
import com.lostfound.app.core.network.mapObjects
import com.lostfound.app.core.network.str
import com.lostfound.app.core.session.SessionStore
import com.lostfound.app.core.supabase.SupabaseRest
import com.lostfound.app.data.model.Category
import com.lostfound.app.data.model.Post
import com.lostfound.app.data.model.TodoCounts
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Tham số của hàm SQL search_posts (docs/api.md mục 4.1). */
data class SearchParams(
    val type: String? = null,          // LOST | FOUND | null = Tất cả
    val categoryId: Int? = null,
    val query: String? = null,
    val postedAfter: String? = null,   // ISO có múi giờ
    val tagNames: List<String> = emptyList(),
    val sortNearest: Boolean = false,
    val lat: Double? = null,
    val lng: Double? = null,
    val limit: Int = PostRepository.PAGE_SIZE,
    val offset: Int = 0
)

/** Dữ liệu form Đăng bài, gửi cho POST /posts sau khi upload ảnh. */
data class PostDraft(
    val type: String,
    val title: String,
    val description: String?,
    val categoryId: Int,
    val locationText: String,
    val latitude: Double?,
    val longitude: Double?,
    val eventTimeIso: String,
    val tags: List<String>,
    val questions: List<String>,
    val images: List<Uri>
)

object PostRepository {
    const val PAGE_SIZE = 20

    private var categoryCache: List<Category>? = null

    // ---------------- Đọc ----------------

    suspend fun categories(): List<Category> {
        categoryCache?.let { return it }
        val rows = SupabaseRest.select(
            "categories",
            listOf("select" to "id,code,name", "order" to "sort_order.asc")
        )
        val list = rows.mapObjects {
            Category(id = it.getInt("id"), code = it.optString("code"), name = it.optString("name"))
        }
        categoryCache = list
        return list
    }

    /** Bảng tin và Tìm kiếm. Trả về một trang (tối đa [PAGE_SIZE] bài). */
    suspend fun search(params: SearchParams): List<Post> {
        val tagIds = if (params.tagNames.isEmpty()) null else {
            // Tag lọc kiểu AND: có tag không tồn tại thì chắc chắn không có bài nào khớp
            tagIdsByNames(params.tagNames) ?: return emptyList()
        }
        val args = JSONObject()
        params.type?.let { args.put("p_type", it) }
        params.categoryId?.let { args.put("p_category", it) }
        params.query?.trim()?.takeIf { it.isNotEmpty() }?.let { args.put("p_query", it) }
        params.postedAfter?.let { args.put("p_posted_after", it) }
        tagIds?.let { args.put("p_tag_ids", JSONArray(it)) }
        if (params.sortNearest && params.lat != null && params.lng != null) {
            args.put("p_sort", "NEAREST")
            args.put("p_lat", params.lat)
            args.put("p_lng", params.lng)
        } else {
            args.put("p_sort", "NEWEST")
        }
        args.put("p_limit", params.limit)
        args.put("p_offset", params.offset)

        val rows = JSONArray(SupabaseRest.rpc("search_posts", args).ifBlank { "[]" })
        return enrich(rows.mapObjects(::parsePost))
    }

    /** Một bài theo ID (RLS cho xem bài ACTIVE, bài của mình hoặc bài mình đã gửi xác minh). */
    suspend fun getPost(id: String): Post? {
        val rows = SupabaseRest.select("posts", listOf("select" to "*", "id" to "eq.$id"))
        if (rows.length() == 0) return null
        return enrich(listOf(parsePost(rows.getJSONObject(0)))).firstOrNull()
    }

    /** Bài của tôi theo nhóm trạng thái (không bao giờ thấy bài DELETED). */
    suspend fun myPosts(statuses: List<String>): List<Post> {
        val uid = SessionStore.requireUserId()
        val rows = SupabaseRest.select(
            "posts",
            listOf(
                "select" to "*",
                "author_id" to "eq.$uid",
                "status" to SupabaseRest.inList(statuses),
                "order" to "created_at.desc",
                "limit" to "100"
            )
        )
        return enrich(rows.mapObjects(::parsePost))
    }

    suspend fun todoCounts(): TodoCounts {
        val body = SupabaseRest.rpc("my_todo_counts", JSONObject()).trim()
        val row = when {
            body.startsWith("[") -> JSONArray(body).optJSONObject(0)
            body.startsWith("{") -> JSONObject(body)
            else -> null
        } ?: return TodoCounts()
        return TodoCounts(
            pendingVerifications = row.optInt("pending_verifications"),
            unreadMessages = row.optInt("unread_messages"),
            awaitingConfirmation = row.optInt("awaiting_confirmation")
        )
    }

    private suspend fun tagIdsByNames(names: List<String>): List<Int>? {
        val normalized = names.map { it.trim().lowercase().replace(Regex("\\s+"), " ") }
            .filter { it.isNotEmpty() }.distinct()
        if (normalized.isEmpty()) return emptyList()
        val rows = SupabaseRest.select(
            "tags",
            listOf("select" to "id,name", "name" to SupabaseRest.inList(normalized))
        )
        val ids = rows.mapObjects { it.getInt("id") }
        return if (ids.size < normalized.size) null else ids
    }

    private fun parsePost(o: JSONObject) = Post(
        id = o.getString("id"),
        authorId = o.optString("author_id"),
        type = o.optString("type"),
        title = o.optString("title"),
        description = o.str("description"),
        categoryId = o.optInt("category_id"),
        locationText = o.optString("location_text"),
        latitude = o.dbl("latitude"),
        longitude = o.dbl("longitude"),
        eventTime = o.optString("event_time"),
        status = o.optString("status"),
        createdAt = o.optString("created_at"),
        expiresAt = o.str("expires_at"),
        resolvedAt = o.str("resolved_at"),
        removedReason = o.str("removed_reason")
    )

    /** Ghép ảnh, tag, tên người đăng, tên danh mục cho danh sách bài (4 truy vấn chạy song song). */
    private suspend fun enrich(posts: List<Post>): List<Post> {
        if (posts.isEmpty()) return posts
        val postIds = posts.map { it.id }
        val authorIds = posts.map { it.authorId }.distinct()

        return coroutineScope {
            val imagesJob = async {
                SupabaseRest.select(
                    "post_images",
                    listOf(
                        "select" to "post_id,storage_path,sort_order",
                        "post_id" to SupabaseRest.inList(postIds),
                        "order" to "sort_order.asc"
                    )
                )
            }
            val tagsJob = async {
                SupabaseRest.select(
                    "post_tags",
                    listOf(
                        "select" to "post_id,tags(name)",
                        "post_id" to SupabaseRest.inList(postIds)
                    )
                )
            }
            val authorsJob = async {
                SupabaseRest.select(
                    "profiles",
                    listOf(
                        "select" to "id,username,display_name,avatar_url",
                        "id" to SupabaseRest.inList(authorIds)
                    )
                )
            }
            val categoriesJob = async { runCatching { categories() }.getOrDefault(emptyList()) }

            val images = imagesJob.await().mapObjects { it.optString("post_id") to it.optString("storage_path") }
                .groupBy({ it.first }, { it.second })
            val tags = tagsJob.await().mapObjects {
                it.optString("post_id") to (it.optJSONObject("tags")?.optString("name").orEmpty())
            }.filter { it.second.isNotEmpty() }.groupBy({ it.first }, { it.second })
            val authors = authorsJob.await().mapObjects { it }.associateBy { it.optString("id") }
            val categoryNames = categoriesJob.await().associate { it.id to it.name }

            posts.map { p ->
                val author = authors[p.authorId]
                p.copy(
                    imagePaths = images[p.id].orEmpty(),
                    tags = tags[p.id].orEmpty(),
                    authorName = author?.str("display_name"),
                    authorUsername = author?.str("username"),
                    authorAvatarUrl = author?.str("avatar_url"),
                    categoryName = categoryNames[p.categoryId]
                )
            }
        }
    }

    // ---------------- Ghi ----------------

    /**
     * Đăng bài: nén + upload từng ảnh vào <user_id>/<uuid>.jpg, rồi gọi POST /posts.
     * Nếu POST /posts lỗi thì xóa các ảnh vừa upload để không để rác trong Storage.
     * [onProgress] nhận chuỗi mô tả bước đang làm để hiển thị.
     */
    suspend fun createPost(
        context: Context,
        draft: PostDraft,
        onProgress: (String) -> Unit = {}
    ): String {
        val uid = SessionStore.requireUserId()
        val uploaded = mutableListOf<String>()
        try {
            draft.images.forEachIndexed { index, uri ->
                onProgress("Đang tải ảnh ${index + 1}/${draft.images.size}…")
                val bytes = ImageCompressor.compress(context, uri)
                val path = "$uid/${UUID.randomUUID()}.jpg"
                SupabaseRest.uploadImage(path, bytes)
                uploaded += path
            }
            onProgress("Đang đăng bài…")
            val body = JSONObject()
                .put("type", draft.type)
                .put("title", draft.title.trim())
                .put("category_id", draft.categoryId)
                .put("location_text", draft.locationText.trim())
                .put("event_time", draft.eventTimeIso)
                .put("tags", JSONArray(draft.tags))
                .put("image_paths", JSONArray(uploaded))
                .put("questions", JSONArray(draft.questions.map { it.trim() }))
            draft.description?.trim()?.takeIf { it.isNotEmpty() }?.let { body.put("description", it) }
            if (draft.latitude != null && draft.longitude != null) {
                // Cột numeric(9,6): làm tròn 6 chữ số thập phân
                body.put("latitude", Math.round(draft.latitude * 1_000_000) / 1_000_000.0)
                body.put("longitude", Math.round(draft.longitude * 1_000_000) / 1_000_000.0)
            }
            val created = BackendApi.createPost(body)
            return created.getString("id")
        } catch (e: Exception) {
            SupabaseRest.removeImages(uploaded)
            throw e
        }
    }

    /** Ẩn (ARCHIVED), hiện lại (ACTIVE) hoặc xóa (DELETED) bài của mình. */
    suspend fun changeStatus(postId: String, status: String) {
        BackendApi.changePostStatus(postId, status)
    }
}
