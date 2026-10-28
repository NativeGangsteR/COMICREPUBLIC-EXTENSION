package eu.kanade.tachiyomi.extension.en.comicrepublic

import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.online.HttpSource
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ComicRepublic : HttpSource() {

    override val name = "Comic Republic"
    override val baseUrl = "https://www.thecomicrepublic.com"
    override val lang = "en"
    override val supportsLatest = true

    // --- Popular / Catalog ---
    override fun popularMangaRequest(page: Int): Request {
        return GET("$baseUrl/user/read-comics", headers)
    }

    override fun popularMangaParse(response: Response): MangasPage {
        val document = response.asJsoup()
        val mangaList = mutableListOf<SManga>()

        // Parses comic cards rendered in catalogue sections
        document.select("a[href*='/comic-reader/']").forEach { element ->
            val manga = SManga.create().apply {
                title = element.text().ifEmpty { "Comic Republic Title" }
                setUrlWithoutDomain(element.attr("href"))
                thumbnail_url = element.select("img").attr("abs:src")
            }
            mangaList.add(manga)
        }

        return MangasPage(mangaList, hasNextPage = false)
    }

    // --- Latest Updates ---
    override fun latestUpdatesRequest(page: Int): Request = popularMangaRequest(page)
    override fun latestUpdatesParse(response: Response): MangasPage = popularMangaParse(response)

    // --- Search ---
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        return GET("$baseUrl/user/read-comics", headers)
    }
    override fun searchMangaParse(response: Response): MangasPage = popularMangaParse(response)

    // --- Details & Chapter List ---
    override fun mangaDetailsParse(response: Response): SManga {
        val document = response.asJsoup()
        return SManga.create().apply {
            title = document.select("h1").text().ifEmpty { "Comic Republic Series" }
            description = "Published by Comic Republic"
            artist = "Comic Republic Team"
            author = "Comic Republic"
        }
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        val chapters = mutableListOf<SChapter>()
        val requestUrl = response.request.url.toString()

        // Extract issue ID from URL (e.g., /user/comic-reader/[issueId])
        val chapter = SChapter.create().apply {
            name = "Chapter / Issue"
            setUrlWithoutDomain(requestUrl)
            date_upload = System.currentTimeMillis()
        }
        chapters.add(chapter)

        return chapters
    }

    // --- Page List / Reader ---
    override fun pageListParse(response: Response): List<Page> {
        val html = response.body.string()
        val pages = mutableListOf<Page>()

        // Extracts image URLs directly from Next.js payload hydration or DOM img tags
        val imgRegex = """https://[^\s"]+\.(?:png|jpg|jpeg|webp)""".toRegex()
        val matches = imgRegex.findAll(html).map { it.value }.distinct().toList()

        matches.filter { !it.contains("logo") && !it.contains("icon") }.forEachIndexed { index, imageUrl ->
            pages.add(Page(index, "", imageUrl))
        }

        return pages
    }

    override fun imageUrlParse(response: Response): String {
        throw UnsupportedOperationException("Not used")
    }
}