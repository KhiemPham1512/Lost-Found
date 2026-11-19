
package com.lostfound.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lostfound.app.data.remote.VerificationRepository
import com.lostfound.app.data.remote.ClaimAnswer

private val VerifyBackground = Color(0xFFD7DBDE)
private val VerifyBlue = Color(0xFF28627A)
private val VerifyWhite = Color(0xFFF5F7F8)
private val VerifyGreen = Color(0xFFD6E9DD)
private val VerifyGreenText = Color(0xFF39845D)
private val VerifyYellow = Color(0xFFF3E7B9)

@Composable
fun VerifyScreen(
    questions: List<String> = emptyList(),
    attemptsLeft: Int = 2,
    onSubmit: (List<String>) -> Unit = {},
    onBack: () -> Unit = {}
) {
    val answers = remember(questions) {
        mutableStateListOf<String>().apply {
            repeat(questions.size) { add("") }
        }
    }

    Scaffold(
        containerColor = VerifyBackground
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onBack,
                    color = VerifyWhite,
                    shape = RoundedCornerShape(9.dp)
                ) {
                    Text(
                        text = "‹",
                        modifier = Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 3.dp
                        ),
                        fontSize = 28.sp,
                        color = VerifyBlue
                    )
                }

                Text(
                    text = "Xác minh quyền sở hữu",
                    modifier = Modifier.padding(start = 12.dp),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF26343B)
                )
            }

            // Post information
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = VerifyWhite,
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .background(VerifyGreen, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ĐT",
                            color = VerifyGreenText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Column(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .weight(1f)
                    ) {
                        Surface(
                            color = VerifyGreen,
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = "FOUND",
                                modifier = Modifier.padding(
                                    horizontal = 9.dp,
                                    vertical = 3.dp
                                ),
                                color = VerifyGreenText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }

                        Text(
                            text = "Nhặt được điện thoại ốp xanh",
                            modifier = Modifier.padding(top = 4.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF35434A)
                        )

                        Text(
                            text = "Bến xe Miền Đông · 3 giờ trước",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Warning and attempts
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = VerifyYellow,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (questions.isEmpty()) {
                        "Bài đăng này không có câu hỏi xác minh. Bạn có thể gửi yêu cầu liên hệ đến chủ bài."
                    } else {
                        "Trả lời chính xác các câu hỏi của người nhặt. Bạn chỉ nhắn tin được sau khi được duyệt. Còn $attemptsLeft lần thử."
                    },
                    modifier = Modifier.padding(13.dp),
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = Color(0xFF796B32)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Questions
            if (questions.isNotEmpty()) {

                questions.forEachIndexed { index, question ->

                    Text(
                        text = "${index + 1}. $question",
                        modifier = Modifier.padding(bottom = 8.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF35434A)
                    )

                    OutlinedTextField(
                        value = answers[index],
                        onValueChange = {
                            answers[index] = it
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 68.dp),
                        placeholder = {
                            Text(
                                text = "Nhập câu trả lời...",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = VerifyWhite,
                            unfocusedContainerColor = VerifyWhite,
                            focusedBorderColor = VerifyBlue,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        minLines = 2
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                Button(
                    onClick = {
                        onSubmit(answers.toList())
                    },
                    enabled = answers.all { it.isNotBlank() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VerifyBlue,
                        disabledContainerColor = Color(0xFF9DB1B9)
                    )
                ) {
                    Text(
                        text = "Gửi câu trả lời",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

            } else {

                Button(
                    onClick = {
                        onSubmit(emptyList())
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VerifyBlue
                    )
                ) {
                    Text(
                        text = "Gửi yêu cầu liên hệ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
