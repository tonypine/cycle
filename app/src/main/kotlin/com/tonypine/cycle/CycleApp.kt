package com.tonypine.cycle

import androidx.annotation.StringRes
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.tonypine.cycle.core.data.CycleData
import com.tonypine.cycle.core.designsystem.CycleIcons
import com.tonypine.cycle.core.designsystem.CycleTheme
import com.tonypine.cycle.core.designsystem.NavigationBar
import com.tonypine.cycle.core.designsystem.NavigationDestination
import com.tonypine.cycle.core.designsystem.OpenSourceNotices
import com.tonypine.cycle.core.ui.DeviceLock
import com.tonypine.cycle.feature.calendar.CalendarRoute
import com.tonypine.cycle.feature.calendar.CalendarViewModel
import com.tonypine.cycle.feature.history.CycleDetailRoute
import com.tonypine.cycle.feature.history.CycleDetailViewModel
import com.tonypine.cycle.feature.history.EditPeriodRoute
import com.tonypine.cycle.feature.history.EditPeriodViewModel
import com.tonypine.cycle.feature.history.HistoryRoute
import com.tonypine.cycle.feature.history.HistoryViewModel
import com.tonypine.cycle.feature.onboarding.OnboardingRoute
import com.tonypine.cycle.feature.onboarding.OnboardingViewModel
import com.tonypine.cycle.feature.settings.AddMethodRoute
import com.tonypine.cycle.feature.settings.AddMethodViewModel
import com.tonypine.cycle.feature.settings.ContraceptionRoute
import com.tonypine.cycle.feature.settings.ContraceptionViewModel
import com.tonypine.cycle.feature.settings.OpenSourceNoticeScreen
import com.tonypine.cycle.feature.settings.OpenSourceNoticesScreen
import com.tonypine.cycle.feature.settings.SettingsRoute
import com.tonypine.cycle.feature.settings.SettingsViewModel
import com.tonypine.cycle.feature.settings.StopMethodRoute
import com.tonypine.cycle.feature.settings.StretchRoute
import com.tonypine.cycle.feature.settings.StretchViewModel
import com.tonypine.cycle.feature.settings.UsualLengthsRoute
import com.tonypine.cycle.feature.settings.UsualLengthsViewModel
import com.tonypine.cycle.feature.settings.WhatToLogRoute
import com.tonypine.cycle.feature.settings.WhatToLogViewModel
import com.tonypine.cycle.feature.settings.rememberRestoreFromExport
import com.tonypine.cycle.feature.today.TodayRoute
import com.tonypine.cycle.feature.today.TodayViewModel
import java.time.LocalDate
import java.time.YearMonth

/** The four tabs, in the navigation bar's order. The app starts on [Today]. */
enum class TopLevelDestination(val route: String, @param:StringRes val label: Int, val icon: CycleIcons) {
    Today("today", R.string.tab_today, CycleIcons.Today),
    Calendar("calendar", R.string.tab_calendar, CycleIcons.Calendar),
    History("history", R.string.tab_history, CycleIcons.History),
    Settings("settings", R.string.tab_settings, CycleIcons.Settings)
}

/**
 * The app: the welcome and setup on the first launch, and again after "Delete everything", then one
 * screen per tab above the navigation bar. Nothing shows until the settings are read, so the welcome
 * never flashes on her way to Today. The welcome's "Restore from a Cycle export" imports a file with
 * Settings' import, then opens Today.
 *
 * The app draws edge to edge. Every screen pads its content below the status bar, but a screen
 * scrolls under it, so a `surface` strip covers the status bar on every screen: nothing scrolls
 * under the clock and system icons. Sheets and dialogs open in their own windows, above it.
 *
 * @param deviceLock the phone's lock, which Settings' "Lock Cycle" asks for.
 * @param today her day, from the phone's clock in its current zone.
 */
@Composable
fun CycleApp(
    data: CycleData,
    deviceLock: DeviceLock,
    modifier: Modifier = Modifier,
    today: () -> LocalDate = LocalDate::now
) {
    val onboarding = viewModel {
        OnboardingViewModel(data.settingsRepository, data.dayLogRepository, data.contraceptionRepository, today)
    }
    val showWelcome by onboarding.showWelcome.collectAsStateWithLifecycle()
    Box(modifier.fillMaxSize().background(CycleTheme.colors.surface)) {
        when (showWelcome) {
            null -> Unit

            true -> {
                val restore = viewModel {
                    SettingsViewModel(
                        data.settingsRepository,
                        data.yourDataRepository,
                        deviceLock,
                        data.contraceptionRepository,
                        today
                    )
                }
                OnboardingRoute(
                    onboarding,
                    onRestore = rememberRestoreFromExport(restore, onRestored = onboarding::onRestored)
                )
            }

            // A new controller each time, so she is on Today after the welcome.
            false -> CycleTabs(data, deviceLock, today, rememberNavController())
        }
        Box(
            Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.safeDrawing)
                .background(CycleTheme.colors.surface)
        )
    }
}

