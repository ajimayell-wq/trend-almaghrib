package ma.trend.almaghrib.engine

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import java.net.URLDecoder
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.xml.parsers.DocumentBuilderFactory

/** Analyse des flux (fonctions pures, sans réseau). */
object Parsers {
    private const val NS_ATOM = "http://www.w3.org/2005/Atom"
    private const val NS_MEDIA = "http://search.yahoo.com/mrss/"
    private const val NS_DC = "http://purl.org/dc/elements/1.1/"
    private const val NS_HT = "https://trends.google.com/trending/rss"

    /** On ne garde qu'un court extrait, jamais l'article complet (droit d'auteur). */
    private const val MAX_SUMMARY = 600

    private val wpFooter = Regex("The post .*? appeared first on .*$", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val ellipsis = Regex("\\s*\\[(?:&#8230;|…|\\.\\.\\.)]\\s*")
    private val utm = Regex("[?&]utm_[^&]+")
    private val spam = Regex("(live\\s*free|canl[ıi]|streaming|izle|justin\\s*tv)", RegexOption.IGNORE_CASE)
    private val json = Json { ignoreUnknownKeys = true }

    fun parseDate(value: String?): Instant? {
        val v = value?.trim().orEmpty()
        if (v.isEmpty()) return null
        runCatching { return ZonedDateTime.parse(v, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant() }
        runCatching { return OffsetDateTime.parse(v).toInstant() }
        runCatching { return Instant.parse(v) }
        return null
    }

    fun cleanSummary(raw: String?): String {
        var t = ellipsis.replace(raw.orEmpty(), " … ")
        t = TextNorm.stripHtml(wpFooter.replace(t, "")).trim()
        if (t.length > MAX_SUMMARY) t = t.take(MAX_SUMMARY).substringBeforeLast(' ') + " …"
        return t
    }

    /** URL comparable entre sources : décodée (%d9 == %D9), sans traceurs utm_. */
    fun canonicalUrl(url: String): String {
        val decoded = runCatching { URLDecoder.decode(url.trim().replace("+", "%2B"), "UTF-8") }.getOrDefault(url.trim())
        return utm.replace(decoded, "").trimEnd('?', '&')
    }

    private fun parseXml(bytes: ByteArray): Element {
        val f = DocumentBuilderFactory.newInstance()
        f.isNamespaceAware = true
        runCatching { f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
        runCatching { f.isExpandEntityReferences = false }
        return f.newDocumentBuilder().parse(ByteArrayInputStream(bytes)).documentElement
    }

    private fun Element.children(local: String, ns: String? = null): List<Element> {
        val out = ArrayList<Element>()
        val nodes = childNodes
        for (i in 0 until nodes.length) {
            val n = nodes.item(i)
            if (n.nodeType != Node.ELEMENT_NODE) continue
            val name = n.localName ?: n.nodeName
            if (name == local && (ns == null || n.namespaceURI == ns)) out.add(n as Element)
        }
        return out
    }

    private fun Element.child(local: String, ns: String? = null): Element? = children(local, ns).firstOrNull()
    private fun Element?.text(): String = this?.textContent?.trim().orEmpty()

    private fun descendants(root: Element, local: String, ns: String?): List<Element> {
        val list = if (ns == null) root.getElementsByTagName(local) else root.getElementsByTagNameNS(ns, local)
        return (0 until list.length).map { list.item(it) as Element }
    }

    fun parseRss(bytes: ByteArray, source: Source): List<Article> {
        val root = parseXml(bytes)
        var entries = descendants(root, "item", null).filter { it.namespaceURI == null || it.namespaceURI.isEmpty() }
        val atom = entries.isEmpty()
        if (atom) entries = descendants(root, "entry", NS_ATOM)
        val out = ArrayList<Article>()
        for (e in entries) {
            var title: String
            val link: String
            val date: Instant?
            val summary: String
            val tags: List<String>
            if (atom) {
                title = e.child("title", NS_ATOM).text()
                val links = e.children("link", NS_ATOM)
                val l = links.firstOrNull { it.getAttribute("rel") == "alternate" } ?: links.firstOrNull()
                link = l?.getAttribute("href").orEmpty()
                date = parseDate(e.child("updated", NS_ATOM).text().ifEmpty { e.child("published", NS_ATOM).text() })
                summary = e.child("summary", NS_ATOM).text().ifEmpty { e.child("content", NS_ATOM).text() }
                tags = e.children("category", NS_ATOM).map { it.getAttribute("term") }
            } else {
                title = e.child("title").text()
                // Certains flux (Yabiladi) répètent <link> : on garde le dernier, sans traceur.
                link = e.children("link").map { it.text() }.lastOrNull { it.isNotEmpty() }.orEmpty()
                date = parseDate(e.child("pubDate").text().ifEmpty { e.child("date", NS_DC).text() })
                summary = e.child("description").text()
                tags = e.children("category").map { it.text() }
            }
            var image = (e.child("content", NS_MEDIA) ?: e.child("thumbnail", NS_MEDIA))?.getAttribute("url")
            if (image.isNullOrBlank()) {
                val enc = e.child("enclosure")
                image = if (enc != null && enc.getAttribute("type").contains("image")) enc.getAttribute("url") else null
            }
            title = TextNorm.stripHtml(title)
            if (title.isEmpty() || link.isEmpty()) continue
            var sourceName = source.name
            if (source.platform == "aggregator") {
                // Google Actualités : « Titre - Média ». On garde le média d'origine comme source.
                val src = e.child("source").text()
                if (src.isNotEmpty()) {
                    sourceName = src
                    title = title.replace(Regex("\\s+-\\s+[^-]+$"), "")
                }
            }
            out.add(
                Article(
                    sourceId = source.id, sourceName = sourceName, platform = source.platform,
                    url = canonicalUrl(link), title = title, summary = cleanSummary(summary),
                    lang = if (source.lang in setOf("fr", "ar", "en")) source.lang else TextNorm.detectLang(title),
                    publishedAt = date, tags = tags.filter { it.isNotBlank() },
                    imageUrl = image?.takeIf { it.isNotBlank() }, institutional = source.institutional,
                )
            )
        }
        return out
    }

    /** « 2000+ » -> 2000 ; « 20K+ » -> 20000. Valeur plancher fournie par Google. */
    fun parseTraffic(value: String): Long? {
        val m = Regex("([\\d,.\\s]+)\\s*([KkMm]?)").find(value) ?: return null
        val num = m.groupValues[1].replace(",", "").replace(" ", "").toDoubleOrNull() ?: return null
        val mult = when (m.groupValues[2].lowercase()) { "k" -> 1_000; "m" -> 1_000_000; else -> 1 }
        return (num * mult).toLong()
    }

    fun parseGoogleTrends(bytes: ByteArray, source: Source): List<Article> {
        val root = parseXml(bytes)
        val out = ArrayList<Article>()
        for (e in descendants(root, "item", null)) {
            val query = e.child("title").text()
            if (query.isEmpty()) continue
            val traffic = parseTraffic(e.child("approx_traffic", NS_HT).text())
            val urls = ArrayList<String>()
            val titles = ArrayList<String>()
            for (n in e.children("news_item", NS_HT)) {
                val t = n.child("news_item_title", NS_HT).text()
                val u = n.child("news_item_url", NS_HT).text()
                if (u.isEmpty() || spam.containsMatchIn(t)) continue // liens de streaming illégal écartés
                urls.add(canonicalUrl(u))
                titles.add(TextNorm.decodeEntities(t))
                if (urls.size >= 5) break
            }
            out.add(
                Article(
                    sourceId = source.id, sourceName = source.name, platform = "google_trends",
                    url = "https://trends.google.com/trends/explore?geo=MA&q=" +
                        java.net.URLEncoder.encode(query, "UTF-8"),
                    title = query, lang = TextNorm.detectLang(query),
                    publishedAt = parseDate(e.child("pubDate").text()),
                    imageUrl = e.child("picture", NS_HT).text().ifEmpty { null },
                    metrics = if (traffic != null) mapOf("approx_traffic" to traffic) else emptyMap(),
                    relatedUrls = urls, relatedTitles = titles,
                )
            )
        }
        return out
    }

    private const val NS_YT = "http://www.youtube.com/xml/schemas/2015"

    /** Flux public d'une chaîne YouTube : titre, date, miniature et nombre de vues fournis par YouTube. */
    fun parseYoutubeRss(bytes: ByteArray, source: Source): List<Article> {
        val root = parseXml(bytes)
        val channel = root.child("title", NS_ATOM).text().ifEmpty { source.name }
        return descendants(root, "entry", NS_ATOM).mapNotNull { e ->
            val id = e.child("videoId", NS_YT).text().ifEmpty { return@mapNotNull null }
            val title = TextNorm.stripHtml(e.child("title", NS_ATOM).text())
            val group = e.child("group", NS_MEDIA)
            val community = group?.child("community", NS_MEDIA)
            val views = community?.child("statistics", NS_MEDIA)?.getAttribute("views")?.toLongOrNull()
            val likes = community?.child("starRating", NS_MEDIA)?.getAttribute("count")?.toLongOrNull()
            Article(
                sourceId = source.id, sourceName = "YouTube · $channel", platform = "youtube",
                url = "https://www.youtube.com/watch?v=$id", title = title,
                summary = cleanSummary(group?.child("description", NS_MEDIA).text()).take(300),
                lang = TextNorm.detectLang(title),
                publishedAt = parseDate(e.child("published", NS_ATOM).text()),
                imageUrl = "https://i.ytimg.com/vi/$id/hqdefault.jpg",
                tags = source.tags,
                metrics = buildMap { views?.let { put("views", it) }; likes?.let { put("likes", it) } },
                videoId = id,
            )
        }
    }

    /** Commentaires d'une discussion Reddit (flux public). Le premier élément est la publication elle-même. */
    fun parseRedditComments(bytes: ByteArray): List<Comment> {
        val root = parseXml(bytes)
        return descendants(root, "entry", NS_ATOM).drop(1).mapNotNull { e ->
            val text = TextNorm.stripHtml(e.child("content", NS_ATOM).text())
                .replace(Regex("^submitted by .*$"), "").trim()
            if (text.length < 3) null else Comment("reddit", text.take(500), parseDate(e.child("updated", NS_ATOM).text()))
        }
    }

    /** API YouTube (clé facultative) : vidéos tendance au Maroc ou détail de vidéos. */
    fun parseYoutubeApi(body: String, source: Source): List<Article> {
        val root = json.parseToJsonElement(body).jsonObject
        val items = (root["items"] as? JsonArray) ?: return emptyList()
        return items.mapNotNull { el ->
            val v = el as? JsonObject ?: return@mapNotNull null
            val sn = v["snippet"]?.jsonObject ?: return@mapNotNull null
            val st = v["statistics"] as? JsonObject
            fun stat(k: String) = st?.get(k)?.jsonPrimitive?.contentOrNull?.toLongOrNull()
            val metrics = buildMap {
                stat("viewCount")?.let { put("views", it) }
                stat("likeCount")?.let { put("likes", it) }
                stat("commentCount")?.let { put("comments", it) }
            }
            val idEl = v["id"]
            val id = (idEl as? JsonObject)?.get("videoId")?.jsonPrimitive?.contentOrNull
                ?: runCatching { idEl?.jsonPrimitive?.contentOrNull }.getOrNull() ?: return@mapNotNull null
            val title = TextNorm.decodeEntities(sn["title"]?.jsonPrimitive?.contentOrNull.orEmpty())
            val channel = sn["channelTitle"]?.jsonPrimitive?.contentOrNull.orEmpty()
            Article(
                sourceId = source.id, sourceName = "YouTube · $channel", platform = "youtube",
                url = "https://www.youtube.com/watch?v=$id", title = title,
                summary = cleanSummary(sn["description"]?.jsonPrimitive?.contentOrNull).take(300),
                lang = TextNorm.detectLang(title),
                publishedAt = parseDate(sn["publishedAt"]?.jsonPrimitive?.contentOrNull),
                tags = (sn["tags"] as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull }?.take(10).orEmpty(),
                imageUrl = "https://i.ytimg.com/vi/$id/hqdefault.jpg", metrics = metrics, videoId = id,
            )
        }
    }

    /** API YouTube : fils de commentaires publics d'une vidéo. */
    fun parseYoutubeComments(body: String): List<Comment> {
        val root = json.parseToJsonElement(body).jsonObject
        val items = (root["items"] as? JsonArray) ?: return emptyList()
        return items.mapNotNull { el ->
            val top = (el as? JsonObject)?.get("snippet")?.jsonObject?.get("topLevelComment")?.jsonObject
                ?.get("snippet")?.jsonObject ?: return@mapNotNull null
            val text = TextNorm.stripHtml(top["textDisplay"]?.jsonPrimitive?.contentOrNull)
            if (text.length < 3) return@mapNotNull null
            Comment("youtube", text.take(500), parseDate(top["publishedAt"]?.jsonPrimitive?.contentOrNull),
                top["likeCount"]?.jsonPrimitive?.contentOrNull?.toLongOrNull())
        }
    }
}
