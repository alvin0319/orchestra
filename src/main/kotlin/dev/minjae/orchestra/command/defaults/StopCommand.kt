package dev.minjae.orchestra.command.defaults

import dev.minjae.orchestra.Bot
import dev.minjae.orchestra.command.BaseCommand
import dev.minjae.orchestra.util.replyEphemeral
import dev.minn.jda.ktx.messages.reply_
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent

class StopCommand(bot: Bot) : BaseCommand(bot, "stop", "Stop the current playing track and clear the queue") {
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
        bot.closeAudioConnection(channel.guild)
        event.reply_("Stopped the current playing track and cleared the queue.")
    }
}
