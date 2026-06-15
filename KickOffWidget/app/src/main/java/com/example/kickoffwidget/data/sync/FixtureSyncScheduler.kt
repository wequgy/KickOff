package com.example.kickoffwidget.data.sync

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.kickoffwidget.data.models.MatchCardState
import java.util.concurrent.TimeUnit

object FixtureSyncScheduler {
    private const val TAG = "FixtureSyncScheduler"
    private const val PERIODIC_WORK_NAME = "fixture_periodic_sync"
    private const val ONE_TIME_WORK_NAME = "fixture_one_time_sync"

    fun start(context: Context) {
        Log.d(TAG, "Starting scheduler...")
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<FixtureSyncWorker>(30, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 2, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            periodicRequest
        )

        // Run an immediate one-off sync to populate data on startup/enable
        val immediateRequest = OneTimeWorkRequestBuilder<FixtureSyncWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "immediate_sync",
            ExistingWorkPolicy.REPLACE,
            immediateRequest
        )
    }

    fun stop(context: Context) {
        Log.d(TAG, "Stopping scheduler...")
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_NAME)
        WorkManager.getInstance(context).cancelUniqueWork(ONE_TIME_WORK_NAME)
        WorkManager.getInstance(context).cancelUniqueWork("immediate_sync")
    }

    fun scheduleOneTimeTick(context: Context, state: MatchCardState.Match) {
        val now = System.currentTimeMillis()
        val remaining = state.kickoffEpochMillis - now

        val delayMinutes = when {
            // Match is live (1H, HT, 2H, ET, etc.) or just finished (FT)
            state.status in setOf("1H", "2H", "HT", "ET", "P", "LIVE", "IN_PLAY", "PAUSED") -> 5L
            // Countdown < 30 minutes: refresh every 5 minutes
            remaining <= 30 * 60 * 1000 -> 5L
            // Countdown between 30 mins and 3 hours: refresh every 15 minutes
            remaining <= 3 * 60 * 60 * 1000 -> 15L
            // Countdown > 3 hours: let the 30-min periodic worker handle it
            else -> return
        }

        Log.d(TAG, "Scheduling one-time tick in $delayMinutes minutes. Status=${state.status}, Remaining=${remaining / 60000}m")

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val oneTimeRequest = OneTimeWorkRequestBuilder<FixtureSyncWorker>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            ONE_TIME_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            oneTimeRequest
        )
    }
}
