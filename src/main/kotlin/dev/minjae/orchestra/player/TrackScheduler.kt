package dev.minjae.orchestra.player

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import dev.minjae.orchestra.Bot
import dev.minn.jda.ktx.messages.Embed
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TrackScheduler(
    private val bot: Bot,
    private val channel: GuildMessageChannel,
    private val guild: Guild
) : AudioEventAdapter() {

    private val logger: Logger = LoggerFactory.getLogger("TrackScheduler - ${guild.idLong}")

    override fun onTrackStart(player: AudioPlayer, track: AudioTrack) {
        logger.debug("Starting track: ${track.info.title} by ${track.info.author}")
        channel.sendMessageEmbeds(
            Embed {
                title = "Now Playing"
                description = "Now playing ${track.info.title} by ${track.info.author}"
                image = track.info.artworkUrl
            }
        ).queue(null) {}
    }

    override fun onTrackEnd(player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason) {
        PlayerDataStore.getPlayerData(guild)?.let { playerData ->
            if (endReason.mayStartNext && playerData.tracks.isNotEmpty()) {
                logger.debug("Track ended normally, and there are more tracks to play. Playing next track.")
                playerData.audioPlayer.playTrack(playerData.tracks.poll())
                return
            } else if (endReason == AudioTrackEndReason.REPLACED) {
                return
            }
            logger.debug("Track ended normally, and there are no more tracks to play. Closing audio connection.")
            bot.closeAudioConnection(guild)
        }
    }

    override fun onTrackException(player: AudioPlayer, track: AudioTrack, exception: FriendlyException) {
        logger.error("Track got exception!", exception)
        channel.sendMessageEmbeds(
            Embed {
                title = "Error"
                description =
                    "An error occurred while playing ${track.info.title} by ${track.info.author}. Skipping to next track."
            }
        ).queue(null) {}
        PlayerDataStore.getPlayerData(guild)?.let { playerData ->
            if (playerData.tracks.isNotEmpty()) {
                playerData.audioPlayer.playTrack(playerData.tracks.poll())
            } else {
                bot.closeAudioConnection(guild)
            }
        }
    }
}
