package com.lostfound.app.ui.auth

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lostfound.app.core.network.ApiException
import com.lostfound.app.core.session.SessionStore
import com.lostfound.app.core.util.Validators
import com.lostfound.app.core.util.userMessage
import com.lostfound.app.data.repository.AuthRepository
import com.lostfound.app.ui.components.FormError
import com.lostfound.app.ui.components.FormField
import com.lostfound.app.ui.components.PasswordField
import com.lostfound.app.ui.components.PrimaryButton
import com.lostfound.app.ui.components.TopBar
import com.lostfound.app.ui.theme.BrandBlue
import com.lostfound.app.ui.theme.TextSecondary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// =====================================================================
// Splash
// =====================================================================

@Composable
fun SplashScreen(onLoggedIn: () -> Unit, onLoggedOut: () -> Unit) {
    LaunchedEffect(Unit) {
        if (SessionStore.current == null) {
            onLoggedOut()
            return@LaunchedEffect
        }
        val ok = try {
            // Làm mới token nếu đã hết hạn; refresh token hỏng thì phiên bị xóa
            SessionStore.validAccessToken() != null
        } catch (e: ApiException) {
            // Mất mạng hoặc server lỗi: vẫn vào app, các màn sẽ báo lỗi và cho thử lại
            true
        }
        if (ok) onLoggedIn() else onLoggedOut()
    }
    Box(
        modifier = Modifier.fillMaxSize().background(BrandBlue),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AppLogo(light = true)
            Spacer(Modifier.height(32.dp))
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
        }
    }
}

@Composable
internal fun AppLogo(light: Boolean = false) {
    val fg = if (light) Color.White else BrandBlue
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(if (light) Color.White.copy(alpha = 0.18f) else BrandBlue.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = fg, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text("Lost & Found", color = fg, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(
            "Tìm lại đồ thất lạc, an toàn và đúng chủ",
            color = if (light) Color.White.copy(alpha = 0.85f) else TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

// =====================================================================
// Đăng nhập
// =====================================================================

data class LoginState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val error: String? = null,
    val loading: Boolean = false,
    val done: Boolean = false
)

class LoginViewModel : ViewModel() {
    private val _state = MutableStateFlow(LoginState())
    val state = _state.asStateFlow()

    fun onEmail(v: String) = _state.update { it.copy(email = v, emailError = null, error = null) }
    fun onPassword(v: String) = _state.update { it.copy(password = v, passwordError = null, error = null) }

    fun submit() {
        val s = _state.value
        val emailError = if (!Validators.isEmail(s.email)) "Email không đúng định dạng" else null
        val passwordError = if (s.password.isEmpty()) "Vui lòng nhập mật khẩu" else null
        if (emailError != null || passwordError != null) {
            _state.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                AuthRepository.signIn(s.email, s.password)
                _state.update { it.copy(loading = false, done = true) }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.userMessage()) }
            }
        }
    }
}

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onRegister: () -> Unit,
    onForgotPassword: () -> Unit,
    vm: LoginViewModel = viewModel()
) {
    val s by vm.state.collectAsState()
    LaunchedEffect(s.done) { if (s.done) onLoggedIn() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { AppLogo() }
        Spacer(Modifier.height(16.dp))
        Text("Đăng nhập", style = MaterialTheme.typography.headlineSmall)

        FormField(
            value = s.email,
            onValueChange = vm::onEmail,
            label = "Email",
            placeholder = "ten@gmail.com",
            error = s.emailError,
            keyboardType = KeyboardType.Email
        )
        PasswordField(
            value = s.password,
            onValueChange = vm::onPassword,
            label = "Mật khẩu",
            error = s.passwordError
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onForgotPassword) { Text("Quên mật khẩu?") }
        }
        FormError(s.error)
        PrimaryButton(text = "Đăng nhập", onClick = vm::submit, loading = s.loading)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Chưa có tài khoản?", color = TextSecondary)
            TextButton(onClick = onRegister) { Text("Đăng ký") }
        }
    }
}

// =====================================================================
// Đăng ký
// =====================================================================

data class RegisterState(
    val displayName: String = "",
    val email: String = "",
    val password: String = "",
    val confirm: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmError: String? = null,
    val error: String? = null,
    val loading: Boolean = false,
    val done: Boolean = false
)

class RegisterViewModel : ViewModel() {
    private val _state = MutableStateFlow(RegisterState())
    val state = _state.asStateFlow()

    fun onName(v: String) = _state.update { it.copy(displayName = v, error = null) }
    fun onEmail(v: String) = _state.update { it.copy(email = v, emailError = null, error = null) }
    fun onPassword(v: String) = _state.update { it.copy(password = v, passwordError = null, error = null) }
    fun onConfirm(v: String) = _state.update { it.copy(confirm = v, confirmError = null, error = null) }

    fun submit() {
        val s = _state.value
        val emailError = if (!Validators.isEmail(s.email)) "Email không đúng định dạng" else null
        val passwordError = Validators.passwordError(s.password)
        val confirmError = if (s.confirm != s.password) "Mật khẩu xác nhận không khớp" else null
        if (emailError != null || passwordError != null || confirmError != null) {
            _state.update {
                it.copy(emailError = emailError, passwordError = passwordError, confirmError = confirmError)
            }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                AuthRepository.register(s.email, s.password, s.displayName)
                _state.update { it.copy(loading = false, done = true) }
            } catch (e: ApiException) {
                // 409: email đã đăng ký → báo ngay dưới ô email
                if (e.status == 409) {
                    _state.update { it.copy(loading = false, emailError = e.userMessage()) }
                } else {
                    _state.update { it.copy(loading = false, error = e.userMessage()) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.userMessage()) }
            }
        }
    }
}

@Composable
fun RegisterScreen(
    onRegistered: () -> Unit,
    onBack: () -> Unit,
    vm: RegisterViewModel = viewModel()
) {
    val s by vm.state.collectAsState()
    LaunchedEffect(s.done) { if (s.done) onRegistered() }

    Column(Modifier.fillMaxSize()) {
        TopBar(title = "Tạo tài khoản", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Email chỉ dùng để đăng nhập và không hiển thị công khai. " +
                    "Người khác chỉ thấy tên hiển thị và tên người dùng của bạn.",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            FormField(
                value = s.displayName,
                onValueChange = vm::onName,
                label = "Tên hiển thị (không bắt buộc)",
                maxLength = 50
            )
            FormField(
                value = s.email,
                onValueChange = vm::onEmail,
                label = "Email",
                placeholder = "ten@gmail.com",
                error = s.emailError,
                keyboardType = KeyboardType.Email
            )
            PasswordField(
                value = s.password,
                onValueChange = vm::onPassword,
                label = "Mật khẩu (tối thiểu 8 ký tự, có chữ và số)",
                error = s.passwordError
            )
            PasswordField(
                value = s.confirm,
                onValueChange = vm::onConfirm,
                label = "Nhập lại mật khẩu",
                error = s.confirmError
            )
            FormError(s.error)
            Spacer(Modifier.height(4.dp))
            PrimaryButton(text = "Đăng ký", onClick = vm::submit, loading = s.loading)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Đã có tài khoản?", color = TextSecondary)
                TextButton(onClick = onBack) { Text("Đăng nhập") }
            }
        }
    }
}
