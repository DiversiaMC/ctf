package com.nolly.mc.diversia.ctf.commands

import com.nolly.mc.diversia.ctf.config.CtfConfig
import com.nolly.mc.diversia.ctf.game.GameManager
import com.nolly.mc.textapi.api.TextAPI
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class CtfLeaveCommand(
	private val config: CtfConfig,
	private val game: GameManager
) : CommandExecutor {
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

		val team = game.teamManager.getPlayerTeam(sender.uniqueId)
		if (team == null) {
			TextAPI.send(sender, config.messages.notInTeam)
			return true
		}

		game.leaveTeam(sender)
		TextAPI.send(sender, config.messages.leftTeam.replace("{team}", team.displayName))
		return true
	}
}
