package com.tutorials.chessscanner.util

import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import org.json.JSONObject
import java.net.URL

private val client = HttpClient(OkHttp) {
    install(ContentNegotiation) {
        json()
    }
}

@Serializable
data class StockfishResponse(
    val success: Boolean,
    val evaluation: Double? = null,
    val mate: Int? = null,
    val bestmove: String? = null,
    val continuation: String? = null,
    val data: String? = null
)

fun fetchStockfishEval(
    fen: String,
    callback: (Double?, Int?, String?) -> Unit
) {
    val url = "https://stockfish.online/api/s/v2.php?fen=${fen}&depth=12"

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val response: StockfishResponse = client.get(url).body()

            withContext(Dispatchers.Main) {
                if (response.success) {
                    callback(response.evaluation, response.mate, response.continuation)
                } else {
                    callback(null, null, null)
                }
            }
        } catch (e: Exception) {
            Log.e("StockfishFetch", "Error: ${e.message}")
            withContext(Dispatchers.Main) {
                callback(null, null, null)
            }
        }
    }
}
