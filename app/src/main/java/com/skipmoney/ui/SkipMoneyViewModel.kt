package com.skipmoney.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skipmoney.R
import com.skipmoney.data.SkipMoneyData
import com.skipmoney.data.SkipMoneyRepository
import com.skipmoney.data.SkippedPurchase
import com.skipmoney.data.local.SkipMoneyDatabase
import com.skipmoney.notifications.DailyReminderScheduler
import com.skipmoney.settings.ReminderSettings
import com.skipmoney.settings.ReminderSettingsRepository
import com.skipmoney.widget.SkipMoneyWidgetUpdater
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class SkipMoneyUiState(
    val isLoading: Boolean = true,
    val showOnboarding: Boolean = false,
    val currentStreakDays: Int = 0,
    val streakHeadline: String = "",
    val milestoneMessage: String? = null,
    val todaySaved: String = "$0.00",
    val totalSaved: String = "$0.00",
    val currentMonthSaved: String = "$0.00",
    val calendarMonthLabel: String = "",
    val calendarMarkedDays: Set<Int> = emptySet(),
    val monthlyChartValues: List<Float> = emptyList(),
    val monthlyChartDayLabels: List<String> = emptyList(),
    val monthlyChartVersion: Int = 0,
    val notificationsEnabled: Boolean = true,
    val notificationTimeLabel: String = "",
    val skippedPurchases: List<SkippedPurchase> = emptyList(),
)

class SkipMoneyViewModel(application: Application) : AndroidViewModel(application) {
    private companion object {
        const val TAG = "SkipMoneyViewModel"
    }

    private val appContext = application.applicationContext
    private val repository = SkipMoneyRepository(
        SkipMoneyDatabase.getInstance(appContext).skipMoneyDao(),
    )
    private val reminderSettingsRepository = ReminderSettingsRepository(appContext)
    private val moneyFormatter = MoneyFormatter.forJapaneseYen()
    val messages = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val _uiState = MutableStateFlow(SkipMoneyUiState())

    val uiState: StateFlow<SkipMoneyUiState> = _uiState

    init {
        viewModelScope.launch {
            combine(
            repository.data,
            reminderSettingsRepository.settings,
        ) { data, settings ->
            toUiState(data, settings)
        }
                .collect { latestState ->
                    _uiState.value = latestState
                }
        }
    }

    private fun toUiState(
        data: SkipMoneyData,
        settings: ReminderSettings,
    ): SkipMoneyUiState =
        LocalDate.now().let { today ->
            // Root cause note:
            // The recent list updated because Compose consumed the latest item list directly,
            // but the chart is backed by AndroidView/MPAndroidChart and was more sensitive to
            // stale derived state. We explicitly rebuild the current-month series from the latest
            // saved snapshot and expose a version token so the chart subtree can be recreated.
            val monthlyChartValues = buildMonthlyChartValues(
                currentMonthDailySavedCents = data.currentMonthDailySavedCents,
                daysInMonth = today.lengthOfMonth(),
            )
            val monthlyChartDayLabels = (1..today.lengthOfMonth()).map(Int::toString)
            logChartComputation(
                month = today.format(monthFormatter()),
                currentMonthDailySavedCents = data.currentMonthDailySavedCents,
                monthlyChartValues = monthlyChartValues,
            )
            SkipMoneyUiState(
                isLoading = false,
                showOnboarding = !settings.onboardingCompleted,
                currentStreakDays = data.currentStreakDays,
                streakHeadline = appContext.getString(
                    R.string.motivational_streak,
                    appContext.getString(R.string.flame_emoji),
                    data.currentStreakDays,
                ),
                milestoneMessage = milestoneMessageFor(data.currentStreakDays),
                todaySaved = moneyFormatter.formatMinorUnits(data.todaySavedCents),
                totalSaved = moneyFormatter.formatMinorUnits(data.totalSavedCents),
                currentMonthSaved = moneyFormatter.formatMinorUnits(
                    data.currentMonthDailySavedCents.values.sum(),
                ),
                calendarMonthLabel = today.format(monthFormatter()),
                calendarMarkedDays = data.savingDates
                    .filter { it.year == today.year && it.month == today.month }
                    .mapTo(linkedSetOf()) { it.dayOfMonth },
                monthlyChartValues = monthlyChartValues,
                monthlyChartDayLabels = monthlyChartDayLabels,
                monthlyChartVersion = monthlyChartValues.hashCode(),
                notificationsEnabled = settings.notificationsEnabled,
                notificationTimeLabel = formatNotificationTime(
                    settings.notificationHour,
                    settings.notificationMinute,
                ),
                skippedPurchases = data.skippedPurchases,
            )
        }

    fun recordSkippedPurchase(
        title: String,
        amountText: String,
    ) {
        val normalizedTitle = title.trim()
        val amountCents = parseAmountToCents(amountText) ?: return
        Log.d(
            TAG,
            "recordSkippedPurchase: rawInputAmount='$amountText', normalizedTitle='$normalizedTitle', parsedAmountCents=$amountCents",
        )
        if (normalizedTitle.isEmpty()) return

        viewModelScope.launch {
            val result = repository.recordSkippedPurchase(
                title = normalizedTitle,
                amountCents = amountCents,
            )
            Log.d(
                TAG,
                "recordSkippedPurchase: insertedId=${result.insertedId}, amountCents=${result.amountCents}, createdAt=${result.createdAt}",
            )
            refreshUiState()
            SkipMoneyWidgetUpdater.updateAll(appContext)
            val snackbarMessage = if (result.streakBranch == "same_day") {
                appContext.getString(R.string.streak_already_counted)
            } else {
                buildList {
                    add(
                        appContext.getString(
                            R.string.celebration_streak_up,
                            appContext.getString(R.string.flame_emoji),
                            result.currentStreakDays,
                        ),
                    )
                    milestoneMessageFor(result.currentStreakDays)?.let(::add)
                }.joinToString(separator = "\n")
            }
            Log.d(
                TAG,
                "recordSkippedPurchaseSnackbar: branch=${result.streakBranch}, savedStreak=${result.currentStreakDays}, snackbarValue=${result.currentStreakDays}",
            )
            messages.emit(snackbarMessage)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            reminderSettingsRepository.setNotificationsEnabled(enabled)
            val settings = reminderSettingsRepository.getCurrentSettings()
            DailyReminderScheduler.scheduleNextReminder(appContext, settings)
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            reminderSettingsRepository.completeOnboarding()
        }
    }

