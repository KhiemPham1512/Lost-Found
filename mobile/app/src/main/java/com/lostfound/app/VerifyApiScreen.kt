package com.lostfound.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lostfound.app.data.remote.VerificationRepository
import com.lostfound.app.data.remote.ClaimAnswer
import com.lostfound.app.data.remote.QuestionsResponse
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import com.lostfound.app.data.model.Post
import com.lostfound.app.data.repository.PostRepository
import com.lostfound.app.ui.components.TopBar
import com.lostfound.app.ui.components.LoadingView
import com.lostfound.app.data.repository.OwnClaim
import com.lostfound.app.data.repository.OwnClaimRepository
import com.lostfound.app.ui.claims.OwnClaimScreen
import com.lostfound.app.core.network.ApiException
import com.lostfound.app.core.util.userMessage

@Composable
fun VerifyApiScreen(
    postId: String,
    onBack: () -> Unit = {},
    onOpenChat: (String) -> Unit = {}
) {
    val repository = remember { VerificationRepository() }
    val scope = rememberCoroutineScope()
    var post by remember(postId) { mutableStateOf<Post?>(null) }
    var ownClaim by remember(postId) { mutableStateOf<OwnClaim?>(null) }
    var retryForm by remember(postId) { mutableStateOf(false) }

    var response by remember(postId) {
        mutableStateOf<QuestionsResponse?>(null)
    }

    var isLoading by remember(postId) {
        mutableStateOf(true)
    }

    var loadError by remember(postId) {
        mutableStateOf<String?>(null)
    }

    var submitError by remember(postId) {
        mutableStateOf<String?>(null)
    }

    var isSubmitting by remember(postId) {
        mutableStateOf(false)
    }

    var retryKey by remember(postId) {
        mutableStateOf(0)
    }

    LaunchedEffect(postId, retryKey) {
        isLoading = true
        loadError = null
        post = null
        response = null
        try {
            ownClaim = OwnClaimRepository.latest(postId)
            if (ownClaim != null && (!retryForm || !ownClaim!!.canCheckAttempts)) {
                retryForm = false
                isLoading = false
                return@LaunchedEffect
            }
            post = PostRepository.getPost(postId)
                ?: throw IllegalStateException("Không xem được bài đăng này")
            repository.getQuestions(postId).fold(
                onSuccess = {
                    response = it
                },
                onFailure = {
                    if (it is CancellationException) throw it
                    loadError = it.message ?: "Không tải được câu hỏi"
                }
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            loadError = e.message ?: "Không tải được bài đăng"
        }

        isLoading = false
    }

    when {
        isLoading -> {
            Column(Modifier.fillMaxSize()) {
                TopBar("Xác minh quyền sở hữu", onBack)
                LoadingView(message = "Đang tải bài đăng và câu hỏi")
            }
        }

        ownClaim != null && !retryForm -> {
            OwnClaimScreen(ownClaim!!, onBack, onRetry = { retryForm = true; retryKey++ }, onOpenChat = onOpenChat)
        }

        loadError != null -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(loadError ?: "Có lỗi xảy ra")

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = { retryKey++ }) {
                    Text("Thử lại")
                }

                TextButton(onClick = onBack) {
                    Text("Quay lại")
                }
                if (ownClaim != null) TextButton(onClick = { retryForm = false }) { Text("Xem yêu cầu đã gửi") }
            }
        }

        response != null && post != null -> {
            val currentResponse = response!!
            val orderedQuestions =
                currentResponse.questions.sortedBy { it.position }

            VerifyScreen(
                post = post!!,
                questions = orderedQuestions.map { it.questionText },
                attemptsLeft = currentResponse.attemptsLeft,
                submitting = isSubmitting,
                onBack = onBack,
                onSubmit = { answerTexts ->

                    if (!isSubmitting) {
                        isSubmitting = true
                        val answers = answerTexts.mapIndexedNotNull {
                                index, answer ->
                            orderedQuestions.getOrNull(index)?.let { question ->
                                ClaimAnswer(
                                    questionId = question.id,
                                    answerText = answer.trim()
                                )
                            }
                        }

                        scope.launch {
                            submitError = null
                            val previous = ownClaim?.id
                            try {
                                val sent = OwnClaimRepository.submit(postId, answers)
                                ownClaim = OwnClaim(sent.claimId, postId, sent.status, post?.title)
                                retryForm = false
                            } catch (e: CancellationException) { throw e
                            } catch (e: Exception) {
                                submitError = if (e is ApiException && e.status in 400..499) e.userMessage()
                                    else "Chưa xác nhận được yêu cầu đã gửi. Kiểm tra trạng thái trước khi gửi lại. ${e.userMessage()}"
                                try {
                                    val saved = OwnClaimRepository.latest(postId)
                                    if (saved != null && (saved.id != previous || saved.status in setOf("SUBMITTED", "PASSED"))) {
                                        ownClaim = saved
                                        retryForm = false
                                        submitError = null
                                    }
                                } catch (c: CancellationException) { throw c
                                } catch (_: Exception) { /* Giữ biểu mẫu và lỗi, không tự gửi lại. */ }
                            } finally { isSubmitting = false }
                        }
                    }
                }
            )

            if (submitError != null) {
                AlertDialog(
                    onDismissRequest = {
                        submitError = null
                    },
                    title = {
                        Text("Không thể gửi yêu cầu")
                    },
                    text = {
                        Text(submitError ?: "")
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                submitError = null
                            }
                        ) {
                            Text("Đóng")
                        }
                    }
                )
            }
        }
    }
}
