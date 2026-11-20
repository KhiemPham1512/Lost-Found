package com.lostfound.app.data.repository

import android.content.Context
import android.net.Uri
import com.lostfound.app.core.AppConfig
import com.lostfound.app.core.api.BackendApi
import com.lostfound.app.core.image.ImageCompressor
import com.lostfound.app.core.network.str
import com.lostfound.app.core.session.SessionStore
import com.lostfound.app.core.supabase.SupabaseRest
import com.lostfound.app.data.model.Profile
import org.json.JSONObject
import java.util.UUID

object ProfileRepository {

    suspend fun myProfile(): Profile? = profile(SessionStore.requireUserId())

    suspend fun profile(userId: String): Profile? {
        val rows = SupabaseRest.select(
            "profiles",
            listOf(
                "select" to "id,username,display_name,avatar_url,bio,role,is_banned,created_at",
                "id" to "eq.$userId"
            )
        )
        val o = rows.optJSONObject(0) ?: return null
        return Profile(
            id = o.getString("id"),
            username = o.optString("username"),
            displayName = o.str("display_name"),
            avatarUrl = o.str("avatar_url"),
            bio = o.str("bio"),
            role = o.optString("role", "USER"),
            isBanned = o.optBoolean("is_banned"),
            createdAt = o.str("created_at")
        )
    }

    /** Số điện thoại của chính mình (bảng user_private, RLS chỉ cho chính chủ đọc). */
    suspend fun myPhone(): String? {
        val rows = SupabaseRest.select(
            "user_private",
            listOf("select" to "phone", "user_id" to "eq.${SessionStore.requireUserId()}")
        )
        return rows.optJSONObject(0)?.str("phone")
    }

    /** Sửa hồ sơ: chỉ được ghi display_name, bio, avatar_url (quyền cột trong database). */
    suspend fun updateProfile(displayName: String?, bio: String?, avatarUrl: String? = null) {
        val values = JSONObject()
            .put("display_name", displayName?.trim()?.takeIf { it.isNotEmpty() } ?: JSONObject.NULL)
            .put("bio", bio?.trim()?.takeIf { it.isNotEmpty() } ?: JSONObject.NULL)
        if (avatarUrl != null) values.put("avatar_url", avatarUrl)
        SupabaseRest.update(
            "profiles",
            listOf("id" to "eq.${SessionStore.requireUserId()}"),
            values
        )
    }

    /**
     * Ảnh đại diện: nén rồi upload vào thư mục của chính mình trong bucket post-images
     * (bucket duy nhất của dự án, policy chỉ cho ghi vào <user_id>/...). Trả về URL công khai.
     */
    suspend fun uploadAvatar(context: Context, uri: Uri): String {
        val uid = SessionStore.requireUserId()
        val bytes = ImageCompressor.compress(context, uri)
        val path = "$uid/avatar-${UUID.randomUUID()}.jpg"
        SupabaseRest.uploadImage(path, bytes)
        return AppConfig.publicImageUrl(path)
    }

    /** PUT /me/phone (số lưu riêng tư, không ai khác thấy). */
    suspend fun savePhone(phone: String) {
        BackendApi.savePhone(phone)
    }
}
