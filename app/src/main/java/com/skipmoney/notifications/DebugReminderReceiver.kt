package com.skipmoney.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class DebugReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.d(
            "DebugReminderReceiver",
            "onReceive: action=${intent?.action}, scheduling debug reminder in 10 seconds",
        )
        DailyReminderScheduler.scheduleDebugReminderIn10Seconds(context)
    }
}
