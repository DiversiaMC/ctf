package com.nolly.mc.diversia.ctf

import com.nolly.mc.diversia.ctf.commands.CtfCommand
import com.nolly.mc.diversia.ctf.commands.CtfJoinCommand
import com.nolly.mc.diversia.ctf.commands.CtfKitCommand
import com.nolly.mc.diversia.ctf.commands.CtfLeaveCommand
import com.nolly.mc.diversia.ctf.config.CtfConfig
import com.nolly.mc.diversia.ctf.display.CtfBossBar
import com.nolly.mc.diversia.ctf.game.FlagManager
import com.nolly.mc.diversia.ctf.game.GameManager
import com.nolly.mc.diversia.ctf.game.KitManager
import com.nolly.mc.diversia.ctf.game.RespawnManager
import com.nolly.mc.diversia.ctf.game.TeamManager
import com.nolly.mc.diversia.ctf.listeners.GameListener
import com.nolly.mc.diversia.ctf.listeners.NoCommandListener
import org.bukkit.plugin.java.JavaPlugin

class DiversiaCtf : JavaPlugin() {
	private lateinit var ctfConfig: CtfConfig
	private lateinit var gameManager: GameManager

	override fun onEnable() {
		logger.info("[Diversia] CTF plugin enabling...")
		try {
			ctfConfig = CtfConfig(this)

			val teamManager = TeamManager(ctfConfig)
			val flagManager = FlagManager(this, ctfConfig)
			val kitManager = KitManager(ctfConfig)
			val respawnManager = RespawnManager(this, ctfConfig, teamManager, kitManager)

			gameManager = GameManager(
				plugin = this,
				config = ctfConfig,
				teamManager = teamManager,
				flagManager = flagManager,
				kitManager = kitManager,
				respawnManager = respawnManager
			)
			gameManager.bossBar = CtfBossBar(this, ctfConfig, gameManager)

			respawnManager.kitResolver = { uuid ->
				gameManager.getPlayerKit(uuid) ?: teamManager.getPlayerTeam(uuid)?.let {
					teamManager.getTeam(it.id)?.kitId
				}
			}

			val ctfCommand = CtfCommand(ctfConfig, gameManager)
			getCommand("ctf")?.apply {
				setExecutor(ctfCommand)
				tabCompleter = ctfCommand
			}

			val joinCommand = CtfJoinCommand(ctfConfig, gameManager)
			getCommand("ctfjoin")?.apply {
				setExecutor(joinCommand)
				tabCompleter = joinCommand
			}

			getCommand("ctfleave")?.setExecutor(CtfLeaveCommand(ctfConfig, gameManager))

			val kitCommand = CtfKitCommand(ctfConfig, gameManager)
			getCommand("ctfkit")?.apply {
				setExecutor(kitCommand)
				tabCompleter = kitCommand
			}

			server.pluginManager.registerEvents(GameListener(gameManager), this)
			server.pluginManager.registerEvents(NoCommandListener(ctfConfig), this)

			logger.info("[Diversia] CTF plugin enabled.")
		} catch (e: Exception) {
			logger.severe("[Diversia] Failed to enable CTF plugin: ${e.message}")
			e.printStackTrace()
			server.pluginManager.disablePlugin(this)
		}
	}

	override fun onDisable() {
		logger.info("[Diversia] CTF plugin disabling...")
		if (::gameManager.isInitialized) gameManager.resetGame()
		logger.info("[Diversia] CTF plugin disabled.")
	}
}
