package com.nolly.mc.diversia.ctf.listeners

import com.nolly.mc.diversia.ctf.config.CtfConfig
import com.nolly.mc.textapi.api.TextAPI
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.player.PlayerCommandSendEvent

class NoCommandListener(private val config: CtfConfig) : Listener {
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	fun onCommand(event: PlayerCommandPreprocessEvent) {
		val player = event.player
		if (player.isOp) return
		event.isCancelled = true
		TextAPI.send(player, config.messages.noPermission)
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	fun onCommandSend(event: PlayerCommandSendEvent) {
		if (event.player.isOp) return
		event.commands.clear()
	}
}
