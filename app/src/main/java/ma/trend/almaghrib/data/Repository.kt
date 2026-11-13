package ma.trend.almaghrib.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import ma.trend.almaghrib.engine.Article
import ma.trend.almaghrib.engine.Categorizer
import ma.trend.almaghrib.engine.Cluster
import ma.trend.almaghrib.engine.Comment
import ma.trend.almaghrib.engine.Engine
import ma.trend.almaghrib.engine.Opinion
import ma.trend.almaghrib.engine.OpinionReport
import ma.trend.almaghrib.engine.Parsers
import ma.trend.almaghrib.engine.PlatformCoverage
import ma.trend.almaghrib.engine.Source
import ma.trend.almaghrib.engine.SourceResult
import ma.trend.almaghrib.engine.Sources
import ma.trend.almaghrib.engine.Topic
import java.net.URLEncoder
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

/** Vidéo avec ses mesures réelles et sa vitesse de diffusion calculée. */
data class VideoItem(
    val article: Article,
    val categories: List<String>,
    val controversial: Boolean,
    /** Vues par heure depuis la publication (vues réelles ÷ heures écoulées). */
    val viewsPerHour: Double?,
    /** Vues gagnées depuis l'actualisation précédente, et durée correspondante. */
    val deltaViews: Long?,
    val deltaMinutes: Long?,
    val fromTrendingChart: Boolean,
    /** Rang dans la liste officielle « tendances YouTube au Maroc » (clé YouTube), sinon null. */
    val chartRank: Int? = null,
)

data class AppState(
    val topics: List<Topic> = emptyList(),
    val videos: List<VideoItem> = emptyList(),
    val coverage: Map<String, PlatformCoverage> = emptyMap(),
    val sources: List<CachedSourceState> = emptyList(),
    val lastRefresh: Instant? = null,
    val refreshing: Boolean = false,
    val error: String? = null,
    val posts24h: Int = 0,
    val hasYoutubeKey: Boolean = false,
    val loaded: Boolean = false,
    /** Recherches en hausse sur Google au Maroc (24 h), les plus recherchées d'abord. */
    val searches: List<Article> = emptyList(),
    /** Ancien identifiant de sujet -> identifiant actuel (un sujet change d'identifiant quand ses articles évoluent). */
    val aliases: Map<Int, Int> = emptyMap(),
) {
    /** Retrouve un sujet même si son identifiant a changé depuis l'actualisation précédente. */
    fun find(id: Int): Topic? = topics.firstOrNull { it.id == id }
        ?: aliases[id]?.let { a -> topics.firstOrNull { it.id == a } }
}

class Repository(context: Context) {
    private val store = Store(context)
    private val http = OkHttpTransport()
    private val engine = Engine(http)
    private val mutex = Mutex()
    private val _state = MutableStateFlow(AppState(hasYoutubeKey = store.youtubeKey.isNotBlank()))
    val state: StateFlow<AppState> = _state.asStateFlow()
    private var snapshot = Snapshot()
    private var trending: List<Article> = emptyList()
    private val opinionCache = HashMap<Int, Pair<Instant, OpinionResult>>()
    private var aliases: Map<Int, Int> = emptyMap()
    private var lastTopicUrls: Map<Int, Set<String>> = emptyMap()

    var youtubeKey: String
        get() = store.youtubeKey
        set(v) { store.youtubeKey = v; _state.update { it.copy(hasYoutubeKey = v.isNotBlank()) } }

    var history: List<String>
        get() = store.history
        set(v) { store.history = v }

    suspend fun load() = withContext(Dispatchers.Default) {
        if (_state.value.loaded) return@withContext
        snapshot = withContext(Dispatchers.IO) { store.load() }
        publish(Instant.now())
    }

    fun isStale(maxAge: Duration = Duration.ofMinutes(10)): Boolean {
        val last = _state.value.lastRefresh ?: return true
        return Duration.between(last, Instant.now()) > maxAge
    }

