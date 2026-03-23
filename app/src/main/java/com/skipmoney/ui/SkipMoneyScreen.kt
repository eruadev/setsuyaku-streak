package com.skipmoney.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.skipmoney.R
import com.skipmoney.data.SkippedPurchase
import kotlinx.coroutines.flow.Flow
import android.util.Log
import java.time.Instant
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val moneyFormatter = MoneyFormatter.forJapaneseYen()

private fun currentLocale(): Locale = Locale.getDefault()

private fun recentPurchaseDateFormatter(): DateTimeFormatter =
    DateTimeFormatter.ofPattern(
        if (currentLocale().language == Locale.JAPANESE.language) "MM/dd" else "M/d",
        currentLocale(),
    )

private fun monthFormatter(): DateTimeFormatter =
    DateTimeFormatter.ofPattern(
        if (currentLocale().language == Locale.JAPANESE.language) "yyyy年M月" else "MMM yyyy",
        currentLocale(),
    )

private object ExternalLinks {
    const val privacyPolicyUrl = "https://sites.google.com/view/setsuyaku-policy/%E3%83%9B%E3%83%BC%E3%83%A0"
    const val playStoreUrl = "https://play.google.com/store/apps/details?id=com.skipmoney"
    const val packageName = "com.skipmoney"
}

private enum class ScreenDestination {
    Home,
    Settings,
}

@Composable
fun SkipMoneyApp(
    state: SkipMoneyUiState,
    onSaveSkippedPurchase: (String, String) -> Unit,
    onUpdateSkippedPurchase: (Long, String, String) -> Unit,
    onDeleteSkippedPurchase: (Long) -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onUpdateNotificationTime: (Int, Int) -> Unit,
    onResetData: () -> Unit,
    onCompleteOnboarding: () -> Unit,
    messages: Flow<String>,
) {
    if (state.isLoading) {
        LoadingScreen()
        return
    }

    if (state.showOnboarding) {
        OnboardingScreen(onComplete = onCompleteOnboarding)
        return
    }

    var isDialogVisible by rememberSaveable { mutableStateOf(false) }
    var currentScreen by rememberSaveable { mutableStateOf(ScreenDestination.Home) }
    var editingPurchase by remember { mutableStateOf<SkippedPurchase?>(null) }
    var deletingPurchase by remember { mutableStateOf<SkippedPurchase?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val navigateBackToHome = { currentScreen = ScreenDestination.Home }

    BackHandler(enabled = currentScreen == ScreenDestination.Settings) {
        navigateBackToHome()
    }

    LaunchedEffect(messages) {
        messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .navigationBarsPadding(),
            ) { snackbarData ->
                Snackbar(snackbarData = snackbarData)
            }
        },
        bottomBar = {
            if (currentScreen == ScreenDestination.Home) {
                BannerBottomBar {
                    AdMobBanner()
                }
            }
        },
    ) { innerPadding ->
        when (currentScreen) {
            ScreenDestination.Home -> {
                HomeScreen(
                    state = state,
                    innerPadding = innerPadding,
                    onOpenSettings = { currentScreen = ScreenDestination.Settings },
                    onSkipToday = { isDialogVisible = true },
                    onEditPurchase = { editingPurchase = it },
                    onDeletePurchase = { deletingPurchase = it },
                )
            }

            ScreenDestination.Settings -> {
                SettingsScreen(
                    state = state,
                    innerPadding = innerPadding,
                    onBack = navigateBackToHome,
                    onToggleNotifications = onToggleNotifications,
                    onUpdateNotificationTime = onUpdateNotificationTime,
                    onResetData = onResetData,
                )
            }
        }
    }

    if (isDialogVisible) {
        AddSkippedPurchaseDialog(
            onDismiss = { isDialogVisible = false },
            onSave = { title, amount ->
                onSaveSkippedPurchase(title, amount)
                isDialogVisible = false
            },
        )
    }

    editingPurchase?.let { purchase ->
        EditSkippedPurchaseDialog(
            purchase = purchase,
            onDismiss = { editingPurchase = null },
            onSave = { title, amount ->
                onUpdateSkippedPurchase(purchase.id, title, amount)
                editingPurchase = null
            },
        )
    }

    deletingPurchase?.let { purchase ->
        DeleteSkippedPurchaseDialog(
            purchase = purchase,
            onDismiss = { deletingPurchase = null },
            onConfirm = {
                onDeleteSkippedPurchase(purchase.id)
                deletingPurchase = null
            },
        )
    }
}

