package com.nolly.mc.diversia.ctf.listeners

import com.nolly.mc.diversia.ctf.game.GameManager
import com.nolly.mc.diversia.ctf.model.GameState
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.inventory.EquipmentSlot

class GameListener(private val game: GameManager) : Listener {
	private val activeStates = setOf(GameState.RUNNING, GameState.PAUSED, GameState.WAITING)

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	fun onEntityDamage(event: EntityDamageEvent) {
		val player = event.entity as? Player ?: return
		if (!game.isInGame(player.uniqueId)) {
			event.isCancelled = true
			return
		}
		if (game.state != GameState.RUNNING) return
		if (player.health - event.finalDamage > 0) return
		event.isCancelled = true
		player.health = 20.0
		game.respawnManager.teleportToSpawn(player)
		game.handleDeath(player)
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	fun onPlayerDeath(event: PlayerDeathEvent) {
		val player = event.entity
		if (!game.isInGame(player.uniqueId)) return
		if (game.state !in activeStates) return
		event.keepInventory = true
		event.drops.clear()
		event.deathMessage = null
		game.handleDeath(player)
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	fun onPlayerRespawn(event: PlayerRespawnEvent) {
		val player = event.player
		if (!game.isInGame(player.uniqueId)) return
		game.handleRespawn(player)
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	fun onItemDrop(event: PlayerDropItemEvent) {
		if (!game.isInGame(event.player.uniqueId)) return
		if (game.state !in activeStates) return
		event.isCancelled = true
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	fun onItemPickup(event: EntityPickupItemEvent) {
		val player = event.entity as? Player ?: return
		if (!game.isInGame(player.uniqueId)) return
		if (game.state !in activeStates) return
		event.isCancelled = true
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	fun onInventoryClick(event: InventoryClickEvent) {
		val player = event.whoClicked as? Player ?: return
		if (!game.isInGame(player.uniqueId)) return
		if (game.state !in activeStates) return
		event.isCancelled = true
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	fun onInventoryDrag(event: InventoryDragEvent) {
		val player = event.whoClicked as? Player ?: return
		if (!game.isInGame(player.uniqueId)) return
		if (game.state !in activeStates) return
		event.isCancelled = true
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	fun onPlayerInteract(event: PlayerInteractEvent) {
		val player = event.player
		if (!game.isInGame(player.uniqueId)) return
		if (game.state !in activeStates) return
		if (event.hand == EquipmentSlot.OFF_HAND && game.flagManager.isCarrying(player.uniqueId)) {
			event.isCancelled = true
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	fun onFoodLevelChange(event: FoodLevelChangeEvent) {
		val player = event.entity as? Player ?: return
		if (!game.isInGame(player.uniqueId)) {
			event.isCancelled = true
			event.foodLevel = 20
			return
		}
		if (game.state !in activeStates) return
		event.isCancelled = true
		event.foodLevel = 20
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	fun onPlayerMove(event: PlayerMoveEvent) {
		val player = event.player
		if (player.uniqueId !in game.lockedInBox) return
		val from = event.from
		val to = event.to ?: return
		if (from.blockX != to.blockX || from.blockY != to.blockY || from.blockZ != to.blockZ) {
			event.isCancelled = true
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
		val victim = event.entity as? Player ?: return
		val attacker = event.damager as? Player ?: return
		if (!game.isInGame(attacker.uniqueId) || !game.isInGame(victim.uniqueId)) {
			event.isCancelled = true
			return
		}
		if (game.state != GameState.RUNNING) return
		val victimTeam = game.teamManager.getPlayerTeam(victim.uniqueId)?.id
		val attackerTeam = game.teamManager.getPlayerTeam(attacker.uniqueId)?.id
		if (victimTeam != null && victimTeam == attackerTeam) {
			event.isCancelled = true
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	fun onPlayerQuit(event: PlayerQuitEvent) {
		val player = event.player
		if (!game.isInGame(player.uniqueId)) return
		game.leaveTeam(player)
		game.removePlayerFromBossBar(player)
	}

	@EventHandler(priority = EventPriority.LOWEST)
	fun onPlayerJoin(event: PlayerJoinEvent) {
		val player = event.player
		player.teleport(game.config.resolveLobbyLocation())
		when (game.state) {
			GameState.RUNNING, GameState.PAUSED -> {
				player.gameMode = GameMode.SPECTATOR
				game.addPlayerToBossBar(player)
			}
			else -> {}
		}
	}
}
