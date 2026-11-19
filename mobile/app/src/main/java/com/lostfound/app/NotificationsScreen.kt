
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

private data class NotificationItem(
    val initials: String,
    val title: String,
    val description: String,
    val time: String,
    val isUnread: Boolean
)

@Composable
fun NotificationsScreen() {
    val newNotifications = remember {
        mutableStateListOf(
            NotificationItem(
                "NA", "Nguyễn An gửi yêu cầu xác minh",
                "Người này muốn nhận lại món đồ bạn đăng.", "5 phút trước", true
            ),
            NotificationItem(
                "TH", "Trần Huy gửi yêu cầu xác minh",
                "Có một yêu cầu mới cho bài đăng của bạn.", "30 phút trước", true
            ),
            NotificationItem(
                "MK", "Yêu cầu xác minh được chấp nhận",
                "Bạn có thể bắt đầu trò chuyện với chủ bài đăng.", "2 giờ trước", true
            ),
            NotificationItem(
                "LH", "Chủ bài xác nhận đã trao trả",
                "Vui lòng xác nhận bạn đã nhận được đồ.", "4 giờ trước", true
            )
        )
    }

    val oldNotifications = listOf(
        NotificationItem(
            "!", "Báo cáo của bạn đã được xử lý",
            "Cảm ơn bạn đã góp phần xây dựng cộng đồng.", "Hôm qua", false
        ),
        NotificationItem(
            "LF", "Bài đăng đã hoàn tất",
            "Bài đăng của bạn đã được đánh dấu DONE.", "2 ngày trước", false
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F7F7))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Thông báo",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF202020)
            )

            TextButton(
                onClick = {
                    for (i in newNotifications.indices) {
                        newNotifications[i] =
                            newNotifications[i].copy(isUnread = false)
                    }
                }
            ) {
                Text(
                    "Đọc hết",
                    color = Color(0xFF2878D0),
                    fontSize = 14.sp
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    "Mới",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(newNotifications) { notification ->
                NotificationCard(notification)
            }

            item {
                Text(
                    "Trước đó",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(
                        top = 16.dp,
                        bottom = 8.dp
                    )
                )
            }

            items(oldNotifications) { notification ->
                NotificationCard(notification)
            }
        }

        BottomNavigationBar()
    }
}

@Composable
private fun NotificationCard(notification: NotificationItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        Color(0xFFE8F1FC),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    notification.initials,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2878D0),
                    fontSize = 13.sp
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 8.dp)
            ) {
                Text(
                    notification.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF222222)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    notification.description,
                    fontSize = 12.sp,
                    color = Color(0xFF777777),
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    notification.time,
                    fontSize = 11.sp,
                    color = Color(0xFF999999)
                )
            }

            if (notification.isUnread) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .background(
                            Color(0xFFE64B4B),
                            CircleShape
                        )
                )
            }
        }
    }
}

@Composable
private fun BottomNavigationBar() {
    Surface(
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem("⌂", "Home")
            BottomNavItem("▤", "Bài của tôi")

            Text(
                "+",
                fontSize = 30.sp,
                fontWeight = FontWeight.Light,
                color = Color(0xFF2878D0)
            )

            BottomNavItem("▢", "Tin nhắn")
            BottomNavItem("○", "Cá nhân")
        }
    }
}

@Composable
private fun BottomNavItem(
    symbol: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            symbol,
            fontSize = 20.sp,
            color = Color(0xFF777777)
        )
        Text(
            label,
            fontSize = 10.sp,
            color = Color(0xFF777777)
        )
    }
}
