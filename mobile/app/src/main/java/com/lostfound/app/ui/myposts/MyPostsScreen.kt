package com.lostfound.app.ui.myposts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lostfound.app.core.util.Formatters
import com.lostfound.app.core.util.userMessage
import com.lostfound.app.data.model.Post
import com.lostfound.app.data.repository.PostRepository
import com.lostfound.app.ui.components.EmptyView
import com.lostfound.app.ui.components.ErrorView
import com.lostfound.app.ui.components.LoadingView
import com.lostfound.app.ui.components.PostCard
import com.lostfound.app.ui.components.SmallChip
import com.lostfound.app.ui.home.ChoiceChip
import com.lostfound.app.ui.theme.ErrorRed
import com.lostfound.app.ui.theme.FoundGreen
import com.lostfound.app.ui.theme.FoundText
import com.lostfound.app.ui.theme.TextSecondary
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.OffsetDateTime

enum class MyPostsTab(val label: String, val statuses: List<String>) {
    ACTIVE("Đang đăng", listOf("ACTIVE")),
    DONE("Đã DONE", listOf("RESOLVED")),
    HIDDEN("Đã ẩn", listOf("ARCHIVED", "EXPIRED", "REMOVED"))
}

data class MyPostsState(
    val tab: MyPostsTab = MyPostsTab.ACTIVE,
    val posts: List<Post> = emptyList(),
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val error: String? = null,
    val busyPostId: String? = null
)

class MyPostsViewModel : ViewModel() {
    private val _state = MutableStateFlow(MyPostsState())
    val state = _state.asStateFlow()
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages = _messages.asSharedFlow()
    private var job: Job? = null

    fun setTab(tab: MyPostsTab) {
        if (tab == _state.value.tab && _state.value.posts.isNotEmpty()) return
        _state.update { it.copy(tab = tab, posts = emptyList()) }
        load()
    }