/**
 * One screen per tab above the navigation bar. Each tab keeps its state when she leaves it, and back
 * from any tab returns to Today, then leaves the app. Today's "missed a period?" card opens the
 * calendar on the month the period was likely in, and a cycle's "See it in the calendar" on the
 * month it started. Settings opens its pages inside its tab.
 */
@Composable
private fun CycleTabs(
    data: CycleData,
    deviceLock: DeviceLock,
    today: () -> LocalDate,
    navController: NavHostController
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val current = backStackEntry?.destination
    val selected = TopLevelDestination.entries
        .indexOfFirst { destination -> current?.hierarchy?.any { it.route == destination.route } == true }
        .coerceAtLeast(0)
    val motion = CycleTheme.motion
    // The month Today's "Add a past period" or a cycle's "See it in the calendar" asks the calendar to
    // open on, until it has.
    var calendarMonth by rememberSaveable { mutableStateOf<YearMonth?>(null) }
    val context = LocalContext.current
    val versionName = remember(context) {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }

    Column(Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.Today.route,
            modifier = Modifier.weight(1f),
            enterTransition = { fadeIn(motion.defaultEffectsSpec()) },
            exitTransition = { fadeOut(motion.fastEffectsSpec()) }
        ) {
            composable(TopLevelDestination.Today.route) {
                TodayRoute(
                    viewModel {
                        TodayViewModel(data.cycleRepository, data.dayLogRepository, data.settingsRepository, today)
                    },
                    onAddPastPeriod = { month ->
                        calendarMonth = month
                        navController.navigateToTab(TopLevelDestination.Calendar)
                    }
                )
            }
            composable(TopLevelDestination.Calendar.route) {
                CalendarRoute(
                    viewModel { CalendarViewModel(data.cycleRepository, data.dayLogRepository, today) },
                    requestedMonth = calendarMonth,
                    onMonthShown = { calendarMonth = null }
                )
            }
            navigation(startDestination = HISTORY_LIST_ROUTE, route = TopLevelDestination.History.route) {
                composable(HISTORY_LIST_ROUTE) {
                    HistoryRoute(
                        viewModel { HistoryViewModel(data.cycleRepository, today) },
                        onCycleClick = { start -> navController.navigate("$HISTORY_CYCLE_PREFIX$start") },
                        onSeeInCalendar = { month ->
                            calendarMonth = month
                            navController.navigateToTab(TopLevelDestination.Calendar)
                        }
                    )
                }
                composable(HISTORY_CYCLE_ROUTE) { entry ->
                    val start = LocalDate.parse(entry.arguments?.getString(START_ARG))
                    CycleDetailRoute(
                        viewModel { CycleDetailViewModel(data.cycleRepository, data.dayLogRepository, start, today) },
                        onBack = { navController.popBackStack() },
                        onSeeInCalendar = { month ->
                            calendarMonth = month
                            navController.navigateToTab(TopLevelDestination.Calendar)
                        },
                        onEditPeriod = { navController.navigate("$HISTORY_CYCLE_PREFIX$it$HISTORY_EDIT_SUFFIX") }
                    )
                }
                composable(HISTORY_EDIT_ROUTE) { entry ->
                    val start = LocalDate.parse(entry.arguments?.getString(START_ARG))
                    EditPeriodRoute(
                        viewModel { EditPeriodViewModel(data.cycleRepository, data.dayLogRepository, start, today) },
                        onBack = { navController.popBackStack() },
                        // The cycle may start on another day now: its detail replaces the old one.
                        onSaved = { newStart ->
                            navController.navigate("$HISTORY_CYCLE_PREFIX$newStart") {
                                popUpTo(HISTORY_LIST_ROUTE)
                            }
                        }
                    )
                }
            }
            navigation(startDestination = SETTINGS_HOME_ROUTE, route = TopLevelDestination.Settings.route) {
                composable(SETTINGS_HOME_ROUTE) {
                    SettingsRoute(
                        viewModel {
                            SettingsViewModel(
                                data.settingsRepository,
                                data.yourDataRepository,
                                deviceLock,
                                data.contraceptionRepository,
                                today
                            )
                        },
                        versionName = versionName,
                        onUsualLengths = { navController.navigate(USUAL_LENGTHS_ROUTE) },
                        onWhatToLog = { navController.navigate(WHAT_TO_LOG_ROUTE) },
                        onContraception = { navController.navigate(CONTRACEPTION_ROUTE) },
                        onNotices = { navController.navigate(NOTICES_ROUTE) }
                    )
                }
                composable(CONTRACEPTION_ROUTE) {
                    ContraceptionRoute(
                        viewModel { ContraceptionViewModel(data.contraceptionRepository, today) },
                        onBack = { navController.popBackStack() },
                        onAdd = { navController.navigate(ADD_METHOD_ROUTE) },
                        onStop = { id -> navController.navigate("$STOP_METHOD_PREFIX$id") },
                        onOpen = { id -> navController.navigate("$STRETCH_PREFIX$id") }
                    )
                }
                composable(ADD_METHOD_ROUTE) {
                    AddMethodRoute(
                        viewModel { AddMethodViewModel(data.contraceptionRepository, today) },
                        onBack = { navController.popBackStack() },
                        // None while on a method: "Mark as stopped" takes this page's place.
                        onStop = { id ->
                            navController.navigate("$STOP_METHOD_PREFIX$id") {
                                popUpTo(ADD_METHOD_ROUTE) { inclusive = true }
                            }
                        }
                    )
                }
                composable(STOP_METHOD_ROUTE) { entry ->
                    val id = entry.arguments?.getString(ID_ARG)?.toLongOrNull() ?: 0L
                    StopMethodRoute(
                        viewModel { StretchViewModel(data.contraceptionRepository, id, today) },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(STRETCH_ROUTE) { entry ->
                    val id = entry.arguments?.getString(ID_ARG)?.toLongOrNull() ?: 0L
                    StretchRoute(
                        viewModel { StretchViewModel(data.contraceptionRepository, id, today) },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(USUAL_LENGTHS_ROUTE) {
                    UsualLengthsRoute(
                        viewModel { UsualLengthsViewModel(data.settingsRepository) },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(WHAT_TO_LOG_ROUTE) {
                    WhatToLogRoute(
                        viewModel { WhatToLogViewModel(data.settingsRepository) },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(NOTICES_ROUTE) {
                    OpenSourceNoticesScreen(
                        onOpen = { index -> navController.navigate("$NOTICE_PREFIX$index") },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(NOTICE_ROUTE) { entry ->
                    val index = entry.arguments?.getString(INDEX_ARG)?.toIntOrNull()
                    val notice = index?.let(OpenSourceNotices::getOrNull) ?: OpenSourceNotices.first()
                    OpenSourceNoticeScreen(notice, onBack = { navController.popBackStack() })
                }
            }
        }
        NavigationBar(
            destinations = TopLevelDestination.entries.map { NavigationDestination(stringResource(it.label), it.icon) },
            selectedIndex = selected,
            onSelect = { index -> navController.navigateToTab(TopLevelDestination.entries[index]) }
        )
    }
}

// The screens inside the Settings tab.
private const val SETTINGS_HOME_ROUTE = "settings/home"
private const val USUAL_LENGTHS_ROUTE = "settings/usual_lengths"
private const val WHAT_TO_LOG_ROUTE = "settings/what_to_log"
private const val NOTICES_ROUTE = "settings/notices"
private const val CONTRACEPTION_ROUTE = "settings/contraception"
private const val ADD_METHOD_ROUTE = "settings/contraception/add"
private const val INDEX_ARG = "index"
private const val ID_ARG = "id"

/** "Mark as stopped" for a stretch of contraception, by its id. */
private const val STOP_METHOD_PREFIX = "settings/contraception/stop/"
private const val STOP_METHOD_ROUTE = "$STOP_METHOD_PREFIX{$ID_ARG}"

/** A stretch of contraception's page, by its id. */
private const val STRETCH_PREFIX = "settings/contraception/stretch/"
private const val STRETCH_ROUTE = "$STRETCH_PREFIX{$ID_ARG}"

/** One open-source notice, by its place in `OpenSourceNotices`. */
private const val NOTICE_PREFIX = "settings/notice/"
private const val NOTICE_ROUTE = "$NOTICE_PREFIX{$INDEX_ARG}"

/** Opens [destination] with Today under it, so back returns to Today; each tab keeps its state. */
private fun NavHostController.navigateToTab(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private const val HISTORY_LIST_ROUTE = "history/cycles"
private const val START_ARG = "start"

/** A cycle's details, by the day it started, as `2027-08-05`. */
private const val HISTORY_CYCLE_PREFIX = "history/cycle/"
private const val HISTORY_CYCLE_ROUTE = "$HISTORY_CYCLE_PREFIX{$START_ARG}"

/** The editor for the period of the cycle that starts on a day: `history/cycle/2027-08-05/edit`. */
private const val HISTORY_EDIT_SUFFIX = "/edit"
private const val HISTORY_EDIT_ROUTE = "$HISTORY_CYCLE_ROUTE$HISTORY_EDIT_SUFFIX"
