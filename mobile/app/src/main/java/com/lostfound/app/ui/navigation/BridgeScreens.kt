package com.lostfound.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.lostfound.app.DetailScreen
import com.lostfound.app.core.session.SessionStore
import com.lostfound.app.core.util.userMessage
import com.lostfound.app.data.model.Post
import com.lostfound.app.data.model.ReportTarget
import com.lostfound.app.data.repository.PostRepository
import com.lostfound.app.ui.components.EmptyView
import com.lostfound.app.ui.components.ErrorView
import com.lostfound.app.ui.components.LoadingView
import com.lostfound.app.ui.components.TopBar
import kotlinx.coroutines.CancellationException

/**
 * Tải dữ liệu chi tiết theo postId; chủ bài xem yêu cầu, người khác xác minh
 * hoặc báo cáo bằng đúng ID của bài/người đăng.
 */
@Composable
fun PostDetailRoute(
    postId: String,
    onBack: () -> Unit,
    onVerify: () -> Unit,
    onOpenClaims: () -> Unit,
    onReport: (ReportTarget, String, String?) -> Unit
) {
    var state by remember(postId) { mutableStateOf<Result<Post?>?>(null) }
    var reload by remember(postId) { mutableIntStateOf(0) }
    LaunchedEffect(postId, reload) {
        state = null
        state = try {
            Result.success(PostRepository.getPost(postId))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    var menuOpen by remember(postId) { mutableStateOf(false) }
    val post = state?.getOrNull()
    val error = state?.exceptionOrNull()
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
                error != null -> ErrorView(
                    message = error.userMessage(),
                    onRetry = { reload++ }
                )
                post == null -> EmptyView(
                    title = "Không xem được bài đăng",
                    message = "Bài đã bị ẩn, bị gỡ hoặc không còn tồn tại."
                )
                // Màn của C: chủ bài bấm nút chính thì mở danh sách yêu cầu xác minh
                else -> DetailScreen(
                    post = post,
                    isMine = isMine,
                    onVerify = if (isMine) onOpenClaims else onVerify
                )
            }
        }
    }
}
