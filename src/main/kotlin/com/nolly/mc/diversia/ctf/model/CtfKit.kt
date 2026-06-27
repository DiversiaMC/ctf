package com.nolly.mc.diversia.ctf.model

import org.bukkit.inventory.ItemStack

data class CtfKit(
	val id: String,
	val contents: Array<ItemStack?>,
	val helmet: ItemStack?,
	val chestplate: ItemStack?,
	val leggings: ItemStack?,
	val boots: ItemStack?
) {
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false
		other as CtfKit
		if (id != other.id) return false
		if (!contents.contentEquals(other.contents)) return false
		if (helmet != other.helmet) return false
		if (chestplate != other.chestplate) return false
		if (leggings != other.leggings) return false
		if (boots != other.boots) return false
		return true
	}

	override fun hashCode(): Int {
		var result = id.hashCode()
		result = 31 * result + contents.contentHashCode()
		result = 31 * result + (helmet?.hashCode() ?: 0)
		result = 31 * result + (chestplate?.hashCode() ?: 0)
		result = 31 * result + (leggings?.hashCode() ?: 0)
		result = 31 * result + (boots?.hashCode() ?: 0)
		return result
	}
}