    /** Actualisation : le téléphone interroge lui-même chaque source, puis analyse localement. */
    suspend fun refresh() = mutex.withLock {
        _state.update { it.copy(refreshing = true, error = null) }
        try {
            withContext(Dispatchers.Default) {
                val key = store.youtubeKey
                val extra = if (key.isBlank()) emptyList() else listOf<suspend () -> SourceResult>({ fetchTrendingChart(key) })
                val results = engine.collect(extra)
                val now = Instant.now()
                if (results.none { it.ok }) {
                    _state.update { it.copy(error = "تعذر الوصول إلى المصادر. تحقق من اتصالك بالإنترنت.") }
                    return@withContext
                }
                val chart = results.firstOrNull { it.source.id == CHART_ID }
                if (chart?.ok == true) trending = chart.items
                val fresh = results.filter { it.source.id != CHART_ID }.flatMap { it.items }.toMutableList()
                if (key.isNotBlank()) enrichYoutube(fresh, key)
                // Fusion avec le cache : la version la plus récente d'une publication remplace l'ancienne.
                val oldest = now.minus(Engine.KEEP)
                val merged = LinkedHashMap<String, CachedArticle>()
                snapshot.articles.forEach { merged[it.url] = it }
                fresh.forEach { merged[it.url] = CachedArticle.of(it) }
                val kept = merged.values.filter { a -> a.publishedAt?.let { Instant.ofEpochSecond(it) >= oldest } ?: false }
                val states = results.map {
                    CachedSourceState(it.source.id, it.source.name, it.source.platform, it.ok, it.items.size, it.error)
                }
                val prevScores = snapshot.scores
                val views = snapshot.views
                snapshot = Snapshot(kept, states, now.epochSecond, prevScores, views)
                publish(now, rotate = true)
                withContext(Dispatchers.IO) { store.save(snapshot) }
            }
        } catch (e: Exception) {
            _state.update { it.copy(error = "حدث خطأ أثناء التحديث. حاول مرة أخرى.") }
        } finally {
            _state.update { it.copy(refreshing = false) }
        }
    }

    private fun publish(now: Instant, rotate: Boolean = false) {
        val articles = snapshot.articles.map { it.toArticle() }
        val topics = engine.analyze(articles, now, snapshot.scores)
        val videos = buildVideos(articles, now)
        if (rotate) {
            // Les scores et les vues de cette actualisation servent de référence à la suivante.
            snapshot = snapshot.copy(
                scores = topics.take(200).associate { it.id to it.score },
                views = (articles + trending).filter { it.platform == "youtube" && "views" in it.metrics }
                    .associate { it.url to ViewSnapshot(it.metrics.getValue("views"), now.epochSecond) },
            )
        }
        val states = snapshot.sources.map { Engine.SourceState(it.id, it.name, it.platform, it.ok, it.count) }
        val day = now.minus(Duration.ofHours(24))
        val shown = topics.filter { t -> t.lastSeen?.isAfter(day) == true }
        trackTopics(shown)
        _state.update {
            it.copy(
                topics = shown,
                aliases = aliases,
                videos = videos,
                coverage = engine.coverage(states),
                sources = snapshot.sources,
                lastRefresh = snapshot.lastRefresh?.let { s -> Instant.ofEpochSecond(s) },
                posts24h = articles.count { a -> a.publishedAt?.isAfter(day) == true },
                searches = articles.filter { a -> a.platform == "google_trends" && a.publishedAt?.isAfter(day) == true }
                    .distinctBy { a -> a.title.lowercase() }
                    .sortedWith(compareByDescending<Article> { a -> a.metrics["approx_traffic"] ?: 0L }.thenByDescending { a -> a.publishedAt }),
                loaded = true,
            )
        }
    }

