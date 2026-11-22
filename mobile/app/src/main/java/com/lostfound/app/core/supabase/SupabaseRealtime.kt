package com.lostfound.app.core.supabase

import com.lostfound.app.core.AppConfig
import com.lostfound.app.core.network.Http
import com.lostfound.app.core.session.SessionStore
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.*
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference
import java.util.UUID

data class RealtimeFilter(val table: String, val event: String, val filter: String)
enum class RealtimeEvent { REFRESH, DISCONNECTED }

class SupabaseRealtime(private val sockets: WebSocket.Factory = Http.client) {
    fun changes(filters: List<RealtimeFilter>): Flow<RealtimeEvent> = channelFlow {
        val owner = SessionStore.userId ?: return@channelFlow
        require(filters.isNotEmpty() && filters.distinct().size == filters.size && filters.all {
            val parts = it.filter.split("=eq.", limit = 2)
            val column = when (it.table) {
                "messages" -> "conversation_id"
                "notifications" -> "user_id"
                "claims" -> "id"
                else -> ""
            }
            it.event in setOf("INSERT", "UPDATE") && column.isNotEmpty() && parts.size == 2 &&
                parts[0] == column && runCatching { UUID.fromString(parts[1]).toString() == parts[1] }.getOrDefault(false) &&
                (it.table != "notifications" || parts[1] == owner)
        }) { "Bộ lọc realtime không hợp lệ" }
        SessionStore.session.takeWhile { it?.userId == owner }
            // collectLatest chờ tác vụ cuối khi upstream kết thúc; null buộc hủy socket trước đó.
            .onCompletion { cause -> if (cause == null) emit(null) }
            .collectLatest { session ->
                if (session == null) return@collectLatest
                var backoff = 1_000L
                while (currentCoroutineContext().isActive) {
                    try {
                        connection(filters, owner).collect { event ->
                            backoff = 1_000L
                            send(event)
                        }
                    } catch (e: CancellationException) { throw e
                    } catch (_: Exception) { /* Đọc bù bằng REST sau khi kết nối lại. */ }
                    send(RealtimeEvent.DISCONNECTED)
                    delay(backoff)
                    backoff = (backoff * 2).coerceAtMost(30_000L)
                }
            }
    }.buffer(Channel.CONFLATED)

    private fun connection(filters: List<RealtimeFilter>, owner: String): Flow<RealtimeEvent> = flow {
        val token = SessionStore.requireToken()
        check(SessionStore.userId == owner)
        val url = "${AppConfig.supabaseUrl}/realtime/v1/websocket".toHttpUrl().newBuilder()
            .addQueryParameter("apikey", AppConfig.supabaseKey).addQueryParameter("vsn", "1.0.0").build()
        val frames = Channel<String>(64)
        val heartbeatRef = AtomicReference<String?>(null)
        val heartbeatAck = Channel<String>(Channel.CONFLATED)
        val socket = sockets.newWebSocket(Request.Builder().url(url).build(), object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                val changes = JSONArray(filters.map {
                    JSONObject().put("schema", "public").put("table", it.table).put("event", it.event).put("filter", it.filter)
                })
                webSocket.send(frame("phx_join", JSONObject().put("access_token", token)
                    .put("config", JSONObject().put("postgres_changes", changes))))
            }
            override fun onMessage(webSocket: WebSocket, text: String) {
                if (!frames.trySend(text).isSuccess) frames.close(IOException("Realtime queue overflow"))
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) { frames.close(t) }
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) { webSocket.close(code, null) }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) { frames.close() }
        })
        val registered = mutableMapOf<Long, RealtimeFilter>()
        try {
            coroutineScope {
                val joinTimeout = launch {
                    delay(10_000)
                    frames.close(IOException("Realtime join timeout"))
                }
                val heartbeat = launch {
                    var ref = 1L
                    while (isActive) {
                        delay(15_000)
                        val next = (++ref).toString()
                        heartbeatRef.set(next)
                        check(socket.send(frame("heartbeat", JSONObject(), next, "phoenix")))
                        val acknowledged = withTimeoutOrNull(5_000) {
                            while (heartbeatAck.receive() != next) {}
                            true
                        }
                        if (acknowledged != true) throw IOException("Realtime heartbeat timeout")
                    }
                }
                val refreshToken = launch {
                    while (isActive) { delay(30_000); SessionStore.requireToken() }
                }
                try {
                    for (text in frames) {
                        val message = JSONObject(text)
                        if (message.optString("topic") == "phoenix" && message.optString("event") == "phx_reply" &&
                            message.optString("ref") == heartbeatRef.get()) {
                            check(message.optJSONObject("payload")?.optString("status") == "ok")
                            heartbeatAck.trySend(message.getString("ref"))
                            continue
                        }
                        if (message.optString("topic") != "realtime:android") continue
                        val payload = message.optJSONObject("payload") ?: continue
                        when (message.optString("event")) {
                            "phx_reply" -> if (message.optString("ref") == "1") {
                                check(payload.optString("status") == "ok")
                                val joined = payload.getJSONObject("response").getJSONArray("postgres_changes")
                                for (i in 0 until joined.length()) {
                                    val row = joined.getJSONObject(i)
                                    val filter = filters.single { it.table == row.getString("table") &&
                                        it.event == row.getString("event") && row.getString("schema") == "public" }
                                    registered[row.getLong("id")] = filter
                                }
                                check(registered.size == filters.size)
                                joinTimeout.cancel()
                                emit(RealtimeEvent.REFRESH)
                            }
                            "postgres_changes" -> {
                                val ids = payload.optJSONArray("ids") ?: continue
                                val data = payload.optJSONObject("data") ?: continue
                                if ((0 until ids.length()).any { i ->
                                    val filter = registered[ids.getLong(i)]
                                    filter != null && data.optString("schema") == "public" &&
                                        data.optString("table") == filter.table && data.optString("type") == filter.event &&
                                        data.optJSONObject("record")?.optString(filter.filter.substringBefore("=eq.")) == filter.filter.substringAfter("=eq.")
                                }) emit(RealtimeEvent.REFRESH)
                            }
                            "phx_error", "phx_close" -> throw IOException("Realtime channel closed")
                            "system" -> {
                                check(payload.optString("status") == "ok")
                                if (registered.isNotEmpty()) emit(RealtimeEvent.REFRESH)
                            }
                        }
                    }
                } finally { joinTimeout.cancel(); heartbeat.cancel(); refreshToken.cancel() }
            }
        } finally { socket.cancel(); frames.cancel(); heartbeatAck.cancel() }
    }

    private fun frame(event: String, payload: JSONObject, ref: String = "1", topic: String = "realtime:android") =
        JSONObject().put("topic", topic).put("event", event).put("ref", ref).put("payload", payload).toString()
}
