package ma.trend.almaghrib.engine

import java.time.Instant

/** Une publication collectée : article, recherche Google Trends, vidéo YouTube, discussion Reddit. */
data class Article(
    val sourceId: String,
    val sourceName: String,
    /** media | institution | aggregator | google_trends | youtube | reddit */
    val platform: String,
    val url: String,
    val title: String,
    val summary: String = "",
    val lang: String = "",
    val publishedAt: Instant? = null,
    val tags: List<String> = emptyList(),
    val imageUrl: String? = null,
    /** Mesures réellement fournies par la plateforme (jamais estimées) : views, likes, comments, approx_traffic. */
    val metrics: Map<String, Long> = emptyMap(),
    val relatedUrls: List<String> = emptyList(),
    val relatedTitles: List<String> = emptyList(),
    val institutional: Boolean = false,
    /** Identifiant de la vidéo YouTube, le cas échéant. */
    val videoId: String? = null,
) {
    /** Le titre compte double dans l'analyse. */
    val text: String
        get() = listOf(title, title, summary).plus(relatedTitles).filter { it.isNotBlank() }.joinToString(" ")
}

/** Un événement : groupe de publications parlant de la même chose. */
class Cluster(
    val items: MutableList<Article> = mutableListOf(),
    val vector: MutableMap<String, Double> = mutableMapOf(),
) {
    val firstSeen: Instant? get() = items.mapNotNull { it.publishedAt }.minOrNull()
    val lastSeen: Instant? get() = items.mapNotNull { it.publishedAt }.maxOrNull()
    val platforms: List<String> get() = items.map { it.platform }.toSortedSet().toList()

    /** Titre représentatif : un média de préférence, le plus récent. */
    val representative: Article
        get() {
            val media = items.filter { it.platform == "media" || it.platform == "institution" || it.platform == "aggregator" }
            val pool = media.ifEmpty { items }
            return pool.maxByOrNull { it.publishedAt ?: Instant.EPOCH }!!
        }

    fun addVector(v: Map<String, Double>) {
        for ((t, x) in v) vector[t] = (vector[t] ?: 0.0) + x
    }
}

/** Un commentaire public (Reddit, YouTube). Le nom d'auteur n'est jamais conservé ni affiché. */
data class Comment(
    val platform: String,
    val text: String,
    val publishedAt: Instant?,
    val likes: Long? = null,
)
