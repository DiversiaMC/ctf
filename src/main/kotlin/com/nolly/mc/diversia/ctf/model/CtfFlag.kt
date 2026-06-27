package com.nolly.mc.diversia.ctf.model

data class CtfFlag(
	val id: String,
	val teamId: String,
	val world: String,
	val x: Double,
	val y: Double,
	val z: Double,
	val radius: Double,
	val atBase: Boolean = true
)
