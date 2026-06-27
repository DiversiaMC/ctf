package com.nolly.mc.diversia.ctf.config

import com.nolly.mc.diversia.ctf.model.CtfFlag
import com.nolly.mc.diversia.ctf.model.CtfKit
import com.nolly.mc.diversia.ctf.model.CtfTeam
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

class CtfConfig(private val plugin: JavaPlugin) {
	private lateinit var mainConfig: FileConfiguration

	fun reload() {
		runCatching { plugin.saveDefaultConfig() }
		plugin.reloadConfig()
		mainConfig = plugin.config
	}

	init { reload() }

	val prefix: String get() = mainConfig.getString("prefix").orEmpty()

	val openJoin: Boolean get() = mainConfig.getBoolean("game.open-join", true)

	fun setOpenJoin(value: Boolean) {
		mainConfig.set("game.open-join", value)
		plugin.saveConfig()
	}

	fun resolveLobbyLocation(): Location {
		val data = lobbyLocation
		if (data != null) {
			val world = Bukkit.getWorld(data.world)
			if (world != null) {
				return Location(world, data.x, data.y, data.z, data.yaw, data.pitch)
			}
		}
		return Bukkit.getWorlds().first().spawnLocation
	}

	val messages: MessagesConfig
		get() = MessagesConfig(
			noPermission = resolve(mainConfig.getString("messages.no-permission").orEmpty()),
			playersOnly = resolve(mainConfig.getString("messages.players-only").orEmpty()),
			invalidUsage = resolve(mainConfig.getString("messages.invalid-usage").orEmpty()),
			reload = resolve(mainConfig.getString("messages.reload").orEmpty()),
			gameAlreadyRunning = resolve(mainConfig.getString("messages.game-already-running").orEmpty()),
			gameNotRunning = resolve(mainConfig.getString("messages.game-not-running").orEmpty()),
			gameNotSetup = resolve(mainConfig.getString("messages.game-not-setup").orEmpty()),
			gameNotWaiting = resolve(mainConfig.getString("messages.game-not-waiting").orEmpty()),
			gameStarted = resolve(mainConfig.getString("messages.game-started").orEmpty()),
			gameStopped = resolve(mainConfig.getString("messages.game-stopped").orEmpty()),
			gameReset = resolve(mainConfig.getString("messages.game-reset").orEmpty()),
			gamePaused = resolve(mainConfig.getString("messages.game-paused").orEmpty()),
			gameResumed = resolve(mainConfig.getString("messages.game-resumed").orEmpty()),
			gameWinScore = resolve(mainConfig.getString("messages.game-win-score").orEmpty()),
			gameWinTime = resolve(mainConfig.getString("messages.game-win-time").orEmpty()),
			gameTie = resolve(mainConfig.getString("messages.game-tie").orEmpty()),
			teamCreated = resolve(mainConfig.getString("messages.team-created").orEmpty()),
			teamRemoved = resolve(mainConfig.getString("messages.team-removed").orEmpty()),
			teamNotFound = resolve(mainConfig.getString("messages.team-not-found").orEmpty()),
			teamAlreadyExists = resolve(mainConfig.getString("messages.team-already-exists").orEmpty()),
			teamAssigned = resolve(mainConfig.getString("messages.team-assigned").orEmpty()),
			flagPlaced = resolve(mainConfig.getString("messages.flag-placed").orEmpty()),
			flagRemoved = resolve(mainConfig.getString("messages.flag-removed").orEmpty()),
			flagNotFound = resolve(mainConfig.getString("messages.flag-not-found").orEmpty()),
			flagAlreadyExists = resolve(mainConfig.getString("messages.flag-already-exists").orEmpty()),
			flagPickedUp = resolve(mainConfig.getString("messages.flag-picked-up").orEmpty()),
			flagDropped = resolve(mainConfig.getString("messages.flag-dropped").orEmpty()),
			flagReturned = resolve(mainConfig.getString("messages.flag-returned").orEmpty()),
			flagCaptured = resolve(mainConfig.getString("messages.flag-captured").orEmpty()),
			spawnSet = resolve(mainConfig.getString("messages.spawn-set").orEmpty()),
			lobbySet = resolve(mainConfig.getString("messages.lobby-set").orEmpty()),
			joinedTeam = resolve(mainConfig.getString("messages.joined-team").orEmpty()),
			leftTeam = resolve(mainConfig.getString("messages.left-team").orEmpty()),
			notInTeam = resolve(mainConfig.getString("messages.not-in-team").orEmpty()),
			teamFull = resolve(mainConfig.getString("messages.team-full").orEmpty()),
			gameNotJoinable = resolve(mainConfig.getString("messages.game-not-joinable").orEmpty()),
			respawnCountdown = resolve(mainConfig.getString("messages.respawn-countdown").orEmpty()),
			respawnNow = resolve(mainConfig.getString("messages.respawn-now").orEmpty()),
			kitGiven = resolve(mainConfig.getString("messages.kit-given").orEmpty()),
			kitNotFound = resolve(mainConfig.getString("messages.kit-not-found").orEmpty()),
			kitAlreadyExists = resolve(mainConfig.getString("messages.kit-already-exists").orEmpty()),
			kitCreated = resolve(mainConfig.getString("messages.kit-created").orEmpty()),
			kitRemoved = resolve(mainConfig.getString("messages.kit-removed").orEmpty()),
			kitAssigned = resolve(mainConfig.getString("messages.kit-assigned").orEmpty()),
			statusHeader = resolve(mainConfig.getString("messages.status-header").orEmpty()),
			statusState = resolve(mainConfig.getString("messages.status-state").orEmpty()),
			statusTime = resolve(mainConfig.getString("messages.status-time").orEmpty()),
			statusScores = resolve(mainConfig.getString("messages.status-scores").orEmpty()),
			statusScoreEntry = resolve(mainConfig.getString("messages.status-score-entry").orEmpty())
		)

