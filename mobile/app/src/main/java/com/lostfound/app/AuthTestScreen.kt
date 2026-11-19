package com.lostfound.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.lostfound.app.data.auth.SessionManager
import com.lostfound.app.data.auth.SupabaseAuthRepository
import kotlinx.coroutines.launch

@Composable
fun AuthTestScreen(onLoginSuccess: () -> Unit = {}) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val repository = remember { SupabaseAuthRepository() }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Kiểm thử Supabase Auth",
            style = MaterialTheme.typography.headlineSmall
        )

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                message = ""
            },
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                message = ""
            },
            label = { Text("Mật khẩu") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    message = "Vui lòng nhập email và mật khẩu"
                    return@Button
                }

                scope.launch {
                    isLoading = true
                    message = ""

                    val result = repository.signIn(email, password)

                    result.fold(
                        onSuccess = { session ->
                            SessionManager.saveSession(session)
                            onLoginSuccess()
                            message = "Đăng nhập thành công!\nUser ID: ${session.userId}"
                        },
                        onFailure = { error ->
                            message = "Đăng nhập thất bại: ${error.message}"
                        }
                    )

                    isLoading = false
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Đăng nhập kiểm thử")
            }
        }

        if (message.isNotBlank()) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}