package ma.trend.almaghrib.engine

import java.time.Duration
import java.time.Instant

/**
 * « محلل الترند » : comprend une question en langage naturel (arabe, darija, français)
 * et répond uniquement à partir des sujets réellement collectés, avec leurs sources.
 * Règles locales et transparentes ; aucun texte n'est inventé.
 */
object Assistant {
    data class Answer(
        val intro: String,
        val topics: List<Topic>,
        val notes: List<String>,
        val filters: List<String>,
    )

    private fun has(q: String, vararg words: String) = words.any { q.contains(TextNorm.normalize(it)) }

    private val CAT_WORDS = mapOf(
        "politique" to arrayOf("سياس", "حكوم", "برلمان", "وزير", "politi", "gouvern", "parlement"),
        "economie" to arrayOf("اقتصاد", "اسعار", "أسعار", "ثمن", "غلاء", "econom", "prix", "argent", "flouss", "فلوس"),
        "societe" to arrayOf("مجتمع", "اجتماع", "قضاء", "محكم", "قضاي", "societ", "justice", "tribunal", "affaire"),
        "sport" to arrayOf("رياض", "كره", "الكرة", "مباراه", "مباراة", "المنتخب", "foot", "sport", "match", "botola"),
        "art" to arrayOf("فن", "فنان", "مهرجان", "اغني", "أغني", "مسلسل", "art", "cultur", "musique", "film"),
        "technologie" to arrayOf("تكنولوج", "رقمي", "تقني", "tech", "numerique", "internet"),
        "international" to arrayOf("دولي", "العالم", "international", "monde"),
    )

    fun answer(question: String, topics: List<Topic>, labels: Map<String, String>, now: Instant): Answer {
        val q = TextNorm.normalize(question)
        val notes = mutableListOf<String>()
        val filters = mutableListOf<String>()
        var pool = topics

        // Fenêtre de temps
        val hours = Regex("(\\d+)\\s*(?:ساع|س\\b|h\\b|heure)").find(q)?.groupValues?.get(1)?.toLongOrNull()
            ?: 24L
        pool = pool.filter { t -> t.lastSeen?.let { Duration.between(it, now) <= Duration.ofHours(hours) } ?: false }
        filters += "آخر $hours ساعة"

        // Rubriques
        val cats = CAT_WORDS.filter { (_, w) -> has(q, *w) }.keys
        if (cats.isNotEmpty()) {
            pool = pool.filter { t -> t.categories.any { it in cats } }
            filters += cats.mapNotNull { labels[it] }
        }
        // Vidéos, polémiques, émergents
        if (has(q, "فيديو", "فيديوهات", "video", "vidéo", "viral", "يوتيوب", "youtube")) {
            pool = pool.filter { it.hasVideo }
            filters += "فيديوهات"
        }
        if (has(q, "جدل", "جدال", "بوليميك", "polem", "controvers", "scandal", "فضيح", "غضب")) {
            pool = pool.filter { it.controversial }
            filters += "جدل"
        }
        if (has(q, "صاعد", "صاعده", "ناشي", "emerg", "monte", "montant")) {
            pool = pool.filter { it.emerging || it.trend == "nouveau" || it.trend == "hausse" }
            filters += "صاعد"
        }
        // Plateformes non couvertes : on le dit clairement.
        if (has(q, "فيسبوك", "facebook", "fb")) notes += "فيسبوك لا يتيح وصولاً قانونياً لتطبيقات الرصد؛ النتائج من المصادر المتاحة فقط."
        if (has(q, "تيك توك", "tiktok", "تيكتوك")) notes += "تيك توك لا يتيح وصولاً قانونياً لتطبيقات الرصد؛ النتائج من المصادر المتاحة فقط."
        if (has(q, "انستغرام", "إنستغرام", "instagram")) notes += "إنستغرام لا يتيح وصولاً قانونياً لتطبيقات الرصد؛ النتائج من المصادر المتاحة فقط."
        if (has(q, "تويتر", "twitter", "اكس", " x ")) notes += "منصة X تتطلب اشتراكاً مدفوعاً للوصول؛ غير مفعلة."

        // Mots-clés libres : classement par pertinence quand la question vise un sujet précis.
        val generic = setOf("ماذا", "شنو", "اشنو", "واش", "كيف", "يتحدث", "يتكلم", "تكلم", "المغاربه", "مغاربه", "الناس",
            "quoi", "parle", "parlent", "marocain", "sujet", "sujets", "دابا", "الان", "اكثر", "plus", "derni")
        val words = TextNorm.tokenize(question).filter { w -> w !in generic && CAT_WORDS.values.none { arr -> arr.any { w.startsWith(TextNorm.normalize(it).removePrefix("ال")) } } }
            .filter { it.length >= 3 }
        if (words.isNotEmpty()) {
            val scored = pool.map { t ->
                val toks = TextNorm.tokenize(t.title + " " + t.items.take(6).joinToString(" ") { it.title }).toSet()
                t to words.count { w -> toks.any { it.startsWith(w) || w.startsWith(it) && it.length >= 4 } }
            }
            val matched = scored.filter { it.second > 0 }
            if (matched.isNotEmpty()) pool = matched.sortedWith(compareByDescending<Pair<Topic, Int>> { it.second }.thenByDescending { it.first.score }).map { it.first }
        }

        val result = pool.take(8)
        val intro = when {
            result.isEmpty() -> "لم أجد في المصادر المتاحة مواضيع تطابق سؤالك خلال آخر $hours ساعة."
            else -> buildString {
                append("وجدت ${pool.size} ${if (pool.size == 1) "موضوعاً" else "مواضيع"} تطابق سؤالك")
                append(". الأبرز: «${result.first().title}»")
                result.first().let { t ->
                    append(" — ${t.breakdown.publications} منشوراً من ${t.mediaCount} ${if (t.mediaCount == 1) "مصدر" else "مصادر"}")
                }
                append(".")
            }
        }
        return Answer(intro, result, notes, filters)
    }
}
