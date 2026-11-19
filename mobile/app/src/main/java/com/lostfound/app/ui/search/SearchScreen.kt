package com.lostfound.app.ui.search

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lostfound.app.core.location.LatLng
import com.lostfound.app.core.util.Formatters
import com.lostfound.app.core.util.TimeFilter
import com.lostfound.app.data.model.Category
import com.lostfound.app.data.model.ReportTarget
import com.lostfound.app.data.repository.PostRepository
import com.lostfound.app.data.repository.SearchParams
import com.lostfound.app.ui.components.EmptyView
import com.lostfound.app.ui.components.ErrorView
import com.lostfound.app.ui.components.LoadingView
import com.lostfound.app.ui.feed.FeedLoader
import com.lostfound.app.ui.feed.PagedLazyColumn
import com.lostfound.app.ui.feed.postItems
import com.lostfound.app.ui.feed.rememberLocationRequester
import com.lostfound.app.ui.feed.silentLocation
import com.lostfound.app.ui.home.ChoiceChip
import com.lostfound.app.ui.theme.TextSecondary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchFilters(
    val query: String = "",
    val type: String? = null,
    val categoryId: Int? = null,
    val time: TimeFilter = TimeFilter.ALL,
    val tags: List<String> = emptyList(),
    val tagInput: String = "",
    val sortNearest: Boolean = false,
    val location: LatLng? = null,
    val categories: List<Category> = emptyList()
)

class SearchViewModel : ViewModel() {
    private val _filters = MutableStateFlow(SearchFilters())
    val filters = _filters.asStateFlow()

    val feed = FeedLoader(viewModelScope) {
        val f = _filters.value
        SearchParams(
            type = f.type,
            categoryId = f.categoryId,
            query = f.query,
            postedAfter = Formatters.postedAfter(f.time),
            tagNames = f.tags,
            sortNearest = f.sortNearest,
            lat = f.location?.lat,
            lng = f.location?.lng
        )
    }
    private var debounceJob: Job? = null

    init {
        viewModelScope.launch {
            runCatching { PostRepository.categories() }.onSuccess { list ->
                _filters.update { it.copy(categories = list) }
            }
        }
        feed.reload()
    }

    /** Gõ từ khóa: đợi 400 ms sau lần gõ cuối mới tìm. */
    fun onQuery(v: String) {
        _filters.update { it.copy(query = v) }
        debounceJob?.cancel()
        debounceJob = viewModelScope.launch {
            delay(400)
            feed.reload()
        }
    }

    private fun change(transform: (SearchFilters) -> SearchFilters) {
        _filters.update(transform)
        debounceJob?.cancel()
        feed.reload()
    }

    fun setType(v: String?) = change { it.copy(type = v) }
    fun setCategory(v: Int?) = change { it.copy(categoryId = v) }
    fun setTime(v: TimeFilter) = change { it.copy(time = v) }
    fun setNewest() = change { it.copy(sortNearest = false) }
    fun setNearest(loc: LatLng) = change { it.copy(sortNearest = true, location = loc) }
    fun setLocation(loc: LatLng?) { if (loc != null) _filters.update { it.copy(location = loc) } }

    fun onTagInput(v: String) {
        // Gõ dấu phẩy hoặc Enter để thêm tag
        if (v.endsWith(",") || v.endsWith("\n")) addTag(v.dropLast(1)) else _filters.update { it.copy(tagInput = v) }
    }

    fun addTag(raw: String = _filters.value.tagInput) {
        val tag = raw.trim().lowercase().replace(Regex("\\s+"), " ").take(30)
        if (tag.isEmpty() || tag in _filters.value.tags || _filters.value.tags.size >= 10) {
            _filters.update { it.copy(tagInput = "") }
            return
        }
        change { it.copy(tags = it.tags + tag, tagInput = "") }
    }

    fun removeTag(tag: String) = change { it.copy(tags = it.tags - tag) }

    fun clearAll() = change {
        SearchFilters(categories = it.categories, location = it.location)
    }
}

