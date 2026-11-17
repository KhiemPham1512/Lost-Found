package com.lostfound.app.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lostfound.app.core.session.Session
import com.lostfound.app.core.util.Validators
import com.lostfound.app.core.util.userMessage
import com.lostfound.app.data.repository.AuthRepository
import com.lostfound.app.ui.components.FormError
import com.lostfound.app.ui.components.FormField
import com.lostfound.app.ui.components.PasswordField
import com.lostfound.app.ui.components.PrimaryButton
import com.lostfound.app.ui.components.TopBar
import com.lostfound.app.ui.theme.TextSecondary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// =====================================================================
// Quên mật khẩu: Nhập email → Nhập mã → Đặt lại mật khẩu
// =====================================================================

enum class ForgotStep { EMAIL, CODE, NEW_PASSWORD }

data class ForgotState(
    val step: ForgotStep = ForgotStep.EMAIL,
    val email: String = "",
    val code: String = "",
    val password: String = "",
    val confirm: String = "",
    val fieldError: String? = null,
    val confirmError: String? = null,
    val error: String? = null,
    val info: String? = null,
    val loading: Boolean = false,
    val done: Boolean = false
)

class ForgotPasswordViewModel : ViewModel() {
    private val _state = MutableStateFlow(ForgotState())
    val state = _state.asStateFlow()

    /** Phiên tạm nhận được sau khi nhập đúng mã; chỉ giữ trong bộ nhớ. */
    private var recoverySession: Session? = null

    fun onEmail(v: String) = _state.update { it.copy(email = v, fieldError = null, error = null) }
    fun onCode(v: String) = _state.update {
        it.copy(code = v.filter { c -> c.isDigit() }.take(10), fieldError = null, error = null)
    }
    fun onPassword(v: String) = _state.update { it.copy(password = v, fieldError = null, error = null) }
    fun onConfirm(v: String) = _state.update { it.copy(confirm = v, confirmError = null, error = null) }

    fun back(): Boolean {
        val s = _state.value
        if (s.step == ForgotStep.EMAIL) return false
        _state.update {
            it.copy(
                step = if (s.step == ForgotStep.NEW_PASSWORD) ForgotStep.CODE else ForgotStep.EMAIL,
                error = null, fieldError = null
            )
        }
        return true
    }

    fun sendCode() {
        val s = _state.value
        if (!Validators.isEmail(s.email)) {
            _state.update { it.copy(fieldError = "Email không đúng định dạng") }
            return
        }
        launchStep(onSuccess = {
            _state.update {
                it.copy(
                    step = ForgotStep.CODE,
                    info = "Nếu email đã đăng ký, mã xác nhận đã được gửi tới ${s.email}. " +
                        "Kiểm tra cả hộp thư Spam."
                )
            }
        }) { AuthRepository.sendRecoveryCode(s.email) }
    }

    fun verifyCode() {
        val s = _state.value
        if (s.code.length < 6) {
            _state.update { it.copy(fieldError = "Nhập đủ mã trong email") }
            return
        }
        launchStep(onSuccess = { _state.update { it.copy(step = ForgotStep.NEW_PASSWORD, info = null) } }) {
            recoverySession = AuthRepository.verifyRecoveryCode(s.email, s.code)
        }
    }

    fun resetPassword() {
        val s = _state.value
        val pwError = Validators.passwordError(s.password)
        val confirmError = if (s.confirm != s.password) "Mật khẩu xác nhận không khớp" else null
        if (pwError != null || confirmError != null) {
            _state.update { it.copy(fieldError = pwError, confirmError = confirmError) }
            return
        }
        val session = recoverySession ?: run {
            _state.update { it.copy(step = ForgotStep.CODE, error = "Vui lòng nhập lại mã xác nhận") }
            return
        }
        launchStep(onSuccess = { _state.update { it.copy(done = true) } }) {
            AuthRepository.resetPassword(session, s.password)
        }
    }

    private fun launchStep(onSuccess: () -> Unit, block: suspend () -> Unit) {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                block()
                _state.update { it.copy(loading = false) }
                onSuccess()
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.userMessage()) }
            }
        }
    }
}

