package dev.minjae.orchestra.command

import dev.minjae.orchestra.Bot
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.OptionData

abstract class BaseCommand(
    val bot: Bot,
    val name: String,
    val description: String,
    val aliases: List<String> = emptyList()
) {

    val options: MutableList<OptionData> = mutableListOf()

    val subcommands: MutableList<BaseSubCommand> = mutableListOf()

    abstract fun execute(event: GenericCommandInteractionEvent)
}
