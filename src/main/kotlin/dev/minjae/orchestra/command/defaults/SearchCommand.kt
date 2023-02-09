package dev.minjae.orchestra.command.defaults

import dev.minjae.orchestra.Bot
import dev.minjae.orchestra.command.BaseCommand
import dev.minjae.orchestra.util.replyEphemeral
import dev.minn.jda.ktx.interactions.components.button
import dev.minn.jda.ktx.messages.Embed
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

class SearchCommand(bot: Bot) : BaseCommand(bot, "search", "Search a track") {

    init {
        options.addAll(
            setOf(
                OptionData(OptionType.STRING, "song", "URL or title of track to play", true),
                OptionData(OptionType.BOOLEAN, "music", "Whether to search on YouTube Music", false)
            )
        )
    }

    override fun execute(event: GenericCommandInteractionEvent) {
        val channel = event.messageChannel
        if (channel !is GuildMessageChannel) {
            return
        }
        val member = event.member ?: return
        if (member.voiceState?.inAudioChannel() == false) {
            event.replyEphemeral("You must in an audio channel in order to use this command.").queue(null) {}
            return
        }
        val songURL = event.getOption("song")?.asString ?: run {
            event.replyEphemeral("Please provide a track URL or title.").queue(null) {}
            return
        }
        val musicSearch = event.getOption("music")?.asBoolean ?: false

        event.replyEphemeral("Searching for song...").queue({ hook ->
            bot.searchSong(songURL, musicSearch, 5) { tracks ->
                hook.deleteOriginal().queue(null) {}
                val actionRows = tracks.map {
                    event.jda.button(
                        label = "${it.info.title} by ${it.info.author}",
                        expiration = 10.seconds
                    ) { _ ->
                        bot.playTrack(member.voiceState!!.channel!!, channel, it)
                    }
                }
                event.messageChannel.sendMessageEmbeds(
                    Embed {
                        title = "Search Results"
                        description = "You have 10 seconds to select a track."
                    }
                )
                    .addActionRow(actionRows)
                    .queue({ m ->
                        m.delete().queueAfter(10, TimeUnit.SECONDS, null) {}
                    }) {}
            }
        }) {}
    }
}
