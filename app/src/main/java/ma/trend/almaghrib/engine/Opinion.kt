package ma.trend.almaghrib.engine

import java.time.Duration
import java.time.Instant

/**
 * « ماذا يقول المغاربة؟ » : synthèse des réactions publiques réellement collectées pour un sujet
 * (commentaires Reddit et, avec une clé, YouTube). Tout est extrait des textes collectés :
 * aucune opinion n'est rédigée ni inventée, et la taille de l'échantillon est toujours affichée.
 */
data class OpinionReport(
    val sampleSize: Int,
    val platforms: List<String>,
    val positive: Int,
    val negative: Int,
    val neutral: Int,
    /** Mots les plus répétés (forme d'origine la plus fréquente). Source : commentaires, sinon titres. */
    val topWords: List<Pair<String, Int>>,
    val wordsFromComments: Boolean,
    val supportive: List<Comment>,
    val opposing: List<Comment>,
    val highlights: List<Comment>,
    /** Activité par heure sur 24 h (publications + commentaires datés), de la plus ancienne à maintenant. */
    val hourly: IntArray,
    val peakHoursAgo: Int?,
) {
    fun pct(v: Int): Int = if (sampleSize == 0) 0 else Math.round(v * 100.0 / sampleSize).toInt()
}

object Opinion {
    private val wordRe = Regex("[\\p{L}\\p{N}_']+")
    private val urlRe = Regex("https?://\\S+")

    fun hourly(times: List<Instant?>, now: Instant, hours: Int = 24): IntArray {
        val bins = IntArray(hours)
        for (t in times) {
            if (t == null) continue
            val ago = Duration.between(t, now).toMinutes() / 60.0
            if (ago < 0 || ago >= hours) continue
            bins[hours - 1 - ago.toInt()]++
        }
        return bins
    }

    fun topWords(texts: List<String>, limit: Int = 8): List<Pair<String, Int>> {
        val counts = HashMap<String, Int>()
        val surface = HashMap<String, HashMap<String, Int>>()
        for (t in texts) {
            val clean = urlRe.replace(t, " ")
            val seen = HashSet<String>()
            for (m in wordRe.findAll(clean)) {
                val raw = m.value
                val key = TextNorm.tokenize(raw).firstOrNull() ?: continue
                if (key.length < 3 || !seen.add(key)) continue
                counts[key] = (counts[key] ?: 0) + 1
                val sf = surface.getOrPut(key) { HashMap() }
                sf[raw] = (sf[raw] ?: 0) + 1
            }
        }
        return counts.entries.filter { it.value >= 2 }.sortedByDescending { it.value }.take(limit)
            .map { (k, v) -> (surface[k]?.maxByOrNull { it.value }?.key ?: k) to v }
    }

    private fun pick(list: List<Comment>, n: Int): List<Comment> =
        list.filter { it.text.length in 15..280 }
            .sortedWith(compareByDescending<Comment> { it.likes ?: 0 }.thenByDescending { minOf(it.text.length, 160) })
            .take(n)

    fun analyze(topicItems: List<Article>, comments: List<Comment>, now: Instant): OpinionReport {
        val cls = comments.map { it to Sentiment.classify(it.text) }
        val pos = cls.filter { it.second > 0 }.map { it.first }
        val neg = cls.filter { it.second < 0 }.map { it.first }
        val fromComments = comments.size >= 5
        val words = if (fromComments) topWords(comments.map { it.text })
        else topWords(topicItems.map { it.title }, 7)
        val hourly = hourly(topicItems.map { it.publishedAt } + comments.map { it.publishedAt }, now)
        val peak = hourly.indices.maxByOrNull { hourly[it] }?.takeIf { hourly[it] > 0 }?.let { 23 - it }
        return OpinionReport(
            sampleSize = comments.size,
            platforms = comments.map { it.platform }.distinct(),
            positive = pos.size, negative = neg.size, neutral = comments.size - pos.size - neg.size,
            topWords = words, wordsFromComments = fromComments,
            supportive = pick(pos, 2), opposing = pick(neg, 2),
            highlights = pick(comments, 3),
            hourly = hourly, peakHoursAgo = peak,
        )
    }
}
