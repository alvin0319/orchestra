package dev.minjae.orchestra

import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeSearchMusicProvider
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioReference
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist
import dev.minjae.orchestra.audio.AudioLoadResultHandler
import dev.minjae.orchestra.audio.AudioPlayerSendingHandler
import dev.minjae.orchestra.audio.source.ProxiedYouTubeSearchMusicProvider
import dev.minjae.orchestra.command.BaseCommand
import dev.minjae.orchestra.command.defaults.PlayCommand
import dev.minjae.orchestra.command.defaults.QueueCommand
import dev.minjae.orchestra.command.defaults.SearchCommand
import dev.minjae.orchestra.command.defaults.StopCommand
import dev.minjae.orchestra.config.BotConfig
import dev.minjae.orchestra.player.PlayerData
import dev.minjae.orchestra.player.PlayerDataStore
import dev.minjae.orchestra.player.TrackScheduler
import dev.minjae.orchestra.util.replyEphemeral
import dev.minn.jda.ktx.events.onCommand
import dev.minn.jda.ktx.interactions.commands.slash
import dev.minn.jda.ktx.interactions.commands.updateCommands
import dev.minn.jda.ktx.jdabuilder.injectKTX
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.api.utils.MemberCachePolicy
import net.dv8tion.jda.api.utils.cache.CacheFlag
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.MalformedURLException
import java.net.URL
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

class Bot(val config: BotConfig) {

    private val logger: Logger = LoggerFactory.getLogger(Bot::class.java)

    private val shardManager: ShardManager = DefaultShardManagerBuilder.createDefault(config.token)
        .injectKTX()
        .enableCache(CacheFlag.VOICE_STATE)
        .enableIntents(
            listOf(
                GatewayIntent.GUILD_VOICE_STATES,
                GatewayIntent.DIRECT_MESSAGE_REACTIONS,
                GatewayIntent.GUILD_MESSAGE_REACTIONS,
                GatewayIntent.MESSAGE_CONTENT
            )
        )
        .setMemberCachePolicy(MemberCachePolicy.VOICE)
        .addEventListeners(JDAListener(this))
        .build()

    private val youtubeSearchMusicProvider: YoutubeSearchMusicProvider = if (config.proxiedYTM) {
        ProxiedYouTubeSearchMusicProvider(config.proxiedYTMURL)
    } else {
        YoutubeSearchMusicProvider()
    }

    private val youtubeSearchManager: YoutubeAudioSourceManager = YoutubeAudioSourceManager()

    private val audioPlayerManager: DefaultAudioPlayerManager =
        DefaultAudioPlayerManager().apply(AudioSourceManagers::registerRemoteSources)

    private val requiredPermissions = listOf(
        Permission.MESSAGE_HISTORY,
        Permission.VIEW_CHANNEL,
        Permission.MESSAGE_SEND,
        Permission.MESSAGE_SEND_IN_THREADS,
        Permission.MESSAGE_MANAGE,
        Permission.MESSAGE_EMBED_LINKS,
        Permission.MESSAGE_ADD_REACTION,
        Permission.VOICE_CONNECT,
        Permission.VOICE_SPEAK,
        Permission.PRIORITY_SPEAKER,
        Permission.VOICE_DEAF_OTHERS
    )

    private val connections: MutableMap<Long, Long> = mutableMapOf()

    private val commands: MutableMap<String, BaseCommand> = mutableMapOf()

    init {
        registerCommands(
            PlayCommand(this),
            QueueCommand(this),
            SearchCommand(this),
            StopCommand(this)
        )
    }

    private fun registerCommands(vararg commands: BaseCommand) {
        var i = 0L
        for (shard in shardManager.shards) {
            shard.updateCommands {
                for (command in commands) {
                    slash(command.name, command.description) {
                        if (command.subcommands.isNotEmpty() && command.options.isNotEmpty()) { // blame discord for this
                            throw IllegalStateException("Cannot register both subcommands and options")
                        }
                        if (command.options.isNotEmpty()) {
                            addOptions(command.options)
                        }
                        if (command.subcommands.isNotEmpty()) {
                            command.subcommands.map { it.asSubCommandData() }.forEach(this::addSubcommands)
                        }
                    }
                    shard.onCommand(command.name) { event ->
                        if (!event.isFromGuild) {
                            event.replyEphemeral("Sorry! My commands are supposed to be used only in guilds!")
                                .queue(null) {}
                        }
                        command.execute(event)
                    }
                }
            }.queueAfter(i * 60000, TimeUnit.MILLISECONDS, null) {} // blame discord for rate limiting
            i++
        }
        for (command in commands) {
            this.commands[command.name] = command
        }
    }

