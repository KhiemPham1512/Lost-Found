package com.lostfound.app.core.session

import android.content.Context
import android.content.SharedPreferences
import com.lostfound.app.core.network.ApiException
import com.lostfound.app.core.supabase.SupabaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Phiên đăng nhập do Supabase Auth cấp. */
data class Session(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val email: String?,
    /** Thời điểm access token hết hạn (giây, epoch). */
    val expiresAt: Long
)

/**
 * Lưu phiên đăng nhập trên máy (SharedPreferences riêng của app) để mở lại app không phải
 * đăng nhập lại, và tự làm mới access token (hạn 1 giờ) bằng refresh token.
 *
 * Mọi nơi cần token (Supabase REST, Storage, FastAPI) gọi [requireToken].
 */
object SessionStore {
    private const val PREFS = "lostfound_session"
    private lateinit var prefs: SharedPreferences
    private val refreshLock = Mutex()
    // Đổi khi thay phiên đăng nhập, không đổi khi chỉ xoay token cùng phiên.
    private var generation = 0L

    private val _session = MutableStateFlow<Session?>(null)
    val session: StateFlow<Session?> = _session.asStateFlow()

    val current: Session? get() = _session.value
    val userId: String? get() = _session.value?.userId

    @Synchronized
    fun init(context: Context) {
        generation++
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _session.value = load()
    }

    @Synchronized
    fun save(session: Session) {
        generation++
        writeSession(session)
    }

    private fun writeSession(session: Session) {
        prefs.edit()
            .putString("access_token", session.accessToken)
            .putString("refresh_token", session.refreshToken)
            .putString("user_id", session.userId)
            .putString("email", session.email)
            .putLong("expires_at", session.expiresAt)
            .apply()
        _session.value = session
    }

    @Synchronized
    fun clear() {
        generation++
        prefs.edit().clear().apply()
        _session.value = null
    }

    private fun load(): Session? {
        val access = prefs.getString("access_token", null) ?: return null
        val refresh = prefs.getString("refresh_token", null) ?: return null
        val uid = prefs.getString("user_id", null) ?: return null
        return Session(
            accessToken = access,
            refreshToken = refresh,
            userId = uid,
            email = prefs.getString("email", null),
            expiresAt = prefs.getLong("expires_at", 0L)
        )
    }

    private fun isFresh(s: Session): Boolean =
        s.expiresAt - 60 > System.currentTimeMillis() / 1000

    /**
     * Trả access token còn hạn; tự làm mới nếu sắp hết hạn.
     * Trả null nếu chưa đăng nhập hoặc refresh token không còn dùng được (khi đó phiên bị xóa
     * và app tự quay về màn Đăng nhập).
     */
    suspend fun validAccessToken(): String? {
        val (s, version) = synchronized(this) {
            val active = current ?: return null
            active to generation
        }
        if (isFresh(s)) return s.accessToken
        return refreshLock.withLock {
            val again = synchronized(this) {
                if (generation != version) return@withLock null
                val latest = current ?: return@withLock null
                if (isFresh(latest)) return@withLock latest.accessToken
                latest
            }
            try {
                val renewed = SupabaseAuth.refresh(again.refreshToken)
                synchronized(this) {
                    if (generation != version) return@withLock null
                    writeSession(renewed)
                    renewed.accessToken
                }
            } catch (e: ApiException) {
                synchronized(this) {
                    if (generation != version) return@withLock null
                    // Chỉ xóa đúng phiên đã gửi refresh, không xóa phiên đăng nhập sau đó.
                    if (e.status in 400..499 && e.status != 429) {
                        clear()
                        null
                    } else {
                        throw e
                    }
                }
            }
        }
    }

    suspend fun requireToken(): String =
        validAccessToken()
            ?: throw ApiException(401, "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại")

    fun requireUserId(): String =
        userId ?: throw ApiException(401, "Bạn chưa đăng nhập")
}
