package com.nolly.mc.diversia.ctf.commands

import com.nolly.mc.diversia.ctf.config.CtfConfig
import com.nolly.mc.diversia.ctf.game.GameManager
import com.nolly.mc.diversia.ctf.model.CtfFlag
import com.nolly.mc.diversia.ctf.model.CtfTeam
import com.nolly.mc.diversia.ctf.model.GameState
import com.nolly.mc.textapi.api.TextAPI
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class CtfCommand(
	private val config: CtfConfig,
	private val game: GameManager
) : CommandExecutor, TabCompleter {
	companion object {
		const val PERMISSION = "diversia.ctf.master"

		private val COLORS = listOf(
			"RED", "BLUE", "GREEN", "YELLOW", "AQUA",
			"LIGHT_PURPLE", "WHITE", "GOLD", "DARK_RED",
			"DARK_BLUE", "DARK_GREEN", "DARK_AQUA"
		)
	}

	override fun onCommand(
		sender: CommandSender,
		command: Command,
		label: String,
		args: Array<out String>
	): Boolean {
		if (!sender.isOp && !sender.hasPermission(PERMISSION)) {
			if (sender is Player) TextAPI.send(sender, config.messages.noPermission)
			return true
		}

		if (args.isEmpty()) {
			sendUsage(sender, label)
			return true
		}

		return when (args[0].lowercase()) {
			"setup" -> handleSetup(sender)
			"wait", "waiting" -> handleWaiting(sender)
			"start" -> handleStart(sender)
			"pause" -> handlePause(sender)
			"resume" -> handleResume(sender)
			"stop" -> handleStop(sender)
			"reset" -> handleReset(sender)
			"reload" -> handleReload(sender)
			"status" -> handleStatus(sender)
			"team" -> handleTeam(sender, args.drop(1), label)
			"flag" -> handleFlag(sender, args.drop(1), label)
			"spawn" -> handleSpawn(sender, args.drop(1))
			"lobby" -> handleLobby(sender)
			"kit" -> handleKit(sender, args.drop(1), label)
			"playerkit" -> handlePlayerKit(sender, args.drop(1), label)
			"openjoin" -> handleOpenJoin(sender, args.drop(1))
			else -> {
				sendUsage(sender, label)
				true
			}
		}
	}

	@Suppress("SameReturnValue")
	private fun handleOpenJoin(sender: CommandSender, args: List<String>): Boolean {
		if (args.isEmpty()) {
			val newValue = !config.openJoin
			config.setOpenJoin(newValue)
			val state = if (newValue) "<green>activated</green>" else "<red>deactivated</red>"
			if (sender is Player) TextAPI.send(sender, "<gray>Join : $state</gray>")
			else sender.sendMessage(TextAPI.parse("<gray>Open join: $state</gray>"))
			return true
		}
		val value = when (args[0].lowercase()) {
			"true", "on", "yes" -> true
			"false", "off", "no" -> false
			else -> {
				if (sender is Player) TextAPI.send(sender, config.messages.invalidUsage.replace("{usage}", "/ctf openjoin [true|false]"))
				return true
			}
		}
		config.setOpenJoin(value)
		val state = if (value) "<green>activated</green>" else "<red>deactivated</red>"
		if (sender is Player) TextAPI.send(sender, "<gray>Join : $state</gray>")
		else sender.sendMessage(TextAPI.parse("<gray>Open join: $state</gray>"))
		return true
	}

	@Suppress("SameReturnValue")
	private fun handlePlayerKit(sender: CommandSender, args: List<String>, label: String): Boolean {
		if (args.size < 2) {
			if (sender is Player) TextAPI.send(sender, config.messages.invalidUsage.replace("{usage}", "/$label playerkit <set|clear|get> <player> [kitId]"))
			return true
		}

		val target = org.bukkit.Bukkit.getPlayerExact(args[1])
		if (target == null) {
			if (sender is Player) TextAPI.send(sender, "<red>Player not found.</red>")
			return true
		}

		return when (args[0].lowercase()) {
			"set" -> {
				if (args.size < 3) {
					if (sender is Player) TextAPI.send(sender, config.messages.invalidUsage.replace("{usage}", "/$label playerkit set <player> <kitId>"))
					return true
				}
				val kitId = args[2].lowercase()
				if (!game.kitManager.hasKit(kitId)) {
					if (sender is Player) TextAPI.send(sender, config.messages.kitNotFound.replace("{kit}", kitId))
					return true
				}
				if (!game.setPlayerKit(target.uniqueId, kitId)) {
					if (sender is Player) TextAPI.send(sender, "<red>${target.name} has not joined a team yet.</red>")
					return true
				}
				if (sender is Player) TextAPI.send(sender, "<gray>Kit <white>$kitId</white> assigned to <white>${target.name}</white>.</gray>")
				true
			}
			"clear" -> {
				if (!game.setPlayerKit(target.uniqueId, null)) {
					if (sender is Player) TextAPI.send(sender, "<red>${target.name} has not joined a team yet.</red>")
					return true
				}
				if (sender is Player) TextAPI.send(sender, "<gray>Personal kit cleared for <white>${target.name}</white>. Team kit will be used.</gray>")
				true
			}
			"get" -> {
				val kitId = game.getPlayerKit(target.uniqueId)
				if (sender is Player) TextAPI.send(sender, "<gray>${target.name}'s kit: <white>${kitId ?: "none (uses team kit)"}</white></gray>")
				true
			}
			else -> {
				sendUsage(sender, label)
				true
			}
		}
	}

	override fun onTabComplete(
		sender: CommandSender,
		command: Command,
		alias: String,
		args: Array<out String>
	): List<String> {
		if (!sender.isOp && !sender.hasPermission(PERMISSION)) return emptyList()

		val top = listOf(
			"setup", "waiting", "start", "pause", "resume",
			"stop", "reset", "reload", "status",
			"team", "flag", "spawn", "lobby", "kit", "openjoin", "playerkit"
		)

		if (args.size == 1) return filterCompletions(args[0], top)

		return when (args[0].lowercase()) {
			"openjoin" -> if (args.size == 2) filterCompletions(args[1], listOf("true", "false")) else emptyList()
			"team" -> when (args.size) {
				2 -> filterCompletions(args[1], listOf("add", "remove", "list", "assign"))
				3 -> when (args[1].lowercase()) {
					"remove" -> filterCompletions(args[2], teamIds())
					"assign" -> filterCompletions(args[2], onlinePlayerNames())
					else -> emptyList()
				}
				4 -> when (args[1].lowercase()) {
					"add" -> filterCompletions(args[3], COLORS)
					"assign" -> filterCompletions(args[3], teamIds())
					else -> emptyList()
				}
				else -> emptyList()
			}
			"flag" -> when (args.size) {
				2 -> filterCompletions(args[1], listOf("add", "remove", "list"))
				3 -> when (args[1].lowercase()) {
					"add" -> filterCompletions(args[2], teamIds())
					"remove" -> filterCompletions(args[2], flagIds())
					else -> emptyList()
				}
				else -> emptyList()
			}
			"spawn" -> when (args.size) {
				2 -> filterCompletions(args[1], listOf("set"))
				3 -> if (args[1].lowercase() == "set") filterCompletions(args[2], teamIds()) else emptyList()
				else -> emptyList()
			}
			"kit" -> when (args.size) {
				2 -> filterCompletions(args[1], listOf("add", "remove", "list", "give", "assign"))
				3 -> when (args[1].lowercase()) {
					"remove", "give" -> filterCompletions(args[2], kitIds())
					"assign" -> filterCompletions(args[2], kitIds())
					else -> emptyList()
				}
				4 -> when (args[1].lowercase()) {
					"give" -> filterCompletions(args[3], onlinePlayerNames())
					"assign" -> filterCompletions(args[3], teamIds())
					else -> emptyList()
				}
				else -> emptyList()
			}
			"playerkit" -> when (args.size) {
				2 -> filterCompletions(args[1], listOf("set", "clear", "get"))
				3 -> when (args[1].lowercase()) {
					"set", "clear", "get" -> filterCompletions(args[2], onlinePlayerNames())
					else -> emptyList()
				}
				4 -> if (args[1].lowercase() == "set") filterCompletions(args[3], kitIds()) else emptyList()
				else -> emptyList()
			}
			else -> emptyList()
		}
	}

	@Suppress("SameReturnValue")
	private fun handleSetup(sender: CommandSender): Boolean {
		if (game.state != GameState.IDLE && game.state != GameState.ENDED) {
			if (sender is Player) TextAPI.send(sender, config.messages.gameAlreadyRunning)
			return true
		}
		game.enterSetup()
		if (sender is Player) TextAPI.send(sender, config.messages.gameReset.replace("reset", "setup initialized"))
		return true
	}

	@Suppress("SameReturnValue")
	private fun handleWaiting(sender: CommandSender): Boolean {
		if (game.state != GameState.SETUP) {
			if (sender is Player) TextAPI.send(sender, config.messages.gameNotSetup)
			return true
		}
		game.enterWaiting()
		if (sender is Player) TextAPI.send(sender, config.messages.gameNotWaiting.replace("not ", "now in "))
		return true
	}

	@Suppress("SameReturnValue")
	private fun handleStart(sender: CommandSender): Boolean {
		if (game.state != GameState.WAITING && game.state != GameState.SETUP) {
			if (sender is Player) TextAPI.send(sender, if (game.state == GameState.IDLE) config.messages.gameNotSetup else config.messages.gameAlreadyRunning)
			return true
		}
		if (game.state == GameState.SETUP) game.enterWaiting()
		game.startGame()
		return true
	}

	@Suppress("SameReturnValue")
	private fun handlePause(sender: CommandSender): Boolean {
		if (game.state != GameState.RUNNING) {
			if (sender is Player) TextAPI.send(sender, config.messages.gameNotRunning)
			return true
		}
		game.pauseGame()
		return true
	}

	@Suppress("SameReturnValue")
	private fun handleResume(sender: CommandSender): Boolean {
		if (game.state != GameState.PAUSED) {
			if (sender is Player) TextAPI.send(sender, config.messages.gameNotRunning)
			return true
		}
		game.resumeGame()
		return true
	}

	@Suppress("SameReturnValue")
	private fun handleStop(sender: CommandSender): Boolean {
		if (game.state !in listOf(GameState.RUNNING, GameState.PAUSED, GameState.WAITING)) {
			if (sender is Player) TextAPI.send(sender, config.messages.gameNotRunning)
			return true
		}
		game.stopGame()
		return true
	}

	private fun handleReset(sender: CommandSender): Boolean {
		game.resetGame()
		return true
	}

	private fun handleReload(sender: CommandSender): Boolean {
		game.reloadConfig()
		if (sender is Player) TextAPI.send(sender, config.messages.reload)
		return true
	}

	private fun handleStatus(sender: CommandSender): Boolean {
		val msgs = config.messages
		if (sender is Player) TextAPI.send(sender, msgs.statusHeader)
		if (sender is Player) TextAPI.send(sender, msgs.statusState.replace("{state}", game.state.name))

		val minutes = game.timeRemainingSeconds / 60
		val seconds = game.timeRemainingSeconds % 60
		if (sender is Player) TextAPI.send(sender, msgs.statusTime.replace("{time}", "%02d:%02d".format(minutes, seconds)))

		if (sender is Player) TextAPI.send(sender, msgs.statusScores)
		game.teamManager.getAllTeams().forEach { team ->
			if (sender is Player) TextAPI.send(sender, msgs.statusScoreEntry
				.replace("{team}", team.displayName)
				.replace("{score}", game.teamManager.getScore(team.id).toString()))
		}
		return true
	}

	@Suppress("SameReturnValue")
	private fun handleTeam(sender: CommandSender, args: List<String>, label: String): Boolean {
		if (args.isEmpty()) {
			if (sender is Player) TextAPI.send(sender, config.messages.invalidUsage.replace("{usage}", "/$label team <add|remove|list|assign>"))
			return true
		}

		return when (args[0].lowercase()) {
			"add" -> {
				if (args.size < 2) {
					if (sender is Player) TextAPI.send(sender, config.messages.invalidUsage.replace("{usage}", "/$label team add <id> [color] [displayName...]"))
					return true
				}
				val id = args[1].lowercase()
				if (game.teamManager.hasTeam(id)) {
					if (sender is Player) TextAPI.send(sender, config.messages.teamAlreadyExists.replace("{team}", id))
					return true
				}
				val color = args.getOrNull(2)?.uppercase() ?: "WHITE"
				val display = if (args.size > 3) args.drop(3).joinToString(" ") else id
				val team = CtfTeam(id = id, displayName = display, color = color, maxPlayers = 16)
				game.teamManager.addTeam(team)
				if (sender is Player) TextAPI.send(sender, config.messages.teamCreated.replace("{team}", id))
				true
			}
			"remove" -> {
				if (args.size < 2) {
					if (sender is Player) TextAPI.send(sender, config.messages.invalidUsage.replace("{usage}", "/$label team remove <id>"))
					return true
				}
				val id = args[1].lowercase()
				if (!game.teamManager.hasTeam(id)) {
					if (sender is Player) TextAPI.send(sender, config.messages.teamNotFound.replace("{team}", id))
					return true
				}
				game.teamManager.removeTeam(id)
				if (sender is Player) TextAPI.send(sender, config.messages.teamRemoved.replace("{team}", id))
				true
			}
			"list" -> {
				game.teamManager.getAllTeams().forEach { team ->
					val members = game.teamManager.getMemberCount(team.id)
					if (sender is Player) TextAPI.send(sender, "<gray>- <white>${team.id}</white> (<aqua>${team.displayName}</aqua>) ${members}/${team.maxPlayers} kit=<white>${team.kitId ?: "none"}</white></gray>")
				}
				true
			}
			"assign" -> {
				if (args.size < 3) {
					if (sender is Player) TextAPI.send(sender, config.messages.invalidUsage.replace("{usage}", "/$label team assign <player> <team>"))
					return true
				}
				val target = org.bukkit.Bukkit.getPlayerExact(args[1])
				if (target == null) {
					if (sender is Player) TextAPI.send(sender, "<red>Player not found.</red>")
					return true
				}
				val teamId = args[2].lowercase()
				if (!game.teamManager.hasTeam(teamId)) {
					if (sender is Player) TextAPI.send(sender, config.messages.teamNotFound.replace("{team}", teamId))
					return true
				}
				game.joinTeam(target, teamId)
				if (sender is Player) TextAPI.send(sender, config.messages.teamAssigned
					.replace("{player}", target.name)
					.replace("{team}", teamId))
				true
			}
			else -> {
				sendUsage(sender, label)
				true
			}
		}
	}

	@Suppress("SameReturnValue")
	private fun handleFlag(sender: CommandSender, args: List<String>, label: String): Boolean {
		if (args.isEmpty()) {
			if (sender is Player) TextAPI.send(sender, config.messages.invalidUsage.replace("{usage}", "/$label flag <add|remove|list>"))
			return true
		}
		return when (args[0].lowercase()) {
			"add" -> {
				if (sender !is Player) {
					sender.sendMessage(TextAPI.parse(config.messages.playersOnly))
					return true
				}
				if (args.size < 2) {
					TextAPI.send(sender, config.messages.invalidUsage.replace("{usage}", "/$label flag add <teamId> [flagId] [radius]"))
					return true
				}
				val teamId = args[1].lowercase()
				if (!game.teamManager.hasTeam(teamId)) {
					TextAPI.send(sender, config.messages.teamNotFound.replace("{team}", teamId))
					return true
				}
				val flagId = args.getOrNull(2)?.lowercase() ?: "${teamId}_flag"
				if (config.hasFlag(flagId)) {
					TextAPI.send(sender, config.messages.flagAlreadyExists.replace("{flag}", flagId))
					return true
				}
				val radius = args.getOrNull(3)?.toDoubleOrNull() ?: 2.0
				val loc = sender.location
				val flag = CtfFlag(
					id = flagId,
					teamId = teamId,
					world = loc.world?.name ?: "world",
					x = loc.x,
					y = loc.y,
					z = loc.z,
					radius = radius
				)
				game.flagManager.addFlag(flag)
				TextAPI.send(sender, config.messages.flagPlaced
					.replace("{flag}", flagId)
					.replace("{team}", teamId))
				true
			}
			"remove" -> {
				if (args.size < 2) {
					if (sender is Player) TextAPI.send(sender, config.messages.invalidUsage.replace("{usage}", "/$label flag remove <flagId>"))
					return true
				}
				val flagId = args[1].lowercase()
				if (!config.hasFlag(flagId)) {
					if (sender is Player) TextAPI.send(sender, config.messages.flagNotFound.replace("{flag}", flagId))
					return true
				}
				game.flagManager.removeFlag(flagId)
				if (sender is Player) TextAPI.send(sender, config.messages.flagRemoved.replace("{flag}", flagId))
				true
			}
			"list" -> {
				game.flagManager.getAllFlags().forEach { flag ->
					val status = if (flag.atBase) "<green>at base</green>" else "<red>taken</red>"
					if (sender is Player) TextAPI.send(sender, "<gray>- <white>${flag.id}</white> (team: <aqua>${flag.teamId}</aqua>, r=${flag.radius}) $status</gray>")
				}
				true
			}
			else -> {
				sendUsage(sender, label)
				true
			}
		}
	}

	@Suppress("SameReturnValue")
	private fun handleSpawn(sender: CommandSender, args: List<String>): Boolean {
		if (sender !is Player) {
			sender.sendMessage(config.messages.playersOnly)
			return true
		}
		if (args.size < 2 || args[0].lowercase() != "set") {
			TextAPI.send(sender, config.messages.invalidUsage.replace("{usage}", "/ctf spawn set <teamId>"))
			return true
		}
		val teamId = args[1].lowercase()
		if (!game.teamManager.hasTeam(teamId)) {
			TextAPI.send(sender, config.messages.teamNotFound.replace("{team}", teamId))
			return true
		}
		config.saveSpawn(teamId, sender.location)
		TextAPI.send(sender, config.messages.spawnSet.replace("{team}", teamId))
		return true
	}

	@Suppress("SameReturnValue")
	private fun handleLobby(sender: CommandSender): Boolean {
		if (sender !is Player) {
			sender.sendMessage(TextAPI.parse(config.messages.playersOnly))
			return true
		}
		config.saveLobby(sender.location)
		TextAPI.send(sender, config.messages.lobbySet)
		return true
	}

	@Suppress("SameReturnValue")
	private fun handleKit(sender: CommandSender, args: List<String>, label: String): Boolean {
		if (args.isEmpty()) {
			if (sender is Player) TextAPI.send(sender, config.messages.invalidUsage.replace("{usage}", "/$label kit <add|remove|list|give|assign>"))
			return true
		}
		return when (args[0].lowercase()) {
			"add" -> {
				if (sender !is Player) {
					sender.sendMessage(TextAPI.parse(config.messages.playersOnly))
					return true
				}
				if (args.size < 2) {
					TextAPI.send(sender, config.messages.invalidUsage.replace("{usage}", "/$label kit add <id>"))
					return true
				}
				val id = args[1].lowercase()
				if (game.kitManager.hasKit(id)) {
					TextAPI.send(sender, config.messages.kitAlreadyExists.replace("{kit}", id))
					return true
				}
				game.kitManager.saveFromPlayer(id, sender)
				TextAPI.send(sender, config.messages.kitCreated.replace("{kit}", id))
				true
			}
			"remove" -> {
				if (args.size < 2) {
					if (sender is Player) TextAPI.send(sender, config.messages.invalidUsage.replace("{usage}", "/$label kit remove <id>"))
					return true
				}
				val id = args[1].lowercase()
				if (!game.kitManager.hasKit(id)) {
					if (sender is Player) TextAPI.send(sender, config.messages.kitNotFound.replace("{kit}", id))
					return true
				}
				game.kitManager.removeKit(id)
				if (sender is Player) TextAPI.send(sender, config.messages.kitRemoved.replace("{kit}", id))
				true
			}
			"list" -> {
				val ids = game.kitManager.getAllKitIds()
				if (ids.isEmpty()) {
					if (sender is Player) TextAPI.send(sender, "<gray>No kits configured.</gray>")
				} else {
					ids.forEach { if (sender is Player) TextAPI.send(sender, "<gray>- <white>$it</white></gray>") }
				}
				true
			}
			"give" -> {
				if (args.size < 3) {
					if (sender is Player) TextAPI.send(sender, config.messages.invalidUsage.replace("{usage}", "/$label kit give <id> <player>"))
					return true
				}
				val id = args[1].lowercase()
				val target = org.bukkit.Bukkit.getPlayerExact(args[2])
				if (target == null) {
					if (sender is Player) TextAPI.send(sender, "<red>Player not found.</red>")
					return true
				}
				if (!game.kitManager.applyKit(id, target)) {
					if (sender is Player) TextAPI.send(sender, config.messages.kitNotFound.replace("{kit}", id))
					return true
				}
				if (sender is Player) TextAPI.send(sender, config.messages.kitGiven.replace("{kit}", id))
				true
			}
			"assign" -> {
				if (args.size < 3) {
					if (sender is Player) TextAPI.send(sender, config.messages.invalidUsage.replace("{usage}", "/$label kit assign <kitId> <teamId>"))
					return true
				}
				val kitId = args[1].lowercase()
				val teamId = args[2].lowercase()
				if (!game.kitManager.hasKit(kitId)) {
					if (sender is Player) TextAPI.send(sender, config.messages.kitNotFound.replace("{kit}", kitId))
					return true
				}
				if (!game.teamManager.hasTeam(teamId)) {
					if (sender is Player) TextAPI.send(sender, config.messages.teamNotFound.replace("{team}", teamId))
					return true
				}
				val existing = game.teamManager.getTeam(teamId)!!
				val updated = existing.copy(kitId = kitId)
				game.teamManager.addTeam(updated)
				if (sender is Player) TextAPI.send(sender, config.messages.kitAssigned
					.replace("{kit}", kitId)
					.replace("{team}", teamId))
				true
			}
			else -> {
				sendUsage(sender, label)
				true
			}
		}
	}

	private fun sendUsage(sender: CommandSender, label: String) {
		if (sender is Player) TextAPI.send(sender, "<gray>Usage: <white>/$label <setup|waiting|start|pause|resume|stop|reset|reload|status|team|flag|spawn|lobby|kit></white></gray>")
		else sender.sendMessage(TextAPI.parse("<gray>Usage: <white>/$label <setup|waiting|start|pause|resume|stop|reset|reload|status|team|flag|spawn|lobby|kit></white></gray>"))
	}

	private fun filterCompletions(input: String, values: List<String>): List<String> =
		values.filter { it.startsWith(input, ignoreCase = true) }

	private fun teamIds(): List<String> = game.teamManager.getAllTeams().map { it.id }
	private fun flagIds(): List<String> = game.flagManager.getAllFlags().map { it.id }
	private fun kitIds(): List<String> = game.kitManager.getAllKitIds()
	private fun onlinePlayerNames(): List<String> = org.bukkit.Bukkit.getOnlinePlayers().map { it.name }
}
