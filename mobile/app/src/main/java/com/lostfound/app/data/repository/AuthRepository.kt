package com.lostfound.app.data.repository

import com.lostfound.app.core.api.BackendApi
import com.lostfound.app.core.network.ApiException
import com.lostfound.app.core.session.Session
import com.lostfound.app.core.session.SessionStore
import com.lostfound.app.core.supabase.SupabaseAuth

/**
 * Đăng ký, đăng nhập, đăng xuất, quên và đổi mật khẩu.
 * Đăng ký đi qua FastAPI (kiểm tra luật mật khẩu, tạo hồ sơ + handle);
 * còn lại dùng Supabase Auth.
 */
object AuthRepository {

    suspend fun signIn(email: String, password: String) {
        SessionStore.save(SupabaseAuth.signIn(email, password))
    }

    /** POST /auth/register rồi tự đăng nhập. */
    suspend fun register(email: String, password: String, displayName: String?) {
        BackendApi.register(email, password, displayName)
        signIn(email, password)
    }

    suspend fun signOut() {
        val token = SessionStore.current?.accessToken
        SessionStore.clear()
        if (token != null) SupabaseAuth.signOut(token)
    }

    // ---- Quên mật khẩu: gửi mã → nhập mã → đặt mật khẩu mới ----

    suspend fun sendRecoveryCode(email: String) = SupabaseAuth.sendRecoveryCode(email)

    /** Mã đúng thì nhận một phiên tạm, giữ trong bộ nhớ cho tới khi đặt xong mật khẩu. */
    suspend fun verifyRecoveryCode(email: String, code: String): Session =
        SupabaseAuth.verifyRecoveryCode(email, code)

    /** Đặt mật khẩu mới bằng phiên tạm rồi lưu phiên, người dùng vào thẳng app. */
    suspend fun resetPassword(recoverySession: Session, newPassword: String) {
        SupabaseAuth.updatePassword(recoverySession.accessToken, newPassword)
        SessionStore.save(recoverySession)
    }

    // ---- Đổi mật khẩu khi đang đăng nhập ----

    /** Kiểm tra mật khẩu hiện tại bằng cách đăng nhập lại, rồi đặt mật khẩu mới. */
    suspend fun changePassword(currentPassword: String, newPassword: String) {
        val email = SessionStore.current?.email
            ?: throw IllegalStateException("Không xác định được email của tài khoản")
        val fresh = try {
            SupabaseAuth.signIn(email, currentPassword)
        } catch (e: ApiException) {
            // 400 = sai mật khẩu; lỗi mạng (0) hay lỗi server thì giữ nguyên câu báo
            if (e.status == 400) throw ApiException(400, "Mật khẩu hiện tại không đúng")
            throw e
        }
        SupabaseAuth.updatePassword(fresh.accessToken, newPassword)
        SessionStore.save(fresh)
    }
}
