package com.nolly.mc.diversia.ctf.game

import com.nolly.mc.diversia.ctf.config.CtfConfig
import com.nolly.mc.diversia.ctf.model.CtfKit
import org.bukkit.entity.Player

class KitManager(private val config: CtfConfig) {
	fun getKit(id: String): CtfKit? = config.loadKit(id)
	fun getAllKitIds(): List<String> = config.loadAllKitIds()
	fun hasKit(id: String): Boolean = config.hasKit(id)

	fun saveFromPlayer(id: String, player: Player) {
		val inv = player.inventory
		val kit = CtfKit(
			id = id,
			contents = inv.storageContents.copyOf(),
			helmet = inv.helmet,
			chestplate = inv.chestplate,
			leggings = inv.leggings,
			boots = inv.boots
		)
		config.saveKit(kit)
	}

	fun removeKit(id: String) = config.deleteKit(id)

	fun applyKit(id: String, player: Player): Boolean {
		val kit = getKit(id) ?: return false
		applyKit(kit, player)
		return true
	}

	fun applyKit(kit: CtfKit, player: Player) {
		val inv = player.inventory
		val offhand = inv.itemInOffHand.clone()

		inv.clear()
		inv.storageContents = kit.contents
		inv.helmet = kit.helmet
		inv.chestplate = kit.chestplate
		inv.leggings = kit.leggings
		inv.boots = kit.boots

		inv.setItemInOffHand(offhand)
	}
}
