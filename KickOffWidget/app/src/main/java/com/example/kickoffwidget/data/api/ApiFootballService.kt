package com.example.kickoffwidget.data.api

import com.example.kickoffwidget.data.models.MatchesResponse
import com.example.kickoffwidget.data.models.StandingsResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiFootballService {
    @GET("competitions/{code}/matches")
    suspend fun getMatches(
        @Header("X-Auth-Token") apiKey: String,
        @Path("code") code: String,
        @Query("season") season: Int
    ): MatchesResponse

    @GET("competitions/{code}/standings")
    suspend fun getStandings(
        @Header("X-Auth-Token") apiKey: String,
        @Path("code") code: String,
        @Query("season") season: Int
    ): StandingsResponse
}