    fun updateNotificationTime(
        hour: Int,
        minute: Int,
    ) {
        viewModelScope.launch {
            reminderSettingsRepository.updateNotificationTime(hour, minute)
            val settings = reminderSettingsRepository.getCurrentSettings()
            DailyReminderScheduler.scheduleNextReminder(appContext, settings)
            messages.emit(appContext.getString(R.string.notification_time_updated, formatNotificationTime(hour, minute)))
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            repository.resetAllData()
            refreshUiState()
            SkipMoneyWidgetUpdater.updateAll(appContext)
            messages.emit(appContext.getString(R.string.data_reset_completed))
        }
    }

    fun updateSkippedPurchase(
        id: Long,
        title: String,
        amountText: String,
    ) {
        val normalizedTitle = title.trim()
        val amountCents = parseAmountToCents(amountText) ?: return
        if (normalizedTitle.isEmpty()) return

        viewModelScope.launch {
            val updated = repository.updateSkippedPurchase(
                id = id,
                title = normalizedTitle,
                amountCents = amountCents,
            )
            if (!updated) return@launch
            refreshUiState()
            SkipMoneyWidgetUpdater.updateAll(appContext)
            messages.emit(appContext.getString(R.string.purchase_updated))
        }
    }

    fun deleteSkippedPurchase(id: Long) {
        viewModelScope.launch {
            val deleted = repository.deleteSkippedPurchase(id)
            if (!deleted) return@launch
            refreshUiState()
            SkipMoneyWidgetUpdater.updateAll(appContext)
            messages.emit(appContext.getString(R.string.purchase_deleted))
        }
    }

    private fun milestoneMessageFor(days: Int): String? =
        when (days) {
            7 -> appContext.getString(R.string.milestone_7_days)
            30 -> appContext.getString(R.string.milestone_30_days)
            100 -> appContext.getString(R.string.milestone_100_days)
            else -> null
        }

    private fun parseAmountToCents(amountText: String): Long? {
        val rawInput = amountText.trim()
        val amount = rawInput.toBigDecimalOrNull() ?: run {
            Log.d(TAG, "parseAmountToCents: rawInput='$rawInput' -> invalid number")
            return null
        }
        if (amount <= java.math.BigDecimal.ZERO) return null
        return try {
            // Root cause note:
            // The app is Japanese-first and the user enters yen, but this path previously
            // multiplied by 100 as if the input were a 2-decimal currency. That hid itself in
            // text formatting, while the chart exposed the mismatch. We now normalize to whole
            // yen here and persist that value consistently end-to-end.
            val normalizedYen = amount.setScale(0, RoundingMode.HALF_UP)
            val storedAmount = normalizedYen.longValueExact()
            Log.d(
                TAG,
                "parseAmountToCents: rawInput='$rawInput', parsedBigDecimal=$amount, normalizedYen=$normalizedYen, storedAmountCents=$storedAmount",
            )
            storedAmount
        } catch (_: ArithmeticException) {
            Log.d(TAG, "parseAmountToCents: rawInput='$rawInput' -> overflow/invalid exact long")
            null
        }
    }

    private fun formatNotificationTime(
        hour: Int,
        minute: Int,
    ): String = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)

    private fun monthFormatter(): DateTimeFormatter =
        DateTimeFormatter.ofPattern(
            if (Locale.getDefault().language == Locale.JAPANESE.language) "yyyy年M月" else "MMM yyyy",
            Locale.getDefault(),
        )

    private fun buildMonthlyChartValues(
        currentMonthDailySavedCents: Map<Int, Long>,
        daysInMonth: Int,
    ): List<Float> =
        (1..daysInMonth).map { day ->
            // Stored values are normalized to whole yen even though the legacy field name still says cents.
            amountMinorToChartYen(currentMonthDailySavedCents[day] ?: 0L)
        }

    private fun logChartComputation(
        month: String,
        currentMonthDailySavedCents: Map<Int, Long>,
        monthlyChartValues: List<Float>,
    ) {
        Log.d(TAG, "chartComputation: month=$month, dailyMap=$currentMonthDailySavedCents")
        Log.d(TAG, "chartComputation: chartEntriesCount=${monthlyChartValues.size}")
        monthlyChartValues.forEachIndexed { index, value ->
            Log.d(TAG, "chartComputation: entry x=${index + 1}, yYen=$value")
        }
    }

    private suspend fun refreshUiState() {
        val settings = reminderSettingsRepository.getCurrentSettings()
        val data = repository.getCurrentData()
        Log.d(
            TAG,
            "refreshUiState: entries=${data.skippedPurchases.size}, monthDaysWithSavings=${data.currentMonthDailySavedCents.size}, monthTotal=${data.currentMonthDailySavedCents.values.sum()}",
        )
        _uiState.value = toUiState(data, settings)
    }

    private fun amountMinorToChartYen(amountCents: Long): Float = amountCents.toFloat()
}
