package dev.minjae.orchestra.player

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel
import java.util.concurrent.ConcurrentLinkedQueue

data class PlayerData(
    val audioPlayer: AudioPlayer,
    val guild: Guild,
    val trackScheduler: TrackScheduler,
    val tracks: ConcurrentLinkedQueue<AudioTrack>,
    val audioChannel: AudioChannel
)
