package dev.minjae.orchestra.audio

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import dev.minjae.orchestra.player.PlayerDataStore
import dev.minn.jda.ktx.messages.Embed
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel

class AudioLoadResultHandler(
    val channel: GuildMessageChannel
) : AudioLoadResultHandler {
    override fun trackLoaded(track: AudioTrack) {
        PlayerDataStore.getPlayerData(channel.guild)?.let { playerData ->
            if (playerData.audioPlayer.playingTrack != null) {
                playerData.tracks.add(track)
                channel.sendMessageEmbeds(
                    Embed {
                        title = "Queued"
                        description = "Queued ${track.info.title} by ${track.info.author}"
                        image = track.info.artworkUrl
//                        field {
//                            name = "Duration"
//                        }
                    }
                ).queue(null) {}
            } else {
                playerData.audioPlayer.playTrack(track)
            }
        }
    }

    override fun playlistLoaded(playlist: AudioPlaylist) {
        if (playlist.tracks.isEmpty()) {
            noMatches()
            return
        }
        if (playlist.isSearchResult) {
            playlist.tracks.firstOrNull()?.let { track ->
                trackLoaded(track)
            }
        } else {
            PlayerDataStore.getPlayerData(channel.guild)?.let { playerData ->
                val tracks = playlist.tracks.toMutableList()
                if (playerData.audioPlayer.playingTrack != null) {
                    channel.sendMessageEmbeds(
                        Embed {
                            title = "Queued"
                            description = "Queued ${tracks.size} tracks from ${playlist.name}"
                        }
                    ).queue(null) {}
                    playerData.tracks.addAll(tracks)
                } else {
                }
            }
        }
    }

    override fun noMatches() {
        channel.sendMessageEmbeds(
            Embed {
                title = "No matches"
                description = "No matches found"
            }
        ).queue(null) {}
    }

    override fun loadFailed(exception: FriendlyException) {
        channel.sendMessageEmbeds(
            Embed {
                title = "Load failed"
                description = "Load failed: ${exception.message}"
            }
        ).queue(null) {}
    }
}
