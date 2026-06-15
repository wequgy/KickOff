package com.example.kickoffwidget.ui.standings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.kickoffwidget.data.models.StandingItem
import com.example.kickoffwidget.data.repository.StandingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface StandingsUiState {
    object Loading : StandingsUiState
    data class Success(val standings: List<StandingItem>) : StandingsUiState
    data class Error(val message: String) : StandingsUiState
}

class StandingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = StandingsRepository(application)

    private val _uiState = MutableStateFlow<StandingsUiState>(StandingsUiState.Loading)
    val uiState: StateFlow<StandingsUiState> = _uiState.asStateFlow()

    init {
        loadStandings()
    }

    fun loadStandings() {
        viewModelScope.launch {
            _uiState.value = StandingsUiState.Loading
            try {
                val data = repository.fetchStandings()
                _uiState.value = StandingsUiState.Success(data)
            } catch (e: Exception) {
                _uiState.value = StandingsUiState.Error(e.message ?: "Failed to fetch standings")
            }
        }
    }
}
