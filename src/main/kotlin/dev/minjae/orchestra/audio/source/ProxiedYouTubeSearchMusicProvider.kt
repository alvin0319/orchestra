package dev.minjae.orchestra.audio.source

import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeClientConfig
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeSearchMusicProvider
import com.sedmelluq.discord.lavaplayer.tools.DataFormatTools
import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser
import com.sedmelluq.discord.lavaplayer.tools.PBJUtils
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterfaceManager
import com.sedmelluq.discord.lavaplayer.track.AudioItem
import com.sedmelluq.discord.lavaplayer.track.AudioReference
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.util.EntityUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.function.Consumer
import java.util.function.Function

class ProxiedYouTubeSearchMusicProvider(val url: String) : YoutubeSearchMusicProvider() {

    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    private val httpInterfaceManager: HttpInterfaceManager = HttpClientTools.createCookielessThreadLocalManager()

    override fun loadSearchMusicResult(
        query: String,
        trackFactory: Function<AudioTrackInfo, AudioTrack>
    ): AudioItem {
        log.debug("Performing a search music with query {}", query)
        try {
            httpInterfaceManager.getInterface().use { httpInterface ->
                val post =
                    HttpPost(url)
                val clientConfig = YoutubeClientConfig.MUSIC.copy()
                    .withRootField("query", query)
                    .withRootField("params", ProxiedYouTubeConstants.SEARCH_MUSIC_PARAMS)
                    .setAttribute(httpInterface)
                val payload =
                    StringEntity(clientConfig.toJsonString(), "UTF-8")
                post.setHeader("Referer", "music.youtube.com")
                post.entity = payload
                httpInterface.execute(post).use { response ->
                    HttpClientTools.assertSuccessWithContent(response, "search music response")
                    val responseText =
                        EntityUtils.toString(
                            response.entity,
                            StandardCharsets.UTF_8
                        )
                    val jsonBrowser = JsonBrowser.parse(responseText)
                    return extractSearchResults(jsonBrowser, query, trackFactory)
                }
            }
        } catch (e: Exception) {
            throw ExceptionTools.wrapUnfriendlyExceptions(e)
        }
    }

    private fun extractSearchResults(
        jsonBrowser: JsonBrowser,
        query: String,
        trackFactory: Function<AudioTrackInfo, AudioTrack>
    ): AudioItem {
        log.debug("Attempting to parse results from music search page")
        val tracks: List<AudioTrack> = try {
            extractMusicSearchPage(jsonBrowser, trackFactory)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        return if (tracks.isEmpty()) {
            AudioReference.NO_TRACK
        } else {
            BasicAudioPlaylist("Search results for: $query", tracks, null, true)
        }
    }

    @Throws(IOException::class)
    private fun extractMusicSearchPage(
        jsonBrowser: JsonBrowser,
        trackFactory: Function<AudioTrackInfo, AudioTrack>
    ): List<AudioTrack> {
        val list = ArrayList<AudioTrack>()
        var tracks = jsonBrowser["contents"]["tabbedSearchResultsRenderer"]["tabs"]
            .index(0)["tabRenderer"]["content"]["sectionListRenderer"]["contents"]
            .index(0)["musicShelfRenderer"]["contents"]
        if (tracks === JsonBrowser.NULL_BROWSER) {
            tracks = jsonBrowser["contents"]["tabbedSearchResultsRenderer"]["tabs"]
                .index(0)["tabRenderer"]["content"]["sectionListRenderer"]["contents"]
                .index(1)["musicShelfRenderer"]["contents"]
        }
        tracks.values().forEach(
            Consumer { jsonTrack: JsonBrowser ->
                val track = extractMusicTrack(jsonTrack, trackFactory)
                if (track != null) list.add(track)
            }
        )
        return list
    }

    private fun extractMusicTrack(
        jsonBrowser: JsonBrowser,
        trackFactory: Function<AudioTrackInfo, AudioTrack>
    ): AudioTrack? {
        val thumbnail = jsonBrowser["musicResponsiveListItemRenderer"]["thumbnail"]["musicThumbnailRenderer"]
        val columns = jsonBrowser["musicResponsiveListItemRenderer"]["flexColumns"]
        if (columns.isNull) {
            // Somehow don't get track info, ignore
            return null
        }
        val firstColumn = columns.index(0)["musicResponsiveListItemFlexColumnRenderer"]["text"]["runs"]
            .index(0)
        val title = firstColumn["text"].text()
        val videoId = firstColumn["navigationEndpoint"]["watchEndpoint"]["videoId"].text()
            ?: // If track is not available on YouTube Music videoId will be empty
            return null
        val secondColumn = columns.index(1)["musicResponsiveListItemFlexColumnRenderer"]["text"]["runs"].values()
        val author = secondColumn[0]["text"].text()
        val lastElement = secondColumn[secondColumn.size - 1]
        if (!lastElement["navigationEndpoint"].isNull) {
            // The duration element should not have this key, if it does, then duration is probably missing, so return
            return null
        }
        val duration = DataFormatTools.durationTextToMillis(lastElement["text"].text())
        val info = AudioTrackInfo(
            title,
            author,
            duration,
            videoId,
            false,
            ProxiedYouTubeConstants.WATCH_URL_PREFIX + videoId,
            PBJUtils.getYouTubeMusicThumbnail(thumbnail, videoId),
            null
        )
        return trackFactory.apply(info)
    }
}
