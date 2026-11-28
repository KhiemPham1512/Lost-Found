package com.lostfound.app.ui.report

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lostfound.app.core.network.ApiException
import com.lostfound.app.core.util.Formatters
import com.lostfound.app.core.util.userMessage
import com.lostfound.app.data.model.ReportReason
import com.lostfound.app.data.model.ReportTarget
import com.lostfound.app.data.model.SentReport
import com.lostfound.app.data.repository.ReportRepository
import com.lostfound.app.ui.components.EmptyView
import com.lostfound.app.ui.components.ErrorView
import com.lostfound.app.ui.components.FormError
import com.lostfound.app.ui.components.FormField
import com.lostfound.app.ui.components.LoadingView
import com.lostfound.app.ui.components.PrimaryButton
import com.lostfound.app.ui.components.SmallChip
import com.lostfound.app.ui.components.TopBar
import com.lostfound.app.ui.theme.ErrorRed
import com.lostfound.app.ui.theme.FoundGreen
import com.lostfound.app.ui.theme.FoundText
import com.lostfound.app.ui.theme.LostOrange
import com.lostfound.app.ui.theme.LostText
import com.lostfound.app.ui.theme.TextSecondary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// =====================================================================
// Form báo cáo (mở từ menu ba chấm của thẻ bài, chi tiết bài, hoặc màn Trao trả)
// =====================================================================

data class ReportFormState(
    val reason: ReportReason? = null,
    val description: String = "",
    val reasonError: String? = null,
    val error: String? = null,
    val sending: Boolean = false,
    val submissionUncertain: Boolean = false,
    val sentCode: String? = null
)

class ReportViewModel : ViewModel() {
    private val _state = MutableStateFlow(ReportFormState())
    val state = _state.asStateFlow()
    private var initialized = false

    fun init(reason: ReportReason?) {
        if (initialized) return
        initialized = true
        if (reason != null) _state.update { it.copy(reason = reason) }
    }

    fun setReason(r: ReportReason) = _state.update { it.copy(reason = r, reasonError = null, error = null) }
    fun setDescription(v: String) = _state.update { it.copy(description = v, error = null) }

    fun send(target: ReportTarget, targetId: String) {
        val s = _state.value
        val reason = s.reason ?: run {
            _state.update { it.copy(reasonError = "Chọn một lý do") }
            return
        }
        if (reason == ReportReason.OTHER && s.description.isBlank()) {
            _state.update { it.copy(error = "Vui lòng mô tả vấn đề khi chọn lý do Khác") }
            return
        }
        _state.update { it.copy(sending = true, error = null, submissionUncertain = false) }
        viewModelScope.launch {
            try {
                val code = ReportRepository.send(target, targetId, reason, s.description)
                _state.update { it.copy(sending = false, sentCode = code.ifBlank { "đã gửi" }) }
            } catch (e: ApiException) {
                val msg = when (e.status) {
                    409 -> "Bạn đã báo cáo mục này và báo cáo vẫn đang chờ xử lý."
                    429 -> "Bạn đã gửi tối đa 10 báo cáo trong 24 giờ. Vui lòng thử lại sau."
                    else -> e.userMessage()
                }
                _state.update {
                    it.copy(
                        sending = false,
                        error = msg,
                        submissionUncertain = e.status == 0 || e.status == 408 || e.status in 500..599
                    )
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(sending = false, error = e.userMessage(), submissionUncertain = true) }
            }
        }
    }
}

