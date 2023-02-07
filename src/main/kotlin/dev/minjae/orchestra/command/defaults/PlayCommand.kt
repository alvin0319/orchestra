package dev.minjae.orchestra.command.defaults

import dev.minjae.orchestra.Bot
import dev.minjae.orchestra.command.BaseCommand
import dev.minjae.orchestra.util.replyEphemeral
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData

class PlayCommand(bot: Bot) : BaseCommand(bot, "play", "Play a track") {

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
        val audioChannel = member.voiceState!!.channel!!
        val songURL = event.getOption("song")?.asString ?: run {
            event.replyEphemeral("Please provide a track URL or title.").queue(null) {}
            return
        }
        val musicSearch = event.getOption("music")?.asBoolean ?: false

        event.replyEphemeral("Searching for song...").queue(null) {}

        bot.searchSong(songURL, channel, audioChannel, musicSearch)
    }
}