    fun load(pullToRefresh: Boolean = false) {
        job?.cancel()
        val tab = _state.value.tab
        _state.update { it.copy(loading = !pullToRefresh, refreshing = pullToRefresh, error = null) }
        job = viewModelScope.launch {
            try {
                val posts = PostRepository.myPosts(tab.statuses)
                _state.update { it.copy(posts = posts, loading = false, refreshing = false) }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, refreshing = false, error = e.userMessage()) }
            }
        }
    }

    /** Ẩn / hiện lại / xóa qua PATCH /posts/{id}/status. */
    fun changeStatus(post: Post, status: String) {
        _state.update { it.copy(busyPostId = post.id) }
        viewModelScope.launch {
            try {
                PostRepository.changeStatus(post.id, status)
                _state.update { it.copy(busyPostId = null) }
                load()
                _messages.emit(
                    when (status) {
                        "ARCHIVED" -> "Đã ẩn bài. Xem lại ở tab Đã ẩn."
                        "ACTIVE" -> "Đã hiện lại bài."
                        else -> "Đã xóa bài."
                    }
                )
            } catch (e: Exception) {
                _state.update { it.copy(busyPostId = null) }
                _messages.emit(e.userMessage())
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPostsScreen(
    onOpenPost: (String) -> Unit,
    onOpenClaims: (String) -> Unit,
    onCreatePost: () -> Unit,
    vm: MyPostsViewModel = viewModel()
) {
    val s by vm.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var confirmDelete by remember { mutableStateOf<Post?>(null) }

    LaunchedEffect(Unit) { vm.load() }
    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }

    Box(Modifier.fillMaxSize().statusBarsPadding()) {
        Column(Modifier.fillMaxSize()) {
            Text(
                "Bài của bạn",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp)
            )
            FlowRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MyPostsTab.entries.forEach { tab ->
                    ChoiceChip(tab.label, s.tab == tab) { vm.setTab(tab) }
                }
            }

            PullToRefreshBox(
                isRefreshing = s.refreshing,
                onRefresh = { vm.load(pullToRefresh = true) },
                modifier = Modifier.weight(1f)
            ) {
                when {
                    s.loading -> LoadingView()
                    s.error != null -> ErrorView(s.error ?: "", onRetry = { vm.load() })
                    s.posts.isEmpty() -> LazyColumn(Modifier.fillMaxSize()) {
                        // Dùng LazyColumn để vẫn kéo xuống tải lại được khi danh sách trống
                        item {
                            when (s.tab) {
                                MyPostsTab.ACTIVE -> EmptyView(
                                    title = "Bạn chưa có bài nào đang đăng",
                                    actionLabel = "Đăng bài",
                                    onAction = onCreatePost
                                )
                                MyPostsTab.DONE -> EmptyView(
                                    title = "Chưa có bài nào hoàn tất",
                                    message = "Bài chuyển sang DONE khi hai bên cùng xác nhận đã trao trả."
                                )
                                MyPostsTab.HIDDEN -> EmptyView(
                                    title = "Không có bài bị ẩn",
                                    message = "Bài bạn tự ẩn, bài hết hạn sau 30 ngày và bài bị gỡ sẽ nằm ở đây."
                                )
                            }
                        }
                    }
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(s.posts, key = { it.id }) { post ->
                            PostCard(
                                post = post,
                                onClick = { onOpenPost(post.id) },
                                footer = {
                                    MyPostFooter(
                                        post = post,
                                        busy = s.busyPostId == post.id,
                                        onClaims = { onOpenClaims(post.id) },
                                        onHide = { vm.changeStatus(post, "ARCHIVED") },
                                        onShow = { vm.changeStatus(post, "ACTIVE") },
                                        onDelete = { confirmDelete = post }
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }

    confirmDelete?.let { post ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Xóa bài đăng?") },
            text = { Text("Bài \"${post.title}\" sẽ bị xóa và không thể khôi phục. Các yêu cầu xác minh đang mở sẽ bị đóng.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.changeStatus(post, "DELETED")
                    confirmDelete = null
                }) { Text("Xóa", color = ErrorRed) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Hủy") } }
        )
    }
}

@Composable
private fun MyPostFooter(
    post: Post,
    busy: Boolean,
    onClaims: () -> Unit,
    onHide: () -> Unit,
    onShow: () -> Unit,
    onDelete: () -> Unit
) {
    Column {
        // Dòng trạng thái
        when (post.status) {
            "ACTIVE" -> post.expiresAt?.let {
                Text("Hết hạn: ${Formatters.date(it)}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            "RESOLVED" -> SmallChip(
                "DONE" + (post.resolvedAt?.let { " · ${Formatters.date(it)}" } ?: ""),
                background = FoundGreen, color = FoundText
            )
            "ARCHIVED" -> SmallChip("Đã ẩn")
            "EXPIRED" -> SmallChip("Hết hạn sau 30 ngày")
            "REMOVED" -> Text(
                "Bị quản trị viên gỡ" + (post.removedReason?.let { ": $it" } ?: ""),
                color = ErrorRed,
                style = MaterialTheme.typography.bodySmall
            )
        }

        // Hành động
        val canReactivate = post.status == "ARCHIVED" &&
            (Formatters.parse(post.expiresAt)?.isAfter(OffsetDateTime.now()) ?: true)
        FlowRow(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            horizontalArrangement = Arrangement.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (busy) {
                Text("Đang xử lý…", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                return@FlowRow
            }
            if (post.status == "ACTIVE") {
                TextButton(onClick = onClaims) { Text("Yêu cầu") }
                TextButton(onClick = onHide) { Text("Ẩn") }
            }
            if (canReactivate) TextButton(onClick = onShow) { Text("Hiện lại") }
            if (post.status == "ACTIVE" || post.status == "ARCHIVED") {
                TextButton(onClick = onDelete) { Text("Xóa", color = ErrorRed) }
            }
        }
    }
}
