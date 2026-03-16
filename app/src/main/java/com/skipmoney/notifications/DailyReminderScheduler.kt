package com.skipmoney.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.skipmoney.MainActivity
import com.skipmoney.R
import com.skipmoney.settings.ReminderSettings
import com.skipmoney.settings.ReminderSettingsRepository
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import java.time.ZoneId

object DailyReminderScheduler {
    private const val CHANNEL_ID = "daily_saving_reminder"
    private const val REQUEST_CODE = 2001
    private const val NOTIFICATION_ID = 3001
    private const val TAG = "DailyReminderScheduler"
    const val ACTION_DEBUG_SCHEDULE_10S = "com.skipmoney.action.DEBUG_SCHEDULE_10S"

    fun scheduleNextReminder(context: Context) {
        val settings = runBlocking {
            ReminderSettingsRepository(context.applicationContext).getCurrentSettings()
        }
        scheduleNextReminder(context, settings)
    }

    fun scheduleNextReminder(
        context: Context,
        settings: ReminderSettings,
    ) {
        createNotificationChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = reminderPendingIntent(context)
        alarmManager.cancel(pendingIntent)

        if (!settings.notificationsEnabled) {
            Log.d(TAG, "scheduleNextReminder: notifications disabled, alarm cancelled")
            return
        }

        val triggerAtMillis = nextReminderTimeMillis(
            hour = settings.notificationHour,
            minute = settings.notificationMinute,
        )
        Log.d(
            TAG,
            "scheduleNextReminder: hour=${settings.notificationHour}, minute=${settings.notificationMinute}, triggerAtMillis=$triggerAtMillis, canExact=${Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()}",
        )
        scheduleAlarmSafely(
            alarmManager = alarmManager,
            triggerAtMillis = triggerAtMillis,
            pendingIntent = pendingIntent,
            logPrefix = "scheduleNextReminder",
        )
    }

    fun scheduleDebugReminderIn10Seconds(context: Context) {
        createNotificationChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = reminderPendingIntent(context)
        val triggerAtMillis = System.currentTimeMillis() + 10_000L
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "scheduleDebugReminderIn10Seconds: triggerAtMillis=$triggerAtMillis")
        scheduleAlarmSafely(
            alarmManager = alarmManager,
            triggerAtMillis = triggerAtMillis,
            pendingIntent = pendingIntent,
            logPrefix = "scheduleDebugReminderIn10Seconds",
        )
    }

    fun cancelReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(reminderPendingIntent(context))
        Log.d(TAG, "cancelReminder: alarm cancelled")
    }

    fun showNotification(context: Context) {
        createNotificationChannel(context)

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "showNotification: skipped because POST_NOTIFICATIONS is not granted")
            return
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_skipmoney)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(context.getString(R.string.notification_message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent(context))
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        Log.d(TAG, "showNotification: notification posted")
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notification_channel_description)
        }

        notificationManager.createNotificationChannel(channel)
    }

    private fun reminderPendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, DailyReminderReceiver::class.java).apply {
                action = "com.skipmoney.notifications.ACTION_DAILY_REMINDER"
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun contentPendingIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            REQUEST_CODE + 1,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun nextReminderTimeMillis(
        now: LocalDateTime = LocalDateTime.now(),
        hour: Int = 20,
        minute: Int = 0,
    ): Long {
        val reminderTime = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        val nextRun = if (now < reminderTime) reminderTime else reminderTime.plusDays(1)
        return nextRun.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun scheduleAlarmSafely(
        alarmManager: AlarmManager,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent,
        logPrefix: String,
    ) {
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        try {
            if (canExact) {
                Log.d(TAG, "$logPrefix: scheduling with setExactAndAllowWhileIdle")
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent,
                )
            } else {
                Log.d(TAG, "$logPrefix: scheduling with setAndAllowWhileIdle fallback because canExact=false")
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent,
                )
            }
        } catch (securityException: SecurityException) {
            Log.w(
                TAG,
                "$logPrefix: exact scheduling failed, falling back to setAndAllowWhileIdle",
                securityException,
            )
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent,
            )
        }
    }
}
