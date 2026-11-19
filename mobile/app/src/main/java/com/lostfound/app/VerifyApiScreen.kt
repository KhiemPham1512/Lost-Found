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

@Composable
fun VerifyApiScreen(
    postId: String,
    onBack: () -> Unit = {},
    onSubmitSuccess: (String, String) -> Unit = { _, _ -> }
) {
    val repository = remember { VerificationRepository() }
    val scope = rememberCoroutineScope()

    var response by remember(postId) {
        mutableStateOf<QuestionsResponse?>(null)
    }

    var isLoading by remember(postId) {
        mutableStateOf(true)
    }

    var loadError by remember(postId) {
        mutableStateOf<String?>(null)
    }

    var submitError by remember {
        mutableStateOf<String?>(null)
    }

    var isSubmitting by remember {
        mutableStateOf(false)
    }

    var retryKey by remember(postId) {
        mutableStateOf(0)
    }

    LaunchedEffect(postId, retryKey) {
        isLoading = true
        loadError = null

        repository.getQuestions(postId).fold(
            onSuccess = {
                response = it
            },
            onFailure = {
                loadError = it.message ?: "Không tải được câu hỏi"
            }
        )

        isLoading = false
    }

    when {
        isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
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
            }
        }

        response != null -> {
            val currentResponse = response!!
            val orderedQuestions =
                currentResponse.questions.sortedBy { it.position }

            VerifyScreen(
                questions = orderedQuestions.map { it.questionText },
                attemptsLeft = currentResponse.attemptsLeft,
                onBack = onBack,
                onSubmit = { answerTexts ->

                    if (!isSubmitting) {
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
                            isSubmitting = true
                            submitError = null

                            repository.submitClaim(
                                postId = postId,
                                answers = answers
                            ).fold(
                                onSuccess = { claim ->
                                    onSubmitSuccess(
                                        claim.claimId,
                                        claim.status
                                    )
                                },
                                onFailure = {
                                    submitError =
                                        it.message ?: "Gửi yêu cầu thất bại"
                                }
                            )

                            isSubmitting = false
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
