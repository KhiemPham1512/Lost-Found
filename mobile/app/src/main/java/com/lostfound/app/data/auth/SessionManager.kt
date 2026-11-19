package com.lostfound.app.data.auth

import com.lostfound.app.core.session.Session
import com.lostfound.app.core.session.SessionStore

/**
 * Giữ nguyên các hàm cũ để code của C không phải sửa, nhưng phiên giờ được lưu bền
 * trên máy và tự làm mới qua [SessionStore] (tầng core của B).
 */
object SessionManager {

    fun saveSession(session: AuthSession) {
        SessionStore.save(
            Session(
                accessToken = session.accessToken,
                refreshToken = session.refreshToken,
                userId = session.userId,
                email = session.email,
                expiresAt = System.currentTimeMillis() / 1000 + 3600
            )
        )
    }

    /** Token hiện có (có thể đã hết hạn). Code mới nên dùng SessionStore.requireToken(). */
    fun getAccessToken(): String? {
        return SessionStore.current?.accessToken
    }

    fun getCurrentUserId(): String? {
        return SessionStore.userId
    }

    fun getCurrentEmail(): String? {
        return SessionStore.current?.email
    }

    fun isLoggedIn(): Boolean {
        return SessionStore.current != null
    }

    fun clearSession() {
        SessionStore.clear()
    }
}
