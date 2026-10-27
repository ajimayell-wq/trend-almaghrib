package ma.trend.almaghrib

import ma.trend.almaghrib.engine.Assistant
import ma.trend.almaghrib.engine.Engine
import ma.trend.almaghrib.engine.Http
import ma.trend.almaghrib.engine.Opinion
import ma.trend.almaghrib.engine.Parsers
import ma.trend.almaghrib.engine.Sentiment
import ma.trend.almaghrib.engine.Sources
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests sur de vrais extraits de flux (presse marocaine, Google Trends) et des formats YouTube / Reddit. */
class EngineTest {
    private fun fixture(id: String) = javaClass.classLoader!!.getResource("fixtures/$id.xml")?.readBytes()

    private fun articles() = Sources.ALL.flatMap { s ->
        val b = fixture(s.id) ?: return@flatMap emptyList()
        if (s.kind == "gtrends_rss") Parsers.parseGoogleTrends(b, s) else Parsers.parseRss(b, s)
    }

    private val engine = Engine(object : Http {
        override suspend fun get(url: String, headers: Map<String, String>) = ByteArray(0)
    })

    @Test fun parsesRealFeeds() {
        val a = articles()
        assertTrue(a.size >= 20)
        assertTrue(a.all { it.title.isNotBlank() && it.url.startsWith("http") })
    }

    @Test fun clustersAndScores() {
        val a = articles()
        val now = a.mapNotNull { it.publishedAt }.max()
        val topics = engine.analyze(a, now)
        assertTrue(topics.isNotEmpty())
        assertTrue(topics.zipWithNext().all { (x, y) -> x.score >= y.score })
        topics.forEach { assertEquals(24, it.hourly.size) }
    }

    @Test fun parsesYoutubeChannelFeed() {
        val xml = """<?xml version="1.0" encoding="UTF-8"?><feed xmlns:yt="http://www.youtube.com/xml/schemas/2015" xmlns:media="http://search.yahoo.com/mrss/" xmlns="http://www.w3.org/2005/Atom"><title>Hespress</title>
            <entry><yt:videoId>abc123</yt:videoId><title>المنتخب المغربي</title><published>2026-10-22T10:00:00+00:00</published>
            <media:group><media:description>وصف</media:description><media:community><media:starRating count="321"/><media:statistics views="45678"/></media:community></media:group></entry></feed>"""
        val v = Parsers.parseYoutubeRss(xml.toByteArray(), Sources.ALL.first { it.kind == "youtube_rss" })
        assertEquals(1, v.size)
        assertEquals(45678L, v[0].metrics["views"])
        assertEquals("abc123", v[0].videoId)
    }

    @Test fun parsesRedditCommentsWithoutAuthors() {
        val xml = """<?xml version="1.0" encoding="UTF-8"?><feed xmlns="http://www.w3.org/2005/Atom">
            <entry><title>post</title><content type="html">post</content><updated>2026-10-22T10:00:00+00:00</updated></entry>
            <entry><author><name>/u/someone</name></author><content type="html">&lt;p&gt;هاد الشي خايب بزاف&lt;/p&gt;</content><updated>2026-10-22T11:00:00+00:00</updated></entry></feed>"""
        val c = Parsers.parseRedditComments(xml.toByteArray())
        assertEquals(1, c.size)
        assertTrue("someone" !in c[0].text)
    }

    @Test fun sentimentHandlesDarijaFrenchAndNegation() {
        assertTrue(Sentiment.score("هاد الشي خايب بزاف، حشومة") < 0)
        assertTrue(Sentiment.score("Bravo, tbarkallah 3la lmontakhab 🇲🇦") > 0)
        assertTrue(Sentiment.score("ماشي مزيان هاد القرار") < 0)
        assertTrue(Sentiment.score("c'est une honte") < 0)
        assertEquals(0, Sentiment.score("اليوم الجو مشمس"))
    }

    @Test fun opinionNeverInventsAndCountsSample() {
        val r = Opinion.analyze(emptyList(), emptyList(), java.time.Instant.now())
        assertEquals(0, r.sampleSize)
        assertEquals(0, r.pct(0))
    }

    @Test fun analystFiltersByCategoryAndFlagsUncoveredPlatforms() {
        val a = articles()
        val now = a.mapNotNull { it.publishedAt }.max()
        val topics = engine.analyze(a, now)
        val labels = mapOf("politique" to "سياسة")
        val ans = Assistant.answer("آخر قرارات الحكومة", topics, labels, now)
        assertTrue(ans.topics.all { "politique" in it.categories })
        val fb = Assistant.answer("quels sujets font polémique sur Facebook ?", topics, labels, now)
        assertTrue(fb.notes.isNotEmpty())
    }
}
