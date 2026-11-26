package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.ExactAlarmEntity
import com.example.ui.screens.AlarmEditorSheet
import com.example.ui.screens.AlarmsScreen
import com.example.ui.screens.ClockScreen
import com.example.ui.screens.EventsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StandByScreen
import com.example.ui.theme.ExactTimeTheme

enum class MainDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    CLOCK("clock", R.string.nav_clock, Icons.Filled.Schedule, Icons.Outlined.Schedule),
    ALARMS("alarms", R.string.nav_alarms, Icons.Filled.Alarm, Icons.Outlined.Alarm),
    EVENTS("events", R.string.nav_events, Icons.Filled.Public, Icons.Outlined.Public),
    SETTINGS("settings", R.string.nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings)
}

@Composable
fun ExactTimeApp(viewModel: ExactTimeViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val alarms by viewModel.alarms.collectAsStateWithLifecycle()
    val events by viewModel.events.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val nowMillis by viewModel.nowMillis.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    var currentDestination by remember { mutableStateOf(MainDestination.CLOCK) }
    var isStandByOpen by remember { mutableStateOf(false) }
    var showAlarmEditor by remember { mutableStateOf(false) }
    var editingAlarm by remember { mutableStateOf<ExactAlarmEntity?>(null) }
    var showAddCountdownDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Request POST_NOTIFICATIONS smoothly when user arms first alarm on Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    fun ensureNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeSnackbar()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.setForegroundActive(true)
                Lifecycle.Event.ON_STOP -> viewModel.setForegroundActive(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Back handler returns from secondary tabs to Clock
    BackHandler(enabled = !isStandByOpen && currentDestination != MainDestination.CLOCK) {
        currentDestination = MainDestination.CLOCK
    }

    ExactTimeTheme(themeMode = settings.themeMode) {
        if (isStandByOpen) {
            StandByScreen(
                nowMillis = nowMillis,
                alarms = alarms,
                settings = settings,
                onExit = { isStandByOpen = false }
            )
            return@ExactTimeTheme
        }

        if (showAlarmEditor) {
            AlarmEditorSheet(
                initialAlarm = editingAlarm,
                use24Hour = settings.use24HourFormat,
                nowMillis = nowMillis,
                onDismiss = {
                    showAlarmEditor = false
                    editingAlarm = null
                },
                onSave = { entity ->
                    ensureNotificationPermissionIfNeeded()
                    viewModel.saveAlarm(entity)
                    showAlarmEditor = false
                    editingAlarm = null
                }
            )
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavRail = maxWidth >= 600.dp

            val contentSlot: @Composable (Modifier) -> Unit = { contentModifier ->
                Crossfade(
                    targetState = currentDestination,
                    animationSpec = tween(200),
                    modifier = contentModifier,
                    label = "tab_crossfade"
                ) { destination ->
                    when (destination) {
                        MainDestination.CLOCK -> ClockScreen(
                            nowMillis = nowMillis,
                            alarms = alarms,
                            settings = settings,
                            onToggleClockStyle = viewModel::setClockStyleHybrid,
                            onOpenStandBy = { isStandByOpen = true },
                            onQuickPreset = { offset, align, label ->
                                ensureNotificationPermissionIfNeeded()
                                viewModel.createQuickPreset(offset, align, label)
                            },
                            onSelectTickCadence = viewModel::setTickCadence,
                            onToggleHapticTick = viewModel::setHapticTickEnabled,
                            onToggleAudioTick = viewModel::setAudioTickEnabled,
                            onNavigateToAlarms = { currentDestination = MainDestination.ALARMS },
                            onAddAlarm = {
                                editingAlarm = null
                                showAlarmEditor = true
                            }
                        )

                        MainDestination.ALARMS -> AlarmsScreen(
                            nowMillis = nowMillis,
                            alarms = alarms,
                            settings = settings,
                            onAddAlarm = {
                                editingAlarm = null
                                showAlarmEditor = true
                            },
                            onEditAlarm = { alarm ->
                                editingAlarm = alarm
                                showAlarmEditor = true
                            },
                            onToggleAlarm = { alarm, enabled ->
                                if (enabled) ensureNotificationPermissionIfNeeded()
                                viewModel.toggleAlarm(alarm, enabled)
                            },
                            onToggleSkipNext = viewModel::toggleSkipNext,
                            onTestFire = { alarm ->
                                ensureNotificationPermissionIfNeeded()
                                viewModel.testFireAlarm(context, alarm)
                            },
                            onDeleteAlarm = viewModel::deleteAlarm
                        )

                        MainDestination.EVENTS -> EventsScreen(
                            nowMillis = nowMillis,
                            events = events,
                            settings = settings,
                            onSelectCity = viewModel::setReferenceCityIndex,
                            onCreateEvent = { title, target, cat, arm ->
                                if (arm) ensureNotificationPermissionIfNeeded()
                                viewModel.createCountdownEvent(title, target, cat, arm)
                            },
                            onDeleteEvent = viewModel::deleteCountdownEvent,
                            showAddDialog = showAddCountdownDialog,
                            onDismissAddDialog = { showAddCountdownDialog = false },
                            onOpenAddDialog = { showAddCountdownDialog = true }
                        )

                        MainDestination.SETTINGS -> SettingsScreen(
                            settings = settings,
                            canScheduleExactAlarms = viewModel::canScheduleExactAlarms,
                            onSet24Hour = viewModel::setUse24HourFormat,
                            onSetShowCentiseconds = viewModel::setShowCentiseconds,
                            onSetClockStyleHybrid = viewModel::setClockStyleHybrid,
                            onSetTickCadence = viewModel::setTickCadence,
                            onSetHapticTick = viewModel::setHapticTickEnabled,
                            onSetAudioTick = viewModel::setAudioTickEnabled,
                            onSetThemeMode = viewModel::setThemeMode
                        )
                    }
                }
            }

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                floatingActionButton = {
                    when (currentDestination) {
                        MainDestination.ALARMS -> {
                            FloatingActionButton(
                                onClick = {
                                    editingAlarm = null
                                    showAlarmEditor = true
                                },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.testTag("fab_add_alarm")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = stringResource(R.string.alarms_add_new)
                                )
                            }
                        }

                        MainDestination.EVENTS -> {
                            FloatingActionButton(
                                onClick = { showAddCountdownDialog = true },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.testTag("fab_add_event")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = stringResource(R.string.events_add_new)
                                )
                            }
                        }

                        else -> Unit
                    }
                },
                bottomBar = {
                    if (!useNavRail) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 4.dp
                        ) {
                            MainDestination.entries.forEach { dest ->
                                val selected = currentDestination == dest
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentDestination = dest },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = stringResource(dest.labelRes)
                                        )
                                    },
                                    label = { Text(stringResource(dest.labelRes)) },
                                    modifier = Modifier.testTag("nav_bar_${dest.route}"),
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                if (useNavRail) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Spacer(Modifier.weight(0.2f))
                            MainDestination.entries.forEach { dest ->
                                val selected = currentDestination == dest
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { currentDestination = dest },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = stringResource(dest.labelRes)
                                        )
                                    },
                                    label = { Text(stringResource(dest.labelRes)) },
                                    modifier = Modifier.testTag("nav_rail_${dest.route}"),
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            }
                            Spacer(Modifier.weight(1f))
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            contentSlot(Modifier.fillMaxSize())
                        }
                    }
                } else {
                    contentSlot(
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}
