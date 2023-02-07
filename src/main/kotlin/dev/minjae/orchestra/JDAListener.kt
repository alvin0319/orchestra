package dev.minjae.orchestra

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.hooks.EventListener

class JDAListener(private val bot: Bot) : EventListener {

    override fun onEvent(event: GenericEvent) {
        when (event) {
            is GuildVoiceUpdateEvent -> onGuildVoiceUpdate(event)
        }
    }

    private fun onGuildVoiceUpdate(event: GuildVoiceUpdateEvent) {
        val guild = event.guild
        val member = event.member
        val channelJoined = event.channelJoined
        val channelLeft = event.channelLeft
        if (channelLeft != null && channelJoined == null && member.idLong == event.jda.selfUser.idLong) { // Bot left by force or whatever reason
            bot.closeAudioConnection(guild)
            return
        }
        if (channelLeft != null && channelLeft.members.any { it.idLong == event.jda.selfUser.idLong }) {
            if (channelLeft.members.isEmpty() || channelLeft.members.size - 1 <= 0) {
                bot.closeAudioConnection(guild)
            }
        }
        if (channelJoined != null && member.idLong == event.jda.selfUser.idLong) {
            if (bot.hasOpenConnection(guild) && bot.getAudioChannel(guild)!! != channelJoined.idLong) {
                bot.changeAudioChannel(guild, channelJoined)
            }
            if (member.hasPermission(Permission.VOICE_DEAF_OTHERS) && member.voiceState?.isDeafened == false) {
                guild.deafen(event.member, true).queue(null) {}
            }
        }
    }
}
