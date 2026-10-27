package ma.trend.almaghrib.engine

/**
 * Analyse de ton par lexique (arabe, darija en lettres arabes ou latines, français, anglais).
 * Méthode simple et transparente : mots positifs − mots négatifs, avec gestion de la négation.
 * Le résultat est un indicateur approximatif sur un échantillon, jamais une mesure de l'opinion marocaine.
 */
object Sentiment {
    private fun norm(words: String) = words.trim().split(Regex("\\s+")).map { TextNorm.normalize(it) }.toSet()

    private val POS = norm("""
        زوين زوينه زوينين مزيان مزيانه مزيانين واعر واعره رائع رائعه جميل جميله ممتاز ممتازه احسن برافو مبروك مبروك
        شكرا فخور فخوره فخر نجاح ناجح سعيد سعيده فرحه فرحان فرحانين عظيم عظيمه تحفه حلو حلوه بالتوفيق نحبك نحبو
        يستاهل تستاهل خير رجوله تبارك ماشاءالله الحمدلله هنيئا احسنت مفخره ابطال بطل بطله اسود نشاط تالق متالق
        bravo merci excellent excellente super genial geniale magnifique fier fiere fierte felicitation felicitations
        top parfait parfaite beau belle merveilleux heureux heureuse content contente soutien chapeau magnifique
        zwin zwina mzyan mzyana mezyan wa3r waar tbarkallah tbarklah mabrouk mabrok hamdoulah lah ybarek
        good great love amazing proud awesome best nice beautiful congrats
    """)

    private val NEG = norm("""
        خايب خايبه خايبين حشومه عيب فضيحه فضايح كارثه كارثي مشكل مشاكل غالي غاليه غلاء ظلم فساد حكره سرقه شفاره
        كذب كذاب كذابين مقرف مؤسف للاسف حزين حزينه غضب عار تخلف متخلف ضعيف ضعيفه فاشل فاشله فشل زبل مسخوط
        تفو خسرنا سيء سيئ سخيف بهدله تبهديل مهزله حسبي ماكاينش مكاينش مستحيل غش تهميش معاناه استهتار نصب
        honte honteux honteuse scandale scandaleux nul nulle catastrophe inadmissible inacceptable corruption
        cher chere vol voleur mensonge menteur triste degoutant lamentable ridicule colere injustice mauvais
        mauvaise pire echec probleme arnaque decevant decue decu
        hchouma hchoma khayb khayba ghali ghalia chfara hgra fdiha fdi7a khasara
        bad shame worst terrible corrupt expensive sad fail awful disgusting
    """)

    private val NEGATORS = norm("ما لا ماشي مشي ليس لم لن غير pas not machi mashi jamais never ni sans")
    private val PHRASE_POS = listOf("تبارك الله", "ما شاء الله", "الله يحفظ", "الله يبارك", "ديما مغرب", "dima maghrib")
        .map { TextNorm.normalize(it) }
    private val PHRASE_NEG = listOf("حسبي الله", "لا حول ولا قوه", "الله ياخد الحق", "الله يلعن", "c'est la honte")
        .map { TextNorm.normalize(it) }
    private val EMO_POS = listOf("😍", "❤", "👏", "🇲🇦", "💪", "🥰", "😘", "👍", "🙏", "✌")
    private val EMO_NEG = listOf("😡", "🤬", "👎", "💔", "😢", "😭", "🤮", "😤", "😒")
    private val tok = Regex("[\\p{L}\\p{N}']+")
    private val arabizi = mapOf('3' to "a", '7' to "h", '9' to "q", '5' to "kh", '2' to "")

    private fun variants(t: String): List<String> {
        val out = mutableListOf(t)
        for (p in listOf("وال", "فال", "بال", "ال", "و", "ف")) if (t.startsWith(p) && t.length - p.length >= 3) out.add(t.substring(p.length))
        if (t.any { it in arabizi.keys } && t.any { it in 'a'..'z' }) out.add(t.map { arabizi[it] ?: it.toString() }.joinToString(""))
        return out
    }

    /** Score brut : positif > 0, négatif < 0. */
    fun score(text: String): Int {
        val n = TextNorm.normalize(text)
        var s = 0
        PHRASE_POS.forEach { if (n.contains(it)) s += 2 }
        PHRASE_NEG.forEach { if (n.contains(it)) s -= 2 }
        EMO_POS.forEach { if (text.contains(it)) s += 1 }
        EMO_NEG.forEach { if (text.contains(it)) s -= 1 }
        val tokens = tok.findAll(n).map { it.value }.toList()
        tokens.forEachIndexed { i, t ->
            val v = variants(t)
            val pol = when {
                v.any { it in POS } -> 1
                v.any { it in NEG } -> -1
                else -> 0
            }
            if (pol != 0) {
                val negated = (i > 0 && tokens[i - 1] in NEGATORS) || (i > 1 && tokens[i - 2] in NEGATORS)
                s += if (negated) -pol else pol
            }
        }
        return s
    }

    /** 1 = positif, -1 = négatif, 0 = neutre ou indéterminé. */
    fun classify(text: String): Int = score(text).let { if (it > 0) 1 else if (it < 0) -1 else 0 }
}
