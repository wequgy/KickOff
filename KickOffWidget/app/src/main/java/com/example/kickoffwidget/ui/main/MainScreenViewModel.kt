package com.example.kickoffwidget.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.kickoffwidget.data.local.LocalSettings
import com.example.kickoffwidget.data.models.MatchCardState
import com.example.kickoffwidget.data.repository.FixtureRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.glance.appwidget.updateAll

class MainScreenViewModel(application: Application) : AndroidViewModel(application) {
    private val localSettings = LocalSettings(application)
    private val repository = FixtureRepository(application)

    val apiKey: StateFlow<String?> = localSettings.apiKeyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val matchState: StateFlow<MatchCardState> = localSettings.matchStateFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MatchCardState.Loading)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    init {
        triggerManualSync()
    }

    fun saveApiKey(key: String) {
        viewModelScope.launch {
            localSettings.saveApiKey(key)
        }
    }

    fun triggerManualSync() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncError.value = null
            try {
                val state = repository.fetchNextMatchCard()
                try {
                    com.example.kickoffwidget.KickoffWidget().updateAll(getApplication())
                } catch (e: Exception) {
                    android.util.Log.e("MainScreenViewModel", "Failed to update widget in manual sync", e)
                }
                com.example.kickoffwidget.utils.NotificationHelper.updateNotification(getApplication(), state)
            } catch (e: Exception) {
                _syncError.value = e.message ?: "Failed to synchronize data"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun cycleNextMatch() {
        viewModelScope.launch {
            val state = localSettings.getMatchState()
            if (state is MatchCardState.Match && state.cycleMatches.isNotEmpty()) {
                val nextIndex = (state.activeIndex + 1) % state.cycleMatches.size
                val nextDetail = state.cycleMatches[nextIndex]
                val updatedState = state.copy(
                    homeCode = nextDetail.homeCode,
                    awayCode = nextDetail.awayCode,
                    homeLogoPath = nextDetail.homeLogoPath,
                    awayLogoPath = nextDetail.awayLogoPath,
                    kickoffEpochMillis = nextDetail.kickoffEpochMillis,
                    venueName = nextDetail.venueName,
                    badgeText = nextDetail.badgeText,
                    status = nextDetail.status,
                    homeGoals = nextDetail.homeGoals,
                    awayGoals = nextDetail.awayGoals,
                    minute = nextDetail.minute,
                    activeIndex = nextIndex
                )
                localSettings.saveMatchState(updatedState)
                try {
                    com.example.kickoffwidget.KickoffWidget().updateAll(getApplication())
                } catch (e: Exception) {
                    android.util.Log.e("MainScreenViewModel", "Failed to update widget", e)
                }
                com.example.kickoffwidget.utils.NotificationHelper.updateNotification(getApplication(), updatedState)
            }
        }
    }

    fun selectMatchIndex(index: Int) {
        viewModelScope.launch {
            val state = localSettings.getMatchState()
            if (state is MatchCardState.Match && index in state.cycleMatches.indices && index != state.activeIndex) {
                val nextDetail = state.cycleMatches[index]
                val updatedState = state.copy(
                    homeCode = nextDetail.homeCode,
                    awayCode = nextDetail.awayCode,
                    homeLogoPath = nextDetail.homeLogoPath,
                    awayLogoPath = nextDetail.awayLogoPath,
                    kickoffEpochMillis = nextDetail.kickoffEpochMillis,
                    venueName = nextDetail.venueName,
                    badgeText = nextDetail.badgeText,
                    status = nextDetail.status,
                    homeGoals = nextDetail.homeGoals,
                    awayGoals = nextDetail.awayGoals,
                    minute = nextDetail.minute,
                    activeIndex = index
                )
                localSettings.saveMatchState(updatedState)
                try {
                    com.example.kickoffwidget.KickoffWidget().updateAll(getApplication())
                } catch (e: Exception) {
                    android.util.Log.e("MainScreenViewModel", "Failed to update widget", e)
                }
                com.example.kickoffwidget.utils.NotificationHelper.updateNotification(getApplication(), updatedState)
            }
        }
    }
}
