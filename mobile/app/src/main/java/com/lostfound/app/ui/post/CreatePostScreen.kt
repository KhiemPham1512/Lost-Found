package com.lostfound.app.ui.post

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.lostfound.app.core.location.LocationHelper
import com.lostfound.app.core.util.Formatters
import com.lostfound.app.ui.components.FormError
import com.lostfound.app.ui.components.FormField
import com.lostfound.app.ui.components.PrimaryButton
import com.lostfound.app.ui.components.TopBar
import com.lostfound.app.ui.feed.rememberLocationRequester
import com.lostfound.app.ui.home.ChoiceChip
import com.lostfound.app.ui.theme.ErrorRed
import com.lostfound.app.ui.theme.FoundGreen
import com.lostfound.app.ui.theme.FoundText
import com.lostfound.app.ui.theme.LostOrange
import com.lostfound.app.ui.theme.LostText
import com.lostfound.app.ui.theme.TextSecondary
import com.lostfound.app.ui.theme.WarningAmber
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    onBack: () -> Unit,
    onCreated: (String) -> Unit,
    vm: CreatePostViewModel = viewModel()
) {
    val s by vm.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }

    LaunchedEffect(s.createdPostId) { s.createdPostId?.let(onCreated) }

    // Photo Picker của Android: không cần xin quyền đọc bộ nhớ
    val pickImages = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(MAX_IMAGES)
    ) { uris -> if (uris.isNotEmpty()) vm.addImages(uris) }

    val requestLocation = rememberLocationRequester { point ->
        scope.launch {
            val address = point?.let { LocationHelper.addressOf(context, it) }
            vm.setCoordinates(point, address)
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopBar(title = "Đăng bài", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ---- Loại bài ----
            Section("Bạn muốn đăng gì?") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TypeOption(
                        title = "Tôi bị mất",
                        subtitle = "Bài Lost",
                        selected = !s.isFound,
                        bg = LostOrange, fg = LostText,
                        modifier = Modifier.weight(1f)
                    ) { vm.setType("LOST") }
                    TypeOption(
                        title = "Tôi nhặt được",
                        subtitle = "Bài Found",
                        selected = s.isFound,
                        bg = FoundGreen, fg = FoundText,
                        modifier = Modifier.weight(1f)
                    ) { vm.setType("FOUND") }
                }
            }

            // ---- Ảnh ----
            Section("Ảnh (tối đa $MAX_IMAGES)") {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    s.images.forEach { uri ->
                        Box(Modifier.size(88.dp)) {
                            AsyncImage(
                                model = uri,
                                contentDescription = "Ảnh đã chọn",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(88.dp).clip(RoundedCornerShape(12.dp))
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.55f))
                                    .clickable(enabled = !s.submitting) { vm.removeImage(uri) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Bỏ ảnh", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    if (s.images.size < MAX_IMAGES) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                .clickable(enabled = !s.submitting) {
                                    pickImages.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text("${s.images.size}/$MAX_IMAGES", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                        }
                    }
                }
            }

            // ---- Thông tin chính ----
            FormField(
                value = s.title,
                onValueChange = vm::setTitle,
                label = "Tiêu đề",
                placeholder = if (s.isFound) "Nhặt được ví da màu nâu" else "Mất ví da màu nâu",
                error = s.errors["title"],
                maxLength = 120
            )

            Section("Danh mục", error = s.errors["category"]) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (s.categories.isEmpty()) {
                        TextButton(onClick = vm::loadCategories) { Text("Tải danh mục") }
                    }
                    s.categories.forEach { c ->
                        ChoiceChip(c.name, s.categoryId == c.id) { vm.setCategory(c.id) }
                    }
                }
            }

            Section("Tag (giúp tìm kiếm dễ hơn, tối đa $MAX_TAGS)", error = s.errors["tags"]) {
                OutlinedTextField(
                    value = s.tagInput,
                    onValueChange = vm::onTagInput,
                    placeholder = { Text("Ví dụ: màu nâu, da") },
                    singleLine = true,
                    trailingIcon = {
                        TextButton(onClick = { vm.addTag() }, enabled = s.tagInput.isNotBlank()) { Text("Thêm") }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                if (s.tags.isNotEmpty()) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        s.tags.forEach { tag ->
                            InputChip(
                                selected = true,
                                onClick = { vm.removeTag(tag) },
                                label = { Text("#$tag") },
                                trailingIcon = { Icon(Icons.Filled.Close, contentDescription = "Bỏ tag") }
                            )
                        }
                    }
                }
            }

            // ---- Địa điểm ----
            Section(if (s.isFound) "Nơi nhặt được" else "Nơi bị mất") {
                FormField(
                    value = s.locationText,
                    onValueChange = vm::setLocationText,
                    label = "Tên địa điểm",
                    placeholder = "Căn tin UTH, Quận Bình Thạnh",
                    error = s.errors["location"],
                    maxLength = 200
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = {
                            vm.setLocating(true)
                            requestLocation()
                        },
                        enabled = !s.locating && !s.submitting
                    ) {
                        Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (s.locating) "Đang lấy vị trí…" else "Dùng vị trí hiện tại")
                    }
                    if (s.location != null) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Đã gắn tọa độ",
                            color = FoundText,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = vm::clearCoordinates) {
                            Icon(Icons.Filled.Close, contentDescription = "Bỏ tọa độ")
                        }
                    }
                }
                Text(
                    "Có tọa độ thì bài hiện khoảng cách và lên đầu khi người khác sắp xếp Gần nhất.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            // ---- Thời gian ----
            Section(if (s.isFound) "Thời điểm nhặt được" else "Thời điểm bị mất", error = s.errors["time"]) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showDate = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(s.eventTime.toLocalDate().let { "%02d/%02d/%d".format(it.dayOfMonth, it.monthValue, it.year) })
                    }
                    OutlinedButton(onClick = { showTime = true }, modifier = Modifier.weight(1f)) {
                        Text("%02d:%02d".format(s.eventTime.hour, s.eventTime.minute))
                    }
                }
            }

            // ---- Mô tả ----
            FormField(
                value = s.description,
                onValueChange = vm::setDescription,
                label = "Mô tả (không bắt buộc)",
                singleLine = false,
                minLines = 3,
                maxLength = 2000,
                error = s.errors["description"]
            )
            Hint(
                "Không ghi chi tiết nhận dạng (số tiền trong ví, hình nền điện thoại…) vào mô tả. " +
                    "Hãy dùng chúng làm câu hỏi xác minh bên dưới."
            )

            // ---- Câu hỏi xác minh ----
            Section(
                if (s.isFound) "Câu hỏi xác minh (bắt buộc 1–3 câu)" else "Câu hỏi xác minh (không bắt buộc, tối đa 3 câu)",
                error = s.errors["questions"]
            ) {
                Text(
                    if (s.isFound) "Người nhận là chủ đồ phải trả lời đúng thì bạn mới cho phép nhắn tin."
                    else "Người báo đã nhặt được đồ sẽ trả lời; bạn vẫn luôn duyệt trước khi mở chat.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                s.questions.forEachIndexed { index, q ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = q,
                            onValueChange = { if (it.length <= 300) vm.setQuestion(index, it) },
                            label = { Text("Câu hỏi ${index + 1}") },
                            placeholder = { Text("Ví dụ: Mặt trong ví có họa tiết gì?") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        val canRemove = !(s.isFound && s.questions.size == 1)
                        if (canRemove) {
                            IconButton(onClick = { vm.removeQuestion(index) }) {
                                Icon(Icons.Filled.Close, contentDescription = "Xóa câu hỏi")
                            }
                        }
                    }
                }
                if (s.questions.size < MAX_QUESTIONS) {
                    TextButton(onClick = vm::addQuestion) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Thêm câu hỏi")
                    }
                }
            }

            FormError(s.error)
            s.progress?.let {
                Text(it, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            PrimaryButton(
                text = "Đăng bài",
                onClick = { vm.submit(context) },
                loading = s.submitting
            )
            Spacer(Modifier.height(16.dp))
        }
    }

    // ---- Hộp chọn ngày ----
    if (showDate) {
        val zone = ZoneId.systemDefault()
        val todayUtcMillis = LocalDate.now(zone).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = s.eventTime.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= todayUtcMillis
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { millis ->
                        val day = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        vm.setEventTime(s.eventTime.with(day))
                    }
                    showDate = false
                }) { Text("Chọn") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Hủy") } }
        ) {
            DatePicker(state = dateState)
        }
    }

    // ---- Hộp chọn giờ ----
    if (showTime) {
        val timeState = rememberTimePickerState(
            initialHour = s.eventTime.hour,
            initialMinute = s.eventTime.minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTime = false },
            confirmButton = {
                TextButton(onClick = {
                    vm.setEventTime(s.eventTime.withHour(timeState.hour).withMinute(timeState.minute))
                    showTime = false
                }) { Text("Chọn") }
            },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text("Hủy") } },
            text = { TimePicker(state = timeState) }
        )
    }
}

@Composable
private fun Section(
    title: String,
    error: String? = null,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        content()
        if (error != null) {
            Text(error, color = ErrorRed, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun TypeOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    bg: Color,
    fg: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) bg else MaterialTheme.colorScheme.surfaceContainer)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) fg else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Text(title, fontWeight = FontWeight.Bold, color = if (selected) fg else MaterialTheme.colorScheme.onSurface)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
}

@Composable
private fun Hint(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(WarningAmber.copy(alpha = 0.12f))
            .padding(10.dp)
    ) {
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}
