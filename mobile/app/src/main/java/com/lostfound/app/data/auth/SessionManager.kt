package com.lostfound.app.data.auth

object SessionManager {

    private var currentSession: AuthSession? = null

    fun saveSession(session: AuthSession) {
        currentSession = session
    }

    fun getAccessToken(): String? {
        return currentSession?.accessToken
    }

    fun getCurrentUserId(): String? {
        return currentSession?.userId
    }

    fun getCurrentEmail(): String? {
        return currentSession?.email
    }

    fun isLoggedIn(): Boolean {
        return currentSession != null
    }

    fun clearSession() {
        currentSession = null
    }
}