@Composable
private fun BannerBottomBar(
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom),
            ),
        color = MaterialTheme.colorScheme.background,
    ) {
        content()
    }
}

@Composable
private fun OnboardingScreen(
    onComplete: () -> Unit,
) {
    var currentPage by rememberSaveable { mutableStateOf(0) }
    val pages = listOf(
        OnboardingPage(
            title = stringResource(R.string.onboarding_page_1_title),
            body = stringResource(R.string.onboarding_page_1_body),
        ),
        OnboardingPage(
            title = stringResource(R.string.onboarding_page_2_title),
            body = stringResource(R.string.onboarding_page_2_body),
        ),
        OnboardingPage(
            title = stringResource(R.string.onboarding_page_3_title),
            body = stringResource(R.string.onboarding_page_3_body),
        ),
    )
    val isLastPage = currentPage == pages.lastIndex

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onComplete) {
                    Text(stringResource(R.string.onboarding_skip))
                }
            }

            Surface(
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = pages[currentPage].title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = pages[currentPage].body,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.86f),
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    pages.indices.forEach { index ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (index == currentPage) 24.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index == currentPage) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                                ),
                        )
                    }
                }

                Button(
                    onClick = {
                        if (isLastPage) {
                            onComplete()
                        } else {
                            currentPage += 1
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(22.dp),
                ) {
                    Text(
                        text = stringResource(
                            if (isLastPage) R.string.onboarding_start else R.string.onboarding_next,
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

private data class OnboardingPage(
    val title: String,
    val body: String,
)

@Composable
private fun HomeScreen(
    state: SkipMoneyUiState,
    innerPadding: androidx.compose.foundation.layout.PaddingValues,
    onOpenSettings: () -> Unit,
    onSkipToday: () -> Unit,
    onEditPurchase: (SkippedPurchase) -> Unit,
    onDeletePurchase: (SkippedPurchase) -> Unit,
) {
    val context = LocalContext.current
    var displayedMonthKey by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val displayedMonth = YearMonth.parse(displayedMonthKey)
    val displayedMonthChartValues = buildMonthlyChartValuesForMonth(
        purchases = state.skippedPurchases,
        displayedMonth = displayedMonth,
    )
    val displayedMonthChartLabels = (1..displayedMonth.lengthOfMonth()).map(Int::toString)
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            )
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
                Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.screen_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            HeroStatsCard(
                currentStreakDays = state.currentStreakDays,
                todaySaved = state.todaySaved,
                currentMonthSaved = state.currentMonthSaved,
                totalSaved = state.totalSaved,
            )
        }

        state.milestoneMessage?.let { milestoneMessage ->
            item {
                MilestoneMessage(message = milestoneMessage)
            }
        }

        item {
            Button(
                onClick = onSkipToday,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text(
                    text = stringResource(R.string.skip_today),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            OutlinedButton(
                onClick = {
                    val shareText = context.getString(
                        R.string.share_text,
                        context.getString(R.string.flame_emoji),
                        state.currentStreakDays,
                        state.currentMonthSaved,
                        state.totalSaved,
                    )
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(
                        Intent.createChooser(
                            shareIntent,
                            context.getString(R.string.share_chooser_title),
                        ),
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(20.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Share,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    text = stringResource(R.string.share),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        item {
            MonthlyCalendarSection(
                displayedMonth = displayedMonth,
                onPreviousMonth = {
                    displayedMonthKey = displayedMonth.minusMonths(1).toString()
                },
                onNextMonth = {
                    displayedMonthKey = displayedMonth.plusMonths(1).toString()
                },
                purchases = state.skippedPurchases,
            )
        }

        item {
            key(displayedMonthKey to displayedMonthChartValues.hashCode()) {
                MonthlySavingsChart(
                    title = stringResource(
                        R.string.monthly_savings_chart_for_month,
                        displayedMonth.format(monthFormatter()),
                    ),
                    hint = stringResource(R.string.monthly_savings_chart_hint),
                    values = displayedMonthChartValues,
                    dayLabels = displayedMonthChartLabels,
                )
            }
        }

        item {
            Text(
                text = stringResource(R.string.recent_skipped_purchases),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        if (state.skippedPurchases.isEmpty()) {
            item {
                EmptyRecentPurchases()
            }
        } else {
            val recentPurchases = state.skippedPurchases
                .sortedByDescending { purchase -> purchase.createdAt }
                .take(10)
            items(
                items = recentPurchases,
                key = { purchase -> purchase.id },
            ) { purchase ->
                RecentPurchaseRow(
                    purchase = purchase,
                    onEdit = { onEditPurchase(purchase) },
                    onDelete = { onDeletePurchase(purchase) },
                )
            }
        }

        item {
            SettingsEntryCard(onClick = onOpenSettings)
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HeroStatsCard(
    currentStreakDays: Int,
    todaySaved: String,
    currentMonthSaved: String,
    totalSaved: String,
) {
    LaunchedEffect(todaySaved, currentMonthSaved, totalSaved) {
        Log.d(
            "SkipMoneyScreen",
            "HeroStatsCard: todaySaved='$todaySaved', currentMonthSaved='$currentMonthSaved', totalSaved='$totalSaved'",
        )
    }

    Surface(
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                text = stringResource(R.string.hero_monthly_saved_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f),
            )
            Text(
                text = currentMonthSaved,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = stringResource(R.string.hero_today_saved_value, todaySaved),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.92f),
            )
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.26f),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HeroStatBlock(
                        modifier = Modifier.weight(0.8f),
                        label = stringResource(R.string.hero_streak_label),
                        value = stringResource(R.string.streak_days_value, currentStreakDays),
                    )
                    HeroStatBlock(
                        modifier = Modifier.weight(1.2f),
                        label = stringResource(R.string.hero_total_saved_label),
                        value = totalSaved,
                        valueStyle = MaterialTheme.typography.titleMedium,
                        valueMaxLines = 2,
                        valueSoftWrap = true,
                        valueOverflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                text = stringResource(R.string.streak_supporting),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun HeroStatBlock(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    valueStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleLarge,
    valueMaxLines: Int = 1,
    valueSoftWrap: Boolean = false,
    valueOverflow: TextOverflow = TextOverflow.Clip,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f),
        )
        Text(
            text = value,
            style = valueStyle,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = valueMaxLines,
            softWrap = valueSoftWrap,
            overflow = valueOverflow,
            textAlign = TextAlign.Start,
        )
    }
}

@Composable
private fun SettingsEntryCard(
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.settings_entry_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onClick) {
                Text(stringResource(R.string.change))
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    state: SkipMoneyUiState,
    innerPadding: androidx.compose.foundation.layout.PaddingValues,
    onBack: () -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onUpdateNotificationTime: (Int, Int) -> Unit,
    onResetData: () -> Unit,
) {
    val context = LocalContext.current
    var isTimeDialogVisible by rememberSaveable { mutableStateOf(false) }
    var isResetDialogVisible by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            )
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                    )
                }
                Text(
                    text = stringResource(R.string.settings),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        item {
            SettingsActionCard(
                title = stringResource(R.string.settings_notification_time),
                subtitle = stringResource(R.string.notification_time_hint, state.notificationTimeLabel),
                actionLabel = stringResource(R.string.change),
                onClick = { isTimeDialogVisible = true },
            )
        }

        item {
            SettingsToggleCard(
                title = stringResource(R.string.settings_notifications_enabled),
                subtitle = if (state.notificationsEnabled) {
                    stringResource(R.string.enabled)
                } else {
                    stringResource(R.string.disabled)
                },
                checked = state.notificationsEnabled,
                onCheckedChange = onToggleNotifications,
            )
        }

        item {
            SettingsActionCard(
                title = stringResource(R.string.settings_reset_data),
                subtitle = stringResource(R.string.reset_data_confirm_body),
                actionLabel = stringResource(R.string.reset),
                onClick = { isResetDialogVisible = true },
            )
        }

        item {
            SettingsInfoCard(
                title = stringResource(R.string.settings_about),
                body = stringResource(R.string.settings_about_body),
            )
        }

        item {
            SettingsLinksCard(
                title = stringResource(R.string.settings_more),
                items = listOf(
                    SettingsLinkItem(
                        title = stringResource(R.string.privacy_policy),
                        subtitle = stringResource(R.string.privacy_policy_hint),
                        onClick = { openUrlSafely(context, ExternalLinks.privacyPolicyUrl) },
                    ),
                    SettingsLinkItem(
                        title = stringResource(R.string.share_app),
                        subtitle = stringResource(R.string.share_app_hint),
                        onClick = { shareAppSafely(context) },
                    ),
                    SettingsLinkItem(
                        title = stringResource(R.string.write_review),
                        subtitle = stringResource(R.string.write_review_hint),
                        onClick = { openReviewPageSafely(context) },
                    ),
                ),
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (isTimeDialogVisible) {
        NotificationTimeDialog(
            currentTime = state.notificationTimeLabel,
            onDismiss = { isTimeDialogVisible = false },
            onSave = { hour, minute ->
                onUpdateNotificationTime(hour, minute)
                isTimeDialogVisible = false
            },
        )
    }

    if (isResetDialogVisible) {
        AlertDialog(
            onDismissRequest = { isResetDialogVisible = false },
            title = { Text(stringResource(R.string.reset_data_confirm_title)) },
            text = { Text(stringResource(R.string.reset_data_confirm_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        onResetData()
                        isResetDialogVisible = false
                    },
                ) {
                    Text(stringResource(R.string.reset))
                }
            },
            dismissButton = {
                TextButton(onClick = { isResetDialogVisible = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

private data class SettingsLinkItem(
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit,
)

@Composable
private fun SettingsActionCard(
    title: String,
    subtitle: String,
    actionLabel: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.size(12.dp))
            TextButton(onClick = onClick) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun SettingsToggleCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}

@Composable
private fun SettingsInfoCard(
    title: String,
    body: String,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsLinksCard(
    title: String,
    items: List<SettingsLinkItem>,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = title,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            items.forEachIndexed { index, item ->
                if (index > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                }
                ListItem(
                    modifier = Modifier.clickable(onClick = item.onClick),
                    headlineContent = {
                        Text(
                            text = item.title,
                            fontWeight = FontWeight.Medium,
                        )
                    },
                    supportingContent = {
                        Text(text = item.subtitle)
                    },
                )
            }
        }
    }
}

private fun openUrlSafely(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    } else {
        Toast.makeText(context, R.string.external_link_unavailable, Toast.LENGTH_SHORT).show()
    }
}

private fun shareAppSafely(context: Context) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, ExternalLinks.playStoreUrl)
    }
    val chooserIntent = Intent.createChooser(
        shareIntent,
        context.getString(R.string.share_app_chooser_title),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (shareIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(chooserIntent)
    } else {
        Toast.makeText(context, R.string.external_link_unavailable, Toast.LENGTH_SHORT).show()
    }
}

private fun openReviewPageSafely(context: Context) {
    val marketIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("market://details?id=${ExternalLinks.packageName}"),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val browserIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse(ExternalLinks.playStoreUrl),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    when {
        marketIntent.resolveActivity(context.packageManager) != null -> context.startActivity(marketIntent)
        browserIntent.resolveActivity(context.packageManager) != null -> context.startActivity(browserIntent)
        else -> Toast.makeText(context, R.string.external_link_unavailable, Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun NotificationTimeDialog(
    currentTime: String,
    onDismiss: () -> Unit,
    onSave: (Int, Int) -> Unit,
) {
    val initialParts = currentTime.split(":")
    var hourText by rememberSaveable { mutableStateOf(initialParts.getOrNull(0).orEmpty()) }
    var minuteText by rememberSaveable { mutableStateOf(initialParts.getOrNull(1).orEmpty()) }
    var attemptedSave by remember { mutableStateOf(false) }
    val hour = hourText.toIntOrNull()
    val minute = minuteText.toIntOrNull()
    val isValid = hour != null && minute != null && hour in 0..23 && minute in 0..59

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.notification_time_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = hourText,
                        onValueChange = { hourText = it.take(2).filter(Char::isDigit) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text(stringResource(R.string.hour_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    OutlinedTextField(
                        value = minuteText,
                        onValueChange = { minuteText = it.take(2).filter(Char::isDigit) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text(stringResource(R.string.minute_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
                if (attemptedSave && !isValid) {
                    Text(
                        text = stringResource(R.string.time_input_error),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    attemptedSave = true
                    if (isValid) {
                        onSave(hour!!, minute!!)
                    }
                },
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun MonthlyCalendarSection(
    displayedMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    purchases: List<SkippedPurchase>,
) {
    var selectedDay by rememberSaveable { mutableStateOf<Int?>(null) }
    val zoneId = ZoneId.systemDefault()
    LaunchedEffect(displayedMonth) {
        selectedDay = null
    }
    val purchasesByDate = purchases.groupBy { purchase ->
        Instant.ofEpochMilli(purchase.createdAt)
            .atZone(zoneId)
            .toLocalDate()
    }
    val markedDays = purchasesByDate.keys
        .filter { date -> YearMonth.from(date) == displayedMonth }
        .mapTo(linkedSetOf()) { it.dayOfMonth }
    val firstDayOfMonth = displayedMonth.atDay(1)
    val leadingEmptyDays = (firstDayOfMonth.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
    val dayCells = buildList<Int?> {
        repeat(leadingEmptyDays) { add(null) }
        repeat(displayedMonth.lengthOfMonth()) { day ->
            add(day + 1)
        }
    }
    val weeks = dayCells.chunked(7)
    val weekdays = listOf(
        stringResource(R.string.weekday_mon),
        stringResource(R.string.weekday_tue),
        stringResource(R.string.weekday_wed),
        stringResource(R.string.weekday_thu),
        stringResource(R.string.weekday_fri),
        stringResource(R.string.weekday_sat),
        stringResource(R.string.weekday_sun),
    )
    val selectedDate = selectedDay?.let { displayedMonth.atDay(it) }
    val selectedDayPurchases = selectedDate?.let { date -> purchasesByDate[date].orEmpty() }.orEmpty()
    val selectedDayTotal = selectedDayPurchases.sumOf { it.amountCents }
    val shouldShowEmptyState = markedDays.isEmpty() && displayedMonth == YearMonth.now()

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(R.string.monthly_calendar),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = onPreviousMonth,
                ) {
                    Text(stringResource(R.string.previous_month))
                }
                Text(
                    text = displayedMonth.format(monthFormatter()),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                TextButton(
                    onClick = onNextMonth,
                ) {
                    Text(stringResource(R.string.next_month))
                }
            }
            Text(
                text = stringResource(R.string.calendar_saved_legend),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    weekdays.forEach { weekday ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = weekday,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                weeks.forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        repeat(7) { index ->
                            val day = week.getOrNull(index)
                            CalendarDayCell(
                                day = day,
                                isMarked = day != null && markedDays.contains(day),
                                isSelected = day != null && day == selectedDay,
                                onClick = { tappedDay ->
                                    selectedDay = tappedDay
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
            if (shouldShowEmptyState) {
                InlineEmptyState(
                    title = stringResource(R.string.calendar_empty_title),
                    body = stringResource(R.string.calendar_empty_body),
                )
            }
            selectedDate?.let { date ->
                SelectedCalendarDaySummary(
                    date = date,
                    totalSaved = moneyFormatter.formatMinorUnits(selectedDayTotal),
                    purchases = selectedDayPurchases,
                )
            }
        }
    }
}

private fun buildMonthlyChartValuesForMonth(
    purchases: List<SkippedPurchase>,
    displayedMonth: YearMonth,
): List<Long> {
    val zoneId = ZoneId.systemDefault()
    val dailyTotals = purchases
        .groupBy { purchase ->
            Instant.ofEpochMilli(purchase.createdAt)
                .atZone(zoneId)
                .toLocalDate()
        }
        .filterKeys { date -> YearMonth.from(date) == displayedMonth }
        .mapValues { (_, monthPurchases) -> monthPurchases.sumOf { it.amountCents } }

    return (1..displayedMonth.lengthOfMonth()).map { day ->
        dailyTotals[displayedMonth.atDay(day)] ?: 0L
    }
}

@Composable
private fun CalendarDayCell(
    day: Int?,
    isMarked: Boolean,
    isSelected: Boolean,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clickable(enabled = day != null) {
                day?.let(onClick)
            },
        contentAlignment = Alignment.Center,
    ) {
        if (day == null) {
            Spacer(modifier = Modifier.size(36.dp))
            return@Box
        }

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else if (isMarked) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isMarked) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else if (isMarked) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}

@Composable
private fun SelectedCalendarDaySummary(
    date: LocalDate,
    totalSaved: String,
    purchases: List<SkippedPurchase>,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.calendar_selected_day_title, date.monthValue, date.dayOfMonth),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.calendar_selected_day_total, totalSaved),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (purchases.isEmpty()) {
                Text(
                    text = stringResource(R.string.calendar_selected_day_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                purchases.sortedByDescending { it.createdAt }.forEach { purchase ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = purchase.label,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            letterSpacing = 0.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = moneyFormatter.formatMinorUnits(purchase.amountCents),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MilestoneMessage(message: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
    ) {
        Text(
            text = message,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
}

@Composable
internal fun InlineEmptyState(
    title: String,
    body: String,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun AddSkippedPurchaseDialog(
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    SkippedPurchaseInputDialog(
        dialogTitle = stringResource(R.string.add_skipped_purchase),
        initialTitle = "",
        initialAmount = "",
        confirmLabel = stringResource(R.string.save),
        onDismiss = onDismiss,
        onSave = onSave,
    )
}

@Composable
private fun EditSkippedPurchaseDialog(
    purchase: SkippedPurchase,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    SkippedPurchaseInputDialog(
        dialogTitle = stringResource(R.string.edit_skipped_purchase),
        initialTitle = purchase.label,
        initialAmount = purchase.amountCents.toString(),
        confirmLabel = stringResource(R.string.save_changes),
        onDismiss = onDismiss,
        onSave = onSave,
    )
}

@Composable
private fun SkippedPurchaseInputDialog(
    dialogTitle: String,
    initialTitle: String,
    initialAmount: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf(initialTitle) }
    var amount by rememberSaveable { mutableStateOf(initialAmount) }
    var attemptedSave by remember { mutableStateOf(false) }

    val trimmedTitle = title.trim()
    val trimmedAmount = amount.trim()
    val isTitleValid = trimmedTitle.isNotEmpty()
    val isAmountValid = parseWholeYenOrNull(trimmedAmount) != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialogTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.purchase_title_label)) },
                    isError = attemptedSave && !isTitleValid,
                    supportingText = {
                        if (attemptedSave && !isTitleValid) {
                            Text(stringResource(R.string.title_required))
                        }
                    },
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { newValue ->
                        val isDigitsOnly = newValue.all(Char::isDigit)
                        val hasLeadingZero = newValue.length > 1 && newValue.startsWith('0')
                        if (isDigitsOnly && !hasLeadingZero) {
                            amount = newValue
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.amount_saved_label)) },
                    isError = attemptedSave && !isAmountValid,
                    visualTransformation = WholeYenGroupingVisualTransformation,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = {
                        if (attemptedSave && !isAmountValid) {
                            Text(stringResource(R.string.amount_invalid))
                        } else {
                            Text(stringResource(R.string.amount_integer_only_hint))
                        }
                    },
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    attemptedSave = true
                    if (isTitleValid && isAmountValid) {
                        onSave(trimmedTitle, trimmedAmount)
                    }
                },
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun DeleteSkippedPurchaseDialog(
    purchase: SkippedPurchase,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_skipped_purchase_title)) },
        text = {
            Text(
                stringResource(
                    R.string.delete_skipped_purchase_message,
                    purchase.label,
                ),
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(stringResource(R.string.delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: @Composable () -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(10.dp),
            ) {
                icon()
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun EmptyRecentPurchases() {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.no_entries_yet),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.empty_state_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RecentPurchaseRow(
    purchase: SkippedPurchase,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiary),
            )
            Spacer(modifier = Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = purchase.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = stringResource(R.string.recent_purchase_meta,
                        formatRecentPurchaseDate(
                            createdAt = purchase.createdAt,
                            todayLabel = stringResource(R.string.today_label),
                            yesterdayLabel = stringResource(R.string.yesterday_label),
                        ),
                        moneyFormatter.formatMinorUnits(purchase.amountCents),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = stringResource(R.string.edit),
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = stringResource(R.string.delete),
                )
            }
        }
    }
}

private fun formatRecentPurchaseDate(
    createdAt: Long,
    todayLabel: String,
    yesterdayLabel: String,
): String {
    val zoneId = ZoneId.systemDefault()
    val purchaseDate = Instant.ofEpochMilli(createdAt).atZone(zoneId).toLocalDate()
    val today = LocalDate.now(zoneId)
    return when (purchaseDate) {
        today -> todayLabel
        today.minusDays(1) -> yesterdayLabel
        else -> purchaseDate.format(recentPurchaseDateFormatter())
    }
}