@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onOpenPost: (String) -> Unit,
    onReport: (ReportTarget, String, String?) -> Unit,
    vm: SearchViewModel = viewModel()
) {
    val f by vm.filters.collectAsState()
    val feed by vm.feed.state.collectAsState()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val focus = remember { FocusRequester() }
    var showFilters by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        runCatching { focus.requestFocus() }
        vm.setLocation(silentLocation(context))
    }

    val requestLocation = rememberLocationRequester { loc ->
        if (loc != null) vm.setNearest(loc)
        else scope.launch { snackbar.showSnackbar("Cần quyền vị trí để sắp xếp Gần nhất") }
    }

    Box(Modifier.fillMaxSize().statusBarsPadding()) {
        Column(Modifier.fillMaxSize()) {
            // Ô tìm kiếm
            Row(
                modifier = Modifier.fillMaxWidth().padding(end = 12.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                }
                OutlinedTextField(
                    value = f.query,
                    onValueChange = vm::onQuery,
                    placeholder = { Text("Tìm: ví da, iphone, chìa khóa…") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (f.query.isNotEmpty()) {
                            IconButton(onClick = { vm.onQuery("") }) {
                                Icon(Icons.Filled.Close, contentDescription = "Xóa từ khóa")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).focusRequester(focus)
                )
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val active = listOf(
                    f.type != null, f.categoryId != null, f.time != TimeFilter.ALL,
                    f.tags.isNotEmpty(), f.sortNearest
                ).count { it }
                Text(
                    if (active > 0) "Bộ lọc ($active đang chọn)" else "Bộ lọc",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = vm::clearAll) { Text("Xóa lọc") }
                TextButton(onClick = { showFilters = !showFilters }) {
                    Text(if (showFilters) "Thu gọn" else "Mở rộng")
                }
            }

            if (showFilters) {
                Column(
                    Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    FilterLabel("Loại bài")
                    ChipRow {
                        ChoiceChip("Tất cả", f.type == null) { vm.setType(null) }
                        ChoiceChip("Lost (đồ bị mất)", f.type == "LOST") { vm.setType("LOST") }
                        ChoiceChip("Found (đồ nhặt được)", f.type == "FOUND") { vm.setType("FOUND") }
                    }
                    FilterLabel("Danh mục")
                    ChipRow {
                        ChoiceChip("Tất cả", f.categoryId == null) { vm.setCategory(null) }
                        f.categories.forEach { c ->
                            ChoiceChip(c.name, f.categoryId == c.id) { vm.setCategory(c.id) }
                        }
                    }
                    FilterLabel("Thời gian đăng")
                    ChipRow {
                        TimeFilter.entries.forEach { t ->
                            ChoiceChip(t.label, f.time == t) { vm.setTime(t) }
                        }
                    }
                    FilterLabel("Tag (bài phải có đủ các tag đã chọn)")
                    OutlinedTextField(
                        value = f.tagInput,
                        onValueChange = vm::onTagInput,
                        placeholder = { Text("Nhập tag rồi bấm Thêm, ví dụ: màu nâu") },
                        singleLine = true,
                        trailingIcon = {
                            TextButton(onClick = { vm.addTag() }, enabled = f.tagInput.isNotBlank()) {
                                Text("Thêm")
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (f.tags.isNotEmpty()) {
                        ChipRow {
                            f.tags.forEach { tag ->
                                InputChip(
                                    selected = true,
                                    onClick = { vm.removeTag(tag) },
                                    label = { Text("#$tag") },
                                    trailingIcon = {
                                        Icon(Icons.Filled.Close, contentDescription = "Bỏ tag")
                                    }
                                )
                            }
                        }
                    }
                    FilterLabel("Sắp xếp")
                    ChipRow {
                        ChoiceChip("Mới nhất", !f.sortNearest) { vm.setNewest() }
                        ChoiceChip("Gần nhất", f.sortNearest) { requestLocation() }
                    }
                }
            }

            Box(Modifier.weight(1f)) {
                PagedLazyColumn(
                    itemCount = feed.posts.size,
                    onLoadMore = vm.feed::loadMore,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 8.dp, bottom = 16.dp)
                ) {
                    when {
                        feed.loading -> item(key = "loading") { LoadingView(Modifier.height(200.dp)) }
                        feed.error != null -> item(key = "error") {
                            ErrorView(feed.error ?: "", onRetry = { vm.feed.reload() }, modifier = Modifier.height(260.dp))
                        }
                        feed.loadedOnce && feed.posts.isEmpty() -> item(key = "empty") {
                            EmptyView(
                                title = "Không tìm thấy bài phù hợp",
                                message = "Thử từ khóa khác, bỏ bớt bộ lọc hoặc chọn thời gian dài hơn."
                            )
                        }
                        else -> item(key = "count") {
                            Text(
                                "Kết quả" + if (f.query.isNotBlank()) " cho \"${f.query.trim()}\"" else "",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                    postItems(
                        state = feed,
                        myLocation = f.location,
                        onOpenPost = onOpenPost,
                        onReport = onReport,
                        onRetryMore = vm.feed::loadMore
                    )
                }
            }
        }
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun FilterLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = TextSecondary,
        modifier = Modifier.padding(top = 6.dp)
    )
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) { content() }
}
