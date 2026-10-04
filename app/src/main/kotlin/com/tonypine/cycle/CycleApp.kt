package com.tonypine.cycle

import androidx.annotation.StringRes
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tonypine.cycle.core.data.CycleData
import com.tonypine.cycle.core.designsystem.CycleIcons
import com.tonypine.cycle.core.designsystem.CycleTheme
import com.tonypine.cycle.core.designsystem.NavigationBar
import com.tonypine.cycle.core.designsystem.NavigationDestination
import com.tonypine.cycle.feature.onboarding.OnboardingRoute
import com.tonypine.cycle.feature.onboarding.OnboardingViewModel
import com.tonypine.cycle.feature.today.TodayRoute
import com.tonypine.cycle.feature.today.TodayViewModel
import java.time.LocalDate

/** The four tabs, in the navigation bar's order. The app starts on [Today]. */
enum class TopLevelDestination(val route: String, @param:StringRes val label: Int, val icon: CycleIcons) {
    Today("today", R.string.tab_today, CycleIcons.Today),
    Calendar("calendar", R.string.tab_calendar, CycleIcons.Calendar),
    History("history", R.string.tab_history, CycleIcons.History),
    Settings("settings", R.string.tab_settings, CycleIcons.Settings)
}

/**
 * The app: the welcome and setup on the first launch, then one screen per tab above the navigation
 * bar. Nothing shows until the settings are read, so the welcome never flashes on her way to Today.
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
    val onboarding = viewModel { OnboardingViewModel(data.settingsRepository, data.dayLogRepository, today) }
    val showWelcome by onboarding.showWelcome.collectAsStateWithLifecycle()
    Box(modifier.fillMaxSize().background(CycleTheme.colors.surface)) {
        when (showWelcome) {
            null -> Unit
            true -> OnboardingRoute(onboarding)
            false -> CycleTabs(data, today, navController)
        }
    }
}

/** Each tab keeps its state when she leaves it, and back from any tab returns to Today, then leaves the app. */
@Composable
private fun CycleTabs(data: CycleData, today: () -> LocalDate, navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val current = backStackEntry?.destination
    val selected = TopLevelDestination.entries
        .indexOfFirst { destination -> current?.hierarchy?.any { it.route == destination.route } == true }
        .coerceAtLeast(0)
    val motion = CycleTheme.motion

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
                    }
                )
            }
            composable(TopLevelDestination.Calendar.route) { PlaceholderScreen(TopLevelDestination.Calendar) }
            composable(TopLevelDestination.History.route) { PlaceholderScreen(TopLevelDestination.History) }
            composable(TopLevelDestination.Settings.route) { PlaceholderScreen(TopLevelDestination.Settings) }
        }
        NavigationBar(
            destinations = TopLevelDestination.entries.map { NavigationDestination(stringResource(it.label), it.icon) },
            selectedIndex = selected,
            onSelect = { index -> navController.navigateToTab(TopLevelDestination.entries[index]) }
        )
    }
}

/** Opens [destination] with Today under it, so back returns to Today; each tab keeps its state. */
private fun NavHostController.navigateToTab(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
