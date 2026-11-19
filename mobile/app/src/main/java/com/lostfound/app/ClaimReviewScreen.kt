package com.lostfound.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ReviewBackground = Color(0xFFD7DBDE)
private val ReviewBlue = Color(0xFF28627A)
private val ReviewWhite = Color(0xFFF5F7F8)
private val ReviewGreen = Color(0xFFD6E9DD)
private val ReviewGreenText = Color(0xFF39845D)
private val ReviewYellow = Color(0xFFF3E7B9)
private val ReviewAnswerBackground = Color(0xFFD4D9DC)
private val ReviewRed = Color(0xFFC65B52)

data class ClaimAnswerUi(
    val question: String,
    val answer: String
)

data class ClaimReviewUi(
    val username: String,
    val avatar: String,
    val submittedTime: String,
    val answers: List<ClaimAnswerUi>
)

@Composable
fun ClaimReviewScreen(
    claims: List<ClaimReviewUi> = listOf(
        ClaimReviewUi(
            username = "thulan_k",
            avatar = "TL",
            submittedTime = "Gửi 20 phút trước",
            answers = listOf(
                ClaimAnswerUi(
                    question = "Màu ốp lưng?",
                    answer = "Xanh dương, có hình cánh bướm"
                ),
                ClaimAnswerUi(
                    question = "Hình nền màn hình?",
                    answer = "Ảnh con mèo"
                )
            )
        ),
        ClaimReviewUi(
            username = "vp_2001",
            avatar = "VP",
            submittedTime = "Gửi 1 giờ trước",
            answers = listOf(
                ClaimAnswerUi(
                    question = "Màu ốp lưng?",
                    answer = "Đen"
                ),
                ClaimAnswerUi(
                    question = "Hình nền màn hình?",
                    answer = "Không nhớ"
                )
            )
        )
    ),
    onDecision: (username: String, decision: String) -> Unit = { _, _ -> }
) {
    Scaffold(
        containerColor = ReviewBackground
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp)
        ) {

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = { },
                    color = ReviewWhite,
                    shape = RoundedCornerShape(9.dp)
                ) {
                    Text(
                        text = "‹",
                        modifier = Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 3.dp
                        ),
                        fontSize = 28.sp,
                        color = ReviewBlue
                    )
                }

                Text(
                    text = "Yêu cầu xác minh",
                    modifier = Modifier.padding(start = 12.dp),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF26343B)
                )
            }

            // Post information
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = ReviewWhite,
                shape = RoundedCornerShape(13.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                ReviewGreen,
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ĐT",
                            color = ReviewGreenText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(
                        modifier = Modifier
                            .padding(start = 10.dp)
                            .weight(1f)
                    ) {
                        Surface(
                            color = ReviewGreen,
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = "FOUND",
                                modifier = Modifier.padding(
                                    horizontal = 8.dp,
                                    vertical = 2.dp
                                ),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = ReviewGreenText
                            )
                        }

                        Text(
                            text = "Điện thoại ốp xanh",
                            modifier = Modifier.padding(top = 3.dp),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF35434A)
                        )

                        Text(
                            text = "${claims.size} yêu cầu chờ duyệt",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Warning
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = ReviewYellow,
                shape = RoundedCornerShape(11.dp)
            ) {
                Text(
                    text = "So sánh với thông tin bạn biết. Chỉ cho nhắn tin khi câu trả lời khớp.",
                    modifier = Modifier.padding(12.dp),
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = Color(0xFF796B32)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Claim cards
            claims.forEach { claim ->

                ClaimReviewCard(
                    claim = claim,
                    onReject = {
                        onDecision(claim.username, "FAIL")
                    },
                    onApprove = {
                        onDecision(claim.username, "PASS")
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ClaimReviewCard(
    claim: ClaimReviewUi,
    onReject: () -> Unit,
    onApprove: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ReviewWhite,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(11.dp)
        ) {

            // Claimant information
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            Color(0xFFDCE9ED),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = claim.avatar,
                        color = ReviewBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(
                    modifier = Modifier.padding(start = 9.dp)
                ) {
                    Text(
                        text = claim.username,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF35434A)
                    )

                    Text(
                        text = claim.submittedTime,
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(9.dp))

            // Questions and answers
            claim.answers.forEach { answer ->

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 7.dp),
                    color = ReviewAnswerBackground,
                    shape = RoundedCornerShape(9.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(
                            horizontal = 10.dp,
                            vertical = 8.dp
                        )
                    ) {
                        Text(
                            text = answer.question,
                            fontSize = 10.sp,
                            color = Color(0xFF647078)
                        )

                        Text(
                            text = answer.answer,
                            modifier = Modifier.padding(top = 3.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF26343B)
                        )
                    }
                }
            }

            // Decision buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier
                        .weight(1f)
                        .height(43.dp),
                    shape = RoundedCornerShape(9.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        ReviewRed
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = ReviewRed
                    ),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text(
                        text = "Bỏ qua",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onApprove,
                    modifier = Modifier
                        .weight(1f)
                        .height(43.dp),
                    shape = RoundedCornerShape(9.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ReviewBlue
                    ),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text(
                        text = "Xác nhận – mở chat",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
