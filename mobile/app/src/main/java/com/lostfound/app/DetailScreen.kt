
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PageBackground = Color(0xFFD7DBDE)
private val PrimaryBlue = Color(0xFF28627A)
private val CardWhite = Color(0xFFF5F7F8)
private val FoundGreen = Color(0xFFD6E9DD)
private val FoundText = Color(0xFF39845D)
private val LostOrange = Color(0xFFF2DFD2)
private val LostText = Color(0xFFB86B43)

@Composable
fun DetailScreen(
    isFound: Boolean = true,
    onVerify: () -> Unit = {}
) {
    val title = if (isFound) {
        "Nhặt được điện thoại ốp xanh"
    } else {
        "Mất ví da màu nâu"
    }

    val category = if (isFound) "Điện thoại" else "Ví/Túi"
    val location = if (isFound) {
        "Bến xe Miền Đông, Bình Thạnh"
    } else {
        "Quận 1, TP.HCM"
    }
    val eventTime = if (isFound) {
        "Nhặt lúc 09:30 hôm nay"
    } else {
        "Mất lúc 08:30 hôm nay"
    }
    val posterName = if (isFound) "Hoàng Nam" else "Thu Lan"
    val posterHandle = if (isFound) "@hoang_nam" else "@thulan_k"
    val avatar = if (isFound) "HN" else "TL"
    val description = if (isFound) {
        "Mình nhặt được điện thoại ở ghế chờ bến xe. Chủ máy vui lòng trả lời các câu hỏi xác minh để mình trả lại."
    } else {
        "Ví da màu nâu, bên trong có CCCD và vài tấm thẻ. Mất khoảng 8h30 gần công viên. Ai nhặt được xin liên hệ giúp mình."
    }

    var showOptions by remember { mutableStateOf(false) }
    var selectedImage by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = PageBackground,
        bottomBar = {
            Surface(
                color = CardWhite,
                shadowElevation = 8.dp
            ) {
                Button(
                    onClick = {
                            onVerify()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue
                    )
                ) {
                    Text(
                        text = if (isFound) "Đây là đồ của tôi"
                        else "Tôi nhặt được món này",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
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
                    .padding(top = 12.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = { },
                    shape = RoundedCornerShape(10.dp),
                    color = CardWhite
                ) {
                    Text(
                        text = "‹",
                        modifier = Modifier.padding(
                            horizontal = 13.dp,
                            vertical = 3.dp
                        ),
                        fontSize = 30.sp,
                        color = PrimaryBlue
                    )
                }

                Text(
                    text = "Chi tiết bài đăng",
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF26343B)
                )

                Surface(
                    onClick = { showOptions = true },
                    shape = RoundedCornerShape(10.dp),
                    color = CardWhite
                ) {
                    Text(
                        text = "⋯",
                        modifier = Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 5.dp
                        ),
                        fontSize = 23.sp,
                        color = Color.DarkGray
                    )
                }
            }

            // Image placeholder
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(205.dp)
                    .background(
                        color = if (isFound) FoundGreen else LostOrange,
                        shape = RoundedCornerShape(14.dp)
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (isFound) "Điện thoại ốp xanh" else "Ví da màu nâu",
                    color = if (isFound) FoundText else LostText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = "Ảnh ${selectedImage + 1} / 3 · vuốt để xem thêm",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            // Image indicators
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (selectedImage == index) 8.dp else 6.dp)
                            .background(
                                color = if (selectedImage == index)
                                    PrimaryBlue else Color.LightGray,
                                shape = CircleShape
                            )
                    )
                }
            }

            // Type and posting time
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = if (isFound) FoundGreen else LostOrange,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = if (isFound) "FOUND · NHẶT ĐƯỢC" else "LOST · MẤT ĐỒ",
                        modifier = Modifier.padding(
                            horizontal = 10.dp,
                            vertical = 5.dp
                        ),
                        color = if (isFound) FoundText else LostText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = if (isFound) "Đăng 3 giờ trước" else "Đăng 15 phút trước",
                    modifier = Modifier.padding(start = 10.dp),
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            // Title
            Text(
                text = title,
                modifier = Modifier.padding(top = 10.dp, bottom = 8.dp),
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF26343B)
            )

            // Information
            DetailInfoRow("📍", location)
            DetailInfoRow("◷", eventTime)
            DetailInfoRow("▣", category)

            // Tags
            Row(
                modifier = Modifier.padding(top = 10.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DetailTag(if (isFound) "#iPhone" else "#ví da")
                DetailTag(if (isFound) "#ốp xanh" else "#CCCD")
            }

            // Description card
            DetailCard {
                Text(
                    text = "Mô tả",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF35434A)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = description,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = Color(0xFF46545B)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Poster information
            DetailCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(
                                Color(0xFFDCE9ED),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = avatar,
                            color = PrimaryBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .weight(1f)
                    ) {
                        Text(
                            text = posterName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF35434A)
                        )
                        Text(
                            text = "$posterHandle · ${if (isFound) "12" else "4"} bài đăng",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Safety notice
            Surface(
                color = Color(0xFFD5E7ED),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (isFound) {
                        "🔒 Bài này có câu hỏi xác minh (không công khai). Bạn cần trả lời trước khi nhắn tin cho người nhặt."
                    } else {
                        "Không chuyển tiền hay gửi số điện thoại. Hãy trao đổi trong chat của app."
                    },
                    modifier = Modifier.padding(12.dp),
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = Color(0xFF416675)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Options menu
    if (showOptions) {
        AlertDialog(
            onDismissRequest = { showOptions = false },
            title = {
                Text("Tùy chọn bài đăng")
            },
            text = {
                Text(
                    "Báo cáo bài đăng\n" +
                            "Spam, lừa đảo, thông tin sai, đòi tiền..."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showOptions = false }
                ) {
                    Text("Đóng")
                }
            }
        )
    }
}

@Composable
private fun DetailInfoRow(
    icon: String,
    text: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, fontSize = 15.sp)
        Text(
            text = text,
            modifier = Modifier.padding(start = 9.dp),
            fontSize = 13.sp,
            color = Color(0xFF46545B)
        )
    }
}

@Composable
private fun DetailTag(text: String) {
    Surface(
        color = CardWhite,
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(
                horizontal = 13.dp,
                vertical = 7.dp
            ),
            fontSize = 12.sp,
            color = Color(0xFF46545B)
        )
    }
}

@Composable
private fun DetailCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CardWhite,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(13.dp),
            content = content
        )
    }
}
