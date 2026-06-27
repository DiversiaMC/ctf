package com.nolly.mc.diversia.ctf.commands

import com.nolly.mc.diversia.ctf.config.CtfConfig
import com.nolly.mc.diversia.ctf.game.GameManager
import com.nolly.mc.diversia.ctf.model.GameState
import com.nolly.mc.textapi.api.TextAPI
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class CtfJoinCommand(
	private val config: CtfConfig,
	private val game: GameManager
) : CommandExecutor, TabCompleter {
	override fun onCommand(
		sender: CommandSender,
		command: Command,
		label: String,
		args: Array<out String>
	): Boolean {
		if (sender !is Player) {
			sender.sendMessage(TextAPI.parse(config.messages.playersOnly))
			return true
		}

		if (game.state !in listOf(GameState.SETUP, GameState.WAITING)) {
			TextAPI.send(sender, config.messages.gameNotJoinable)
			return true
		}

		if (args.isEmpty()) {
			TextAPI.send(sender, config.messages.invalidUsage.replace("{usage}", "/$label <team>"))
			return true
		}

		val teamId = args[0].lowercase()
		if (!game.teamManager.hasTeam(teamId)) {
			TextAPI.send(sender, config.messages.teamNotFound.replace("{team}", teamId))
			return true
		}

		if (game.teamManager.isFull(teamId)) {
			TextAPI.send(sender, config.messages.teamFull.replace("{team}", teamId))
			return true
		}

		if (!game.joinTeam(sender, teamId)) {
			TextAPI.send(sender, config.messages.gameNotJoinable)
			return true
		}

		TextAPI.send(sender, config.messages.joinedTeam.replace("{team}", teamId))
		return true
	}

	override fun onTabComplete(
		sender: CommandSender,
		command: Command,
		alias: String,
		args: Array<out String>
	): List<String> {
		if (args.size != 1) return emptyList()
		return game.teamManager.getAllTeams()
			.filter { !game.teamManager.isFull(it.id) }
			.map { it.id }
			.filter { it.startsWith(args[0], ignoreCase = true) }
	}
}