	val timeLimitSeconds: Int get() = mainConfig.getInt("game.time-limit-seconds", 600)
	val maxScore: Int get() = mainConfig.getInt("game.max-score", 3)
	val winCondition: String get() = mainConfig.getString("game.win-condition", "BOTH").orEmpty()
	val respawnDelaySeconds: Int get() = mainConfig.getInt("game.respawn-delay-seconds", 5)
	val friendlyFire: Boolean get() = mainConfig.getBoolean("game.friendly-fire", false)
	val flagReturnDelaySeconds: Int get() = mainConfig.getInt("game.flag-return-delay-seconds", 30)
	val flagParticle: String get() = mainConfig.getString("game.flag-particle", "FLAME").orEmpty()
	val flagCarrierParticle: String get() = mainConfig.getString("game.flag-carrier-particle", "CRIT").orEmpty()
	val flagItem: String get() = mainConfig.getString("game.flag-item", "WHITE_BANNER").orEmpty()

	val lobbyLocation: LocationData?
		get() {
			val world = mainConfig.getString("lobby.world") ?: return null
			return LocationData(
				world = world,
				x = mainConfig.getDouble("lobby.x"),
				y = mainConfig.getDouble("lobby.y"),
				z = mainConfig.getDouble("lobby.z"),
				yaw = mainConfig.getDouble("lobby.yaw").toFloat(),
				pitch = mainConfig.getDouble("lobby.pitch").toFloat()
			)
		}

	fun saveLobby(location: Location) {
		mainConfig.set("lobby.world", location.world?.name)
		mainConfig.set("lobby.x", location.x)
		mainConfig.set("lobby.y", location.y)
		mainConfig.set("lobby.z", location.z)
		mainConfig.set("lobby.yaw", location.yaw.toDouble())
		mainConfig.set("lobby.pitch", location.pitch.toDouble())
		plugin.saveConfig()
	}

	fun loadTeams(): List<CtfTeam> {
		val section = mainConfig.getConfigurationSection("teams") ?: return emptyList()
		return section.getKeys(false).map { id ->
			val base = "teams.$id"
			CtfTeam(
				id = id,
				displayName = mainConfig.getString("$base.display-name", id).orEmpty(),
				color = mainConfig.getString("$base.color", "WHITE").orEmpty(),
				maxPlayers = mainConfig.getInt("$base.max-players", 16),
				kitId = mainConfig.getString("$base.kit")?.takeIf { it.isNotBlank() }
			)
		}
	}

	fun saveTeam(team: CtfTeam) {
		val base = "teams.${team.id}"
		mainConfig.set("$base.display-name", team.displayName)
		mainConfig.set("$base.color", team.color)
		mainConfig.set("$base.max-players", team.maxPlayers)
		mainConfig.set("$base.kit", team.kitId)
		plugin.saveConfig()
	}

	fun deleteTeam(id: String) {
		mainConfig.set("teams.$id", null)
		plugin.saveConfig()
	}

	fun hasTeam(id: String): Boolean = mainConfig.contains("teams.$id")

	fun loadFlags(): List<CtfFlag> {
		val section = mainConfig.getConfigurationSection("flags") ?: return emptyList()
		return section.getKeys(false).map { id ->
			val base = "flags.$id"
			CtfFlag(
				id = id,
				teamId = mainConfig.getString("$base.team").orEmpty(),
				world = mainConfig.getString("$base.world").orEmpty(),
				x = mainConfig.getDouble("$base.x"),
				y = mainConfig.getDouble("$base.y"),
				z = mainConfig.getDouble("$base.z"),
				radius = mainConfig.getDouble("$base.radius", 2.0),
				atBase = true
			)
		}
	}

