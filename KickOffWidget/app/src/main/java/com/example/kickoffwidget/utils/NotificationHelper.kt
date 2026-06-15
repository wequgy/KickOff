package com.example.kickoffwidget.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.kickoffwidget.MainActivity
import com.example.kickoffwidget.R
import com.example.kickoffwidget.data.models.MatchCardState
import java.io.File
import java.util.Locale

object NotificationHelper {
    private const val CHANNEL_ID = "kickoff_match_status_v2"
    private const val CHANNEL_NAME = "Match Status Widget"
    private const val NOTIFICATION_ID = 2026

    fun updateNotification(context: Context, state: MatchCardState) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // 1. Create channel on API 26+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Shows the ongoing next match countdown and live score"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // 2. Check if we should dismiss or show
        if (state !is MatchCardState.Match) {
            notificationManager.cancel(NOTIFICATION_ID)
            return
        }

        // Check permission on API 33+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val now = System.currentTimeMillis()
        val remaining = state.kickoffEpochMillis - now
        val isLive = state.status in setOf("1H", "2H", "HT", "ET", "P", "LIVE", "IN_PLAY", "PAUSED")

        val centerLabel = when {
            isLive || state.status == "FT" || state.status == "FINISHED" -> {
                val homeG = state.homeGoals ?: 0
                val awayG = state.awayGoals ?: 0
                "$homeG-$awayG"
            }
            remaining <= 0 -> "LIVE"
            else -> {
                val formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
                    .withZone(java.time.ZoneId.of("Asia/Kolkata"))
                formatter.format(java.time.Instant.ofEpochMilli(state.kickoffEpochMillis))
            }
        }

        val captionLabel = when {
            state.status == "FT" || state.status == "FINISHED" -> "FULL TIME"
            state.status == "PAUSED" || state.status == "HT" -> "HT • HALF TIME"
            isLive -> {
                if (state.minute != null) "${state.minute}' • LIVE" else "LIVE"
            }
            remaining <= 0 -> "LIVE"
            else -> "KICKOFF (IST)"
        }

        // Generate the rotated pill bitmap
        val density = context.resources.displayMetrics.density
        val pillBitmap = BitmapUtils.createRotatedPill(context, centerLabel, density)

        // 3. Build RemoteViews
        val collapsedViews = RemoteViews(context.packageName, R.layout.notification_widget_collapsed)
        val expandedViews = RemoteViews(context.packageName, R.layout.notification_widget_expanded)

        // Setup Collapsed Views
        collapsedViews.setTextViewText(R.id.home_code, state.homeCode)
        collapsedViews.setTextViewText(R.id.away_code, state.awayCode)
        collapsedViews.setImageViewBitmap(R.id.countdown_pill, pillBitmap)
        
        val homeLogoFile = File(state.homeLogoPath)
        if (homeLogoFile.exists() && homeLogoFile.length() > 0L) {
            val homeBitmap = BitmapFactory.decodeFile(state.homeLogoPath)
            collapsedViews.setImageViewBitmap(R.id.home_logo, homeBitmap)
            expandedViews.setImageViewBitmap(R.id.home_logo, homeBitmap)
        }
        val awayLogoFile = File(state.awayLogoPath)
        if (awayLogoFile.exists() && awayLogoFile.length() > 0L) {
            val awayBitmap = BitmapFactory.decodeFile(state.awayLogoPath)
            collapsedViews.setImageViewBitmap(R.id.away_logo, awayBitmap)
            expandedViews.setImageViewBitmap(R.id.away_logo, awayBitmap)
        }

        // Setup Expanded Views
        expandedViews.setTextViewText(R.id.home_code, state.homeCode)
        expandedViews.setTextViewText(R.id.away_code, state.awayCode)
        expandedViews.setImageViewBitmap(R.id.countdown_pill, pillBitmap)
        expandedViews.setTextViewText(R.id.venue_name, state.venueName.uppercase(Locale.getDefault()))
        expandedViews.setTextViewText(R.id.group_badge_text, state.badgeText)

        val isCaptionLive = isLive && state.status != "PAUSED" && state.status != "HT" && state.status != "FT" && state.status != "FINISHED"
        if (isCaptionLive) {
            val liveText = if (state.minute != null) "${state.minute}' • LIVE" else "LIVE"
            expandedViews.setViewVisibility(R.id.live_badge_container, View.VISIBLE)
            expandedViews.setTextViewText(R.id.live_badge_text, liveText)
            expandedViews.setViewVisibility(R.id.caption_text, View.GONE)
        } else {
            expandedViews.setViewVisibility(R.id.live_badge_container, View.GONE)
            expandedViews.setViewVisibility(R.id.caption_text, View.VISIBLE)
            expandedViews.setTextViewText(R.id.caption_text, captionLabel)
        }

        // Intent to open MainActivity on click
        val clickIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 4. Build Notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stadium)
            .setContentTitle("${state.homeCode} vs ${state.awayCode}")
            .setContentText(centerLabel)
            .setCustomContentView(collapsedViews)
            .setCustomBigContentView(expandedViews)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun dismissNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
