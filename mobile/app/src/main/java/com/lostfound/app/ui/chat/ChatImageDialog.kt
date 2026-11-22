package com.lostfound.app.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.lostfound.app.core.util.userMessage
import com.lostfound.app.data.repository.ChatMessage
import com.lostfound.app.data.repository.ChatRepository
import com.lostfound.app.ui.components.TopBar
import kotlinx.coroutines.CancellationException

@Composable
fun ChatImageDialog(conversationId: String, image: ChatMessage, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var attempt by remember(image.id) { mutableIntStateOf(0) }
    var model by remember(image.id) { mutableStateOf<ImageRequest?>(null) }
    var error by remember(image.id) { mutableStateOf<String?>(null) }
    var loading by remember(image.id) { mutableStateOf(true) }
    LaunchedEffect(conversationId, image.id, attempt) {
        loading = true
        error = null
        model = null
        try {
            val request = ChatRepository.imageRequest(conversationId, image)
            model = ImageRequest.Builder(context).data(request.url.toString()).headers(request.headers)
                .memoryCachePolicy(CachePolicy.DISABLED).diskCachePolicy(CachePolicy.DISABLED).build()
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) { error = e.userMessage(); loading = false }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                TopBar("Ảnh hội thoại", onDismiss)
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    model?.let { request ->
                        AsyncImage(
                            model = request, contentDescription = "Ảnh trong hội thoại riêng tư",
                            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit,
                            onSuccess = { loading = false },
                            onError = { loading = false; error = "Không tải được ảnh. Kiểm tra kết nối và quyền truy cập." }
                        )
                    }
                    if (loading) CircularProgressIndicator()
                    error?.let { detail ->
                        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(detail, color = MaterialTheme.colorScheme.error)
                            TextButton(onClick = { attempt++ }) { Text("Thử lại") }
                        }
                    }
                }
            }
        }
    }
}
