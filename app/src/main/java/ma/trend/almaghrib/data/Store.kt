package ma.trend.almaghrib.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ma.trend.almaghrib.engine.Article
import java.io.File
import java.time.Instant

/** Copie sérialisable d'une publication (cache local, 48 h au maximum). */
@Serializable
data class CachedArticle(
    val sourceId: String,
    val sourceName: String,
    val platform: String,
    val url: String,
    val title: String,
    val summary: String = "",
    val lang: String = "",
    val publishedAt: Long? = null,
    val tags: List<String> = emptyList(),
    val imageUrl: String? = null,
    val metrics: Map<String, Long> = emptyMap(),
    val relatedUrls: List<String> = emptyList(),
    val relatedTitles: List<String> = emptyList(),
    val institutional: Boolean = false,
    val videoId: String? = null,
) {
    fun toArticle() = Article(sourceId, sourceName, platform, url, title, summary, lang,
        publishedAt?.let { Instant.ofEpochSecond(it) }, tags, imageUrl, metrics, relatedUrls, relatedTitles, institutional, videoId)

    companion object {
        fun of(a: Article) = CachedArticle(a.sourceId, a.sourceName, a.platform, a.url, a.title, a.summary, a.lang,
            a.publishedAt?.epochSecond, a.tags, a.imageUrl, a.metrics, a.relatedUrls, a.relatedTitles, a.institutional, a.videoId)
    }
}

@Serializable
data class CachedSourceState(val id: String, val name: String, val platform: String, val ok: Boolean, val count: Int, val error: String = "")

/** Mesure de vues observée lors d'une actualisation (sert à calculer la progression réelle). */
@Serializable
data class ViewSnapshot(val views: Long, val at: Long)

@Serializable
data class Snapshot(
    val articles: List<CachedArticle> = emptyList(),
    val sources: List<CachedSourceState> = emptyList(),
    val lastRefresh: Long? = null,
    val scores: Map<Int, Double> = emptyMap(),
    val views: Map<String, ViewSnapshot> = emptyMap(),
)

/** Stockage local : un seul fichier JSON dans l'espace privé de l'application. */
class Store(context: Context) {
    private val file = File(context.filesDir, "cache.json")
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

    fun load(): Snapshot = runCatching { json.decodeFromString<Snapshot>(file.readText()) }.getOrDefault(Snapshot())

    fun save(s: Snapshot) {
        val tmp = File(file.parentFile, "cache.json.tmp")
        tmp.writeText(json.encodeToString(s))
        tmp.renameTo(file)
    }

    var youtubeKey: String
        get() = prefs.getString("youtube_key", "").orEmpty()
        set(v) = prefs.edit().putString("youtube_key", v.trim()).apply()

    var history: List<String>
        get() = prefs.getString("history", "").orEmpty().split('\n').filter { it.isNotBlank() }
        set(v) = prefs.edit().putString("history", v.take(10).joinToString("\n")).apply()
}
