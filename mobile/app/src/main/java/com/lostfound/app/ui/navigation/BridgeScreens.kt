package com.lostfound.app.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lostfound.app.DetailScreen
import com.lostfound.app.core.session.SessionStore
import com.lostfound.app.data.model.Post
import com.lostfound.app.data.model.ReportTarget
import com.lostfound.app.data.repository.PostRepository
import com.lostfound.app.ui.components.EmptyView
import com.lostfound.app.ui.components.LoadingView
import com.lostfound.app.ui.components.TopBar

/**
 * Bọc màn Chi tiết của C: B tải bài theo postId để biết loại bài, chủ bài,
 * rồi thêm thanh tiêu đề có nút quay lại và menu ba chấm (Báo cáo).
 * Khi C làm Chi tiết bằng dữ liệu thật, chỉ cần truyền postId vào DetailScreen.
 */
@Composable
fun PostDetailRoute(
    postId: String,
    onBack: () -> Unit,
    onVerify: () -> Unit,
    onOpenClaims: () -> Unit,
    onReport: (ReportTarget, String, String?) -> Unit
) {
    val state by produceState<Result<Post?>?>(initialValue = null, postId) {
        value = runCatching { PostRepository.getPost(postId) }
    }
    var menuOpen by remember { mutableStateOf(false) }
    val post = state?.getOrNull()
    val isMine = post != null && post.authorId == SessionStore.userId

    Column(Modifier.fillMaxSize()) {
        TopBar(title = "Chi tiết bài đăng", onBack = onBack) {
            if (post != null && !isMine) {
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Tùy chọn")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Báo cáo bài đăng") },
                            onClick = {
                                menuOpen = false
                                onReport(ReportTarget.POST, post.id, post.title)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Báo cáo người đăng") },
                            onClick = {
                                menuOpen = false
                                onReport(
                                    ReportTarget.USER,
                                    post.authorId,
                                    post.authorUsername?.let { "@$it" }
                                )
                            }
                        )
                    }
                }
            }
        }

        Box(Modifier.weight(1f)) {
            when {
                state == null -> LoadingView()
                post == null -> EmptyView(
                    title = "Không xem được bài đăng",
                    message = state?.exceptionOrNull()?.message
                        ?: "Bài đã bị ẩn, bị gỡ hoặc không còn tồn tại."
                )
                // Màn của C: chủ bài bấm nút chính thì mở danh sách yêu cầu xác minh
                else -> DetailScreen(
                    isFound = post.isFound,
                    onVerify = if (isMine) onOpenClaims else onVerify
                )
            }
        }
    }
}

/**
 * Tab "Tin nhắn". Danh sách hội thoại thuộc phần của C (ui/chat), chưa có trong develop.
 * Màn tạm này giữ chỗ cho tab để điều hướng chạy được; C thay bằng danh sách thật.
 */
@Composable
fun MessagesTabScreen(onOpenChat: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Tin nhắn", style = MaterialTheme.typography.headlineSmall)
        EmptyView(
            title = "Danh sách hội thoại đang được làm",
            message = "Cuộc trò chuyện được tạo tự động khi yêu cầu xác minh được chấp nhận."
        )
        OutlinedButton(onClick = onOpenChat, modifier = Modifier.fillMaxWidth()) {
            Text("Xem màn chat mẫu")
        }
    }
}
