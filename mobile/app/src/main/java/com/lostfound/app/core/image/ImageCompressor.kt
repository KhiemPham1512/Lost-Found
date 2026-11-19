package com.lostfound.app.core.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Nén ảnh trước khi upload: cạnh dài tối đa ~1280 px, JPEG chất lượng 80%,
 * xoay đúng chiều theo EXIF (ảnh chụp dọc từ camera).
 */
object ImageCompressor {
    private const val MAX_SIDE = 1280
    private const val QUALITY = 80

    suspend fun compress(context: Context, uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver

        // 1) Đọc kích thước gốc để chọn hệ số thu nhỏ khi giải mã (tiết kiệm bộ nhớ)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val longSide = max(bounds.outWidth, bounds.outHeight)
        require(longSide > 0) { "Không đọc được ảnh" }

        var sample = 1
        while (longSide / (sample * 2) >= MAX_SIDE) sample *= 2

        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: throw IllegalArgumentException("Không đọc được ảnh")

        // 2) Thu nhỏ đúng về 1280 px cạnh dài
        val scale = MAX_SIDE.toFloat() / max(decoded.width, decoded.height)
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * scale).roundToInt(),
                (decoded.height * scale).roundToInt(),
                true
            )
        } else decoded

        // 3) Xoay theo EXIF
        val rotation = try {
            resolver.openInputStream(uri)?.use {
                when (ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
                )) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        } catch (_: Exception) {
            0f
        }
        val oriented = if (rotation != 0f) {
            Bitmap.createBitmap(
                scaled, 0, 0, scaled.width, scaled.height,
                Matrix().apply { postRotate(rotation) }, true
            )
        } else scaled

        // 4) Nén JPEG
        val out = ByteArrayOutputStream()
        oriented.compress(Bitmap.CompressFormat.JPEG, QUALITY, out)
        if (oriented !== scaled) oriented.recycle()
        if (scaled !== decoded) scaled.recycle()
        decoded.recycle()
        out.toByteArray()
    }
}
