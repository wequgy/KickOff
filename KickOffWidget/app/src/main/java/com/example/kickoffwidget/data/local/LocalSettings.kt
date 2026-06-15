package com.example.kickoffwidget.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.kickoffwidget.data.models.MatchCardState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "kickoff_widget_prefs")

class LocalSettings(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private val KEY_API_KEY = stringPreferencesKey("api_key")
        private val KEY_MATCH_STATE = stringPreferencesKey("match_state")
        private val KEY_STANDINGS_CACHE = stringPreferencesKey("standings_cache")
        private val KEY_STANDINGS_CACHE_TIME = stringPreferencesKey("standings_cache_time")
    }

    val apiKeyFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        val saved = prefs[KEY_API_KEY]
        if (!saved.isNullOrBlank()) {
            saved
        } else {
            val buildKey = com.example.kickoffwidget.BuildConfig.API_FOOTBALL_KEY
            if (buildKey.isNotBlank()) buildKey else null
        }
    }

    suspend fun getApiKey(): String? {
        val saved = apiKeyFlow.first()
        if (!saved.isNullOrBlank()) return saved
        val buildKey = com.example.kickoffwidget.BuildConfig.API_FOOTBALL_KEY
        return if (buildKey.isNotBlank()) buildKey else null
    }

    suspend fun saveApiKey(apiKey: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_API_KEY] = apiKey
        }
    }

    val matchStateFlow: Flow<MatchCardState> = context.dataStore.data.map { prefs ->
        val jsonStr = prefs[KEY_MATCH_STATE]
        if (jsonStr != null) {
            try {
                json.decodeFromString<MatchCardState>(jsonStr)
            } catch (e: Exception) {
                MatchCardState.Loading
            }
        } else {
            MatchCardState.Loading
        }
    }

    suspend fun getMatchState(): MatchCardState = matchStateFlow.first()

    suspend fun saveMatchState(state: MatchCardState) {
        val jsonStr = json.encodeToString(state)
        context.dataStore.edit { prefs ->
            prefs[KEY_MATCH_STATE] = jsonStr
        }
    }

    suspend fun getStandingsCache(): String? {
        val prefs = context.dataStore.data.first()
        val cacheTime = prefs[KEY_STANDINGS_CACHE_TIME]?.toLongOrNull() ?: 0L
        if (System.currentTimeMillis() - cacheTime < 24 * 60 * 60 * 1000) {
            return prefs[KEY_STANDINGS_CACHE]
        }
        return null
    }

    suspend fun saveStandingsCache(standingsJson: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_STANDINGS_CACHE] = standingsJson
            prefs[KEY_STANDINGS_CACHE_TIME] = System.currentTimeMillis().toString()
        }
    }
}
