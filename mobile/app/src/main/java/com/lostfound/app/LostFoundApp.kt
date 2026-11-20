package com.lostfound.app

import android.app.Application
import com.lostfound.app.core.session.SessionStore

/**
 * Điểm khởi động của ứng dụng: nạp phiên đăng nhập đã lưu trước khi mở màn hình đầu tiên.
 */
class LostFoundApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SessionStore.init(this)
    }
}
