package com.lostfound.app.core.util

import android.location.Location
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object Formatters {
    private val zone: ZoneId get() = ZoneId.systemDefault()

    /** Định dạng gửi cho server: 2026-10-23T08:30:00+07:00 */
    private val ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
    private val DATE_TIME = DateTimeFormatter.ofPattern("HH:mm, dd/MM/yyyy")
    private val DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun toIso(time: ZonedDateTime): String = time.withNano(0).format(ISO)

    fun parse(iso: String?): OffsetDateTime? =
        try {
            if (iso.isNullOrBlank()) null else OffsetDateTime.parse(iso)
        } catch (_: Exception) {
            null
        }

    /** "5 phút trước", "2 giờ trước", "3 ngày trước", hoặc ngày cụ thể nếu đã lâu. */
    fun timeAgo(iso: String?): String {
        val time = parse(iso) ?: return ""
        val minutes = Duration.between(time.toInstant(), java.time.Instant.now()).toMinutes()
        return when {
            minutes < 1 -> "Vừa xong"
            minutes < 60 -> "$minutes phút trước"
            minutes < 60 * 24 -> "${minutes / 60} giờ trước"
            minutes < 60 * 24 * 7 -> "${minutes / (60 * 24)} ngày trước"
            else -> time.atZoneSameInstant(zone).format(DATE)
        }
    }

    fun dateTime(iso: String?): String =
        parse(iso)?.atZoneSameInstant(zone)?.format(DATE_TIME).orEmpty()

    fun date(iso: String?): String =
        parse(iso)?.atZoneSameInstant(zone)?.format(DATE).orEmpty()

    fun dateTime(time: ZonedDateTime): String = time.format(DATE_TIME)

    /** Mốc bắt đầu cho bộ lọc "Thời gian đăng". */
    fun postedAfter(filter: TimeFilter): String? = when (filter) {
        TimeFilter.ALL -> null
        TimeFilter.TODAY -> toIso(ZonedDateTime.now(zone).toLocalDate().atStartOfDay(zone))
        TimeFilter.WEEK -> toIso(ZonedDateTime.now(zone).minusDays(7))
        TimeFilter.MONTH -> toIso(ZonedDateTime.now(zone).minusDays(30))
    }

    /** Khoảng cách từ vị trí của máy tới tọa độ của bài, ví dụ "850 m" hoặc "1.2 km". */
    fun distance(fromLat: Double?, fromLng: Double?, toLat: Double?, toLng: Double?): String? {
        if (fromLat == null || fromLng == null || toLat == null || toLng == null) return null
        val result = FloatArray(1)
        Location.distanceBetween(fromLat, fromLng, toLat, toLng, result)
        val meters = result[0]
        return if (meters < 1000) "${meters.toInt()} m"
        else String.format(Locale.US, "%.1f km", meters / 1000f)
    }
}

enum class TimeFilter(val label: String) {
    ALL("Tất cả"),
    TODAY("Hôm nay"),
    WEEK("7 ngày"),
    MONTH("30 ngày")
}

object Validators {
    private val EMAIL = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val PHONE = Regex("^(0|\\+84)[0-9]{9,10}$")

    fun isEmail(value: String) = EMAIL.matches(value.trim())

    /** Trả câu báo lỗi, hoặc null nếu mật khẩu hợp lệ (8–72 ký tự, có chữ và số). */
    fun passwordError(value: String): String? = when {
        value.length < 8 -> "Mật khẩu tối thiểu 8 ký tự"
        value.length > 72 -> "Mật khẩu tối đa 72 ký tự"
        !value.any { it.isLetter() } || !value.any { it.isDigit() } ->
            "Mật khẩu phải có cả chữ và số"
        else -> null
    }

    /** Bỏ khoảng trắng, dấu chấm, gạch ngang giống server rồi kiểm tra số Việt Nam. */
    fun normalizePhone(value: String): String = value.replace(Regex("[\\s.\\-]"), "")
    fun isPhone(value: String) = PHONE.matches(normalizePhone(value))
}
