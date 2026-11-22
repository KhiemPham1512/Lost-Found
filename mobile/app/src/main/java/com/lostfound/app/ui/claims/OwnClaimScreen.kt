package com.lostfound.app.ui.claims

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lostfound.app.core.supabase.RealtimeFilter
import com.lostfound.app.core.util.userMessage
import com.lostfound.app.data.repository.OwnClaim
import com.lostfound.app.data.repository.OwnClaimRepository
import com.lostfound.app.ui.components.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Composable
fun OwnClaimScreen(initial: OwnClaim, onBack: () -> Unit, onRetry: () -> Unit, onOpenChat: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var claim by remember(initial.id) { mutableStateOf(initial) }
    var busy by remember(initial.id) { mutableStateOf(false) }
    var opening by remember(initial.id) { mutableStateOf(false) }
    var error by remember(initial.id) { mutableStateOf<String?>(null) }
    var openError by remember(initial.id) { mutableStateOf<String?>(null) }
    val mutex = remember(initial.id) { Mutex() }
    suspend fun load() = mutex.withLock {
        busy = true
        try {
            claim = OwnClaimRepository.latest(initial.postId) ?: throw IllegalStateException("Không xem được yêu cầu này")
            error = null
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) { error = e.userMessage()
        } finally { busy = false }
    }
    val filters = remember(claim.id) { listOf(RealtimeFilter("claims", "UPDATE", "id=eq.${claim.id}")) }
    val connected = rememberRealtimeRefresh(filters) { load() }
    Column(Modifier.fillMaxSize()) {
        TopBar("Yêu cầu của bạn", onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(claim.postTitle ?: "Bài đăng không còn xem được", style = MaterialTheme.typography.titleMedium)
            Text(if (connected) "Cập nhật trực tiếp" else "Đang kết nối lại · Có thể tải lại", style = MaterialTheme.typography.bodySmall)
            Text(when (claim.status) {
                "SUBMITTED" -> "Đang chờ chủ bài duyệt"
                "PASSED" -> "Yêu cầu đã được duyệt"
                "FAILED" -> "Yêu cầu bị từ chối"
                "CANCELLED" -> "Yêu cầu đã hủy"
                "COMPLETED" -> "Đã hoàn tất trao trả"
                "CLOSED" -> "Yêu cầu đã đóng"
                else -> "Chưa đọc được trạng thái của yêu cầu"
            }, style = MaterialTheme.typography.titleLarge)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            OutlinedButton(onClick = { scope.launch { load() } }, enabled = !busy && !opening) { Text("Tải lại trạng thái") }
            if (claim.status in setOf("PASSED", "COMPLETED")) {
                Button(onClick = {
                    if (!opening) {
                        opening = true
                        openError = null
                        scope.launch {
                            try { onOpenChat(OwnClaimRepository.conversationId(claim))
                            } catch (e: CancellationException) { throw e
                            } catch (e: Exception) { openError = e.userMessage()
                            } finally { opening = false }
                        }
                    }
                }, enabled = !busy && !opening && error == null) {
                    Text(if (opening) "Đang mở…" else if (claim.status == "COMPLETED") "Xem hội thoại" else "Mở hội thoại")
                }
            }
            if (claim.canCheckAttempts) {
                Text("Kiểm tra số lượt còn lại trước khi gửi yêu cầu mới.")
                Button(onClick = onRetry, enabled = !busy && !opening && error == null) { Text("Kiểm tra lượt còn lại") }
            }
            openError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}
