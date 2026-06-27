package com.nolly.mc.diversia.ctf.game

import com.nolly.mc.diversia.ctf.config.CtfConfig
import com.nolly.mc.diversia.ctf.model.CtfTeam
import java.util.UUID

class TeamManager(private val config: CtfConfig) {
	private val teams = mutableMapOf<String, CtfTeam>()
	private val scores = mutableMapOf<String, Int>()
	private val members = mutableMapOf<String, MutableSet<UUID>>()

	fun load() {
		teams.clear()
		scores.clear()
		members.clear()
		config.loadTeams().forEach { team ->
			teams[team.id] = team
			scores[team.id] = 0
			members[team.id] = mutableSetOf()
		}
	}

	fun resetScores() { scores.keys.forEach { scores[it] = 0 } }

	fun getTeam(id: String): CtfTeam? = teams[id]
	fun getAllTeams(): List<CtfTeam> = teams.values.toList()
	fun hasTeam(id: String): Boolean = teams.containsKey(id)

	fun addTeam(team: CtfTeam) {
		teams[team.id] = team
		scores[team.id] = 0
		members[team.id] = mutableSetOf()
		config.saveTeam(team)
	}

	fun removeTeam(id: String) {
		teams.remove(id)
		scores.remove(id)
		members.remove(id)
		config.deleteTeam(id)
	}

	fun getScore(teamId: String): Int = scores[teamId] ?: 0
	fun incrementScore(teamId: String) { scores[teamId] = (scores[teamId] ?: 0) + 1 }

	fun getMembers(teamId: String): Set<UUID> = members[teamId] ?: emptySet()
	fun getMemberCount(teamId: String): Int = members[teamId]?.size ?: 0

	fun assignPlayer(uuid: UUID, teamId: String) {
		members.values.forEach { it.remove(uuid) }
		members[teamId]?.add(uuid)
	}

	fun removePlayer(uuid: UUID) { members.values.forEach { it.remove(uuid) } }

	fun getPlayerTeam(uuid: UUID): CtfTeam? =
		teams.entries.firstOrNull { (id, _) -> members[id]?.contains(uuid) == true }?.value

	fun isFull(teamId: String): Boolean {
		val max = teams[teamId]?.maxPlayers ?: return true
		return getMemberCount(teamId) >= max
	}

	fun getLeadingTeam(): CtfTeam? {
		val maxScore = scores.values.maxOrNull() ?: return null
		val leaders = scores.entries.filter { it.value == maxScore }
		if (leaders.size != 1) return null
		return teams[leaders.first().key]
	}

	fun isTie(): Boolean {
		val maxScore = scores.values.maxOrNull() ?: return true
		return scores.values.count { it == maxScore } > 1
	}
}
