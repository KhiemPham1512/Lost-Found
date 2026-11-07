
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

private val HandoverBackground = Color(0xFFD7DBDE)
private val HandoverBlue = Color(0xFF28627A)
private val HandoverWhite = Color(0xFFF5F7F8)
private val HandoverGreen = Color(0xFFD6E9DD)
private val HandoverGreenText = Color(0xFF39845D)
private val HandoverYellow = Color(0xFFF3E7B9)
private val HandoverRed = Color(0xFFC65B52)

@Composable
fun HandoverScreen(
    onConfirmReceived: () -> Unit = {},
    onReportProblem: () -> Unit = {}
) {
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showProblemDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = HandoverBackground
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
                    .padding(top = 18.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = { },
                    color = HandoverWhite,
                    shape = RoundedCornerShape(9.dp)
                ) {
                    Text(
                        text = "‹",
                        modifier = Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 3.dp
                        ),
                        fontSize = 28.sp,
                        color = HandoverBlue
                    )
                }

                Text(
                    text = "Xác nhận trao trả",
                    modifier = Modifier.padding(start = 12.dp),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF26343B)
                )
            }

            // Post information
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = HandoverWhite,
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
                                HandoverGreen,
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ĐT",
                            color = HandoverGreenText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(
                        modifier = Modifier
                            .padding(start = 10.dp)
                            .weight(1f)
                    ) {
                        Text(
                            text = "FOUND",
                            color = HandoverGreenText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Điện thoại ốp xanh",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF35434A)
                        )

                        Text(
                            text = "Bến xe Miền Đông",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            // Progress
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 13.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HandoverProgressStep("1", "Xác minh", true)
                HandoverProgressLine()
                HandoverProgressStep("2", "Chat", true)
                HandoverProgressLine()
                HandoverProgressStep("3", "Trao trả", true)
                HandoverProgressLine()
                HandoverProgressStep("4", "Hoàn tất", false)
            }

            // Owner
            HandoverPersonCard(
                initials = "HN",
                name = "Hoàng Nam (người nhặt)",
                subtitle = "Chủ bài đăng",
                confirmed = true
            )

            Spacer(modifier = Modifier.height(9.dp))

            // Claimant
            HandoverPersonCard(
                initials = "MA",
                name = "Bạn (người mất)",
                subtitle = "Người nhận đồ",
                confirmed = false
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Warning
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = HandoverYellow,
                shape = RoundedCornerShape(11.dp)
            ) {
                Text(
                    text = "Chỉ xác nhận khi bạn đã nhận lại đúng tài sản. Khi cả hai cùng xác nhận, bài đăng sẽ chuyển sang trạng thái hoàn tất.",
                    modifier = Modifier.padding(12.dp),
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = Color(0xFF796B32)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Confirm button
            Button(
                onClick = { showConfirmDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(9.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HandoverBlue
                )
            ) {
                Text(
                    text = "Tôi đã nhận lại tài sản",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Problem button
            OutlinedButton(
                onClick = { showProblemDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(9.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    HandoverRed
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = HandoverRed
                )
            ) {
                Text(
                    text = "Chưa nhận được / Có vấn đề",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }

    // Confirmation dialog
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                showConfirmDialog = false
            },
            title = {
                Text("Xác nhận đã nhận tài sản?")
            },
            text = {
                Text(
                    "Bạn xác nhận đã nhận lại đúng món đồ từ người nhặt?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        onConfirmReceived()
                    }
                ) {
                    Text("Xác nhận")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                    }
                ) {
                    Text("Hủy")
                }
            }
        )
    }

    // Problem dialog
    if (showProblemDialog) {
        AlertDialog(
            onDismissRequest = {
                showProblemDialog = false
            },
            title = {
                Text("Có vấn đề khi trao trả?")
            },
            text = {
                Text(
                    "Nếu bạn chưa nhận được tài sản, hãy tiếp tục trao đổi với người còn lại hoặc báo cáo vấn đề."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showProblemDialog = false
                        onReportProblem()
                    }
                ) {
                    Text("Tiếp tục")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showProblemDialog = false
                    }
                ) {
                    Text("Đóng")
                }
            }
        )
    }
}

@Composable
private fun HandoverPersonCard(
    initials: String,
    name: String,
    subtitle: String,
    confirmed: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = HandoverWhite,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
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
                    text = initials,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = HandoverBlue
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 9.dp)
            ) {
                Text(
                    text = name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF35434A)
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }

            Surface(
                color = if (confirmed) HandoverGreen else HandoverYellow,
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = if (confirmed) "✓ Đã xác nhận" else "Chờ xác nhận",
                    modifier = Modifier.padding(
                        horizontal = 8.dp,
                        vertical = 5.dp
                    ),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (confirmed) HandoverGreenText
                    else Color(0xFF8B7433)
                )
            }
        }
    }
}

@Composable
private fun HandoverProgressStep(
    number: String,
    label: String,
    completed: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(23.dp)
                .background(
                    if (completed) HandoverGreenText
                    else Color(0xFFB9C1C5),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (completed) "✓" else number,
                color = Color.White,
                fontSize = 10.sp,
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
private fun HandoverProgressLine() {
    Box(
        modifier = Modifier
            .width(16.dp)
            .height(2.dp)
            .background(Color(0xFFB9C1C5))
    )
}
