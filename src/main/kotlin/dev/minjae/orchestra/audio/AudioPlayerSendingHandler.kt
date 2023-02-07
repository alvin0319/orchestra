package dev.minjae.orchestra.audio

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame
import net.dv8tion.jda.api.audio.AudioSendHandler
import java.nio.ByteBuffer

class AudioPlayerSendingHandler(private val audioPlayer: AudioPlayer) : AudioSendHandler {

    private var lastFrame: AudioFrame? = null

    override fun canProvide(): Boolean {
        lastFrame = audioPlayer.provide()
        return lastFrame != null
    }

    override fun provide20MsAudio(): ByteBuffer? = lastFrame?.let {
        return@let ByteBuffer.wrap(it.data)
    }

    override fun isOpus(): Boolean = true
}
