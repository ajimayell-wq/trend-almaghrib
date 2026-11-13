package ma.trend.almaghrib.engine

/**
 * Registre des sources interrogées directement par le téléphone (aucun serveur).
 * `verified` = flux ouvert et analysé avec succès pendant le développement.
 * Les autres sont testées à chaque actualisation ; celles en panne sont signalées dans « التغطية ».
 * Ajouter une source = ajouter une ligne ici.
 */
data class Source(
    val id: String,
    val name: String,
    val platform: String, // media | institution | aggregator | google_trends | youtube | reddit
    val kind: String,     // rss | gtrends_rss | youtube_rss | reddit_rss
    val url: String,
    val lang: String,
    val institutional: Boolean = false,
    val verified: Boolean = false,
    /** Rubriques déclarées par défaut pour cette source (ex. chaîne sportive). */
    val tags: List<String> = emptyList(),
)

object Sources {
    private fun yt(id: String, name: String, channel: String, tags: List<String> = emptyList()) =
        Source(id, name, "youtube", "youtube_rss", "https://www.youtube.com/feeds/videos.xml?channel_id=$channel", "multi", tags = tags)

    val ALL: List<Source> = listOf(
        // Presse arabophone
        Source("hespress_ar", "هسبريس", "media", "rss", "https://www.hespress.com/feed", "ar", verified = true),
        Source("alyaoum24", "اليوم 24", "media", "rss", "https://alyaoum24.com/feed", "ar"),
        Source("telquel_ar", "تيل كيل عربي", "media", "rss", "https://ar.telquel.ma/feed", "ar"),
        Source("goud", "كود", "media", "rss", "https://www.goud.ma/feed/", "ar"),
        Source("barlamane", "برلمان.كوم", "media", "rss", "https://barlamane.com/feed/", "ar"),
        Source("alayam24", "الأيام 24", "media", "rss", "https://www.alayam24.com/feed", "ar"),
        // Presse francophone
        Source("hespress_fr", "Hespress FR", "media", "rss", "https://fr.hespress.com/feed", "fr", verified = true),
        Source("medias24", "Médias24", "media", "rss", "https://medias24.com/feed/", "fr", verified = true),
        Source("yabiladi", "Yabiladi", "media", "rss", "https://www.yabiladi.com/rss/news.xml", "fr", verified = true),
        Source("telquel_fr", "TelQuel", "media", "rss", "https://telquel.ma/feed", "fr"),
        Source("leseco", "Les Inspirations ÉCO", "media", "rss", "https://leseco.ma/feed", "fr"),
        Source("lavieeco", "La Vie éco", "media", "rss", "https://www.lavieeco.com/feed/", "fr"),
        Source("lesiteinfo_fr", "Le Site Info", "media", "rss", "https://www.lesiteinfo.com/feed/", "fr"),
        // Source officielle
        Source("map_fr", "MAP Express", "institution", "rss", "https://www.mapexpress.ma/feed/", "fr", institutional = true),
        // Agrégateurs (renvoient vers les articles d'origine)
        Source("gnews_ar", "أخبار Google", "aggregator", "rss",
            "https://news.google.com/rss/search?q=%D8%A7%D9%84%D9%85%D8%BA%D8%B1%D8%A8+when:1d&hl=ar&gl=MA&ceid=MA:ar", "ar"),
        Source("gnews_fr", "Google Actualités", "aggregator", "rss",
            "https://news.google.com/rss/search?q=Maroc+when:1d&hl=fr&gl=MA&ceid=MA:fr", "fr"),
        // Recherches en hausse au Maroc
        Source("gtrends_ma", "Google Trends", "google_trends", "gtrends_rss",
            "https://trends.google.com/trending/rss?geo=MA", "multi", verified = true),
        // Chaînes YouTube marocaines (flux publics : vues réelles fournies par YouTube)
        yt("yt_hespress", "Hespress", "UCvyivffb2WwNKonKiLwVAEQ"),
        yt("yt_2m", "2M", "UClozwh5URilIQpDFeXch8qw"),
        yt("yt_medi1", "Medi1 TV", "UCfXlL8avQjg74WbRyksr9bQ"),
        yt("yt_chouf", "Chouf TV", "UCI3ESb2GoE1w5MIKosbmutQ"),
        yt("yt_aoula", "Al Aoula", "UCuSyI3P8JZOD1nPa_pqjutg"),
        yt("yt_kifache", "Kifache TV", "UCDnHrNrivT4AkwJdAgE8ZCw"),
        yt("yt_telemaroc", "Télé Maroc", "UC6eBD5wxTbOdZbZSTlHKtVg"),
        // Chaînes sportives marocaines
        yt("yt_elbotola", "Elbotola", "UCeOK-Kblp7K-M9xbAPSL7VQ", listOf("sport")),
        yt("yt_mfmsport", "MFM Sport", "UCnDy06vggD-48O8ePrXMhAw", listOf("sport")),
        yt("yt_kora_darija", "الكرة المغربية بالدارجة", "UCsL8ZAvWwE2r1CTcRA1JEkg", listOf("sport")),
        // Discussions publiques
        Source("reddit_morocco", "Reddit r/Morocco", "reddit", "reddit_rss", "https://www.reddit.com/r/Morocco/.rss", "multi"),
    )

    /** Plateformes sans accès légal pour une application : affichées comme « غير مغطاة ». */
    val UNAVAILABLE = listOf("facebook", "instagram", "tiktok", "x")

    val NEWS = setOf("media", "institution", "aggregator")
}
