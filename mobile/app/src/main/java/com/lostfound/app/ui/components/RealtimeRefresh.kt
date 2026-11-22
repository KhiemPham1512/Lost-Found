package com.lostfound.app.ui.components

import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.lostfound.app.core.session.SessionStore
import com.lostfound.app.core.supabase.RealtimeEvent
import com.lostfound.app.core.supabase.RealtimeFilter
import com.lostfound.app.core.supabase.SupabaseRealtime
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

/** Event chỉ báo dữ liệu cần đọc lại; REST/RLS vẫn là nguồn bản ghi thật. */
@Composable
fun rememberRealtimeRefresh(filters: List<RealtimeFilter>, refresh: suspend () -> Unit): Boolean {
    val lifecycle = LocalLifecycleOwner.current
    val latestRefresh by rememberUpdatedState(refresh)
    val owner = SessionStore.userId
    var connected by remember(filters, owner) { mutableStateOf(false) }
    LaunchedEffect(filters, lifecycle, owner) {
        if (filters.isEmpty() || owner == null) return@LaunchedEffect
        lifecycle.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            coroutineScope {
                val pending = Channel<Unit>(Channel.CONFLATED)
                launch {
                    try {
                        SupabaseRealtime().changes(filters).collect { event ->
                            connected = event == RealtimeEvent.REFRESH
                            if (connected) pending.trySend(Unit)
                        }
                    } catch (e: CancellationException) { throw e
                    } catch (_: Exception) { connected = false }
                }
                launch {
                    // shortcut: đọc bù 30 giây để phục hồi sự kiện bị lỡ; không phải push khi app đóng.
                    while (isActive) { pending.send(Unit); delay(30_000) }
                }
                try {
                    for (signal in pending) if (SessionStore.userId == owner) latestRefresh()
                } finally { connected = false; pending.cancel() }
            }
        }
    }
    return connected
}
