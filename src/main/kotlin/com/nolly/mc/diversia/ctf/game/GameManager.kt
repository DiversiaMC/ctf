package com.nolly.mc.diversia.ctf.game

import com.nolly.mc.diversia.ctf.config.CtfConfig
import com.nolly.mc.diversia.ctf.display.CtfBossBar
import com.nolly.mc.diversia.ctf.model.CtfPlayer
import com.nolly.mc.diversia.ctf.model.GameState
import com.nolly.mc.textapi.api.TextAPI
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID

class GameManager(
	private val plugin: JavaPlugin,
	val config: CtfConfig,
	val teamManager: TeamManager,
	val flagManager: FlagManager,
	val kitManager: KitManager,
	val respawnManager: RespawnManager,
) {
	var state: GameState = GameState.IDLE
		private set

	var bossBar: CtfBossBar? = null
	val spawnBoxManager = SpawnBoxManager(config)
	private var countdownTaskId: Int = -1
	val lockedInBox = mutableSetOf<UUID>()

	private val ctfPlayers = mutableMapOf<UUID, CtfPlayer>()
	private var timerTaskId: Int = -1
	private var particleTaskId: Int = -1
	private var captureCheckTaskId: Int = -1
	var timeRemainingSeconds: Int = 0
		private set

	fun enterSetup() {
		state = GameState.SETUP
		teamManager.load()
		flagManager.load()
	}

	fun enterWaiting() {
		state = GameState.WAITING
	}

	fun startGame() {
		state = GameState.RUNNING
		timeRemainingSeconds = config.timeLimitSeconds
		teamManager.resetScores()
		flagManager.reset()
		bossBar?.start()

		teamManager.getAllTeams().forEach { team -> spawnBoxManager.placeBox(team.id) }

		teleportAllToSpawns()
		lockedInBox.addAll(ctfPlayers.keys)

		giveAllKits()
		startParticleTask()
		startCaptureCheckTask()

		startCountdown(10)
	}

	private fun startCountdown(seconds: Int) {
		var remaining = seconds
		countdownTaskId = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
			if (remaining > 0) {
				ctfPlayers.keys.forEach { uuid ->
					Bukkit.getPlayer(uuid)?.sendTitle(
						"§e${remaining}",
						"§7Get ready!",
						5, 25, 5
					)
				}
				remaining--
			} else {
				teamManager.getAllTeams().forEach { team -> spawnBoxManager.restoreBox(team.id) }
				lockedInBox.clear()
				startTimerTask()
				broadcast(config.messages.gameStarted)
				Bukkit.getScheduler().cancelTask(countdownTaskId)
				countdownTaskId = -1
			}
		}, 0L, 20L).taskId
	}

	fun pauseGame() {
		if (state != GameState.RUNNING) return
		state = GameState.PAUSED
		stopTimerTask()
		broadcast(config.messages.gamePaused)
	}

	fun resumeGame() {
		if (state != GameState.PAUSED) return
		state = GameState.RUNNING
		startTimerTask()
		broadcast(config.messages.gameResumed)
	}

	fun stopGame() {
		if (state !in listOf(GameState.RUNNING, GameState.PAUSED, GameState.WAITING)) return
		endGame(forced = true)
		broadcast(config.messages.gameStopped)
	}

	fun resetGame() {
		stopAllTasks()
		bossBar?.stop()
		respawnManager.cancelAll()
		dropAllCarriedFlags()
		restoreAllInventories()
		spawnBoxManager.restoreAll()
		lockedInBox.clear()
		teleportAllToLobby()
		ctfPlayers.clear()
		teamManager.resetScores()
		flagManager.reset()
		state = GameState.IDLE
		broadcast(config.messages.gameReset)
	}

	fun reloadConfig() {
		config.reload()
		teamManager.load()
		flagManager.load()
	}

	fun joinTeam(player: Player, teamId: String): Boolean {
		if (state !in listOf(GameState.SETUP, GameState.WAITING)) return false
		if (!teamManager.hasTeam(teamId)) return false
		if (teamManager.isFull(teamId)) return false

		saveInventoryAndClear(player)
		teamManager.assignPlayer(player.uniqueId, teamId)
		player.gameMode = GameMode.ADVENTURE
		player.isGlowing = false

		ctfPlayers[player.uniqueId] = CtfPlayer(uuid = player.uniqueId, teamId = teamId)
		return true
	}

	fun leaveTeam(player: Player) {
		val uuid = player.uniqueId
		val carried = flagManager.getFlagCarriedBy(uuid)
		if (carried != null) {
			flagManager.drop(carried.id, player)
			broadcast(
				config.messages.flagDropped
					.replace("{player}", player.name)
					.replace("{team}", carried.teamId)
			)
		}

		teamManager.removePlayer(uuid)
		respawnManager.cancel(uuid)
		ctfPlayers.remove(uuid)

		restoreInventory(player)
		teleportToLobby(player)
		player.gameMode = GameMode.ADVENTURE
		player.isGlowing = false
	}

	fun handleDeath(player: Player) {
		val uuid = player.uniqueId
		if (!isInGame(uuid)) return

		val carried = flagManager.getFlagCarriedBy(uuid)
		if (carried != null) {
			flagManager.drop(carried.id, player)
			ctfPlayers[uuid]?.carriedFlagId = null
			broadcast(
				config.messages.flagDropped
					.replace("{player}", player.name)
					.replace("{team}", carried.teamId)
			)
		}

		ctfPlayers[uuid]?.isAlive = false
		respawnManager.scheduleRespawn(player)
	}

	fun handleRespawn(player: Player) {
		ctfPlayers[player.uniqueId]?.isAlive = true
	}

	private fun handleCapture(player: Player, flagId: String) {
		val uuid = player.uniqueId
		val ctfPlayer = ctfPlayers[uuid] ?: return
		val teamId = ctfPlayer.teamId

		val capturedFlag = flagManager.getFlag(flagId) ?: return

		flagManager.returnToBase(flagId)
		player.inventory.setItemInOffHand(null)
		player.isGlowing = false
		ctfPlayer.carriedFlagId = null
		ctfPlayer.captures++

		teamManager.incrementScore(teamId)
		val score = teamManager.getScore(teamId)

		broadcast(
			config.messages.flagCaptured
				.replace("{player}", player.name)
				.replace("{team}", capturedFlag.teamId)
				.replace("{score}", score.toString())
		)

		checkWinCondition()
	}

	fun isInGame(uuid: UUID): Boolean = ctfPlayers.containsKey(uuid)

	private fun checkWinCondition() {
		val condition = config.winCondition.uppercase()
		if (condition == "FIRST_TO_X" || condition == "BOTH") {
			val winner = teamManager.getAllTeams().firstOrNull { team ->
				teamManager.getScore(team.id) >= config.maxScore
			}
			if (winner != null) {
				endGame(winner = winner.id, byScore = true)
			}
		}
	}

	private fun endGame(winner: String? = null, byScore: Boolean = false, forced: Boolean = false) {
		state = GameState.ENDED
		stopAllTasks()
		respawnManager.cancelAll()
		spawnBoxManager.restoreAll()
		lockedInBox.clear()

		if (!forced) {
			when {
				winner != null -> {
					val team = teamManager.getTeam(winner)
					val score = teamManager.getScore(winner)
					val msg = if (byScore) config.messages.gameWinScore else config.messages.gameWinTime
					broadcast(
						msg
							.replace("{team}", team?.displayName ?: winner)
							.replace("{score}", score.toString())
					)
				}

				teamManager.isTie() -> broadcast(config.messages.gameTie)
				else -> {
					val lead = teamManager.getLeadingTeam()
					if (lead != null) {
						broadcast(
							config.messages.gameWinTime
								.replace("{team}", lead.displayName)
								.replace("{score}", teamManager.getScore(lead.id).toString())
						)
					}
				}
			}
		}

		bossBar?.stop()
		dropAllCarriedFlags()
		restoreAllInventories()
	}

	private fun startTimerTask() {
		timerTaskId = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
			if (state != GameState.RUNNING) return@Runnable
			if (timeRemainingSeconds <= 0) {
				val condition = config.winCondition.uppercase()
				if (condition == "TIMER_END" || condition == "BOTH") {
					val lead = teamManager.getLeadingTeam()
					endGame(winner = lead?.id, byScore = false)
				} else {
					endGame(forced = true)
				}
				return@Runnable
			}
			timeRemainingSeconds--
		}, 20L, 20L).taskId
	}

	private fun startParticleTask() {
		particleTaskId = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
			if (state != GameState.RUNNING) return@Runnable
			flagManager.tickParticles()
		}, 0L, 4L).taskId
	}

	private fun startCaptureCheckTask() {
		captureCheckTaskId = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
			if (state != GameState.RUNNING) return@Runnable

			ctfPlayers.values.filter { it.isAlive }.forEach { ctfPlayer ->
				val player = Bukkit.getPlayer(ctfPlayer.uuid) ?: return@forEach

				if (ctfPlayer.carriedFlagId != null) {
					val capturableId = flagManager.getCapturableFlag(player, ctfPlayer.teamId)
					if (capturableId != null) {
						handleCapture(player, capturableId)
						return@forEach
					}
				}

				if (ctfPlayer.carriedFlagId == null) {
					val pickup = flagManager.getPickupableFlag(player, ctfPlayer.teamId)
					if (pickup != null && flagManager.pickup(pickup.id, player)) {
						ctfPlayer.carriedFlagId = pickup.id
						broadcast(
							config.messages.flagPickedUp
								.replace("{player}", player.name)
								.replace("{team}", pickup.teamId)
						)
					}
				}
			}
		}, 0L, 5L).taskId
	}

	private fun stopTimerTask() {
		if (timerTaskId != -1) Bukkit.getScheduler().cancelTask(timerTaskId)
		timerTaskId = -1
	}

	private fun stopAllTasks() {
		stopTimerTask()
		if (countdownTaskId != -1) Bukkit.getScheduler().cancelTask(countdownTaskId)
		if (particleTaskId != -1) Bukkit.getScheduler().cancelTask(particleTaskId)
		if (captureCheckTaskId != -1) Bukkit.getScheduler().cancelTask(captureCheckTaskId)
		countdownTaskId = -1
		particleTaskId = -1
		captureCheckTaskId = -1
	}

	fun broadcast(message: String) {
		Bukkit.getOnlinePlayers().forEach { TextAPI.send(it, message) }
	}

	private fun teleportAllToSpawns() {
		ctfPlayers.values.forEach { ctfPlayer ->
			val player = Bukkit.getPlayer(ctfPlayer.uuid) ?: return@forEach
			val spawnData = config.loadSpawn(ctfPlayer.teamId) ?: return@forEach
			val world = Bukkit.getWorld(spawnData.world) ?: return@forEach
			player.teleport(Location(world, spawnData.x, spawnData.y, spawnData.z, spawnData.yaw, spawnData.pitch))
		}
	}

	private fun giveAllKits() {
		ctfPlayers.values.forEach { ctfPlayer ->
			val player = Bukkit.getPlayer(ctfPlayer.uuid) ?: return@forEach
			val kitId = teamManager.getTeam(ctfPlayer.teamId)?.kitId ?: return@forEach
			kitManager.applyKit(kitId, player)
		}
	}

	private fun teleportAllToLobby() {
		val lobbyData = config.lobbyLocation ?: return
		val world = Bukkit.getWorld(lobbyData.world) ?: return
		val loc = Location(world, lobbyData.x, lobbyData.y, lobbyData.z, lobbyData.yaw, lobbyData.pitch)

		ctfPlayers.keys.forEach { uuid ->
			Bukkit.getPlayer(uuid)?.let { player ->
				player.gameMode = GameMode.SURVIVAL
				player.teleport(loc)
			}
		}

		Bukkit.getOnlinePlayers()
			.filter { it.uniqueId !in ctfPlayers }
			.forEach { player ->
				if (player.gameMode == GameMode.SPECTATOR) {
					player.gameMode = GameMode.SURVIVAL
					player.teleport(loc)
				}
			}
	}

	private fun teleportToLobby(player: Player) {
		val lobby = config.lobbyLocation ?: return
		val world = Bukkit.getWorld(lobby.world) ?: return
		player.teleport(Location(world, lobby.x, lobby.y, lobby.z, lobby.yaw, lobby.pitch))
	}

	private fun saveInventoryAndClear(player: Player) {
		val ctfPlayer = ctfPlayers.getOrPut(player.uniqueId) {
			CtfPlayer(uuid = player.uniqueId, teamId = "")
		}
		ctfPlayer.savedInventory = player.inventory.contents.copyOf()
		player.inventory.clear()
	}

	@Suppress("UnstableApiUsage")
	private fun restoreInventory(player: Player) {
		val ctfPlayer = ctfPlayers[player.uniqueId] ?: return
		player.inventory.contents = ctfPlayer.savedInventory
		player.updateInventory()
	}

	@Suppress("UnstableApiUsage")
	private fun restoreAllInventories() {
		ctfPlayers.values.forEach { ctfPlayer ->
			val player = Bukkit.getPlayer(ctfPlayer.uuid) ?: return@forEach
			player.inventory.contents = ctfPlayer.savedInventory
			player.isGlowing = false
			player.updateInventory()
		}
	}

	private fun dropAllCarriedFlags() {
		ctfPlayers.values.filter { it.carriedFlagId != null }.forEach { ctfPlayer ->
			val player = Bukkit.getPlayer(ctfPlayer.uuid)
			if (player != null) {
				flagManager.drop(ctfPlayer.carriedFlagId!!, player)
			} else {
				flagManager.returnToBase(ctfPlayer.carriedFlagId!!)
			}
			ctfPlayer.carriedFlagId = null
		}
	}

	fun addPlayerToBossBar(player: org.bukkit.entity.Player) {
		bossBar?.addPlayer(player)
	}

	fun removePlayerFromBossBar(player: org.bukkit.entity.Player) {
		bossBar?.removePlayer(player)
	}
}
