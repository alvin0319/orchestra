package dev.minjae.orchestra.util // ktlint-disable filename

import dev.minn.jda.ktx.messages.reply_
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction

fun IReplyCallback.replyEphemeral(message: String): ReplyCallbackAction {
    return this.reply_(message, ephemeral = true)
}
