package com.lostfound.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lostfound.app.core.location.LatLng
import com.lostfound.app.data.model.ReportTarget
import com.lostfound.app.data.model.TodoCounts
import com.lostfound.app.data.repository.PostRepository
import com.lostfound.app.data.repository.ProfileRepository
import com.lostfound.app.data.repository.SearchParams
import com.lostfound.app.ui.components.EmptyView
import com.lostfound.app.ui.components.ErrorView
import com.lostfound.app.ui.components.LoadingView
import com.lostfound.app.ui.feed.FeedLoader
import com.lostfound.app.ui.feed.PagedLazyColumn
import com.lostfound.app.ui.feed.postItems
import com.lostfound.app.ui.feed.rememberLocationRequester
import com.lostfound.app.ui.feed.silentLocation
import com.lostfound.app.ui.theme.BrandBlue
import com.lostfound.app.ui.theme.ErrorRed
import com.lostfound.app.ui.theme.TextSecondary
import com.lostfound.app.ui.theme.WarningAmber
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeHeader(
    val name: String? = null,
    val todo: TodoCounts = TodoCounts(),
    val type: String? = null,           // null = Tất cả
    val sortNearest: Boolean = false,
    val location: LatLng? = null
)

class HomeViewModel : ViewModel() {
    private val _header = MutableStateFlow(HomeHeader())
    val header = _header.asStateFlow()

    val feed = FeedLoader(viewModelScope) {
        val h = _header.value
        SearchParams(
            type = h.type,
            sortNearest = h.sortNearest,
            lat = h.location?.lat,
            lng = h.location?.lng
        )
    }
    private var lastLoadMs = 0L

    init {
        feed.reload()
        lastLoadMs = System.currentTimeMillis()
    }

    /** Gọi mỗi lần quay lại Home: cập nhật lời chào, khối Cần xử lý và làm mới bài nếu đã cũ. */
    fun onResumed() {
        loadHeader()
        if (System.currentTimeMillis() - lastLoadMs > 30_000) refresh()
    }

    fun refresh() {
        lastLoadMs = System.currentTimeMillis()
        feed.reload(pullToRefresh = true)
        loadHeader()
    }

    private fun loadHeader() {
        viewModelScope.launch {
            runCatching { PostRepository.todoCounts() }.onSuccess { todo ->
                _header.update { it.copy(todo = todo) }
            }
        }
        viewModelScope.launch {
            runCatching { ProfileRepository.myProfile() }.onSuccess { p ->
                if (p != null) _header.update { it.copy(name = p.shownName) }
            }
        }
    }

    fun setType(type: String?) {
        if (_header.value.type == type) return
        _header.update { it.copy(type = type) }
        feed.reload()
    }

    fun setNewest() {
        if (!_header.value.sortNearest) return
        _header.update { it.copy(sortNearest = false) }
        feed.reload()
    }

    /** Đã lấy được GPS: chuyển sang sắp xếp Gần nhất. */
    fun setNearest(location: LatLng) {
        _header.update { it.copy(sortNearest = true, location = location) }
        feed.reload()
    }

    /** Có vị trí sẵn (quyền đã cấp từ trước) thì hiện khoảng cách trên thẻ. */
    fun setLocation(location: LatLng?) {
        if (location != null) _header.update { it.copy(location = location) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenPost: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenPendingVerifications: () -> Unit,
    onOpenUnreadMessages: () -> Unit,
    onOpenAwaitingHandover: () -> Unit,
    onCreatePost: () -> Unit,
    onReport: (ReportTarget, String, String?) -> Unit,
    vm: HomeViewModel = viewModel()
) {
    val header by vm.header.collectAsState()
    val feed by vm.feed.state.collectAsState()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        vm.onResumed()
        vm.setLocation(silentLocation(context))
    }

    val requestLocation = rememberLocationRequester { loc ->
        if (loc != null) vm.setNearest(loc)
        else scope.launch {
            snackbar.showSnackbar("Cần bật định vị và cho phép quyền vị trí để sắp xếp Gần nhất")
        }
    }

    Box(Modifier.fillMaxSize().statusBarsPadding()) {
        PullToRefreshBox(
            isRefreshing = feed.refreshing,
            onRefresh = vm::refresh,
            modifier = Modifier.fillMaxSize()
        ) {
            PagedLazyColumn(
                itemCount = feed.posts.size,
                onLoadMore = vm.feed::loadMore,
                modifier = Modifier.fillMaxSize()
            ) {
                item(key = "greeting") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Xin chào,", color = TextSecondary)
                            Text(
                                header.name ?: "bạn",
                                style = MaterialTheme.typography.headlineSmall,
                                maxLines = 1
                            )
                        }
                        IconButton(onClick = onOpenNotifications) {
                            Icon(Icons.Filled.Notifications, contentDescription = "Thông báo", tint = BrandBlue)
                        }
                    }
                }
                item(key = "todo") {
                    TodoBlock(
                        todo = header.todo,
                        onPending = onOpenPendingVerifications,
                        onUnread = onOpenUnreadMessages,
                        onAwaiting = onOpenAwaitingHandover
                    )
                }
                item(key = "search") {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .clickable(onClick = onOpenSearch)
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = TextSecondary)
                        Spacer(Modifier.width(8.dp))
                        Text("Tìm đồ thất lạc: ví, điện thoại, chìa khóa…", color = TextSecondary)
                    }
                }
                item(key = "filters") {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ChoiceChip("Tất cả", header.type == null) { vm.setType(null) }
                            ChoiceChip("Lost", header.type == "LOST") { vm.setType("LOST") }
                            ChoiceChip("Found", header.type == "FOUND") { vm.setType("FOUND") }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Sắp xếp:", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                            ChoiceChip("Mới nhất", !header.sortNearest) { vm.setNewest() }
                            ChoiceChip("Gần nhất", header.sortNearest) { requestLocation() }
                        }
                    }
                }

                when {
                    feed.loading -> item(key = "loading") {
                        LoadingView(Modifier.height(240.dp))
                    }
                    feed.error != null -> item(key = "error") {
                        ErrorView(feed.error ?: "", onRetry = { vm.feed.reload() }, modifier = Modifier.height(300.dp))
                    }
                    feed.loadedOnce && feed.posts.isEmpty() -> item(key = "empty") {
                        EmptyView(
                            title = "Chưa có bài đăng nào",
                            message = "Hãy là người đầu tiên đăng bài tìm đồ hoặc trả đồ.",
                            actionLabel = "Đăng bài",
                            onAction = onCreatePost
                        )
                    }
                }

                postItems(
                    state = feed,
                    myLocation = header.location,
                    onOpenPost = onOpenPost,
                    onReport = onReport,
                    onRetryMore = vm.feed::loadMore
                )
            }
        }
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
internal fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@Composable
private fun TodoBlock(
    todo: TodoCounts,
    onPending: () -> Unit,
    onUnread: () -> Unit,
    onAwaiting: () -> Unit
) {
    Card(
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text("Cần xử lý", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TodoItem(todo.pendingVerifications, "Yêu cầu xác minh chờ duyệt", WarningAmber, onPending, Modifier.weight(1f))
                TodoItem(todo.unreadMessages, "Tin nhắn chưa đọc", BrandBlue, onUnread, Modifier.weight(1f))
                TodoItem(todo.awaitingConfirmation, "Chờ xác nhận trao trả", ErrorRed, onAwaiting, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TodoItem(
    count: Int,
    label: String,
    accent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = 0.10f))
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(34.dp).clip(CircleShape).background(accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text(count.toString(), color = accent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            minLines = 2
        )
    }
}
