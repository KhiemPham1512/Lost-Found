package com.lostfound.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.lostfound.app.core.image.ImageCompressor
import com.lostfound.app.core.session.SessionStore
import com.lostfound.app.core.network.ApiException
import com.lostfound.app.core.util.Formatters
import com.lostfound.app.core.util.userMessage
import com.lostfound.app.data.repository.ChatMessage
import com.lostfound.app.data.repository.ChatRepository
import com.lostfound.app.data.repository.Conversation
import com.lostfound.app.data.repository.MessageHistory
import com.lostfound.app.ui.components.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle

@Composable
fun ChatScreen(conversationId: String, onBack: () -> Unit, onReport: (String, String?) -> Unit, onOpenHandover: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var chat by remember(conversationId) { mutableStateOf<Conversation?>(null) }
    var messages by remember(conversationId) { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var busy by remember(conversationId) { mutableStateOf(false) }
    var error by remember(conversationId) { mutableStateOf<String?>(null) }
    var older by remember(conversationId) { mutableStateOf(false) }
    var draft by rememberSaveable(conversationId) { mutableStateOf("") }
    var sending by remember(conversationId) { mutableStateOf(false) }
    var sendError by remember(conversationId) { mutableStateOf<String?>(null) }
    var readError by remember(conversationId) { mutableStateOf<String?>(null) }
    var selectedImage by remember(conversationId) { mutableStateOf<Uri?>(null) }
    var viewingImage by remember(conversationId) { mutableStateOf<ChatMessage?>(null) }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (!sending) { selectedImage = uri; sendError = null }
    }
    suspend fun load(before: Long? = null) {
        if (busy) return
        busy = true
        error = null
        try {
            chat = ChatRepository.get(conversationId)
            val history = ChatRepository.history(conversationId, MessageHistory(messages, older), before)
            messages = history.items
            older = history.hasOlder
            try {
                ChatRepository.markRead(conversationId, history.newlyLoaded)
                readError = null
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) { readError = "Chưa đánh dấu đã đọc: ${e.userMessage()}" }
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) { error = e.userMessage()
        } finally { busy = false }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    // shortcut: đọc lại mỗi 5 giây khi mở chat; dùng Realtime khi cần giảm độ trễ hoặc số request.
    LaunchedEffect(conversationId, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                load()
                delay(5_000)
            }
        }
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        TopBar(chat?.peerName ?: "Hội thoại", onBack) {
            if (chat != null) TextButton(onClick = { onReport(conversationId, chat?.postTitle) }) { Text("Báo cáo") }
        }
        when {
            error != null -> ErrorView(error!!, { scope.launch { load() } }, Modifier.weight(1f))
            chat == null -> LoadingView(Modifier.weight(1f))
            else -> {
                Text(chat!!.postTitle ?: "Bài đăng không còn xem được", Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.titleMedium)
                chat!!.peerUsername?.let { Text("@$it", Modifier.padding(horizontal = 16.dp)) }
                if (chat!!.locked) Text("Hội thoại đã khóa · Chỉ đọc", Modifier.padding(16.dp))
                OutlinedButton(onClick = onOpenHandover, modifier = Modifier.padding(horizontal = 16.dp)) { Text("Xác nhận trao trả") }
                TextButton(onClick = { scope.launch { load() } }, enabled = !busy) { Text("Tải lại") }
                readError?.let { Text(it, Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.error) }
                LazyColumn(Modifier.weight(1f), reverseLayout = true, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (messages.isEmpty()) item { Text("Chưa có tin nhắn") }
                    items(messages, key = { it.id }) { message ->
                        val mine = message.senderId == SessionStore.userId
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
                            Surface(
                                modifier = Modifier.widthIn(max = 300.dp),
                                shape = MaterialTheme.shapes.medium,
                                color = if (mine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                            ) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(if (message.type == "SYSTEM") "Hệ thống" else if (mine) "Bạn" else chat!!.peerName, style = MaterialTheme.typography.labelSmall)
                                    if (message.type == "IMAGE") {
                                        TextButton(onClick = { viewingImage = message }) { Text("Xem ảnh riêng tư") }
                                    } else Text(message.content.orEmpty())
                                    Text(Formatters.dateTime(message.createdAt), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                    if (older) item {
                        OutlinedButton(onClick = { scope.launch { load(messages.lastOrNull()?.id) } }, enabled = !busy) {
                            Text("Tin nhắn cũ hơn")
                        }
                    }
                }
                sendError?.let { Text(it, Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.error) }
                if (!chat!!.locked) {
                    val content = draft.trim()
                    val count = content.codePointCount(0, content.length)
                    TextButton(onClick = {
                        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }, enabled = !sending) { Text("Chọn ảnh") }
                    selectedImage?.let { uri ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(uri)
                                    .memoryCachePolicy(CachePolicy.DISABLED).diskCachePolicy(CachePolicy.DISABLED).build(),
                                contentDescription = "Ảnh đã chọn, chưa gửi",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(88.dp)
                            )
                            TextButton(onClick = { selectedImage = null }, enabled = !sending) { Text("Bỏ ảnh") }
                        }
                    }
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        label = { Text("Tin nhắn") },
                        supportingText = { Text("$count / 2000") },
                        isError = count > 2000,
                        enabled = !sending,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    )
                    Button(
                        onClick = {
                            if (!sending && (selectedImage != null || count in 1..2000)) {
                                val image = selectedImage
                                sending = true
                                sendError = null
                                scope.launch {
                                    try {
                                        val sent = if (image == null) ChatRepository.send(conversationId, content)
                                            else ChatRepository.sendImage(conversationId, ImageCompressor.compress(context, image))
                                        // Quay về cửa sổ mới nhất; tin gửi có thể cách lịch sử cũ hơn 50 mục.
                                        messages = listOf(sent)
                                        older = true
                                        if (image == null) draft = "" else selectedImage = null
                                        load()
                                    } catch (e: CancellationException) { throw e
                                    } catch (e: Exception) {
                                        sendError = if (e is ApiException && e.status in 400..499)
                                            "Không gửi được: ${e.userMessage()}"
                                        else "Chưa xác nhận được tin đã gửi. Kiểm tra lịch sử trước khi gửi lại. ${e.userMessage()}"
                                        load()
                                    } finally { sending = false }
                                }
                            }
                        },
                        enabled = !sending && (selectedImage != null || count in 1..2000),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                    ) { Text(if (sending) "Đang gửi…" else if (selectedImage != null) "Gửi ảnh" else "Gửi") }
                }
            }
        }
    }
    viewingImage?.let { image ->
        com.lostfound.app.ui.chat.ChatImageDialog(conversationId, image, onDismiss = { viewingImage = null })
    }
}
