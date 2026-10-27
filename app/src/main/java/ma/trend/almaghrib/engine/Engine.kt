package ma.trend.almaghrib.engine

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Duration
import java.time.Instant

/** Accès réseau minimal : OkHttp dans l'application, simulé dans les tests. */
interface Http {
    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): ByteArray
}

data class SourceResult(val source: Source, val ok: Boolean, val items: List<Article> = emptyList(), val error: String = "")

data class Topic(
    val id: Int,
    val title: String,
    /** Extrait d'un article de la source la plus représentative (aucune génération de texte). */
    val summary: String,
    val summarySource: String,
    val items: List<Article>,
    val categories: List<String>,
    val score: Double,
    val breakdown: Scoring.Breakdown,
    val reliability: String,
    val trend: String,
    val firstSeen: Instant?,
    val lastSeen: Instant?,
    val platforms: List<String>,
    val imageUrl: String?,
    val emerging: Boolean,
    val controversial: Boolean,
    /** Publications par heure sur les 24 dernières heures (mesure réelle, sert aux mini-graphiques). */
    val hourly: IntArray,
    /** Publications des 6 dernières heures et des 6 heures précédentes (base de l'indicateur de progression). */
    val last6h: Int,
    val prev6h: Int,
) {
    /** Progression en % entre les deux fenêtres de 6 h ; null quand la fenêtre précédente est vide (= nouveau). */
    val progression: Int? get() = if (prev6h == 0) null else Math.round((last6h - prev6h) * 100.0 / prev6h).toInt()
    val hasVideo: Boolean get() = "youtube" in platforms
    val totalViews: Long? get() = items.mapNotNull { it.metrics["views"] }.takeIf { it.isNotEmpty() }?.sum()
    val googleTraffic: Long? get() = items.mapNotNull { it.metrics["approx_traffic"] }.takeIf { it.isNotEmpty() }?.sum()
    val mediaCount: Int get() = breakdown.distinctMedia
}

data class PlatformCoverage(
    val total: Int = 0,
    val active: Int = 0,
    val publications: Int = 0,
    val failing: List<String> = emptyList(),
    val unavailable: Boolean = false,
)

/** Moteur de « ترند المغرب », exécuté sur le téléphone. */
class Engine(private val http: Http, private val userAgent: String = "TrendAlMaghrib/1.0 (Android)") {
    companion object {
        /** Durée de conservation du cache local : 24 h affichées + 24 h pour comparer. */
        val KEEP: Duration = Duration.ofHours(48)
        private val EMERGING: Duration = Duration.ofHours(3)
        private val redditFooter = Regex("submitted by\\s+/u/\\S+.*$", RegexOption.DOT_MATCHES_ALL)
    }

    /** Interroge toutes les sources en parallèle ; une source en panne ne bloque jamais les autres. */
    suspend fun collect(extra: List<suspend () -> SourceResult> = emptyList()): List<SourceResult> = coroutineScope {
        (Sources.ALL.map { s -> async { fetch(s) } } + extra.map { f -> async { f() } }).awaitAll()
    }

    private suspend fun fetch(s: Source): SourceResult = try {
        val bytes = withTimeoutOrNull(20_000) { http.get(s.url, mapOf("User-Agent" to userAgent)) }
        if (bytes == null) {
            SourceResult(s, ok = false, error = "timeout")
        } else {
            val items = when (s.kind) {
                "gtrends_rss" -> Parsers.parseGoogleTrends(bytes, s)
                "youtube_rss" -> Parsers.parseYoutubeRss(bytes, s)
                "reddit_rss" -> Parsers.parseRss(bytes, s).map { it.copy(summary = redditFooter.replace(it.summary, "").trim()) }
                else -> Parsers.parseRss(bytes, s)
            }
            SourceResult(s, ok = true, items = items)
        }
    } catch (e: Exception) {
        SourceResult(s, ok = false, error = (e.message ?: e.javaClass.simpleName).take(160))
    }