    /**
     * Un sujet est identifié par son article le plus ancien : cet identifiant change quand ses articles évoluent.
     * On relie chaque ancien sujet au nouveau sujet qui partage le plus d'articles avec lui,
     * pour qu'une fiche ouverte reste sur le même sujet après une actualisation.
     */
    private fun trackTopics(shown: List<Topic>) {
        val newIds = shown.map { it.id }.toSet()
        val owner = HashMap<String, Int>()
        shown.forEach { t -> t.items.forEach { a -> owner.putIfAbsent(a.url, t.id) } }
        fun successor(urls: Set<String>): Int? =
            urls.mapNotNull { owner[it] }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
        val next = HashMap<Int, Int>()
        lastTopicUrls.forEach { (oldId, urls) -> if (oldId !in newIds) successor(urls)?.let { next[oldId] = it } }
        aliases.forEach { (k, v) ->
            if (k !in newIds) {
                val target = if (v in newIds) v else next[v]
                if (target != null) next[k] = target
            }
        }
        aliases = next
        lastTopicUrls = shown.associate { t -> t.id to t.items.map { it.url }.toSet() }
    }

    private fun buildVideos(articles: List<Article>, now: Instant): List<VideoItem> {
        val prev = snapshot.views
        val fromFeeds = articles.filter { it.platform == "youtube" && it.publishedAt?.isAfter(now.minus(Duration.ofHours(48))) == true }
        val all = (trending.map { it to true } + fromFeeds.map { it to false }).distinctBy { it.first.url }
        val rank = trending.mapIndexed { i, a -> a.url to i + 1 }.toMap()
        return all.map { (a, chart) ->
            val views = a.metrics["views"]
            val hours = a.publishedAt?.let { Duration.between(it, now).toMinutes() / 60.0 }?.coerceAtLeast(0.5)
            val p = prev[a.url]
            val delta = if (views != null && p != null && views >= p.views) views - p.views else null
            val dMin = if (p != null) Duration.between(Instant.ofEpochSecond(p.at), now).toMinutes() else null
            val c = Cluster(mutableListOf(a))
            VideoItem(
                article = a,
                categories = Categorizer.categorize(c),
                controversial = Categorizer.isControversial(listOf(a)),
                viewsPerHour = if (views != null && hours != null) views / hours else null,
                deltaViews = delta, deltaMinutes = dMin?.takeIf { delta != null && it > 0 },
                fromTrendingChart = chart,
                chartRank = rank[a.url],
            )
        }.sortedByDescending { it.viewsPerHour ?: 0.0 }
    }

    // ---------- YouTube (clé facultative) ----------

    private val chartSource = Source(CHART_ID, "YouTube · الرائج في المغرب", "youtube", "api", "", "multi")

    private suspend fun fetchTrendingChart(key: String): SourceResult = try {
        val url = "https://www.googleapis.com/youtube/v3/videos?part=snippet,statistics&chart=mostPopular&regionCode=MA&maxResults=30&key=" +
            enc(key)
        val body = withTimeoutOrNull(20_000) { String(http.get(url)) }
        if (body == null) SourceResult(chartSource, false, error = "timeout")
        else SourceResult(chartSource, true, Parsers.parseYoutubeApi(body, chartSource))
    } catch (e: Exception) {
        SourceResult(chartSource, false, error = (e.message ?: "").take(160))
    }

    /** Ajoute le nombre de commentaires et de « j'aime » réels aux vidéos des flux (1 unité de quota). */
    private suspend fun enrichYoutube(items: MutableList<Article>, key: String) {
        val ids = items.filter { it.platform == "youtube" && it.videoId != null }.sortedByDescending { it.publishedAt }
            .mapNotNull { it.videoId }.distinct().take(50)
        if (ids.isEmpty()) return
        val body = runCatching {
            withTimeoutOrNull(15_000) {
                String(http.get("https://www.googleapis.com/youtube/v3/videos?part=statistics&id=${ids.joinToString(",")}&key=${enc(key)}"))
            }
        }.getOrNull() ?: return
        val stats = runCatching { Parsers.parseYoutubeApi(fixStatsOnly(body), chartSource) }.getOrNull() ?: return
        val byId = stats.associateBy { it.videoId }
        for (i in items.indices) {
            val a = items[i]
            val s = byId[a.videoId] ?: continue
            items[i] = a.copy(metrics = a.metrics + s.metrics)
        }
    }

