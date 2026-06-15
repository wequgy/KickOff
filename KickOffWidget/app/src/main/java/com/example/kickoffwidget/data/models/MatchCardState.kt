package com.example.kickoffwidget.data.models

import kotlinx.serialization.Serializable

@Serializable
sealed interface MatchCardState {
    @Serializable
    data object Loading : MatchCardState

    @Serializable
    data object NoUpcomingMatch : MatchCardState

    @Serializable
    data class Match(
        val homeCode: String,
        val awayCode: String,
        val homeLogoPath: String,
        val awayLogoPath: String,
        val kickoffEpochMillis: Long,
        val venueName: String,
        val badgeText: String,
        val status: String,          // "NS" | "1H" | "2H" | "HT" | "FT" | ...
        val homeGoals: Int?,
        val awayGoals: Int?,
        val minute: Int? = null,
        val lastUpdatedEpochMillis: Long,
        val cycleMatches: List<MatchDetail> = emptyList(),
        val activeIndex: Int = 0
    ) : MatchCardState
}

@Serializable
data class MatchDetail(
    val homeCode: String,
    val awayCode: String,
    val homeLogoPath: String,
    val awayLogoPath: String,
    val kickoffEpochMillis: Long,
    val venueName: String,
    val badgeText: String,
    val status: String,
    val homeGoals: Int?,
    val awayGoals: Int?,
    val minute: Int? = null
)
