package com.nolly.mc.diversia.ctf.game

import com.nolly.mc.diversia.ctf.config.CtfConfig
import com.nolly.mc.diversia.ctf.model.CtfFlag
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID

class FlagManager(
	private val plugin: JavaPlugin,
	private val config: CtfConfig
) {
	private val flags = mutableMapOf<String, CtfFlag>()

	private val carriedBy = mutableMapOf<String, UUID>()

	private val returnTimers = mutableMapOf<String, Int>()

	fun load() {
		cancelAllReturnTimers()
		flags.clear()
		carriedBy.clear()
		config.loadFlags().forEach { flags[it.id] = it }
	}

	fun reset() {
		cancelAllReturnTimers()
		carriedBy.clear()
		flags.keys.forEach { id -> flags[id] = flags[id]!!.copy(atBase = true) }
	}

	fun getAllFlags(): List<CtfFlag> = flags.values.toList()
	fun getFlag(id: String): CtfFlag? = flags[id]
	fun getFlagsForTeam(teamId: String): List<CtfFlag> = flags.values.filter { it.teamId == teamId }
	fun isAtBase(flagId: String): Boolean = flags[flagId]?.atBase ?: true
	fun getCarrier(flagId: String): UUID? = carriedBy[flagId]
	fun getFlagCarriedBy(uuid: UUID): CtfFlag? = flags.values.firstOrNull { carriedBy[it.id] == uuid }
	fun isCarrying(uuid: UUID): Boolean = carriedBy.values.contains(uuid)

	fun addFlag(flag: CtfFlag) {
		flags[flag.id] = flag
		config.saveFlag(flag)
	}

	fun removeFlag(id: String) {
		flags.remove(id)
		carriedBy.remove(id)
		returnTimers[id]?.let { Bukkit.getScheduler().cancelTask(it) }
		returnTimers.remove(id)
		config.deleteFlag(id)
	}

	fun pickup(flagId: String, player: Player): Boolean {
		val flag = flags[flagId] ?: return false
		if (!flag.atBase) return false
		if (carriedBy.containsKey(flagId)) return false

		flags[flagId] = flag.copy(atBase = false)
		carriedBy[flagId] = player.uniqueId

		player.inventory.setItemInOffHand(buildFlagItem(flag))
		player.isGlowing = true

		return true
	}

	fun drop(flagId: String, player: Player) {
		carriedBy.remove(flagId)
		player.inventory.setItemInOffHand(null)
		player.isGlowing = false
		scheduleReturn(flagId)
	}

	fun returnToBase(flagId: String) {
		flags[flagId] = flags[flagId]?.copy(atBase = true) ?: return
		carriedBy.remove(flagId)
		returnTimers[flagId]?.let { Bukkit.getScheduler().cancelTask(it) }
		returnTimers.remove(flagId)
	}

	fun getPickupableFlag(player: Player, playerTeamId: String): CtfFlag? =
		flags.values.firstOrNull { flag ->
			flag.teamId != playerTeamId
					&& flag.atBase
					&& !carriedBy.containsKey(flag.id)
					&& isInZone(player.location, flag)
		}

	fun getCapturableFlag(player: Player, playerTeamId: String): String? {
		val carried = getFlagCarriedBy(player.uniqueId) ?: return null
		if (carried.teamId == playerTeamId) return null

		val ownFlags = getFlagsForTeam(playerTeamId)
		return ownFlags.firstOrNull { own -> own.atBase && isInZone(player.location, own) }
			?.let { carried.id }
	}

	fun tickParticles() {
		val zoneParticle = runCatching { Particle.valueOf(config.flagParticle) }.getOrNull() ?: Particle.FLAME
		val carrierParticle = runCatching { Particle.valueOf(config.flagCarrierParticle) }.getOrNull() ?: Particle.CRIT

		flags.values.forEach { flag ->
			if (!flag.atBase) return@forEach
			val world = Bukkit.getWorld(flag.world) ?: return@forEach
			val center = Location(world, flag.x, flag.y + 1.0, flag.z)
			world.spawnParticle(zoneParticle, center, 6, 0.4, 0.5, 0.4, 0.0)
		}

		carriedBy.forEach { (_, uuid) ->
			val carrier = Bukkit.getPlayer(uuid) ?: return@forEach
			val loc = carrier.location.clone().add(0.0, 1.5, 0.0)
			loc.world?.spawnParticle(carrierParticle, loc, 4, 0.2, 0.2, 0.2, 0.0)
		}
	}

	fun isInZone(location: Location, flag: CtfFlag): Boolean {
		val world = Bukkit.getWorld(flag.world) ?: return false
		if (location.world?.name != world.name) return false
		val dx = location.x - flag.x
		val dz = location.z - flag.z
		return (dx * dx + dz * dz) <= (flag.radius * flag.radius)
	}

	private fun scheduleReturn(flagId: String) {
		val delayTicks = (config.flagReturnDelaySeconds * 20).toLong()
		val taskId = Bukkit.getScheduler().runTaskLater(plugin, Runnable {
			returnToBase(flagId)
			val flag = flags[flagId] ?: return@Runnable
			Bukkit.getOnlinePlayers().forEach { p ->
				com.nolly.mc.textapi.api.TextAPI.send(
					p,
					config.messages.flagReturned.replace("{team}", flag.teamId)
				)
			}
			returnTimers.remove(flagId)
		}, delayTicks).taskId
		returnTimers[flagId] = taskId
	}

	private fun cancelAllReturnTimers() {
		returnTimers.values.forEach { Bukkit.getScheduler().cancelTask(it) }
		returnTimers.clear()
	}

	private fun buildFlagItem(flag: CtfFlag): ItemStack {
		val material = runCatching {
			org.bukkit.Material.valueOf(config.flagItem)
		}.getOrNull() ?: org.bukkit.Material.WHITE_BANNER
		val item = ItemStack(material)
		val meta = item.itemMeta
		meta?.setDisplayName("§r${flag.teamId}'s Flag")
		item.itemMeta = meta
		return item
	}
}
