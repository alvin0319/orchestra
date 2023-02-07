package dev.minjae.orchestra.command.defaults

import dev.minjae.orchestra.Bot
import dev.minjae.orchestra.command.BaseCommand
import dev.minjae.orchestra.player.PlayerDataStore
import dev.minjae.orchestra.util.replyEphemeral
import dev.minn.jda.ktx.messages.Embed
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent

class QueueCommand(bot: Bot) : BaseCommand(bot, "queue", "Show the current queue") {
    override fun execute(event: GenericCommandInteractionEvent) {
        val channel = event.messageChannel
        if (channel !is GuildMessageChannel) {
            return
        }
        PlayerDataStore.getPlayerData(channel.guild).let { playerData ->
            if (playerData == null) {
                event.replyEphemeral("There is no tracks in queue").queue(null) {}
            } else {
                val queue = playerData.tracks
                event.replyEmbeds(
                    Embed {
                        title = "Queue"
                        val desc = StringBuilder()
                        desc.appendln("Current playing: ${playerData.audioPlayer.playingTrack.info.title}")
                        var position = 0
                        for (track in queue) {
                            position++
                            desc.appendln("$position. ${track.info.title}")
                        }
                    }
                ).queue(null) {}
            }
        }
    }
}