    /** Regroupe, classe et note les publications (issues du cache local). */
    fun analyze(articles: List<Article>, now: Instant, prevScores: Map<Int, Double> = emptyMap()): List<Topic> {
        val oldest = now.minus(KEEP)
        val usable = articles.filter { a ->
            val p = a.publishedAt
            // Sans date fiable, on ne peut pas garantir la fraîcheur : la publication est écartée.
            p != null && p >= oldest && p <= now.plusSeconds(3600)
        }
        return Clustering.cluster(usable).map { c -> toTopic(c, now, prevScores) }.sortedByDescending { it.score }
    }

    private fun toTopic(c: Cluster, now: Instant, prevScores: Map<Int, Double>): Topic {
        val (score, bd) = Scoring.score(c, now)
        val first = c.firstSeen
        val earliest = c.items.minByOrNull { it.publishedAt ?: now }!!
        val id = earliest.url.hashCode() and 0x7fffffff
        val rep = c.representative
        val (summary, summarySource) = extract(c)
        val emerging = first != null && Duration.between(first, now) <= EMERGING &&
            (bd.distinctMedia >= 2 || (bd.distinctMedia >= 1 && ("google_trends" in c.platforms || "youtube" in c.platforms)))
        fun inWindow(fromH: Long, toH: Long) = c.items.count {
            val p = it.publishedAt ?: return@count false
            val d = Duration.between(p, now)
            d >= Duration.ofHours(fromH) && d < Duration.ofHours(toH)
        }
        return Topic(
            id = id,
            title = rep.title,
            summary = summary,
            summarySource = summarySource,
            items = c.items.sortedByDescending { it.publishedAt },
            categories = Categorizer.categorize(c),
            score = score,
            breakdown = bd,
            reliability = Scoring.reliability(c),
            trend = Scoring.trendDirection(score, prevScores[id], bd.velocity, first, now),
            firstSeen = first,
            lastSeen = c.lastSeen,
            platforms = c.platforms,
            imageUrl = c.items.firstOrNull { it.imageUrl != null && it.platform in Sources.NEWS }?.imageUrl
                ?: c.items.firstOrNull { it.imageUrl != null }?.imageUrl,
            emerging = emerging,
            controversial = Categorizer.isControversial(c.items),
            hourly = Opinion.hourly(c.items.map { it.publishedAt }, now),
            last6h = inWindow(0, 6),
            prev6h = inWindow(6, 12),
        )
    }

    /**
     * Résumé extractif : l'extrait de l'article le plus « central » du groupe
     * (celui dont le titre partage le plus de mots avec les autres). Aucun texte n'est inventé.
     */
    private fun extract(c: Cluster): Pair<String, String> {
        val candidates = c.items.filter { it.summary.length >= 40 && it.platform != "reddit" }
        if (candidates.isEmpty()) return "" to ""
        val tokens = c.items.associateWith { TextNorm.tokenize(it.title).toSet() }
        val best = candidates.maxWithOrNull(compareBy<Article> { a ->
            val ta = tokens.getValue(a)
            c.items.filter { it !== a }.sumOf { b ->
                val tb = tokens.getValue(b)
                val union = (ta + tb).size
                if (union == 0) 0.0 else (ta intersect tb).size.toDouble() / union
            }
        }.thenBy { it.publishedAt })!!
        var text = best.summary
        if (text.length > 320) {
            val cut = text.take(320)
            val end = maxOf(cut.lastIndexOf(". "), cut.lastIndexOf("، "), cut.lastIndexOf("? "))
            text = if (end > 120) cut.take(end + 1) else cut.substringBeforeLast(' ') + " …"
        }
        return text to best.sourceName
    }

    data class SourceState(val sourceId: String, val name: String, val platform: String, val ok: Boolean, val count: Int)

    fun coverage(states: List<SourceState>): Map<String, PlatformCoverage> {
        val out = LinkedHashMap<String, PlatformCoverage>()
        for (st in states) {
            val p = out[st.platform] ?: PlatformCoverage()
            out[st.platform] = p.copy(
                total = p.total + 1,
                active = p.active + if (st.ok) 1 else 0,
                publications = p.publications + if (st.ok) st.count else 0,
                failing = if (!st.ok) p.failing + st.name else p.failing,
            )
        }
        for (name in Sources.UNAVAILABLE) out[name] = PlatformCoverage(unavailable = true)
        return out
    }
}
