package com.skipmoney.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.skipmoney.MainActivity
import com.skipmoney.R
import com.skipmoney.data.local.SkipMoneyDatabase
import com.skipmoney.ui.MoneyFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object SkipMoneyWidgetUpdater {
    private val updateScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val moneyFormatter = MoneyFormatter.forJapaneseYen()

    fun updateAll(context: Context) {
        val appContext = context.applicationContext
        val appWidgetManager = AppWidgetManager.getInstance(appContext)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(appContext, SkipMoneyWidgetProvider::class.java),
        )
        if (appWidgetIds.isNotEmpty()) {
            updateWidgets(appContext, appWidgetManager, appWidgetIds)
        }
    }

    fun updateWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val appContext = context.applicationContext
        updateScope.launch {
            val summary = SkipMoneyDatabase.getInstance(appContext).skipMoneyDao().getSummary()
            val streakText = appContext.getString(
                R.string.motivational_streak,
                appContext.getString(R.string.flame_emoji),
                summary?.currentStreakDays ?: 0,
            )
            val totalSavedText = appContext.getString(
                R.string.widget_total_saved_value,
                moneyFormatter.formatMinorUnits(summary?.totalSavedCents ?: 0L),
            )

            appWidgetIds.forEach { appWidgetId ->
                appWidgetManager.updateAppWidget(
                    appWidgetId,
                    RemoteViews(appContext.packageName, R.layout.skip_money_widget).apply {
                        setTextViewText(R.id.widget_streak, streakText)
                        setTextViewText(R.id.widget_total_saved, totalSavedText)
                        setOnClickPendingIntent(
                            R.id.widget_root,
                            PendingIntent.getActivity(
                                appContext,
                                appWidgetId,
                                Intent(appContext, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                },
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                            ),
                        )
                    },
                )
            }
        }
    }
}
