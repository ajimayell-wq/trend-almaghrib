package ma.trend.almaghrib.engine

/**
 * Classement par rubrique : rubriques déclarées par les médias + mots-clés FR / AR / darija.
 * Règles transparentes, sans IA. Ajouter une rubrique = une entrée dans CATEGORIES et ses mots-clés
 * (puis son libellé dans i18n/Strings.kt).
 */
object Categorizer {
    /** Rubriques affichées. Justice, faits divers et régions sont regroupés dans « مجتمع ». */
    val CATEGORIES = listOf("politique", "economie", "societe", "sport", "art", "technologie", "international")
    private val MERGE = mapOf("justice" to "societe", "faits_divers" to "societe", "regions" to "societe")

    private val TAG_MAP = mapOf(
        "politique" to "politique", "سياسه" to "politique", "gouvernement" to "politique",
        "droit" to "justice", "justice" to "justice", "قضاء" to "justice",
        "faits divers" to "faits_divers", "حوادث" to "faits_divers", "faits-divers" to "faits_divers",
        "societe" to "societe", "sante" to "societe", "education" to "societe", "مجتمع" to "societe",
        "مغاربه العالم" to "societe",
        "جهات" to "regions", "regions" to "regions", "region" to "regions",
        "economie" to "economie", "اقتصاد" to "economie", "agriculture" to "economie",
        "finance" to "economie", "bourse" to "economie", "entreprises" to "economie",
        "sport" to "sport", "رياضه" to "sport", "football" to "sport",
        "culture" to "art", "art" to "art", "فن" to "art", "ثقافه" to "art", "people" to "art", "musique" to "art",
        "technologie" to "technologie", "tech" to "technologie", "high-tech" to "technologie",
        "numerique" to "technologie", "تكنولوجيا" to "technologie",
        "monde" to "international", "international" to "international", "دوليه" to "international",
        "عالم" to "international",
    )

    private val KEYWORDS: Map<String, List<String>> = mapOf(
        "art" to listOf(
            "festival", "chanteur", "chanteuse", "chanson", "album", "clip", "film", "serie", "cinema", "concert",
            "artiste", "acteur", "actrice", "theatre", "musique", "rappeur", "influenceu", "youtubeur", "tiktokeu",
            "mawazine", "gnaoua", "ramadan tv",
            "مهرجان", "فنان", "فنانه", "مغني", "مغنيه", "اغنيه", "البوم", "كليب", "فيلم", "مسلسل", "سينما", "حفل",
            "ممثل", "ممثله", "مسرح", "موسيقي", "رابور", "مؤثر", "مؤثره", "يوتيوبر", "طوطو", "موازين",
        ),
        "politique" to listOf(
            "gouvernement", "parlement", "ministre", "chambre des representants", "conseillers", "depute",
            "parti", "pam", "pjd", "rni", "istiqlal", "usfp", "election", "diplomat", "ambassad",
            "sahara", "minurso", "autonomie", "polisario", "conseil de securite", "programme gouvernemental",
            "حكومه", "برلمان", "وزير", "وزيره", "مجلس النواب", "حزب", "انتخاب", "دبلوماس", "سفير", "صحراء",
            "البوليساريو", "الحكم الذاتي", "رييسه الحكومه", "رييس الحكومه",
        ),
        "justice" to listOf(
            "tribunal", "cour d'appel", "cour de cassation", "proces", "condamn", "acquitt", "parquet",
            "garde a vue", "avocat", "juge", "plainte", "detenu", "prison", "affaire",
            "محكمه", "قضاء", "قاضي", "النيابه العامه", "اعتقال", "ادانه", "سجن", "متهم", "محاكمه",
            "جنايات", "استيناف",
        ),
        "faits_divers" to listOf(
            "accident", "incendie", "drame", "noyade", "meurtre", "agression", "faits divers", "victime",
            "interpell", "arrest", "dgsn", "gendarmerie", "enquete", "trafic de", "saisie",
            "حادث", "حريق", "غرق", "جريمه", "سرقه", "مصرع", "توقيف", "الشرطه", "الدرك", "حجز", "اعتداء",
        ),
        "societe" to listOf(
            "ecole", "enseignant", "eleves", "universite", "etudiant", "sante", "hopital", "medecin",
            "manifestation", "greve", "syndicat", "seisme", "inondation", "secheresse", "eau potable",
            "logement", "femmes", "jeunes", "pluie", "meteo", "protection sociale", "amo",
            "تعليم", "مدرسه", "اساتذه", "تلاميذ", "جامعه", "صحه", "مستشفي", "طبيب", "احتجاج", "اضراب",
            "نقابه", "زلزال", "فيضان", "جفاف", "سكن", "نساء", "شباب", "امطار", "طقس",
        ),
        "economie" to listOf(
            "inflation", "prix", "banque", "bank al-maghrib", "bam", "dirham", "investiss", "export",
            "import", "entreprise", "bourse", "ipo", "emploi", "chomage", "croissance", "budget", "impot",
            "fiscal", "hcp", "carburant", "gasoil", "consommation", "tourisme", "commerce", "industrie",
            "automobile", "aivam",
            "تضخم", "اسعار", "ثمن", "بنك", "درهم", "استثمار", "صادرات", "واردات", "شركه", "بورصه", "تشغيل",
            "بطاله", "نمو", "ميزانيه", "ضريبه", "محروقات", "سياحه", "تجاره", "صناعه",
        ),
        "technologie" to listOf(
            "technologie", "numerique", "digital", "intelligence artificielle", "startup", "cybersecurite",
            "cyber", "internet", "5g", "smartphone", "application", "donnees personnelles", "dgssi", "php",
            "رقمي", "رقميه", "الذكاء الاصطناعي", "تكنولوجيا", "الانترنت", "تطبيق", "سيبراني", "معطيات",
        ),
        "sport" to listOf(
            "football", "botola", "lions de l'atlas", "frmf", "wydad", "raja", "far rabat", "rsb", "match",
            "selectionneur", "regragui", "can ", "coupe du monde", "mondial", "futsal", "ligue des champions",
            "transfert", "athletisme", "tennis", "boxe",
            "كره القدم", "البطوله", "المنتخب", "اسود الاطلس", "الوداد", "الرجاء", "مباراه", "الركراكي",
            "كاس العالم", "كاس افريقيا", "لاعب", "مدرب",
        ),
        "regions" to listOf(
            "casablanca", "marrakech", "fes", "tanger", "agadir", "oujda", "meknes", "tetouan", "laayoune",
            "dakhla", "kenitra", "safi", "el jadida", "beni mellal", "nador", "errachidia", "zagora",
            "ouarzazate", "al hoceima", "province", "commune", "conseil regional", "wilaya",
            "الدار البيضاء", "مراكش", "فاس", "طنجه", "اكادير", "وجده", "مكناس", "تطوان", "العيون", "الداخله",
            "القنيطره", "اسفي", "الجديده", "بني ملال", "الناظور", "الرشيديه", "زاكوره", "ورزازات", "الحسيمه",
            "اقليم", "جماعه", "جهه", "عامل", "عماله",
        ),
        "international" to listOf(
            "etats-unis", "france", "espagne", "algerie", "union europeenne", "onu", "gaza", "israel",
            "ukraine", "russie", "chine", "trump", "macron", "sanchez", "feijoo",
            "الولايات المتحده", "فرنسا", "اسبانيا", "الجزاير", "الاتحاد الاوروبي", "الامم المتحده", "غزه",
            "اسراييل", "اوكرانيا", "روسيا", "الصين", "ترامب",
        ),
    )

