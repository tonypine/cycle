package com.tonypine.cycle

import androidx.annotation.StringRes
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tonypine.cycle.core.data.CycleData
import com.tonypine.cycle.core.designsystem.CycleIcons
import com.tonypine.cycle.core.designsystem.CycleTheme
import com.tonypine.cycle.core.designsystem.NavigationBar
import com.tonypine.cycle.core.designsystem.NavigationDestination
import com.tonypine.cycle.feature.history.CycleDetailRoute
import com.tonypine.cycle.feature.history.CycleDetailViewModel
import com.tonypine.cycle.feature.history.HistoryRoute
import com.tonypine.cycle.feature.history.HistoryViewModel
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
 * The app: one screen per tab above the navigation bar. Each tab keeps its state when she leaves it,
 * and back from any tab returns to Today, then leaves the app.
 *
 * @param today her day, from the phone's clock in its current zone.
 */
@Composable
fun CycleApp(
    data: CycleData,
    modifier: Modifier = Modifier,
    today: () -> LocalDate = LocalDate::now,
    navController: NavHostController = rememberNavController()
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val current = backStackEntry?.destination
    val selected = TopLevelDestination.entries
        .indexOfFirst { destination -> current?.hierarchy?.any { it.isTab(destination) } == true }
        .coerceAtLeast(0)
    val motion = CycleTheme.motion

    Column(modifier.fillMaxSize().background(CycleTheme.colors.surface)) {
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
                    }
                )
            }
            composable(
                CALENDAR_ROUTE,
                arguments = listOf(
                    navArgument(MONTH_ARG) {
                        type = NavType.StringType
                        nullable = true
                    }
                )
            ) { PlaceholderScreen(TopLevelDestination.Calendar) }
            navigation(startDestination = HISTORY_LIST_ROUTE, route = TopLevelDestination.History.route) {
                composable(HISTORY_LIST_ROUTE) {
                    HistoryRoute(
                        viewModel { HistoryViewModel(data.cycleRepository, today) },
                        onCycleClick = { start -> navController.navigate("$HISTORY_CYCLE_PREFIX$start") }
                    )
                }
                composable(HISTORY_CYCLE_ROUTE) { entry ->
                    val start = LocalDate.parse(entry.arguments?.getString(START_ARG))
                    CycleDetailRoute(
                        viewModel { CycleDetailViewModel(data.cycleRepository, start, today) },
                        onBack = { navController.popBackStack() },
                        onSeeInCalendar = { month -> navController.navigateToCalendar(month) }
                    )
                }
            }
            composable(TopLevelDestination.Settings.route) { PlaceholderScreen(TopLevelDestination.Settings) }
        }
        NavigationBar(
            destinations = TopLevelDestination.entries.map { NavigationDestination(stringResource(it.label), it.icon) },
            selectedIndex = selected,
            onSelect = { index -> navController.navigateToTab(TopLevelDestination.entries[index]) }
        )
    }
}

/** This is [destination]'s screen, or its graph: a route's arguments, after `?`, do not count. */
private fun NavDestination.isTab(destination: TopLevelDestination): Boolean =
    route?.substringBefore('?') == destination.route

/** Opens [destination] with Today under it, so back returns to Today; each tab keeps its state. */
private fun NavHostController.navigateToTab(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Opens the Calendar tab on [month], with Today under it like any tab. History keeps its state, so
 * her History tab still shows the cycle she came from; the calendar opens fresh on that month.
 */
private fun NavHostController.navigateToCalendar(month: YearMonth) {
    navigate("${TopLevelDestination.Calendar.route}?$MONTH_ARG=$month") {
        popUpTo(graph.findStartDestination().id) { saveState = true }
    }
}

/** The month the Calendar tab opens on, as `2027-08`; none opens it on the current month. */
internal const val MONTH_ARG = "month"
private const val CALENDAR_ROUTE = "calendar?$MONTH_ARG={$MONTH_ARG}"

private const val HISTORY_LIST_ROUTE = "history/cycles"
private const val START_ARG = "start"

/** A cycle's details, by the day it started, as `2027-08-05`. */
private const val HISTORY_CYCLE_PREFIX = "history/cycle/"
private const val HISTORY_CYCLE_ROUTE = "$HISTORY_CYCLE_PREFIX{$START_ARG}"
