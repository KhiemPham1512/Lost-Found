package com.lostfound.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lostfound.app.core.util.Formatters
import com.lostfound.app.core.util.userMessage
import com.lostfound.app.data.repository.AppNotification
import com.lostfound.app.data.repository.NotificationCursor
import com.lostfound.app.data.repository.NotificationRepository
import com.lostfound.app.data.repository.NotificationDestination
import com.lostfound.app.ui.components.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import com.lostfound.app.core.session.SessionStore
import com.lostfound.app.core.supabase.RealtimeFilter
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Composable
fun NotificationsScreen(onBack: () -> Unit, onOpen: (NotificationDestination) -> Unit) {
    val scope = rememberCoroutineScope()
    var rows by remember { mutableStateOf<List<AppNotification>>(emptyList()) }
    var cursor by remember { mutableStateOf<NotificationCursor?>(null) }
    var more by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var writing by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    val loadMutex = remember { Mutex() }
    suspend fun load(before: NotificationCursor? = null) = loadMutex.withLock {
        busy = true
        error = null
        try {
            val page = NotificationRepository.list(before)
            rows = if (before == null) page else (rows + page).distinctBy { it.id }
            cursor = page.lastOrNull()?.cursor
            more = page.size == NotificationRepository.PAGE_SIZE
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) { error = e.userMessage()
        } finally { busy = false }
    }
    val uid = SessionStore.userId
    val filters = remember(uid) {
        if (uid == null) emptyList() else listOf(
            RealtimeFilter("notifications", "INSERT", "user_id=eq.$uid"),
            RealtimeFilter("notifications", "UPDATE", "user_id=eq.$uid")
        )
    }
    val connected = rememberRealtimeRefresh(filters) { load() }
    fun open(notification: AppNotification) {
        if (busy || writing) return
        writing = true
        scope.launch {
            try {
                val destination = NotificationRepository.destination(notification)
                if (notification.readAt == null) NotificationRepository.markRead(notification.id)
                load()
                if (destination != null) onOpen(destination) else notice = "Đã đọc thông báo."
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                notice = "Không mở được thông báo: ${e.userMessage()}"
                load()
            } finally { writing = false }
        }
    }
    Column(Modifier.fillMaxSize()) {
        TopBar("Thông báo", onBack) {
            TextButton(onClick = {
                val through = rows.firstOrNull()?.cursor
                if (!busy && !writing && through != null) {
                    writing = true
                    scope.launch {
                        try {
                            NotificationRepository.markAll(through)
                            notice = "Đã cập nhật trạng thái đã đọc."
                        } catch (e: CancellationException) { throw e
                        } catch (e: Exception) { notice = "Chưa lưu được trạng thái đã đọc: ${e.userMessage()}"
                        } finally { writing = false }
                        load()
                    }
                }
            }, enabled = !busy && !writing && rows.isNotEmpty()) { Text("Đọc hết") }
        }
        Text(if (connected) "Cập nhật trực tiếp" else "Đang kết nối lại · Có thể tải lại", Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = { scope.launch { load() } }, enabled = !busy && !writing) { Text("Tải lại") }
        notice?.let { Text(it, Modifier.padding(horizontal = 16.dp)) }
        if (writing) LinearProgressIndicator(Modifier.fillMaxWidth())
        when {
            busy && rows.isEmpty() -> LoadingView()
            error != null && rows.isEmpty() -> ErrorView(error!!, { scope.launch { load() } })
            else -> LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (rows.isEmpty()) item { Text("Chưa có thông báo") }
                for (unread in listOf(true, false)) {
                    val group = rows.filter { (it.readAt == null) == unread }
                    if (group.isNotEmpty()) item { Text(if (unread) "Mới" else "Trước đó", style = MaterialTheme.typography.titleMedium) }
                    items(group, key = { it.id }) { notification ->
                        Card(onClick = { open(notification) }, enabled = !busy && !writing, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(notification.title, style = MaterialTheme.typography.titleMedium)
                                notification.body?.let { Text(it) }
                                Text(Formatters.dateTime(notification.createdAt), style = MaterialTheme.typography.bodySmall)
                                Text(if (unread) "Chưa đọc" else "Đã đọc", color = if (unread) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
                if (more) item {
                    OutlinedButton(onClick = { scope.launch { load(cursor) } }, enabled = !busy && !writing) { Text("Xem thêm") }
                }
            }
        }
    }
}
