package com.lostfound.app.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lostfound.app.core.util.Formatters
import com.lostfound.app.core.util.userMessage
import com.lostfound.app.data.repository.ChatRepository
import com.lostfound.app.data.repository.Conversation
import com.lostfound.app.ui.components.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import com.lostfound.app.core.supabase.RealtimeFilter
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Composable
fun MessagesScreen(onOpenChat: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var rows by remember { mutableStateOf<List<Conversation>>(emptyList()) }
    var offset by remember { mutableIntStateOf(0) }
    var more by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val loadMutex = remember { Mutex() }
    suspend fun load(next: Int = 0) = loadMutex.withLock {
        loading = true
        error = null
        try {
            val page = ChatRepository.conversations(next)
            rows = if (next == 0) page else (rows + page).distinctBy { it.id }
            offset = next + page.size
            more = page.size == ChatRepository.PAGE_SIZE
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) { error = e.userMessage()
        } finally { loading = false }
    }
    val filters = remember { listOf(RealtimeFilter("messages", "INSERT", ""), RealtimeFilter("claims", "UPDATE", "")) }
    val connected = rememberRealtimeRefresh(filters) { load() }
    Column(Modifier.fillMaxSize()) {
        TopBar("Tin nhắn") {
            TextButton(onClick = { scope.launch { load() } }, enabled = !loading) { Text("Tải lại") }
        }
        Text(if (connected) "Cập nhật trực tiếp" else "Đang kết nối lại · Có thể tải lại", Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.bodySmall)
        when {
            loading && rows.isEmpty() -> LoadingView()
            error != null && rows.isEmpty() -> ErrorView(error!!, { scope.launch { load() } })
            else -> LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (rows.isEmpty()) item { Text("Chưa có hội thoại. Hội thoại được tạo khi yêu cầu được chấp nhận.") }
                items(rows, key = { it.id }) { row ->
                    Card(onClick = { onOpenChat(row.id) }, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Avatar(row.peerAvatar, row.peerName)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(row.peerName, style = MaterialTheme.typography.titleMedium)
                                row.peerUsername?.let { Text("@$it") }
                                Text(row.postTitle ?: "Bài đăng không còn xem được")
                                Text(if (row.locked) "Đã khóa · Chỉ đọc" else "Đang mở")
                                Text(Formatters.dateTime(row.lastMessageAt), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
                if (more) item {
                    OutlinedButton(onClick = { scope.launch { load(offset) } }, enabled = !loading) {
                        Text(if (loading) "Đang tải…" else "Xem thêm")
                    }
                }
            }
        }
    }
}
