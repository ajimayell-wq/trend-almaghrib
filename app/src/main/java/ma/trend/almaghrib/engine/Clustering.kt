package ma.trend.almaghrib.engine

import java.time.Duration
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * Regroupement des publications en événements, sans doublons.
 * 1. Déduplication par URL canonique et par titre.
 * 2. Vecteurs TF-IDF (titre compté double).
 * 3. Affectation au centroïde le plus proche (cosinus >= seuil, écart <= 36 h).
 * 4. Passerelles : une tendance Google qui cite l'URL d'un article rejoint son événement.
 */
object Clustering {
    const val SIM_THRESHOLD = 0.30
    private val MAX_GAP: Duration = Duration.ofHours(36)

    fun dedupe(items: List<Article>): List<Article> {
        val urls = HashSet<String>()
        val titles = HashSet<String>()
        return items.filter { it ->
            val key = "${it.sourceId}|${TextNorm.normalize(it.title)}"
            if (it.url in urls || key in titles) false else { urls.add(it.url); titles.add(key); true }
        }
    }

    private fun idf(docs: List<List<String>>): Map<String, Double> {
        val df = HashMap<String, Int>()
        for (toks in docs) for (t in toks.toSet()) df[t] = (df[t] ?: 0) + 1
        val n = docs.size.coerceAtLeast(1)
        return df.mapValues { (_, c) -> ln((n + 1.0) / (c + 1.0)) + 1.0 }
    }

    private fun vector(tokens: List<String>, idf: Map<String, Double>): Map<String, Double> {
        val tf = tokens.groupingBy { it }.eachCount()
        val vec = tf.mapValues { (t, c) -> (1 + ln(c.toDouble())) * (idf[t] ?: 1.0) }
        val norm = sqrt(vec.values.sumOf { it * it }).takeIf { it > 0 } ?: 1.0
        return vec.mapValues { it.value / norm }
    }

    fun cosine(a: Map<String, Double>, b: Map<String, Double>): Double {
        val (s, l) = if (a.size <= b.size) a to b else b to a
        val dot = s.entries.sumOf { (t, v) -> v * (l[t] ?: 0.0) }
        val na = sqrt(a.values.sumOf { it * it }).takeIf { it > 0 } ?: 1.0
        val nb = sqrt(b.values.sumOf { it * it }).takeIf { it > 0 } ?: 1.0
        return dot / (na * nb)
    }

    fun cluster(input: List<Article>, threshold: Double = SIM_THRESHOLD): List<Cluster> {
        val items = dedupe(input)
        val tokens = items.map { TextNorm.tokenize(it.text) }
        val idf = idf(tokens)
        val vectors = tokens.map { vector(it, idf) }
        val order = items.indices.sortedWith(compareBy({ items[it].publishedAt == null }, { items[it].publishedAt }))
        val clusters = ArrayList<Cluster>()
        val index = HashMap<String, MutableSet<Int>>() // jeton -> événements candidats

        for (i in order) {
            val it = items[i]
            val vec = vectors[i]
            if (vec.isEmpty()) continue
            val candidates = HashSet<Int>()
            for (t in vec.keys) if ((idf[t] ?: 0.0) > 1.5) index[t]?.let { candidates.addAll(it) }
            var best = -1
            var bestSim = 0.0
            for (c in candidates) {
                val last = clusters[c].lastSeen
                if (it.publishedAt != null && last != null && Duration.between(last, it.publishedAt) > MAX_GAP) continue
                val sim = cosine(vec, clusters[c].vector)
                if (sim > bestSim) { best = c; bestSim = sim }
            }
            if (best >= 0 && bestSim >= threshold) {
                clusters[best].items.add(it)
                clusters[best].addVector(vec)
                for (t in vec.keys) index.getOrPut(t) { HashSet() }.add(best)
            } else {
                clusters.add(Cluster(mutableListOf(it), vec.toMutableMap()))
                for (t in vec.keys) index.getOrPut(t) { HashSet() }.add(clusters.size - 1)
            }
        }
        return mergeByLinks(clusters)
    }

    private fun mergeByLinks(clusters: List<Cluster>): List<Cluster> {
        val owner = HashMap<String, Int>()
        clusters.forEachIndexed { idx, c -> c.items.forEach { owner.putIfAbsent(it.url, idx) } }
        val parent = IntArray(clusters.size) { it }
        fun find(x: Int): Int {
            var y = x
            while (parent[y] != y) { parent[y] = parent[parent[y]]; y = parent[y] }
            return y
        }
        clusters.forEachIndexed { idx, c ->
            for (it in c.items) for (u in it.relatedUrls) {
                val other = owner[u] ?: continue
                parent[find(idx)] = find(other)
            }
        }
        val groups = LinkedHashMap<Int, Cluster>()
        clusters.forEachIndexed { idx, c ->
            val g = groups.getOrPut(find(idx)) { Cluster() }
            g.items.addAll(c.items)
            g.addVector(c.vector)
        }
        return groups.values.toList()
    }
}
