package com.example.kickoffwidget.data.sync

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.kickoffwidget.KickoffWidget
import com.example.kickoffwidget.data.models.MatchCardState
import com.example.kickoffwidget.data.repository.FixtureRepository

class FixtureSyncWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d("FixtureSyncWorker", "Starting sync work...")
        val repository = FixtureRepository(context)
        return try {
            val state = repository.fetchNextMatchCard()
            Log.d("FixtureSyncWorker", "Sync successful! State: $state")
            
            // Trigger Glance widget update
            KickoffWidget().updateAll(context)
            
            // Update Notification Widget
            com.example.kickoffwidget.utils.NotificationHelper.updateNotification(context, state)
            
            // Schedule the next one-off countdown refresh if needed
            if (state is MatchCardState.Match) {
                FixtureSyncScheduler.scheduleOneTimeTick(context, state)
            }
            
            Result.success()
        } catch (e: Exception) {
            Log.e("FixtureSyncWorker", "Sync failed", e)
            Result.retry()
        }
    }
}
