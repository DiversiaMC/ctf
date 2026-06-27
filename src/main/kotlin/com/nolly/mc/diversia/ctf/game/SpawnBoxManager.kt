package com.nolly.mc.diversia.ctf.game

import com.nolly.mc.diversia.ctf.config.CtfConfig
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material

class SpawnBoxManager(private val config: CtfConfig) {
	private val savedBlocks = mutableMapOf<String, Map<Location, Material>>()

	private val BOX_OFFSETS: List<Triple<Int, Int, Int>> by lazy {
		val offsets = mutableListOf<Triple<Int, Int, Int>>()
		for (x in -1..1) for (y in 0..2) for (z in -1..1) {
			if (x == 0 && z == 0 && y in 1..1) continue // leave player space open (y=1 is feet, y=2 is head)
			if (x in -1..1 && y in 0..2 && z in -1..1) {
				val isWall = x == -1 || x == 1 || z == -1 || z == 1 || y == 0 || y == 2
				if (isWall) offsets.add(Triple(x, y, z))
			}
		}
		offsets
	}

	fun placeBox(teamId: String) {
		val spawnData = config.loadSpawn(teamId) ?: return
		val world = Bukkit.getWorld(spawnData.world) ?: return
		val center = Location(world, spawnData.x, spawnData.y, spawnData.z)

		val saved = mutableMapOf<Location, Material>()
		for ((dx, dy, dz) in BOX_OFFSETS) {
			val loc = center.clone().add(dx.toDouble(), dy.toDouble(), dz.toDouble())
			saved[loc] = loc.block.type
			loc.block.type = Material.GLASS
		}
		savedBlocks[teamId] = saved
	}

	fun restoreBox(teamId: String) {
		val saved = savedBlocks.remove(teamId) ?: return
		saved.forEach { (loc, material) -> loc.block.type = material }
	}

	fun restoreAll() {
		savedBlocks.keys.toList().forEach { restoreBox(it) }
	}
}