@Composable
fun ReportScreen(
    target: ReportTarget,
    targetId: String,
    initialReason: ReportReason?,
    targetTitle: String?,
    onBack: () -> Unit,
    onOpenSentReports: () -> Unit,
    vm: ReportViewModel = viewModel()
) {
    val s by vm.state.collectAsState()
    LaunchedEffect(Unit) { vm.init(initialReason) }

    Column(Modifier.fillMaxSize()) {
        TopBar(title = "Báo cáo ${target.label}", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!targetTitle.isNullOrBlank()) {
                Text("Đang báo cáo: $targetTitle", fontWeight = FontWeight.SemiBold)
            }
            Text(
                "Báo cáo được gửi ẩn danh tới quản trị viên. Người bị báo cáo không biết ai đã báo cáo.",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
            Text("Lý do", style = MaterialTheme.typography.titleSmall)
            ReportReason.entries.forEach { reason ->
                val selected = s.reason == reason
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (selected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainer
                        )
                        .border(
                            1.dp,
                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { vm.setReason(reason) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = selected, onClick = { vm.setReason(reason) })
                    Text(reason.label)
                }
            }
            s.reasonError?.let { Text(it, color = ErrorRed, style = MaterialTheme.typography.bodySmall) }
            FormField(
                value = s.description,
                onValueChange = vm::setDescription,
                label = "Mô tả thêm" + if (s.reason == ReportReason.OTHER) "" else " (không bắt buộc)",
                singleLine = false,
                minLines = 3,
                maxLength = 1000
            )
            FormError(s.error)
            if (s.submissionUncertain) {
                Text("Chưa rõ báo cáo đã gửi hay chưa. Hãy kiểm tra trước khi gửi lại.", color = TextSecondary)
                TextButton(onClick = onOpenSentReports) { Text("Xem báo cáo đã gửi") }
            }
            PrimaryButton(
                text = "Gửi báo cáo",
                onClick = { vm.send(target, targetId) },
                loading = s.sending,
                enabled = targetId.isNotBlank()
            )
            Spacer(Modifier.height(16.dp))
        }
    }

    s.sentCode?.let { code ->
        AlertDialog(
            onDismissRequest = onBack,
            title = { Text("Đã gửi báo cáo") },
            text = { Text("Mã báo cáo $code. Bạn có thể theo dõi kết quả xử lý ở mục Báo cáo đã gửi.") },
            confirmButton = { TextButton(onClick = onOpenSentReports) { Text("Xem báo cáo đã gửi") } },
            dismissButton = { TextButton(onClick = onBack) { Text("Đóng") } }
        )
    }
}

// =====================================================================
// Báo cáo đã gửi
// =====================================================================

data class SentReportsState(
    val items: List<SentReport> = emptyList(),
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null
)

class SentReportsViewModel : ViewModel() {
    private val _state = MutableStateFlow(SentReportsState())
    val state = _state.asStateFlow()

    init {
        load()
    }

    fun load(pullToRefresh: Boolean = false) {
        _state.update { it.copy(loading = !pullToRefresh, refreshing = pullToRefresh, error = null) }
        viewModelScope.launch {
            try {
                val items = ReportRepository.mySentReports()
                _state.update { it.copy(items = items, loading = false, refreshing = false) }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, refreshing = false, error = e.userMessage()) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SentReportsScreen(
    onBack: () -> Unit,
    onOpenPost: (String) -> Unit,
    vm: SentReportsViewModel = viewModel()
) {
    val s by vm.state.collectAsState()

    Column(Modifier.fillMaxSize()) {
        TopBar(title = "Báo cáo đã gửi", onBack = onBack)
        PullToRefreshBox(
            isRefreshing = s.refreshing,
            onRefresh = { vm.load(pullToRefresh = true) },
            modifier = Modifier.weight(1f)
        ) {
            when {
                s.loading -> LoadingView()
                s.error != null -> ErrorView(s.error ?: "", onRetry = { vm.load() })
                s.items.isEmpty() -> LazyColumn(Modifier.fillMaxSize()) {
                    item {
                        EmptyView(
                            title = "Bạn chưa gửi báo cáo nào",
                            message = "Gặp bài lừa đảo, spam hay đòi tiền? Bấm menu ba chấm trên bài để báo cáo."
                        )
                    }
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(s.items, key = { it.id }) { report ->
                        SentReportCard(report, onClick = report.targetPostId?.let { id -> { onOpenPost(id) } })
                    }
                }
            }
        }
    }
}

@Composable
private fun SentReportCard(r: SentReport, onClick: (() -> Unit)?) {
    val (bg, fg) = when (r.status) {
        "RESOLVED" -> FoundGreen to FoundText
        "DISMISSED" -> MaterialTheme.colorScheme.surfaceVariant to TextSecondary
        "REVIEWING" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
        else -> LostOrange to LostText
    }
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(r.code, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                SmallChip(r.statusLabel, background = bg, color = fg)
            }
            Text(
                buildString {
                    append("Báo cáo ")
                    append(r.target?.label ?: "")
                    if (!r.targetTitle.isNullOrBlank()) append(": ").append(r.targetTitle)
                },
                style = MaterialTheme.typography.bodyMedium
            )
            Row {
                Text("Lý do: ", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                Text(r.reason?.label ?: "-", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.width(12.dp))
                Text(Formatters.dateTime(r.createdAt), color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            if (!r.description.isNullOrBlank()) {
                Text(r.description, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            r.resultLabel?.let {
                Text(
                    "Kết quả: $it",
                    color = Color(0xFF1D4A5C),
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
