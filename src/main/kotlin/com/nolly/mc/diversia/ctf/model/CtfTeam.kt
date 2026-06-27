package com.nolly.mc.diversia.ctf.model

data class CtfTeam(
	val id: String,
	val displayName: String,
	val color: String,
	val maxPlayers: Int,
	val kitId: String? = null
)
