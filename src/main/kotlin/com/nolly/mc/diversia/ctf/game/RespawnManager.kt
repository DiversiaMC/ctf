package com.nolly.mc.diversia.ctf.game

import com.nolly.mc.diversia.ctf.config.CtfConfig
import com.nolly.mc.textapi.api.TextAPI
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID

class RespawnManager(
	private val plugin: JavaPlugin,
	private val config: CtfConfig,
	private val teamManager: TeamManager,
	private val kitManager: KitManager
) {
	private val pending = mutableMapOf<UUID, Int>()

	fun scheduleRespawn(player: Player) {
		val uuid = player.uniqueId
		val teamId = teamManager.getPlayerTeam(uuid)?.id ?: return
		val spawnData = config.loadSpawn(teamId) ?: return

		player.gameMode = GameMode.SPECTATOR
		var remaining = config.respawnDelaySeconds

		val taskId = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
			val online = Bukkit.getPlayer(uuid) ?: run {
				pending[uuid]?.let { Bukkit.getScheduler().cancelTask(it) }
				pending.remove(uuid)
				return@Runnable
			}

			if (remaining > 0) {
				TextAPI.send(online, config.messages.respawnCountdown.replace("{seconds}", remaining.toString()))
				remaining--
				return@Runnable
			}

			pending[uuid]?.let { Bukkit.getScheduler().cancelTask(it) }
			pending.remove(uuid)

			val world = Bukkit.getWorld(spawnData.world) ?: return@Runnable
			val loc = Location(world, spawnData.x, spawnData.y, spawnData.z, spawnData.yaw, spawnData.pitch)
			online.teleport(loc)
			online.gameMode = GameMode.ADVENTURE
			online.health = online.maxHealth
			online.foodLevel = 20

			val team = teamManager.getTeam(teamId)
			team?.kitId?.let { kitId -> kitManager.applyKit(kitId, online) }

			TextAPI.send(online, config.messages.respawnNow)
		}, 0L, 20L).taskId

		pending[uuid] = taskId
	}

	fun cancelAll() {
		pending.values.forEach { Bukkit.getScheduler().cancelTask(it) }
		pending.clear()
	}

	fun cancel(uuid: UUID) {
		pending[uuid]?.let { Bukkit.getScheduler().cancelTask(it) }
		pending.remove(uuid)
	}
}
