package com.lostfound.app.ui.feed

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lostfound.app.core.location.LatLng
import com.lostfound.app.core.location.LocationHelper
import com.lostfound.app.core.session.SessionStore
import com.lostfound.app.core.util.Formatters
import com.lostfound.app.core.util.userMessage
import com.lostfound.app.data.model.Post
import com.lostfound.app.data.model.ReportTarget
import com.lostfound.app.data.repository.PostRepository
import com.lostfound.app.data.repository.SearchParams
import com.lostfound.app.ui.components.CardAction
import com.lostfound.app.ui.components.PostCard
import com.lostfound.app.ui.theme.TextSecondary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Trạng thái một danh sách bài có phân trang (Home, Tìm kiếm). */
data class FeedState(
    val posts: List<Post> = emptyList(),
    val loading: Boolean = false,       // tải trang đầu (hiện vòng xoay giữa màn)
    val refreshing: Boolean = false,    // kéo để tải lại
    val loadingMore: Boolean = false,   // tải trang tiếp theo
    val endReached: Boolean = false,
    val error: String? = null,          // lỗi trang đầu
    val loadMoreError: String? = null,
    val loadedOnce: Boolean = false
)

/**
 * Tải search_posts theo từng trang 20 bài (offset/limit).
 * [params] được gọi mỗi lần tải để lấy bộ lọc mới nhất.
 */
class FeedLoader(
    private val scope: CoroutineScope,
    private val params: () -> SearchParams
) {
    private val _state = MutableStateFlow(FeedState())
    val state: StateFlow<FeedState> = _state.asStateFlow()
    private var job: Job? = null

    fun reload(pullToRefresh: Boolean = false) {
        job?.cancel()
        _state.update {
            it.copy(
                loading = !pullToRefresh,
                refreshing = pullToRefresh,
                error = null,
                loadMoreError = null,
                endReached = false,
                // Đổi bộ lọc thì xóa danh sách cũ; kéo tải lại thì giữ cho đỡ nháy
                posts = if (pullToRefresh) it.posts else emptyList()
            )
        }
        job = scope.launch {
            try {
                val page = PostRepository.search(params().copy(offset = 0))
                _state.update {
                    it.copy(
                        posts = page,
                        loading = false,
                        refreshing = false,
                        endReached = page.size < PostRepository.PAGE_SIZE,
                        loadedOnce = true
                    )
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(loading = false, refreshing = false, error = e.userMessage(), loadedOnce = true)
                }
            }
        }
    }

    fun loadMore() {
        val s = _state.value
        if (s.loading || s.refreshing || s.loadingMore || s.endReached || s.error != null) return
        _state.update { it.copy(loadingMore = true, loadMoreError = null) }
        job = scope.launch {
            try {
                val page = PostRepository.search(params().copy(offset = s.posts.size))
                _state.update { cur ->
                    val known = cur.posts.map { it.id }.toHashSet()
                    cur.copy(
                        posts = cur.posts + page.filter { it.id !in known },
                        loadingMore = false,
                        endReached = page.size < PostRepository.PAGE_SIZE
                    )
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loadingMore = false, loadMoreError = e.userMessage()) }
            }
        }
    }

    /** Bỏ một bài khỏi danh sách (ví dụ sau khi báo cáo). */
    fun remove(postId: String) = _state.update { s -> s.copy(posts = s.posts.filterNot { it.id == postId }) }
}

/** Các mục menu ba chấm của thẻ bài: báo cáo bài và báo cáo người đăng (không hiện với bài của mình). */
fun reportActions(
    post: Post,
    onReport: (ReportTarget, String, String?) -> Unit
): List<CardAction> {
    if (post.authorId == SessionStore.userId) return emptyList()
    return listOf(
        CardAction("Báo cáo bài đăng") { onReport(ReportTarget.POST, post.id, post.title) },
        CardAction("Báo cáo người đăng") {
            onReport(ReportTarget.USER, post.authorId, post.authorUsername?.let { "@$it" })
        }
    )
}

/**
 * Thêm danh sách thẻ bài vào một LazyColumn, kèm dòng "đang tải thêm" / "tải lại" ở cuối.
 */
fun LazyListScope.postItems(
    state: FeedState,
    myLocation: LatLng?,
    onOpenPost: (String) -> Unit,
    onReport: (ReportTarget, String, String?) -> Unit,
    onRetryMore: () -> Unit
) {
    items(state.posts, key = { it.id }) { post ->
        PostCard(
            post = post,
            onClick = { onOpenPost(post.id) },
            distance = Formatters.distance(myLocation?.lat, myLocation?.lng, post.latitude, post.longitude),
            menuActions = reportActions(post, onReport),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
    if (state.loadingMore) {
        item(key = "loading_more") {
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(strokeWidth = 2.dp)
            }
        }
    }
    state.loadMoreError?.let { msg ->
        item(key = "load_more_error") {
            Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                TextButton(onClick = onRetryMore) { Text("$msg · Thử lại") }
            }
        }
    }
    if (state.endReached && state.posts.isNotEmpty()) {
        item(key = "end") {
            Text(
                "Đã hiển thị hết bài",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

/** LazyColumn tự gọi [onLoadMore] khi cuộn gần cuối danh sách. */
@Composable
fun PagedLazyColumn(
    itemCount: Int,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(bottom = 16.dp),
    content: LazyListScope.() -> Unit
) {
    val listState = rememberLazyListState()
    val loadMore by rememberUpdatedState(onLoadMore)
    LaunchedEffect(listState) {
        snapshotFlow {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            // Trả về tổng số mục khi đang ở gần cuối, -1 nếu chưa; danh sách dài thêm mà vẫn
            // ở gần cuối (màn hình cao) thì giá trị đổi và tự tải tiếp
            if (total > 0 && last >= total - 4) total else -1
        }.distinctUntilChanged().collect { nearEndAt -> if (nearEndAt >= 0) loadMore() }
    }
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

/**
 * Xin quyền vị trí (nếu cần) rồi lấy GPS. Gọi hàm trả về để bắt đầu;
 * [onResult] nhận null nếu người dùng từ chối hoặc không lấy được vị trí.
 */
@Composable
fun rememberLocationRequester(onResult: (LatLng?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val callback by rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) {
            scope.launch { callback(LocationHelper.currentLocation(context)) }
        } else {
            callback(null)
        }
    }
    return {
        if (LocationHelper.hasPermission(context)) {
            scope.launch { callback(LocationHelper.currentLocation(context)) }
        } else {
            launcher.launch(LocationHelper.PERMISSIONS)
        }
    }
}

/** Lấy vị trí mà không hỏi quyền (chỉ khi đã được cấp trước đó) để hiện khoảng cách trên thẻ. */
suspend fun silentLocation(context: Context): LatLng? =
    if (LocationHelper.hasPermission(context)) LocationHelper.currentLocation(context) else null
