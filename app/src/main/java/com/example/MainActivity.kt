package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.AlarmClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clock.data.preferences.AppearanceMode
import com.example.clock.data.preferences.ClockPreferences
import com.example.clock.receiver.AlarmReceiver
import com.example.clock.ui.alarms.AlarmsScreen
import com.example.clock.ui.alarms.AlarmsViewModel
import com.example.clock.ui.bedtime.BedtimeScreen
import com.example.clock.ui.bedtime.BedtimeViewModel
import com.example.clock.ui.components.ClockTab
import com.example.clock.ui.components.FrostedGlassTabBar
import com.example.clock.ui.settings.SettingsScreen
import com.example.clock.ui.stopwatch.StopwatchScreen
import com.example.clock.ui.stopwatch.StopwatchViewModel
import com.example.clock.ui.timers.TimersScreen
import com.example.clock.ui.timers.TimersViewModel
import com.example.clock.ui.worldclock.WorldClockScreen
import com.example.clock.ui.worldclock.WorldClockViewModel
import com.example.ui.theme.ClockTheme

class MainActivity : ComponentActivity() {

    private var initialTab = ClockTab.WORLD_CLOCK
    private var pendingIntent: Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingIntent(intent)

        setContent {
            val context = LocalContext.current
            val prefs = remember { ClockPreferences(context) }
            val appearanceMode by prefs.appearanceMode.collectAsStateWithLifecycle(initialValue = AppearanceMode.SYSTEM)

            ClockTheme(appearanceMode = appearanceMode) {
                ClockAppContent(
                    initialTab = initialTab,
                    prefs = prefs,
                    pendingIntent = pendingIntent,
                    onIntentHandled = { pendingIntent = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val action = intent?.action ?: return
        when (action) {
            AlarmClock.ACTION_SHOW_ALARMS -> initialTab = ClockTab.ALARMS
            AlarmClock.ACTION_SHOW_TIMERS -> initialTab = ClockTab.TIMERS
            AlarmClock.ACTION_SET_ALARM -> {
                initialTab = ClockTab.ALARMS
                pendingIntent = intent
            }
            AlarmClock.ACTION_SET_TIMER -> {
                initialTab = ClockTab.TIMERS
                pendingIntent = intent
            }
            AlarmClock.ACTION_DISMISS_ALARM -> {
                val dismissIntent = Intent(this, AlarmReceiver::class.java).apply {
                    this.action = AlarmReceiver.ACTION_DISMISS_ALARM
                }
                sendBroadcast(dismissIntent)
            }
            AlarmClock.ACTION_SNOOZE_ALARM -> {
                val snoozeIntent = Intent(this, AlarmReceiver::class.java).apply {
                    this.action = AlarmReceiver.ACTION_SNOOZE_ALARM
                }
                sendBroadcast(snoozeIntent)
            }
            AlarmClock.ACTION_DISMISS_TIMER -> {
                val timerDismiss = Intent(this, AlarmReceiver::class.java).apply {
                    this.action = AlarmReceiver.ACTION_DISMISS_TIMER
                }
                sendBroadcast(timerDismiss)
            }
            else -> {
                val tabExtra = intent.getStringExtra("open_tab")
                if (tabExtra != null) {
                    when (tabExtra.lowercase()) {
                        "alarms" -> initialTab = ClockTab.ALARMS
                        "bedtime" -> initialTab = ClockTab.BEDTIME
                        "stopwatch" -> initialTab = ClockTab.STOPWATCH
                        "timers" -> initialTab = ClockTab.TIMERS
                        else -> initialTab = ClockTab.WORLD_CLOCK
                    }
                }
            }
        }
    }
}

@Composable
fun ClockAppContent(
    initialTab: ClockTab,
    prefs: ClockPreferences,
    pendingIntent: Intent?,
    onIntentHandled: () -> Unit
) {
    val context = LocalContext.current

    // Request notification permission on Android 13+
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    var selectedTab by remember { mutableStateOf(initialTab) }
    var showSettings by remember { mutableStateOf(false) }

    val worldClockVm = remember { WorldClockViewModel(context) }
    val alarmsVm = remember { AlarmsViewModel(context) }
    val bedtimeVm = remember { BedtimeViewModel(context) }
    val stopwatchVm = remember { StopwatchViewModel(context) }
    val timersVm = remember { TimersViewModel(context) }

    // Handle any incoming standard set alarm or set timer intent
    LaunchedEffect(pendingIntent) {
        if (pendingIntent != null) {
            when (pendingIntent.action) {
                AlarmClock.ACTION_SET_ALARM -> {
                    selectedTab = ClockTab.ALARMS
                    val hour = pendingIntent.getIntExtra(AlarmClock.EXTRA_HOUR, 7)
                    val minute = pendingIntent.getIntExtra(AlarmClock.EXTRA_MINUTES, 0)
                    val label = pendingIntent.getStringExtra(AlarmClock.EXTRA_MESSAGE) ?: "Alarm"
                    val skipUi = pendingIntent.getBooleanExtra(AlarmClock.EXTRA_SKIP_UI, false)
                    if (skipUi) {
                        alarmsVm.saveAlarm(
                            com.example.clock.data.model.Alarm(
                                hour = hour,
                                minute = minute,
                                label = label,
                                isEnabled = true
                            )
                        )
                    } else {
                        alarmsVm.openEditAlarm(
                            com.example.clock.data.model.Alarm(
                                hour = hour,
                                minute = minute,
                                label = label,
                                isEnabled = true
                            )
                        )
                    }
                    onIntentHandled()
                }
                AlarmClock.ACTION_SET_TIMER -> {
                    selectedTab = ClockTab.TIMERS
                    val lengthSec = pendingIntent.getIntExtra(AlarmClock.EXTRA_LENGTH, 300)
                    val label = pendingIntent.getStringExtra(AlarmClock.EXTRA_MESSAGE) ?: "Timer"
                    val h = lengthSec / 3600
                    val m = (lengthSec % 3600) / 60
                    val s = lengthSec % 60
                    timersVm.startNewTimer(h, m, s, label, "procedural:cosmic_pulse", "Cosmic Pulse")
                    onIntentHandled()
                }
            }
        }
    }

    // Predictive back / back handler
    BackHandler(enabled = showSettings || selectedTab != ClockTab.WORLD_CLOCK) {
        if (showSettings) {
            showSettings = false
        } else {
            selectedTab = ClockTab.WORLD_CLOCK
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (!showSettings) {
                FrostedGlassTabBar(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    modifier = Modifier.navigationBarsPadding()
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            // Adaptive width container (caps at 600dp on tablets/foldables)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp)
            ) {
                if (showSettings) {
                    SettingsScreen(
                        prefs = prefs,
                        onClose = { showSettings = false }
                    )
                } else {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "tab_content_animation"
                    ) { tab ->
                        when (tab) {
                            ClockTab.WORLD_CLOCK -> WorldClockScreen(
                                viewModel = worldClockVm,
                                onOpenSettings = { showSettings = true }
                            )
                            ClockTab.ALARMS -> AlarmsScreen(
                                viewModel = alarmsVm,
                                onOpenSettings = { showSettings = true }
                            )
                            ClockTab.BEDTIME -> BedtimeScreen(
                                viewModel = bedtimeVm,
                                onOpenSettings = { showSettings = true }
                            )
                            ClockTab.STOPWATCH -> StopwatchScreen(
                                viewModel = stopwatchVm
                            )
                            ClockTab.TIMERS -> TimersScreen(
                                viewModel = timersVm,
                                onOpenSettings = { showSettings = true }
                            )
                        }
                    }
                }
            }
        }
    }
}