@Composable
fun ForgotPasswordScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    vm: ForgotPasswordViewModel = viewModel()
) {
    val s by vm.state.collectAsState()
    LaunchedEffect(s.done) { if (s.done) onDone() }

    val title = when (s.step) {
        ForgotStep.EMAIL -> "Quên mật khẩu"
        ForgotStep.CODE -> "Nhập mã xác nhận"
        ForgotStep.NEW_PASSWORD -> "Đặt lại mật khẩu"
    }

    Column(Modifier.fillMaxSize()) {
        TopBar(title = title, onBack = { if (!vm.back()) onBack() })
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (s.step) {
                ForgotStep.EMAIL -> {
                    Text(
                        "Nhập email đã đăng ký. Chúng tôi sẽ gửi một mã xác nhận để bạn đặt lại mật khẩu.",
                        color = TextSecondary
                    )
                    FormField(
                        value = s.email,
                        onValueChange = vm::onEmail,
                        label = "Email",
                        error = s.fieldError,
                        keyboardType = KeyboardType.Email
                    )
                    FormError(s.error)
                    PrimaryButton(text = "Gửi mã", onClick = vm::sendCode, loading = s.loading)
                }

                ForgotStep.CODE -> {
                    s.info?.let { Text(it, color = TextSecondary) }
                    FormField(
                        value = s.code,
                        onValueChange = vm::onCode,
                        label = "Mã xác nhận",
                        placeholder = "123456",
                        error = s.fieldError,
                        keyboardType = KeyboardType.NumberPassword
                    )
                    FormError(s.error)
                    PrimaryButton(text = "Xác nhận", onClick = vm::verifyCode, loading = s.loading)
                    TextButton(onClick = vm::sendCode, enabled = !s.loading) { Text("Gửi lại mã") }
                }

                ForgotStep.NEW_PASSWORD -> {
                    Text("Tạo mật khẩu mới cho tài khoản ${s.email}.", color = TextSecondary)
                    PasswordField(
                        value = s.password,
                        onValueChange = vm::onPassword,
                        label = "Mật khẩu mới",
                        error = s.fieldError
                    )
                    PasswordField(
                        value = s.confirm,
                        onValueChange = vm::onConfirm,
                        label = "Nhập lại mật khẩu mới",
                        error = s.confirmError
                    )
                    FormError(s.error)
                    PrimaryButton(text = "Đặt lại mật khẩu", onClick = vm::resetPassword, loading = s.loading)
                }
            }
        }
    }
}

// =====================================================================
// Đổi mật khẩu (khi đang đăng nhập)
// =====================================================================

data class ChangePasswordState(
    val current: String = "",
    val password: String = "",
    val confirm: String = "",
    val currentError: String? = null,
    val passwordError: String? = null,
    val confirmError: String? = null,
    val error: String? = null,
    val loading: Boolean = false,
    val done: Boolean = false
)

class ChangePasswordViewModel : ViewModel() {
    private val _state = MutableStateFlow(ChangePasswordState())
    val state = _state.asStateFlow()

    fun onCurrent(v: String) = _state.update { it.copy(current = v, currentError = null, error = null) }
    fun onPassword(v: String) = _state.update { it.copy(password = v, passwordError = null, error = null) }
    fun onConfirm(v: String) = _state.update { it.copy(confirm = v, confirmError = null, error = null) }

    fun submit() {
        val s = _state.value
        val currentError = if (s.current.isEmpty()) "Vui lòng nhập mật khẩu hiện tại" else null
        val passwordError = Validators.passwordError(s.password)
            ?: if (s.password == s.current) "Mật khẩu mới phải khác mật khẩu cũ" else null
        val confirmError = if (s.confirm != s.password) "Mật khẩu xác nhận không khớp" else null
        if (currentError != null || passwordError != null || confirmError != null) {
            _state.update {
                it.copy(currentError = currentError, passwordError = passwordError, confirmError = confirmError)
            }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                AuthRepository.changePassword(s.current, s.password)
                _state.update { it.copy(loading = false, done = true) }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.userMessage()) }
            }
        }
    }
}

@Composable
fun ChangePasswordScreen(onBack: () -> Unit, vm: ChangePasswordViewModel = viewModel()) {
    val s by vm.state.collectAsState()

    if (s.done) {
        AlertDialog(
            onDismissRequest = onBack,
            confirmButton = { TextButton(onClick = onBack) { Text("Đóng") } },
            title = { Text("Đã đổi mật khẩu") },
            text = { Text("Lần đăng nhập sau, hãy dùng mật khẩu mới.") }
        )
    }

    Column(Modifier.fillMaxSize()) {
        TopBar(title = "Đổi mật khẩu", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PasswordField(s.current, vm::onCurrent, "Mật khẩu hiện tại", error = s.currentError)
            PasswordField(s.password, vm::onPassword, "Mật khẩu mới", error = s.passwordError)
            PasswordField(s.confirm, vm::onConfirm, "Nhập lại mật khẩu mới", error = s.confirmError)
            Text(
                "Tối thiểu 8 ký tự, có cả chữ và số.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            FormError(s.error)
            Spacer(Modifier.height(4.dp))
            PrimaryButton(text = "Lưu mật khẩu mới", onClick = vm::submit, loading = s.loading)
        }
    }
}
