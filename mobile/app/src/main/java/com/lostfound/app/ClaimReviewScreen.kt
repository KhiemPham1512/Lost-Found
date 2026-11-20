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
import com.lostfound.app.data.model.Post
import com.lostfound.app.data.repository.ClaimRepository
import com.lostfound.app.data.repository.PostRepository
import com.lostfound.app.data.repository.ReviewClaim
import com.lostfound.app.ui.components.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun ClaimReviewScreen(postId: String, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var post by remember(postId) { mutableStateOf<Post?>(null) }
    var claims by remember(postId) { mutableStateOf<List<ReviewClaim>>(emptyList()) }
    var total by remember(postId) { mutableIntStateOf(0) }
    var loading by remember(postId) { mutableStateOf(true) }
    var error by remember(postId) { mutableStateOf<String?>(null) }
    var retry by remember(postId) { mutableIntStateOf(0) }
    var pending by remember(postId) { mutableStateOf<Pair<ReviewClaim, String>?>(null) }
    var deciding by remember(postId) { mutableStateOf(false) }
    var notice by remember(postId) { mutableStateOf<String?>(null) }

    suspend fun load(offset: Int = 0) {
        loading = true
        error = null
        try {
            if (offset == 0) {
                claims = emptyList()
                post = null
                post = PostRepository.getPost(postId) ?: error("Không xem được bài đăng này")
            }
            val page = ClaimRepository.list(postId, offset)
            claims = if (offset == 0) page.items else (claims + page.items).distinctBy { it.id }
            total = page.total
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.userMessage()
        } finally {
            loading = false
        }
    }

    LaunchedEffect(postId, retry) { load() }

    pending?.let { (claim, decision) ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(if (decision == "PASS") "Cho phép liên hệ?" else "Từ chối yêu cầu?") },
            text = {
                Text(if (decision == "PASS") "Cho phép @${claim.username} liên hệ về bài đăng này."
                    else "Từ chối @${claim.username} sẽ tính một lượt thử của người gửi.")
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("Quay lại") } },
            confirmButton = {
                TextButton(onClick = {
                    if (!deciding && !loading) {
                        pending = null
                        deciding = true
                        scope.launch {
                            try {
                                ClaimRepository.decide(claim.id, decision)
                                notice = if (decision == "PASS") "Đã cho phép liên hệ." else "Đã từ chối yêu cầu."
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                // Kết quả có thể đã được lưu trước khi mất mạng; chỉ đọc lại, không tự gửi lại.
                                notice = "Chưa xác nhận được kết quả: ${e.userMessage()}. Kiểm tra trạng thái trước khi thử lại."
                            } finally {
                                deciding = false
                            }
                            load()
                        }
                    }
                }) { Text("Xác nhận") }
            }
        )
    }

    Column(Modifier.fillMaxSize()) {
        TopBar("Yêu cầu xác minh", onBack)
        notice?.let { Text(it, Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
        if (deciding) LinearProgressIndicator(Modifier.fillMaxWidth())
        when {
            loading && claims.isEmpty() -> LoadingView(message = "Đang tải yêu cầu")
            error != null && claims.isEmpty() -> ErrorView(error!!, { retry++ })
            post != null -> LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TypeBadge(post!!.isFound)
                        Text(post!!.title, style = MaterialTheme.typography.titleMedium)
                        Text("$total yêu cầu", style = MaterialTheme.typography.bodyMedium)
                        Text("Chủ bài đọc câu trả lời và quyết định cho phép liên hệ.")
                        if (post!!.status != "ACTIVE") Text("Bài không còn hoạt động; chỉ xem lịch sử yêu cầu.")
                        OutlinedButton(onClick = { retry++ }, enabled = !loading && !deciding) { Text("Tải lại") }
                    }
                }
                if (claims.isEmpty()) item { Text("Chưa có yêu cầu") }
                items(claims, key = { it.id }) { claim ->
                    ClaimReviewCard(
                        claim = claim,
                        canDecide = claim.status == "SUBMITTED" && post!!.status == "ACTIVE",
                        enabled = !loading && !deciding,
                        onDecision = { pending = claim to it }
                    )
                }
                if (error != null) item {
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                }
                if (claims.size < total) item {
                    OutlinedButton(
                        onClick = { scope.launch { load(claims.size) } }, enabled = !loading && !deciding
                    ) { Text(if (loading) "Đang tải…" else "Xem thêm") }
                }
            }
        }
    }
}

@Composable
private fun ClaimReviewCard(claim: ReviewClaim, canDecide: Boolean, enabled: Boolean, onDecision: (String) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Avatar(claim.avatarUrl, claim.displayName ?: claim.username)
                Column(Modifier.weight(1f)) {
                    Text(claim.displayName ?: claim.username, style = MaterialTheme.typography.titleMedium)
                    Text("@${claim.username}")
                    Text(Formatters.dateTime(claim.createdAt), style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(when (claim.status) {
                "SUBMITTED" -> "Chờ duyệt"
                "PASSED" -> "Đã chấp nhận"
                "FAILED" -> "Đã từ chối"
                "CANCELLED" -> "Đã hủy"
                "COMPLETED" -> "Đã hoàn tất"
                "CLOSED" -> "Đã đóng"
                else -> claim.status
            })
            if (claim.answers.isEmpty()) Text("Yêu cầu liên hệ, không có câu hỏi xác minh.")
            claim.answers.forEach { answer ->
                Text(answer.question, style = MaterialTheme.typography.labelLarge)
                Text(answer.answer)
            }
            if (canDecide) {
                // Xếp dọc để nhãn và vùng chạm vẫn rõ trên màn nhỏ hoặc cỡ chữ lớn.
                Button(onClick = { onDecision("PASS") }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                    Text("Chấp nhận")
                }
                OutlinedButton(onClick = { onDecision("FAIL") }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                    Text("Từ chối")
                }
            }
        }
    }
}
