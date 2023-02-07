package dev.minjae.orchestra.command

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData

abstract class BaseSubCommand(val name: String, val description: String) {

    val options: MutableList<OptionData> = mutableListOf()

    abstract fun execute(member: Member, args: List<String>, event: GenericCommandInteractionEvent)

    fun asSubCommandData(): SubcommandData {
        return SubcommandData(name, description)
            .addOptions(options)
    }
}
