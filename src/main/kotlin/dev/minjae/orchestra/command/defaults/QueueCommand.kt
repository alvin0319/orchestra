package dev.minjae.orchestra.command.defaults

import dev.minjae.orchestra.Bot
import dev.minjae.orchestra.command.BaseCommand
import dev.minjae.orchestra.player.PlayerDataStore
import dev.minjae.orchestra.util.replyEphemeral
import dev.minn.jda.ktx.messages.Embed
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import kotlin.math.ceil
import kotlin.math.max

class QueueCommand(bot: Bot) : BaseCommand(bot, "queue", "Show the current queue") {

    init {
        options.add(
            OptionData(OptionType.INTEGER, "page", "The page of the queue")
        )
    }

    override fun execute(event: GenericCommandInteractionEvent) {
        val channel = event.messageChannel
        if (channel !is GuildMessageChannel) {
            return
        }
        var page = event.getOption("page")?.asInt ?: run {
            event.replyEphemeral("Please provide a page number").queue(null) {}
            return
        }
        PlayerDataStore.getPlayerData(channel.guild).let { playerData ->
            if (playerData == null) {
                event.replyEphemeral("There is no tracks in queue").queue(null) {}
            } else {
                val queue = playerData.tracks
                val maxPage = max(1, ceil((queue.size / 5).toDouble()).toInt())
                if (page > maxPage) {
                    page = maxPage
                }
                val tracks = queue.toList().subList((page - 1) * 5, minOf(page * 5, queue.size))
                if (tracks.isEmpty()) {
                    // should never happen I guess, doesn't it?
                    event.replyEphemeral("There is no tracks in queue").queue(null) {}
                    return
                }
                event.replyEmbeds(
                    Embed {
                        title = "Track Queue"
                        val desc = StringBuilder()
                        desc.appendln("Current playing: ${playerData.audioPlayer.playingTrack.info.title}")
                        tracks.withIndex().forEach { (index, track) ->
                            desc.appendln("${index + 1}. ${track.info.title}")
                        }
                        description = desc.toString()
                        footer {
                            name = "Page $page/$maxPage"
                        }
                    }
                ).queue(null) {}
            }
        }
    }
}