	fun saveFlag(flag: CtfFlag) {
		val base = "flags.${flag.id}"
		mainConfig.set("$base.team", flag.teamId)
		mainConfig.set("$base.world", flag.world)
		mainConfig.set("$base.x", flag.x)
		mainConfig.set("$base.y", flag.y)
		mainConfig.set("$base.z", flag.z)
		mainConfig.set("$base.radius", flag.radius)
		plugin.saveConfig()
	}

	fun deleteFlag(id: String) {
		mainConfig.set("flags.$id", null)
		plugin.saveConfig()
	}

	fun hasFlag(id: String): Boolean = mainConfig.contains("flags.$id")

	fun loadSpawn(teamId: String): LocationData? {
		val base = "spawns.$teamId"
		val world = mainConfig.getString("$base.world") ?: return null
		return LocationData(
			world = world,
			x = mainConfig.getDouble("$base.x"),
			y = mainConfig.getDouble("$base.y"),
			z = mainConfig.getDouble("$base.z"),
			yaw = mainConfig.getDouble("$base.yaw").toFloat(),
			pitch = mainConfig.getDouble("$base.pitch").toFloat()
		)
	}

	fun saveSpawn(teamId: String, location: Location) {
		val base = "spawns.$teamId"
		mainConfig.set("$base.world", location.world?.name)
		mainConfig.set("$base.x", location.x)
		mainConfig.set("$base.y", location.y)
		mainConfig.set("$base.z", location.z)
		mainConfig.set("$base.yaw", location.yaw.toDouble())
		mainConfig.set("$base.pitch", location.pitch.toDouble())
		plugin.saveConfig()
	}

	fun loadKit(id: String): CtfKit? {
		val base = "kits.$id"
		if (!mainConfig.contains(base)) return null

		val size = mainConfig.getInt("$base.size", 36)
		val contents = arrayOfNulls<ItemStack?>(size)
		val rawContents = mainConfig.getList("$base.contents") ?: emptyList<Any>()
		rawContents.forEachIndexed { index, item ->
			if (index < size && item is ItemStack) contents[index] = item
		}

		return CtfKit(
			id = id,
			contents = contents,
			helmet = mainConfig.get("$base.helmet") as? ItemStack,
			chestplate = mainConfig.get("$base.chestplate") as? ItemStack,
			leggings = mainConfig.get("$base.leggings") as? ItemStack,
			boots = mainConfig.get("$base.boots") as? ItemStack
		)
	}

	fun saveKit(kit: CtfKit) {
		val base = "kits.${kit.id}"
		mainConfig.set("$base.size", kit.contents.size)
		mainConfig.set("$base.contents", kit.contents.toList())
		mainConfig.set("$base.helmet", kit.helmet)
		mainConfig.set("$base.chestplate", kit.chestplate)
		mainConfig.set("$base.leggings", kit.leggings)
		mainConfig.set("$base.boots", kit.boots)
		plugin.saveConfig()
	}

	fun deleteKit(id: String) {
		mainConfig.set("kits.$id", null)
		plugin.saveConfig()
	}

	fun loadAllKitIds(): List<String> {
		return mainConfig.getConfigurationSection("kits")?.getKeys(false)?.toList() ?: emptyList()
	}

	fun hasKit(id: String): Boolean = mainConfig.contains("kits.$id")

	private fun resolve(message: String): String = message.replace("{prefix}", prefix)

	data class LocationData(
		val world: String,
		val x: Double,
		val y: Double,
		val z: Double,
		val yaw: Float,
		val pitch: Float
	)

	data class MessagesConfig(
		val noPermission: String,
		val playersOnly: String,
		val invalidUsage: String,
		val reload: String,
		val gameAlreadyRunning: String,
		val gameNotRunning: String,
		val gameNotSetup: String,
		val gameNotWaiting: String,
		val gameStarted: String,
		val gameStopped: String,
		val gameReset: String,
		val gamePaused: String,
		val gameResumed: String,
		val gameWinScore: String,
		val gameWinTime: String,
		val gameTie: String,
		val teamCreated: String,
		val teamRemoved: String,
		val teamNotFound: String,
		val teamAlreadyExists: String,
		val teamAssigned: String,
		val flagPlaced: String,
		val flagRemoved: String,
		val flagNotFound: String,
		val flagAlreadyExists: String,
		val flagPickedUp: String,
		val flagDropped: String,
		val flagReturned: String,
		val flagCaptured: String,
		val spawnSet: String,
		val lobbySet: String,
		val joinedTeam: String,
		val leftTeam: String,
		val notInTeam: String,
		val teamFull: String,
		val gameNotJoinable: String,
		val respawnCountdown: String,
		val respawnNow: String,
		val kitGiven: String,
		val kitNotFound: String,
		val kitAlreadyExists: String,
		val kitCreated: String,
		val kitRemoved: String,
		val kitAssigned: String,
		val statusHeader: String,
		val statusState: String,
		val statusTime: String,
		val statusScores: String,
		val statusScoreEntry: String
	)
}