    private val arabic = Regex("[\\u0600-\\u06FF]")

    private fun kwRegex(keyword: String): Regex {
        val body = Regex.escape(TextNorm.normalize(keyword).trim())
        val end = if (keyword.endsWith(" ")) "(?![\\p{L}\\p{N}])" else ""
        val prefix = if (arabic.containsMatchIn(keyword)) "(?:وال|بال|فال|لل|ال|و|ب|ل)?" else ""
        return Regex("(?<![\\p{L}\\p{N}])$prefix$body$end")
    }

    private val KW_RE: Map<String, List<Regex>> = KEYWORDS.mapValues { (_, kws) -> kws.map { kwRegex(it) } }
    private val MATCHUP = Regex("^\\S.{1,30}\\s(?:–|-|vs\\.?|v|x|ضد)\\s\\S.{0,30}$", RegexOption.IGNORE_CASE)

    private val ALL_CATS = CATEGORIES + MERGE.keys

    fun scoreItem(item: Article): Map<String, Double> {
        val scores = ALL_CATS.associateWith { 0.0 }.toMutableMap()
        for (tag in item.tags) {
            val cat = TAG_MAP[TextNorm.normalize(tag)] ?: TAG_MAP[tag.lowercase()]
            if (cat != null) scores[cat] = scores.getValue(cat) + 3.0
        }
        val text = TextNorm.normalize("${item.title} ${item.title} ${item.summary} ${item.relatedTitles.joinToString(" ")}")
        for ((cat, regexes) in KW_RE) for (rx in regexes) if (rx.containsMatchIn(text)) scores[cat] = scores.getValue(cat) + 1.0
        if (item.platform == "google_trends" && MATCHUP.matches(item.title)) scores["sport"] = scores.getValue("sport") + 2.0
        val merged = CATEGORIES.associateWith { 0.0 }.toMutableMap()
        for ((k, v) in scores) { val c = MERGE[k] ?: k; merged[c] = merged.getValue(c) + v }
        return merged
    }

    /** Vocabulaire de la polémique (FR / AR / darija). Règle transparente, pas un jugement. */
    private val CONTROVERSY = listOf(
        "polemique", "scandale", "colere", "indignation", "tolle", "controverse", "critique", "fait reagir",
        "s'indigne", "denonce", "fustige", "buzz", "viral",
        "جدل", "غضب", "انتقاد", "انتقادات", "استياء", "فضيحه", "يثير", "تثير", "اثار", "تنديد", "سخط",
        "يشعل", "تشعل", "موجه", "تفاعل واسع", "مواقع التواصل", "رواد", "الشوهه", "حشومه", "بوز",
    ).map { kwRegex(it) }

    fun isControversial(items: List<Article>): Boolean {
        val hits = items.count { a -> val t = TextNorm.normalize(a.title); CONTROVERSY.any { it.containsMatchIn(t) } }
        return hits >= 1
    }

    /** Rubrique principale en premier, puis rubriques secondaires pertinentes (3 au plus). */
    fun categorize(cluster: Cluster): List<String> {
        val total = CATEGORIES.associateWith { 0.0 }.toMutableMap()
        for (it in cluster.items) for ((cat, s) in scoreItem(it)) total[cat] = total.getValue(cat) + s
        val ranked = total.entries.sortedByDescending { it.value }
        if (ranked.first().value <= 0) return listOf("societe")
        val top = ranked.first().value
        return ranked.filter { it.value > 0 && it.value >= 0.5 * top }.map { it.key }.take(3)
    }
}
