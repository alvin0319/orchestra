package dev.minjae.orchestra // ktlint-disable filename

import ch.qos.logback.classic.Level
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import dev.minjae.orchestra.config.BotConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Paths
import kotlin.system.exitProcess
import ch.qos.logback.classic.Logger as LogbackLogger

fun main() {
    val rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as LogbackLogger
    val jsonMapper = JsonMapper().registerKotlinModule()
    val config: BotConfig = Paths.get(System.getProperty("user.dir")).resolve("config.json").toFile().apply {
        if (!exists()) {
            createNewFile()
            appendBytes({}.javaClass.getResourceAsStream("/config.json")!!.readBytes())
        }
    }.inputStream().buffered().use(jsonMapper::readValue)
    rootLogger.level = if (config.debug) Level.DEBUG else Level.INFO
    if (config.token.isBlank()) {
        rootLogger.info("No token provided.")
        exitProcess(1)
    }
    val bot = Bot(config)
    Runtime.getRuntime().addShutdownHook(
        Thread {
            bot.shutdown()
        }
    )
}
