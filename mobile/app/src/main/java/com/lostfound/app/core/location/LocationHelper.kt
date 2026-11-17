package com.lostfound.app.core.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

data class LatLng(val lat: Double, val lng: Double)

/** Lấy GPS bằng Fused Location (phục vụ "Gần nhất" và "Dùng vị trí hiện tại"). */
object LocationHelper {

    val PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    fun hasPermission(context: Context): Boolean =
        PERMISSIONS.any {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    /** Trả vị trí hiện tại, hoặc null nếu chưa có quyền / tắt định vị / quá 15 giây. */
    @SuppressLint("MissingPermission")
    suspend fun currentLocation(context: Context): LatLng? {
        if (!hasPermission(context)) return null
        val client = LocationServices.getFusedLocationProviderClient(context)
        val fresh = withTimeoutOrNull(15_000) {
            suspendCancellableCoroutine<LatLng?> { cont ->
                val cancel = CancellationTokenSource()
                client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancel.token)
                    .addOnSuccessListener { loc ->
                        cont.resume(loc?.let { LatLng(it.latitude, it.longitude) })
                    }
                    .addOnFailureListener { cont.resume(null) }
                cont.invokeOnCancellation { cancel.cancel() }
            }
        }
        if (fresh != null) return fresh
        // Dự phòng: vị trí gần nhất máy còn nhớ
        return suspendCancellableCoroutine { cont ->
            client.lastLocation
                .addOnSuccessListener { loc ->
                    cont.resume(loc?.let { LatLng(it.latitude, it.longitude) })
                }
                .addOnFailureListener { cont.resume(null) }
        }
    }

    /** Gợi ý tên địa điểm từ tọa độ. Có thể trả null nếu máy không có dịch vụ Geocoder. */
    @Suppress("DEPRECATION")
    suspend fun addressOf(context: Context, point: LatLng): String? = withContext(Dispatchers.IO) {
        try {
            if (!Geocoder.isPresent()) return@withContext null
            val list = Geocoder(context, Locale("vi", "VN"))
                .getFromLocation(point.lat, point.lng, 1)
            val address = list?.firstOrNull() ?: return@withContext null
            address.getAddressLine(0)?.take(200)
        } catch (_: Exception) {
            null
        }
    }
}
