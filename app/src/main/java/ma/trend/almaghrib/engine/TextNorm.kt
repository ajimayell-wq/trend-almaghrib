package ma.trend.almaghrib.engine

import java.text.Normalizer

/**
 * Normalisation multilingue : français, arabe standard, darija (écriture arabe et Arabizi).
 * Objectif : que les variantes d'écriture d'un même mot donnent le même jeton.
 */
object TextNorm {
    private val tagRe = Regex("<[^>]+>")
    private val wsRe = Regex("\\s+")
    private val arDiacritics = Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]")
    private val arabicChar = Regex("[\\u0600-\\u06FF]")
    private val tokenRe = Regex("[\\p{L}\\p{N}_']+")
    private val arabiziRe = Regex("[a-z]*[23579][a-z]+|[a-z]+[23579][a-z]*")
    private val combining = Regex("\\p{Mn}+")

    private val arMap = mapOf(
        'أ' to "ا", 'إ' to "ا", 'آ' to "ا", 'ٱ' to "ا",
        'ى' to "ي", 'ئ' to "ي", 'ؤ' to "و", 'ة' to "ه",
        'گ' to "ك", 'ڭ' to "ك", 'ڤ' to "ف", 'پ' to "ب",
        '٠' to "0", '١' to "1", '٢' to "2", '٣' to "3", '٤' to "4",
        '٥' to "5", '٦' to "6", '٧' to "7", '٨' to "8", '٩' to "9",
    )
    private val arabiziMap = mapOf('2' to "", '3' to "a", '5' to "kh", '7' to "h", '9' to "q")

    val STOP_FR: Set<String> = """
        a au aux avec ce ces cet cette dans de des du elle elles en et eux il ils je la le les leur leurs lui
        ma mais me meme mes moi mon ne nos notre nous on ou par pas pour qu que qui sa se ses son sur ta te tes
        toi ton tu un une vos votre vous y ete etre avoir est sont sera seront fait faire plus moins tres tout
        tous toute toutes apres avant entre selon depuis lors sans sous vers chez contre comme ainsi aussi alors
        donc dont ni car si deja encore jusqu quand quel quelle quels quelles cela ceci celui celle ceux
        deux trois premier premiere nouveau nouvelle nouvelles nouveaux an ans annee jour jours hier aujourd hui
        demain lundi mardi mercredi jeudi vendredi samedi dimanche heure heures via lire video photos
        the of and to in for on is with at by from
    """.trimIndent().split(Regex("\\s+")).filter { it.isNotBlank() }.toSet()

    val STOP_AR: Set<String> = """
        في من على الى إلى عن مع هذا هذه ذلك تلك التي الذي الذين اللذين ان أن إن او أو ثم كما قد لقد كان كانت
        يكون تكون بعد قبل عند حتى بين خلال ضد حول لدى لا لم لن ما ماذا هل هو هي هم هن نحن انت انا كل بعض
        غير اي أي منذ اليوم امس أمس غدا وقد وفي ومن وعلى والى وهو وهي ولا ولم الا إلا فقط ايضا أيضا جدا
        عبر وفق حسب اجل أجل سنة عام يوم ساعة شخص بشأن بخصوص لها له لهم عليه عليها فيه فيها منه منها به بها
    """.trimIndent().split(Regex("\\s+")).filter { it.isNotBlank() }.map { mapArabic(it) }.toSet()

    /** Mots trop fréquents dans la presse marocaine pour distinguer un événement. */
    private val GENERIC = setOf(
        "maroc", "marocain", "marocaine", "marocains", "marocaines", "royaume", "rabat",
        "مغرب", "مغربي", "مغربيه", "مغاربه", "مملكه", "هسبريس", "hespress", "medias24",
        "yabiladi", "article", "post", "appeared", "first",
    )

    private fun mapArabic(s: String): String {
        val sb = StringBuilder(s.length)
        for (c in s) sb.append(arMap[c] ?: c)
        return sb.toString()
    }

    fun decodeEntities(text: String): String {
        var t = text
        t = Regex("&#(\\d+);").replace(t) { m -> m.groupValues[1].toIntOrNull()?.let { String(Character.toChars(it)) } ?: m.value }
        t = Regex("&#x([0-9a-fA-F]+);").replace(t) { m -> m.groupValues[1].toIntOrNull(16)?.let { String(Character.toChars(it)) } ?: m.value }
        return t.replace("&quot;", "\"").replace("&apos;", "'").replace("&#039;", "'")
            .replace("&lt;", "<").replace("&gt;", ">").replace("&nbsp;", " ").replace("&amp;", "&")
    }

    fun stripHtml(text: String?): String {
        if (text.isNullOrEmpty()) return ""
        val t = tagRe.replace(decodeEntities(text), " ")
        return wsRe.replace(t, " ").trim()
    }

    fun isArabic(text: String): Boolean {
        val letters = text.filter { it.isLetter() }
        if (letters.isEmpty()) return false
        return letters.count { arabicChar.matches(it.toString()) }.toDouble() / letters.length > 0.4
    }

    fun detectLang(text: String): String {
        if (isArabic(text)) return "ar"
        val low = " ${text.lowercase()} "
        val fr = listOf(" le ", " la ", " les ", " des ", " du ", " et ").sumOf { low.windowed(it.length).count { w -> w == it } }
        val en = listOf(" the ", " and ", " of ", " to ").sumOf { low.windowed(it.length).count { w -> w == it } }
        if (arabiziRe.containsMatchIn(low)) return "darija_latin"
        return if (en > fr) "en" else "fr"
    }

    fun normalizeArabic(text: String): String = mapArabic(arDiacritics.replace(text, "").replace("ـ", ""))

    fun normalizeLatin(text: String): String =
        combining.replace(Normalizer.normalize(text.lowercase(), Normalizer.Form.NFKD), "")

    /** Texte comparable : HTML retiré, arabe unifié, accents retirés, minuscules. */
    fun normalize(text: String): String {
        // L'arabe est normalisé avant la décomposition NFKD (qui séparerait hamza et alif).
        val ar = normalizeArabic(stripHtml(text))
        val sb = StringBuilder()
        for (ch in ar) {
            if (arabicChar.matches(ch.toString())) sb.append(ch) else sb.append(normalizeLatin(ch.toString()))
        }
        return sb.toString().lowercase()
    }

    private fun stemAr(tok: String): String {
        var t = tok
        for (pre in listOf("وال", "بال", "كال", "فال", "لل", "ال")) {
            if (t.startsWith(pre) && t.length - pre.length >= 3) { t = t.substring(pre.length); break }
        }
        for (suf in listOf("ات", "ون", "ين", "ان", "ها", "هم", "ه")) {
            if (t.endsWith(suf) && t.length - suf.length >= 3) return t.substring(0, t.length - suf.length)
        }
        return t
    }

    private fun stemFr(tok: String): String {
        var t = tok
        if (t.length > 4 && (t.endsWith("s") || t.endsWith("x")) && !t.endsWith("ss")) t = t.dropLast(1)
        if (t.length > 5 && t.endsWith("e")) t = t.dropLast(1)
        return t
    }

    /** Jetons normalisés, sans mots vides, avec racinisation légère. */
    fun tokenize(text: String): List<String> {
        val out = ArrayList<String>()
        for (m in tokenRe.findAll(normalize(text))) {
            var tok = m.value.trim('\'', '_')
            if (tok.contains('\'')) tok = tok.substringAfterLast('\'')
            if (tok.isEmpty() || (tok.all { it.isDigit() } && tok.length < 4)) continue
            if (arabicChar.containsMatchIn(tok)) {
                if (tok in STOP_AR) continue
                tok = stemAr(tok)
                if (tok.length < 2 || tok in STOP_AR) continue
            } else {
                if (arabiziRe.matches(tok)) {
                    val sb = StringBuilder()
                    for (c in tok) sb.append(arabiziMap[c] ?: c.toString())
                    tok = sb.toString()
                }
                if (tok.length < 3 || tok in STOP_FR) continue
                tok = stemFr(tok)
            }
            if (tok in GENERIC) continue
            out.add(tok)
        }
        return out
    }
}
