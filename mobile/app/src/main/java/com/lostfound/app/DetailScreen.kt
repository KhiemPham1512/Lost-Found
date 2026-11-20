
package com.lostfound.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lostfound.app.data.model.Post
import com.lostfound.app.core.AppConfig
import com.lostfound.app.core.util.Formatters
import com.lostfound.app.ui.components.Avatar
import coil.compose.SubcomposeAsyncImage
import kotlinx.coroutines.launch

private val PageBackground = Color(0xFFD7DBDE)
private val PrimaryBlue = Color(0xFF28627A)
private val CardWhite = Color(0xFFF5F7F8)
private val FoundGreen = Color(0xFFD6E9DD)
private val FoundText = Color(0xFF39845D)
private val LostOrange = Color(0xFFF2DFD2)
private val LostText = Color(0xFFB86B43)

@Composable
fun DetailScreen(
    post: Post,
    isMine: Boolean,
    onVerify: () -> Unit = {}
) {
    val isFound = post.isFound
    val title = post.title

    val category = post.categoryName ?: "Chưa rõ danh mục"
    val location = post.locationText.ifBlank { "Chưa có địa điểm" }
    val eventTime = Formatters.dateTime(post.eventTime).ifBlank { "Chưa có thời gian" }
    val posterName = post.authorName?.takeIf { it.isNotBlank() }
        ?: post.authorUsername ?: "Người đăng"
    val posterHandle = post.authorUsername?.let { "@$it" }.orEmpty()
    val description = post.description?.takeIf { it.isNotBlank() } ?: "Chưa có mô tả"

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = PageBackground,
        bottomBar = {
            Surface(
                modifier = Modifier.navigationBarsPadding(),
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
                        text = if (isMine) "Xem yêu cầu"
                        else if (isFound) "Đây là đồ của tôi"
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

            PostImages(post)

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
                    text = Formatters.timeAgo(post.createdAt),
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
            DetailInfoRow("◷", "${if (isFound) "Nhặt lúc" else "Mất lúc"} $eventTime")
            DetailInfoRow("▣", category)

            // Tags
            FlowRow(
                modifier = Modifier.padding(top = 10.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                post.tags.forEach { DetailTag("#$it") }
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
                    Avatar(post.authorAvatarUrl, posterName, size = 46.dp)

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
                            text = posterHandle,
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
                    text = if (isMine) {
                        "Bạn là chủ bài. Hãy xem và duyệt yêu cầu trước khi mở chat."
                    } else if (isFound) {
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

}

@Composable
private fun PostImages(post: Post) {
    if (post.imagePaths.isEmpty()) {
        Box(
            Modifier.fillMaxWidth().height(205.dp)
                .background(CardWhite, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) { Text("Chưa có ảnh", color = Color.Gray) }
    } else key(post.id) {
        val pager = rememberPagerState { post.imagePaths.size }
        val scope = rememberCoroutineScope()
        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxWidth().height(205.dp).clip(RoundedCornerShape(14.dp))
        ) { index ->
            SubcomposeAsyncImage(
                model = AppConfig.publicImageUrl(post.imagePaths[index]),
                contentDescription = "Ảnh ${index + 1}/${post.imagePaths.size}: ${post.title}",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().background(CardWhite),
                loading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(28.dp)) } },
                error = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Không tải được ảnh", color = Color.Gray) } }
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            if (post.imagePaths.size > 1) IconButton(
                enabled = pager.currentPage > 0,
                onClick = { scope.launch { pager.scrollToPage(pager.currentPage - 1) } }
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Ảnh trước") }
            Text("Ảnh ${pager.currentPage + 1} / ${post.imagePaths.size}", fontSize = 12.sp)
            if (post.imagePaths.size > 1) IconButton(
                enabled = pager.currentPage < post.imagePaths.lastIndex,
                onClick = { scope.launch { pager.scrollToPage(pager.currentPage + 1) } }
            ) { Icon(Icons.AutoMirrored.Filled.ArrowForward, "Ảnh sau") }
        }
    }
    Spacer(Modifier.height(12.dp))
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
