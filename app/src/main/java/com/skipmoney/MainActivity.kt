package com.skipmoney

import android.Manifest
import android.content.res.Configuration
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.core.content.ContextCompat
import com.google.android.gms.ads.MobileAds
import com.skipmoney.notifications.DailyReminderScheduler
import com.skipmoney.ui.SkipMoneyApp
import com.skipmoney.ui.SkipMoneyViewModel
import com.skipmoney.ui.theme.SkipMoneyTheme

class MainActivity : ComponentActivity() {
    private val viewModel: SkipMoneyViewModel by viewModels()
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            DailyReminderScheduler.scheduleNextReminder(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ) { resources ->
                (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                    Configuration.UI_MODE_NIGHT_YES
            },
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ) { resources ->
                (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                    Configuration.UI_MODE_NIGHT_YES
            },
        )
        requestNotificationPermissionIfNeeded()
        MobileAds.initialize(this)
        DailyReminderScheduler.scheduleNextReminder(this)

        setContent {
            val state by viewModel.uiState.collectAsState()

            SkipMoneyTheme {
                SkipMoneyApp(
                    state = state,
                    onSaveSkippedPurchase = viewModel::recordSkippedPurchase,
                    onUpdateSkippedPurchase = viewModel::updateSkippedPurchase,
                    onDeleteSkippedPurchase = viewModel::deleteSkippedPurchase,
                    onToggleNotifications = viewModel::setNotificationsEnabled,
                    onUpdateNotificationTime = viewModel::updateNotificationTime,
                    onResetData = viewModel::resetAllData,
                    onCompleteOnboarding = viewModel::completeOnboarding,
                    messages = viewModel.messages,
                )
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }

        val hasPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
