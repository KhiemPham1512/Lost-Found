package com.lostfound.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.lostfound.app.core.util.Formatters
import com.lostfound.app.core.network.ApiException
import com.lostfound.app.core.util.userMessage
import com.lostfound.app.data.repository.HandoverRepository
import com.lostfound.app.data.repository.HandoverState
import com.lostfound.app.ui.components.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HandoverScreen(conversationId: String, onBack: () -> Unit, onReport: (String, String?) -> Unit) {
    val scope = rememberCoroutineScope()
    var state by remember(conversationId) { mutableStateOf<HandoverState?>(null) }
    var busy by remember(conversationId) { mutableStateOf(false) }
    var error by remember(conversationId) { mutableStateOf<String?>(null) }
    var submitting by remember(conversationId) { mutableStateOf(false) }
    var showConfirm by remember(conversationId) { mutableStateOf(false) }
    var notice by remember(conversationId) { mutableStateOf<String?>(null) }
    suspend fun load() {
        if (busy || submitting) return
        busy = true
        try {
            state = HandoverRepository.load(conversationId)
            error = null
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) { error = e.userMessage()
        } finally { busy = false }
    }
    if (showConfirm) AlertDialog(
        onDismissRequest = { showConfirm = false },
        title = { Text("Xác nhận phần của bạn?") },
        text = { Text("Chỉ xác nhận khi bạn đã trao/nhận đúng món đồ. Khi cả hai xác nhận, bài được đánh dấu đã giải quyết và hội thoại chỉ còn đọc.") },
        dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("Quay lại") } },
        confirmButton = {
            TextButton(onClick = {
                val current = state
                if (!submitting && !busy && error == null && current?.status == "PASSED" && current.ownConfirmedAt == null) {
                    showConfirm = false
                    submitting = true
                    scope.launch {
                        try {
                            val response = HandoverRepository.confirm(current.conversation.claimId)
                            state = current.copy(status = response.status, authorConfirmedAt = response.authorConfirmedAt, claimantConfirmedAt = response.claimantConfirmedAt)
                            notice = "Máy chủ đã ghi nhận xác nhận của bạn."
                        } catch (e: CancellationException) { throw e
                        } catch (e: Exception) {
                            notice = if (e is ApiException && e.status in 400..499) "Không thể xác nhận: ${e.userMessage()}"
                                else "Chưa xác nhận được kết quả. Tải lại trạng thái trước khi thử lại. ${e.userMessage()}"
                        } finally { submitting = false }
                        load()
                    }
                }
            }, enabled = !busy && !submitting) { Text("Xác nhận") }
        }
    )
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(conversationId, owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) { load(); delay(5_000) }
        }
    }
    Column(Modifier.fillMaxSize()) {
        TopBar("Trao trả", onBack)
        if (submitting) LinearProgressIndicator(Modifier.fillMaxWidth())
        when {
            state == null && error != null -> ErrorView(error!!, { scope.launch { load() } })
            state == null -> LoadingView()
            else -> {
                val current = state!!
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(current.conversation.postTitle ?: "Bài đăng không còn xem được", style = MaterialTheme.typography.titleLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Avatar(current.conversation.peerAvatar, current.conversation.peerName)
                        Column {
                            Text(current.conversation.peerName, style = MaterialTheme.typography.titleMedium)
                            current.conversation.peerUsername?.let { Text("@$it") }
                        }
                    }
                    Text(if (current.isAuthor) "Bạn là chủ bài" else "Bạn là người gửi yêu cầu")
                    Text(if (current.ownConfirmedAt == null) "Bạn chưa xác nhận" else "Bạn đã xác nhận: ${Formatters.dateTime(current.ownConfirmedAt)}")
                    Text(if (current.peerConfirmedAt == null) "Người còn lại chưa xác nhận" else "Người còn lại đã xác nhận: ${Formatters.dateTime(current.peerConfirmedAt)}")
                    Text(when (current.status) {
                        "COMPLETED" -> "Trao trả hoàn tất"
                        "PASSED" -> if (current.ownConfirmedAt == null) "Chỉ xác nhận khi bạn đã trao/nhận đúng món đồ." else "Đang chờ người còn lại xác nhận."
                        "SUBMITTED" -> "Yêu cầu đang chờ duyệt. Chưa thể xác nhận trao trả."
                        "FAILED" -> "Yêu cầu đã bị từ chối. Không thể xác nhận trao trả."
                        "CANCELLED" -> "Yêu cầu đã hủy. Không thể xác nhận trao trả."
                        "CLOSED" -> "Yêu cầu đã đóng. Không thể xác nhận trao trả."
                        else -> "Không thể xác nhận yêu cầu ở trạng thái hiện tại."
                    })
                    notice?.let { Text(it) }
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    if (current.status == "PASSED" && current.ownConfirmedAt == null) {
                        Button(onClick = { showConfirm = true }, enabled = !busy && !submitting && error == null) { Text("Tôi đã trao/nhận đúng đồ") }
                    }
                    OutlinedButton(onClick = { scope.launch { load() } }, enabled = !busy && !submitting) { Text("Tải lại trạng thái") }
                    TextButton(onClick = { onReport(conversationId, current.conversation.postTitle) }) { Text("Báo cáo vấn đề") }
                }
            }
        }
    }
}