    /** La réponse « statistics » seule n'a pas de snippet : on en ajoute un vide pour réutiliser l'analyseur. */
    private fun fixStatsOnly(body: String): String = body.replace("\"statistics\"", "\"snippet\":{},\"statistics\"")

    // ---------- « ماذا يقول المغاربة؟ » ----------

    data class OpinionResult(val report: OpinionReport, val sources: List<String>, val notes: List<String>)

    /** Collecte les commentaires publics liés au sujet, puis les analyse (à la demande, jamais en arrière-plan). */
    suspend fun opinion(topic: Topic): OpinionResult = withContext(Dispatchers.Default) {
        val now = Instant.now()
        // Même sujet ouvert il y a moins de 15 minutes : on réutilise l'analyse (évite les blocages Reddit et le quota YouTube).
        opinionCache[topic.id]?.let { (at, cached) ->
            if (Duration.between(at, now) < Duration.ofMinutes(15)) return@withContext cached
        }
        val notes = mutableListOf<String>()
        val key = store.youtubeKey
        val sources = mutableListOf<String>()
        val comments = coroutineScope {
            val jobs = mutableListOf<kotlinx.coroutines.Deferred<List<Comment>>>()
            topic.items.filter { it.platform == "reddit" }.take(3).forEach { a ->
                jobs += async { runCatching { Parsers.parseRedditComments(http.get(a.url.trimEnd('/') + "/.rss", UA)) }.getOrDefault(emptyList()) }
                sources += "Reddit"
            }
            if (key.isNotBlank()) {
                var vids = topic.items.mapNotNull { it.videoId }.distinct().take(3)
                if (vids.isEmpty()) vids = searchVideos(topic.title, key, now)
                vids.forEach { id -> jobs += async { youtubeComments(id, key) } }
                if (vids.isNotEmpty()) sources += "YouTube"
            } else {
                notes += "أضف مفتاح YouTube في الإعدادات لتحليل تعليقات يوتيوب حول هذا الموضوع."
            }
            jobs.awaitAll().flatten()
        }.distinctBy { it.text }
        val report = Opinion.analyze(topic.items, comments, now)
        val result = OpinionResult(report, sources.distinct(), notes)
        opinionCache[topic.id] = now to result
        result
    }

    private suspend fun searchVideos(q: String, key: String, now: Instant): List<String> = runCatching {
        val after = now.minus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS).toString()
        val url = "https://www.googleapis.com/youtube/v3/search?part=snippet&type=video&regionCode=MA&order=relevance&maxResults=3" +
            "&publishedAfter=${enc(after)}&q=${enc(q.take(80))}&key=${enc(key)}"
        Parsers.parseYoutubeApi(String(http.get(url)), chartSource).mapNotNull { it.videoId }
    }.getOrDefault(emptyList())

    private suspend fun youtubeComments(id: String, key: String): List<Comment> = runCatching {
        val url = "https://www.googleapis.com/youtube/v3/commentThreads?part=snippet&videoId=$id&maxResults=60&order=relevance&textFormat=plainText&key=${enc(key)}"
        Parsers.parseYoutubeComments(String(http.get(url)))
    }.getOrDefault(emptyList())

    /** Vérifie une clé YouTube avant de l'enregistrer. */
    suspend fun testYoutubeKey(key: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val body = String(http.get("https://www.googleapis.com/youtube/v3/videos?part=id&chart=mostPopular&regionCode=MA&maxResults=1&key=${enc(key)}"))
            body.contains("\"items\"")
        }.getOrDefault(false)
    }

    fun topic(id: Int): Topic? = _state.value.find(id)

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    companion object {
        const val CHART_ID = "yt_chart_ma"
        private val UA = mapOf("User-Agent" to "android:ma.trend.almaghrib:v1.0 (lecture de flux publics)")
        val TOTAL_SOURCES get() = Sources.ALL.size
    }
}
