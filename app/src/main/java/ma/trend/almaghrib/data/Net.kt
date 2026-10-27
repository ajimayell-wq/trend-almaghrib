package ma.trend.almaghrib.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import ma.trend.almaghrib.engine.Http
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class HttpError(val code: Int, body: String) : IOException("HTTP $code ${body.take(200)}")

/** Accès réseau direct du téléphone vers les sources (aucun serveur intermédiaire). */
class OkHttpTransport : Http {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(18, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    override suspend fun get(url: String, headers: Map<String, String>): ByteArray = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(url).apply { headers.forEach { (k, v) -> header(k, v) } }.build()
        val call = client.newCall(req)
        suspendCancellableCoroutine { cont ->
            cont.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) { if (cont.isActive) cont.resumeWithException(e) }
                override fun onResponse(call: Call, response: Response) {
                    response.use { r ->
                        val bytes = runCatching { r.body?.bytes() ?: ByteArray(0) }
                        if (!cont.isActive) return
                        bytes.fold(
                            onSuccess = { b -> if (r.isSuccessful) cont.resume(b) else cont.resumeWithException(HttpError(r.code, String(b))) },
                            onFailure = { cont.resumeWithException(it) },
                        )
                    }
                }
            })
        }
    }
}
