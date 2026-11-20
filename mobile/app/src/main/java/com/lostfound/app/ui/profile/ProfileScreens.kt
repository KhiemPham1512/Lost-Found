package com.lostfound.app.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.lostfound.app.core.session.SessionStore
import com.lostfound.app.core.util.Formatters
import com.lostfound.app.core.util.Validators
import com.lostfound.app.core.util.userMessage
import com.lostfound.app.data.model.Profile
import com.lostfound.app.data.repository.AuthRepository
import com.lostfound.app.data.repository.ProfileRepository
import com.lostfound.app.ui.components.Avatar
import com.lostfound.app.ui.components.ErrorView
import com.lostfound.app.ui.components.FormError
import com.lostfound.app.ui.components.FormField
import com.lostfound.app.ui.components.LoadingView
import com.lostfound.app.ui.components.PrimaryButton
import com.lostfound.app.ui.components.SmallChip
import com.lostfound.app.ui.components.TopBar
import com.lostfound.app.ui.theme.ErrorRed
import com.lostfound.app.ui.theme.TextSecondary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// =====================================================================
// Màn Cá nhân
// =====================================================================

data class ProfileState(
    val profile: Profile? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val loggedOut: Boolean = false
)

class ProfileViewModel : ViewModel() {
    private val _state = MutableStateFlow(ProfileState())
    val state = _state.asStateFlow()

    fun load() {
        _state.update { it.copy(loading = it.profile == null, error = null) }
        viewModelScope.launch {
            try {
                val p = ProfileRepository.myProfile()
                _state.update {
                    it.copy(profile = p, loading = false, error = if (p == null) "Tài khoản chưa có hồ sơ" else null)
                }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.userMessage()) }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            AuthRepository.signOut()
            _state.update { it.copy(loggedOut = true) }
        }
    }
}

@Composable
fun ProfileScreen(
    onEditProfile: () -> Unit,
    onMyPosts: () -> Unit,
    onSentReports: () -> Unit,
    onChangePassword: () -> Unit,
    onLoggedOut: () -> Unit,
    vm: ProfileViewModel = viewModel()
) {
    val s by vm.state.collectAsState()
    var confirmLogout by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.load() }
    LaunchedEffect(s.loggedOut) { if (s.loggedOut) onLoggedOut() }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Text(
            "Cá nhân",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 8.dp)
        )
        when {
            s.loading -> LoadingView()
            s.profile == null -> ErrorView(s.error ?: "Không tải được hồ sơ", onRetry = vm::load)
            else -> {
                val p = s.profile!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Avatar(url = p.avatarUrl, name = p.shownName, size = 64.dp)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(p.shownName, style = MaterialTheme.typography.titleLarge)
                                Text("@${p.username}", color = TextSecondary)
                                SessionStore.current?.email?.let {
                                    Text(it, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                }
                                p.createdAt?.let {
                                    Text(
                                        "Tham gia ${Formatters.date(it)}",
                                        color = TextSecondary,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            if (p.role == "ADMIN") SmallChip("Admin")
                        }
                        if (!p.bio.isNullOrBlank()) {
                            Text(
                                p.bio,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                            )
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        MenuRow(Icons.Filled.Edit, "Chỉnh sửa hồ sơ", onEditProfile)
                        HorizontalDivider()
                        MenuRow(Icons.AutoMirrored.Filled.List, "Bài của tôi", onMyPosts)
                        HorizontalDivider()
                        MenuRow(Icons.Filled.Warning, "Báo cáo đã gửi", onSentReports)
                        HorizontalDivider()
                        MenuRow(Icons.Filled.Lock, "Đổi mật khẩu", onChangePassword)
                    }

                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        MenuRow(Icons.AutoMirrored.Filled.ExitToApp, "Đăng xuất", { confirmLogout = true }, danger = true)
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("Đăng xuất?") },
            confirmButton = {
                TextButton(onClick = {
                    confirmLogout = false
                    vm.logout()
                }) { Text("Đăng xuất", color = ErrorRed) }
            },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Hủy") } }
        )
    }
}

@Composable
private fun MenuRow(icon: ImageVector, label: String, onClick: () -> Unit, danger: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val color = if (danger) ErrorRed else MaterialTheme.colorScheme.onSurface
        Icon(icon, contentDescription = null, tint = if (danger) ErrorRed else MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(14.dp))
        Text(label, color = color, modifier = Modifier.weight(1f))
        if (!danger) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextSecondary)
    }
}

// =====================================================================
// Chỉnh sửa hồ sơ: tên hiển thị, giới thiệu, ảnh đại diện, số điện thoại
// =====================================================================

