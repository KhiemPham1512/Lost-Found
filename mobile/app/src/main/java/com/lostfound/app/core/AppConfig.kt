package com.lostfound.app.core

import com.lostfound.app.BuildConfig

/**
 * Cấu hình đọc từ local.properties qua BuildConfig. Không ghi khóa thẳng vào mã nguồn.
 * Chỉ publishable key được phép nằm trong app; secret key chỉ ở backend.
 */
object AppConfig {
    val supabaseUrl: String = BuildConfig.SUPABASE_URL.trimEnd('/')
    val supabaseKey: String = BuildConfig.SUPABASE_PUBLISHABLE_KEY
    val apiBaseUrl: String = BuildConfig.API_BASE_URL.trimEnd('/')

    const val POST_IMAGES_BUCKET = "post-images"

    /** Địa chỉ công khai của một ảnh trong bucket post-images. */
    fun publicImageUrl(path: String): String =
        "$supabaseUrl/storage/v1/object/public/$POST_IMAGES_BUCKET/$path"

    fun requireConfigured() {
        require(supabaseUrl.isNotBlank()) {
            "Chưa cấu hình SUPABASE_URL trong local.properties"
        }
        require(supabaseKey.isNotBlank()) {
            "Chưa cấu hình SUPABASE_PUBLISHABLE_KEY trong local.properties"
        }
    }
}
