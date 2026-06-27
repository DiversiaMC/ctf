package com.nolly.mc.diversia.ctf.display

import com.nolly.mc.diversia.ctf.config.CtfConfig
import com.nolly.mc.diversia.ctf.game.GameManager
import com.nolly.mc.diversia.ctf.model.GameState
import org.bukkit.Bukkit
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.boss.BossBar
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class CtfBossBar(
	private val plugin: JavaPlugin,
	private val config: CtfConfig,
	private val game: GameManager
) {
	private var timerBar: BossBar? = null
	private val teamBars = mutableMapOf<String, BossBar>()
	private var taskId: Int = -1

	fun start() {
		stop()
		buildBars()
		addAllOnlinePlayers()

		taskId = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
			if (game.state != GameState.RUNNING && game.state != GameState.PAUSED) return@Runnable
			updateBars()
		}, 0L, 20L).taskId
	}

	fun stop() {
		if (taskId != -1) {
			Bukkit.getScheduler().cancelTask(taskId)
			taskId = -1
		}
		removeAllOnlinePlayers()
		timerBar?.removeAll()
		timerBar = null
		teamBars.values.forEach { it.removeAll() }
		teamBars.clear()
	}

	fun addPlayer(player: Player) {
		timerBar?.addPlayer(player)
		teamBars.values.forEach { it.addPlayer(player) }
	}

	fun removePlayer(player: Player) {
		timerBar?.removePlayer(player)
		teamBars.values.forEach { it.removePlayer(player) }
	}

	private fun buildBars() {
		val condition = config.winCondition.uppercase()
		val showTimer = condition == "TIMER_END" || condition == "BOTH"
		val showTeams = condition == "FIRST_TO_X" || condition == "BOTH"

		if (showTimer) {
			timerBar = Bukkit.createBossBar("", BarColor.WHITE, BarStyle.SOLID)
			timerBar?.isVisible = true
		}

		if (showTeams) {
			game.teamManager.getAllTeams().forEach { team ->
				val color = resolveBarColor(team.color)
				val bar = Bukkit.createBossBar("", color, BarStyle.SEGMENTED_10)
				bar.isVisible = true
				teamBars[team.id] = bar
			}
		}
	}

	private fun updateBars() {
		timerBar?.let { bar ->
			val total = config.timeLimitSeconds.toDouble().coerceAtLeast(1.0)
			val remaining = game.timeRemainingSeconds.toDouble().coerceAtLeast(0.0)
			val progress = (remaining / total).coerceIn(0.0, 1.0)

			val minutes = game.timeRemainingSeconds / 60
			val seconds = game.timeRemainingSeconds % 60
			bar.setTitle("§f⏱ %02d:%02d".format(minutes, seconds))
			bar.progress = progress
			bar.color = timerBarColor(progress)
		}

		teamBars.forEach { (teamId, bar) ->
			val team = game.teamManager.getTeam(teamId) ?: return@forEach
			val score = game.teamManager.getScore(teamId)
			val max = config.maxScore.coerceAtLeast(1)
			val progress = (score.toDouble() / max.toDouble()).coerceIn(0.0, 1.0)

			bar.setTitle("${teamColorPrefix(team.color)}${team.displayName} §r§7— §f$score §7/ §f$max")
			bar.progress = progress
		}
	}

	private fun addAllOnlinePlayers() {
		Bukkit.getOnlinePlayers().forEach { addPlayer(it) }
	}

	private fun removeAllOnlinePlayers() {
		Bukkit.getOnlinePlayers().forEach { removePlayer(it) }
	}

	private fun resolveBarColor(color: String): BarColor = when (color.uppercase()) {
		"RED", "DARK_RED" -> BarColor.RED
		"BLUE", "DARK_BLUE" -> BarColor.BLUE
		"GREEN", "DARK_GREEN" -> BarColor.GREEN
		"YELLOW", "GOLD" -> BarColor.YELLOW
		"AQUA", "DARK_AQUA" -> BarColor.BLUE
		"LIGHT_PURPLE", "DARK_PURPLE" -> BarColor.PURPLE
		"WHITE" -> BarColor.WHITE
		else -> BarColor.WHITE
	}

	private fun teamColorPrefix(color: String): String = when (color.uppercase()) {
		"RED" -> "§c"
		"DARK_RED" -> "§4"
		"BLUE" -> "§9"
		"DARK_BLUE" -> "§1"
		"GREEN" -> "§a"
		"DARK_GREEN" -> "§2"
		"YELLOW" -> "§e"
		"GOLD" -> "§6"
		"AQUA" -> "§b"
		"DARK_AQUA" -> "§3"
		"LIGHT_PURPLE" -> "§d"
		"DARK_PURPLE" -> "§5"
		"WHITE" -> "§f"
		else -> "§f"
	}

	private fun timerBarColor(progress: Double): BarColor = when {
		progress > 0.5 -> BarColor.GREEN
		progress > 0.25 -> BarColor.YELLOW
		else -> BarColor.RED
	}
}