data class EditProfileState(
    val loading: Boolean = true,
    val username: String = "",
    val displayName: String = "",
    val bio: String = "",
    val avatarUrl: String? = null,
    val newAvatar: Uri? = null,
    val phone: String = "",
    val originalPhone: String = "",
    val nameError: String? = null,
    val phoneError: String? = null,
    val error: String? = null,
    val saving: Boolean = false,
    val saved: Boolean = false
)

class EditProfileViewModel : ViewModel() {
    private val _state = MutableStateFlow(EditProfileState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val p = ProfileRepository.myProfile()
                val phone = runCatching { ProfileRepository.myPhone() }.getOrNull().orEmpty()
                _state.update {
                    it.copy(
                        loading = false,
                        username = p?.username.orEmpty(),
                        displayName = p?.displayName.orEmpty(),
                        bio = p?.bio.orEmpty(),
                        avatarUrl = p?.avatarUrl,
                        phone = phone,
                        originalPhone = phone
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.userMessage()) }
            }
        }
    }

    fun setName(v: String) = _state.update { it.copy(displayName = v, nameError = null, error = null, saved = false) }
    fun setBio(v: String) = _state.update { it.copy(bio = v, error = null, saved = false) }
    fun setPhone(v: String) = _state.update { it.copy(phone = v, phoneError = null, error = null, saved = false) }
    fun setAvatar(uri: Uri) = _state.update { it.copy(newAvatar = uri, error = null, saved = false) }

    fun save(context: android.content.Context) {
        val s = _state.value
        val nameError = if (s.displayName.trim().length > 50) "Tên hiển thị tối đa 50 ký tự" else null
        val phoneChanged = Validators.normalizePhone(s.phone) != Validators.normalizePhone(s.originalPhone)
        val phoneError = when {
            !phoneChanged -> null
            s.phone.isBlank() -> "Không thể xóa số điện thoại, chỉ có thể đổi sang số khác"
            !Validators.isPhone(s.phone) -> "Số điện thoại Việt Nam bắt đầu bằng 0 hoặc +84, gồm 10–11 chữ số"
            else -> null
        }
        if (nameError != null || phoneError != null) {
            _state.update { it.copy(nameError = nameError, phoneError = phoneError) }
            return
        }
        _state.update { it.copy(saving = true, error = null) }
        val appContext = context.applicationContext
        viewModelScope.launch {
            try {
                val avatarUrl = s.newAvatar?.let { ProfileRepository.uploadAvatar(appContext, it) }
                ProfileRepository.updateProfile(s.displayName, s.bio, avatarUrl)
                if (phoneChanged) ProfileRepository.savePhone(Validators.normalizePhone(s.phone))
                _state.update {
                    it.copy(
                        saving = false,
                        saved = true,
                        avatarUrl = avatarUrl ?: it.avatarUrl,
                        newAvatar = null,
                        originalPhone = it.phone
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(saving = false, error = e.userMessage()) }
            }
        }
    }
}

@Composable
fun EditProfileScreen(onBack: () -> Unit, vm: EditProfileViewModel = viewModel()) {
    val s by vm.state.collectAsState()
    val context = LocalContext.current
    val pickAvatar = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.setAvatar(uri)
    }

    Column(Modifier.fillMaxSize()) {
        TopBar(title = "Chỉnh sửa hồ sơ", onBack = onBack)
        if (s.loading) {
            LoadingView()
            return@Column
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .clickable {
                                pickAvatar.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                    ) {
                        if (s.newAvatar != null) {
                            AsyncImage(
                                model = s.newAvatar,
                                contentDescription = "Ảnh đại diện mới",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Avatar(url = s.avatarUrl, name = s.displayName.ifBlank { s.username }, size = 96.dp)
                        }
                    }
                    TextButton(onClick = {
                        pickAvatar.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) { Text("Đổi ảnh đại diện") }
                }
            }

            FormField(
                value = s.username,
                onValueChange = {},
                label = "Tên người dùng (không đổi được)",
                enabled = false
            )
            FormField(
                value = s.displayName,
                onValueChange = vm::setName,
                label = "Tên hiển thị",
                error = s.nameError,
                maxLength = 50
            )
            FormField(
                value = s.bio,
                onValueChange = vm::setBio,
                label = "Giới thiệu ngắn",
                singleLine = false,
                minLines = 2,
                maxLength = 160
            )
            FormField(
                value = s.phone,
                onValueChange = vm::setPhone,
                label = "Số điện thoại",
                placeholder = "0901234567",
                error = s.phoneError,
                keyboardType = KeyboardType.Phone,
                supporting = "Không hiển thị công khai. Chỉ bạn và quản trị viên xem được."
            )
            FormError(s.error)
            if (s.saved) {
                Text("Đã lưu thay đổi", color = MaterialTheme.colorScheme.tertiary)
            }
            PrimaryButton(text = "Lưu", onClick = { vm.save(context) }, loading = s.saving)
            Spacer(Modifier.height(16.dp))
        }
    }
}
