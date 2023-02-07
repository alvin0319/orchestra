package dev.minjae.orchestra.player

import net.dv8tion.jda.api.entities.Guild

object PlayerDataStore {

    private val audioList: MutableMap<Long, PlayerData> = mutableMapOf()

    fun getPlayerData(guild: Guild) = getPlayerData(guild.idLong)

    fun getPlayerData(guildId: Long): PlayerData? {
        return audioList[guildId]
    }

    fun insertPlayerData(guild: Guild, playerData: PlayerData) = insertPlayerData(guild.idLong, playerData)

    fun insertPlayerData(guildId: Long, playerData: PlayerData) {
        audioList[guildId] = playerData
    }

    fun removePlayerData(guild: Guild) = removePlayerData(guild.idLong)

    fun removePlayerData(guildId: Long) {
        audioList.remove(guildId)
    }
}