    fun openAudioConnection(messageChannel: GuildMessageChannel, audioChannel: AudioChannel) {
        val guild = audioChannel.guild
        if (connections.contains(guild.idLong)) {
            logger.debug("Already connected to ${guild.idLong}")
            return
        }
        logger.debug("Initializing connection to ${guild.idLong}")
        connections[guild.idLong] = audioChannel.idLong
        val audioManager = guild.audioManager
        val audioPlayer = audioPlayerManager.createPlayer()
        audioPlayer.volume = 100
        val trackScheduler = TrackScheduler(this, messageChannel, guild)
        audioPlayer.addListener(trackScheduler)
        val playerData = PlayerData(audioPlayer, guild, trackScheduler, ConcurrentLinkedQueue(), audioChannel)
        PlayerDataStore.insertPlayerData(guild, playerData)
        audioManager.sendingHandler = AudioPlayerSendingHandler(audioPlayer)
        audioManager.openAudioConnection(audioChannel)
    }

    fun closeAudioConnection(guild: Guild) {
        val audioManager = guild.audioManager
        audioManager.closeAudioConnection()
        connections.remove(guild.idLong)
        PlayerDataStore.getPlayerData(guild)?.audioPlayer?.destroy()
        PlayerDataStore.removePlayerData(guild)
        logger.debug("Closed connection from ${guild.idLong}")
    }

    fun hasOpenConnection(guild: Guild): Boolean = connections.containsKey(guild.idLong)

    fun getAudioChannel(guild: Guild): Long? = connections[guild.idLong]

    fun changeAudioChannel(guild: Guild, audioChannel: AudioChannel) {
        logger.debug("Changing audio channel for ${guild.idLong} to ${audioChannel.idLong}")
        connections[guild.idLong] = audioChannel.idLong
    }

    fun searchAndLoadSong(
        query: String,
        messageChannel: GuildMessageChannel,
        audioChannel: AudioChannel,
        youtubeMusic: Boolean = false
    ) {
        if (!hasOpenConnection(messageChannel.guild)) {
            try {
                openAudioConnection(messageChannel, audioChannel)
            } catch (e: InsufficientPermissionException) {
                messageChannel.sendMessage("I don't have enough permissions to connect to the voice channel!")
                    .queue(null) {}
                return
            }
        }
        val audioLoadResultHandler = AudioLoadResultHandler(messageChannel)
        if (isURL(query)) {
            audioPlayerManager.loadItem(
                query,
                audioLoadResultHandler
            )
        } else if (youtubeMusic) {
            CoroutineScope(Dispatchers.IO).launch {
                val track = youtubeSearchMusicProvider.loadSearchMusicResult(query) {
                    YoutubeAudioTrack(it, youtubeSearchManager)
                }
                when (track) {
                    AudioReference.NO_TRACK -> {
                        audioLoadResultHandler.noMatches()
                    }

                    is BasicAudioPlaylist -> audioLoadResultHandler.playlistLoaded(track)
                    else -> throw IllegalStateException("Unknown track type: ${track::class.java}")
                }
            }
        } else {
            audioPlayerManager.loadItem("ytsearch:$query", audioLoadResultHandler)
        }
    }

    fun searchSong(query: String, youtubeMusic: Boolean = false, limit: Int = 10, success: Consumer<List<AudioTrack>>) {
        if (isURL(query)) {
            audioPlayerManager.loadItem(
                query,
                SearchOnlyAudioLoadResultHandler(success)
            )
        } else if (youtubeMusic) {
            CoroutineScope(Dispatchers.IO).launch {
                val tracks = youtubeSearchMusicProvider.loadSearchMusicResult(query) {
                    YoutubeAudioTrack(it, youtubeSearchManager)
                }
                when (tracks) {
                    AudioReference.NO_TRACK -> success.accept(emptyList())
                    is BasicAudioPlaylist -> success.accept(tracks.tracks.take(limit))
                    else -> throw IllegalStateException("Unknown track type: ${tracks::class.java}")
                }
            }
        } else {
            audioPlayerManager.loadItem(
                "ytsearch:$query",
                SearchOnlyAudioLoadResultHandler(success)
            )
        }
    }

    fun playTrack(audioChannel: AudioChannel, messageChannel: GuildMessageChannel, track: AudioTrack) {
        if (!hasOpenConnection(audioChannel.guild)) {
            try {
                openAudioConnection(messageChannel, audioChannel)
            } catch (e: InsufficientPermissionException) {
                messageChannel.sendMessage("I don't have enough permissions to connect to the voice channel!")
                    .queue(null) {}
                return
            }
        }
        AudioLoadResultHandler(messageChannel).trackLoaded(track)
    }

    fun shutdown() {
        shardManager.shutdown()
        audioPlayerManager.shutdown()
        youtubeSearchManager.shutdown()
        logger.info("Bot shutdown complete")
    }

    internal class SearchOnlyAudioLoadResultHandler(private val success: Consumer<List<AudioTrack>>) : com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler {
        override fun trackLoaded(track: AudioTrack) {
            success.accept(listOf(track))
        }

        override fun playlistLoaded(playlist: AudioPlaylist) {
            success.accept(playlist.tracks)
        }

        override fun noMatches() {
            success.accept(emptyList())
        }

        override fun loadFailed(exception: FriendlyException) {
            success.accept(emptyList())
        }
    }

    companion object {
        fun isURL(string: String): Boolean {
            return try {
                URL(string)
                true
            } catch (e: MalformedURLException) {
                false
            }
        }
    }
}
