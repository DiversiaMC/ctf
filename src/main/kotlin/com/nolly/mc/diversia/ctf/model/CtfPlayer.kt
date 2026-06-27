package com.nolly.mc.diversia.ctf.model

import org.bukkit.inventory.ItemStack
import java.util.UUID

data class CtfPlayer(
	val uuid: UUID,
	var teamId: String,
	var isAlive: Boolean = true,
	var carriedFlagId: String? = null,
	var savedInventory: Array<ItemStack?> = arrayOfNulls(41),
	var captures: Int = 0,
	var kitId: String? = null
) {
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false
		other as CtfPlayer
		if (isAlive != other.isAlive) return false
		if (captures != other.captures) return false
		if (uuid != other.uuid) return false
		if (teamId != other.teamId) return false
		if (carriedFlagId != other.carriedFlagId) return false
		if (kitId != other.kitId) return false
		if (!savedInventory.contentEquals(other.savedInventory)) return false
		return true
	}

	override fun hashCode(): Int {
		var result = isAlive.hashCode()
		result = 31 * result + captures
		result = 31 * result + uuid.hashCode()
		result = 31 * result + teamId.hashCode()
		result = 31 * result + (carriedFlagId?.hashCode() ?: 0)
		result = 31 * result + (kitId?.hashCode() ?: 0)
		result = 31 * result + savedInventory.contentHashCode()
		return result
	}
}
