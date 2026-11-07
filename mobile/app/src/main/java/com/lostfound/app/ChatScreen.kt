
package com.lostfound.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ChatBackground = Color(0xFFD7DBDE)
private val ChatBlue = Color(0xFF28627A)
private val ChatWhite = Color(0xFFF5F7F8)
private val ChatGreen = Color(0xFFD6E9DD)
private val ChatGreenText = Color(0xFF39845D)
private val ChatYellow = Color(0xFFF3E7B9)

private data class ChatMessage(
    val message: String,
    val isMine: Boolean
)

@Composable
fun ChatScreen(
    onStartHandover: () -> Unit = {}
) {
    var messageInput by remember { mutableStateOf("") }

    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                "Chào bạn, mình nhặt được điện thoại ở gần quán cà phê.",
                false
            ),
            ChatMessage(
                "Cảm ơn bạn, 5h chiều mình qua được không?",
                true
            ),
            ChatMessage(
                "Được, hẹn bạn ở cổng chính.",
                false
            )
        )
    }

    Scaffold(
        containerColor = ChatBackground
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = { },
                    color = ChatWhite,
                    shape = RoundedCornerShape(9.dp)
                ) {
                    Text(
                        text = "‹",
                        modifier = Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 3.dp
                        ),
                        fontSize = 28.sp,
                        color = ChatBlue
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp)
                ) {
                    Text(
                        text = "Hoàng Nam",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF26343B)
                    )
                    Text(
                        text = "Điện thoại ốp xanh · Đang trò chuyện",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }

                Surface(
                    color = ChatGreen,
                    shape = CircleShape
                ) {
                    Text(
                        text = "3",
                        modifier = Modifier.padding(
                            horizontal = 10.dp,
                            vertical = 6.dp
                        ),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ChatGreenText
                    )
                }
            }

            // Post information
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                color = ChatWhite,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                ChatGreen,
                                RoundedCornerShape(9.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ĐT",
                            color = ChatGreenText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Column(
                        modifier = Modifier
                            .padding(start = 9.dp)
                            .weight(1f)
                    ) {
                        Text(
                            text = "FOUND",
                            color = ChatGreenText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                        Text(
                            text = "Điện thoại ốp xanh",
                            color = Color(0xFF35434A),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Bến xe Miền Đông",
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Progress
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 13.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProgressStep("1", "Xác minh", true)
                ProgressLine()
                ProgressStep("2", "Chat", true)
                ProgressLine()
                ProgressStep("3", "Trao trả", false)
                ProgressLine()
                ProgressStep("4", "Hoàn tất", false)
            }

            // Chat messages
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(messages) { item ->
                    ChatBubble(
                        message = item.message,
                        isMine = item.isMine
                    )
                }
            }

            // Handover reminder
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                color = ChatYellow,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "Chỉ xác nhận khi bạn đã nhận lại đúng món đồ. Khi cả hai xác nhận, bài đăng sẽ hoàn tất.",
                    modifier = Modifier.padding(10.dp),
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    color = Color(0xFF796B32)
                )
            }

            // Handover button
            Button(
                onClick = onStartHandover,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 5.dp)
                    .height(42.dp),
                shape = RoundedCornerShape(9.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ChatBlue
                )
            ) {
                Text(
                    text = "Tôi bước trao trả",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Message input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 12.dp,
                        end = 12.dp,
                        top = 5.dp,
                        bottom = 10.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageInput,
                    onValueChange = { messageInput = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            text = "Nhập tin nhắn...",
                            fontSize = 12.sp
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ChatWhite,
                        unfocusedContainerColor = ChatWhite,
                        focusedBorderColor = ChatBlue,
                        unfocusedBorderColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.width(7.dp))

                Button(
                    onClick = {
                        val content = messageInput.trim()
                        if (content.isNotEmpty()) {
                            messages.add(
                                ChatMessage(
                                    message = content,
                                    isMine = true
                                )
                            )
                            messageInput = ""
                        }
                    },
                    modifier = Modifier.height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ChatBlue
                    )
                ) {
                    Text(
                        text = "Gửi",
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressStep(
    number: String,
    label: String,
    completed: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    if (completed) ChatGreenText else Color(0xFFB9C1C5),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (completed) "✓" else number,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = label,
            modifier = Modifier.padding(top = 3.dp),
            fontSize = 9.sp,
            color = Color(0xFF46545B)
        )
    }
}

@Composable
private fun ProgressLine() {
    Box(
        modifier = Modifier
            .width(17.dp)
            .height(2.dp)
            .background(Color(0xFFB9C1C5))
    )
}

@Composable
private fun ChatBubble(
    message: String,
    isMine: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) {
            Arrangement.End
        } else {
            Arrangement.Start
        }
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 270.dp),
            color = if (isMine) ChatBlue else ChatWhite,
            shape = RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 12.dp,
                bottomStart = if (isMine) 12.dp else 3.dp,
                bottomEnd = if (isMine) 3.dp else 12.dp
            )
        ) {
            Text(
                text = message,
                modifier = Modifier.padding(
                    horizontal = 11.dp,
                    vertical = 9.dp
                ),
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = if (isMine) Color.White else Color(0xFF35434A)
            )
        }
    }
}
