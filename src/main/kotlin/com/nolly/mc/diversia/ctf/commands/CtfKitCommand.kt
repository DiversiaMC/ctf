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

class CtfKitCommand(
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

		if (!game.isInGame(sender.uniqueId)) {
			TextAPI.send(sender, config.messages.notInTeam)
			return true
		}

		if (args.isEmpty()) {
			val current = game.getPlayerKit(sender.uniqueId)
			TextAPI.send(sender, "<gray>Your kit: <white>${current ?: "none (team default)"}</white>. Usage: <white>/$label <kitId|clear></white></gray>")
			return true
		}

		if (args[0].lowercase() == "clear") {
			game.setPlayerKit(sender.uniqueId, null)
			TextAPI.send(sender, "<gray>Kit cleared. Team default will be used.</gray>")
			return true
		}

		val kitId = args[0].lowercase()
		if (!game.kitManager.hasKit(kitId)) {
			TextAPI.send(sender, config.messages.kitNotFound.replace("{kit}", kitId))
			return true
		}

		game.setPlayerKit(sender.uniqueId, kitId)
		TextAPI.send(sender, "<gray>Kit set to <white>$kitId</white>.</gray>")
		return true
	}

	override fun onTabComplete(
		sender: CommandSender,
		command: Command,
		alias: String,
		args: Array<out String>
	): List<String> {
		if (args.size != 1) return emptyList()
		val options = game.kitManager.getAllKitIds() + "clear"
		return options.filter { it.startsWith(args[0], ignoreCase = true) }
	}
}
