package com.example.kickoffwidget.data.models

import kotlinx.serialization.Serializable

@Serializable
data class MatchesResponse(
    val matches: List<MatchItem>
)

@Serializable
data class MatchItem(
    val id: Long,
    val utcDate: String,       // ISO UTC date string, e.g. "2026-06-15T16:00:00Z"
    val status: String,        // e.g. "TIMED", "SCHEDULED", "LIVE", "IN_PLAY", "PAUSED", "FINISHED"
    val matchday: Int? = null,
    val stage: String? = null,
    val group: String? = null,
    val minute: Int? = null,
    val homeTeam: TeamInfo,
    val awayTeam: TeamInfo,
    val score: ScoreInfo? = null
)

@Serializable
data class TeamInfo(
    val id: Int? = null,
    val name: String? = null,
    val shortName: String? = null,
    val tla: String? = null,    // 3-letter acronym (e.g. "ESP", "CPV")
    val crest: String? = null   // URL to team crest (can be SVG or PNG)
)

@Serializable
data class ScoreInfo(
    val winner: String? = null,
    val duration: String? = null,
    val fullTime: GoalsInfo? = null,
    val halfTime: GoalsInfo? = null
)

@Serializable
data class GoalsInfo(
    val home: Int? = null,
    val away: Int? = null
)

@Serializable
data class StandingsResponse(
    val standings: List<StandingItem>
)

@Serializable
data class StandingItem(
    val stage: String? = null,
    val type: String? = null,
    val group: String? = null,
    val table: List<TableItem>
)

@Serializable
data class TableItem(
    val position: Int,
    val team: TeamInfo,
    val playedGames: Int,
    val won: Int,
    val draw: Int,
    val lost: Int,
    val points: Int,
    val goalDifference: Int
)

