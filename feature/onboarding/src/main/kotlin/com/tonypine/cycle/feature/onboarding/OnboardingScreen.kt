package com.tonypine.cycle.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.tonypine.cycle.core.designsystem.AppBarAction
import com.tonypine.cycle.core.designsystem.CycleDayState
import com.tonypine.cycle.core.designsystem.CycleIcons
import com.tonypine.cycle.core.designsystem.CycleTheme
import com.tonypine.cycle.core.designsystem.EmptyState
import com.tonypine.cycle.core.designsystem.EmptyStateAction
import com.tonypine.cycle.core.designsystem.EmptyStateIcon
import com.tonypine.cycle.core.designsystem.FilledButton
import com.tonypine.cycle.core.designsystem.MonthCalendar
import com.tonypine.cycle.core.designsystem.TextButton
import com.tonypine.cycle.core.designsystem.TopAppBar
import com.tonypine.cycle.core.domain.CycleRules
import com.tonypine.cycle.core.model.Breaks
import com.tonypine.cycle.core.model.ContraceptionMethod
import com.tonypine.cycle.core.model.ContraceptionStretch
import com.tonypine.cycle.core.ui.BreaksList
import com.tonypine.cycle.core.ui.MethodChoice
import com.tonypine.cycle.core.ui.MethodList
import com.tonypine.cycle.core.ui.R as CoreUiR
import com.tonypine.cycle.core.ui.UsualLengthSliders
import com.tonypine.cycle.core.ui.breaksQuestion
import com.tonypine.cycle.core.ui.sinceWhenTitle
import java.time.LocalDate
import java.time.YearMonth

/**
 * The welcome and the pages of the three setup steps, in order. Back goes one page up. Step 3 is
 * contraception: the method, then since when, then the breaks on a combined method.
 */
enum class OnboardingStep { Welcome, LastPeriod, UsualLengths, Contraception, SinceWhen, Breaks }

/**
 * The welcome and setup, wired to its [viewModel]. [onRestore] opens the file picker for "Restore
 * from a Cycle export"; the app calls [OnboardingViewModel.onRestored] once the days are in.
 */
@Composable
fun OnboardingRoute(viewModel: OnboardingViewModel, onRestore: () -> Unit, modifier: Modifier = Modifier) {
    val today = remember(viewModel) { viewModel.today() }
    OnboardingScreen(
        today = today,
        onRestore = onRestore,
        onSkip = viewModel::onSkip,
        onDone = viewModel::onDone,
        modifier = modifier
    )
}

/**
 * The welcome, then "When did your last period start?", "How long do they usually last?" and the
 * optional "Are you using contraception?" (journey A in `docs/design/contraception.md`). What she
 * picks and sets is kept through back and forth between the steps and a configuration change, and
 * handed over only on Done or step 3's Skip: [onDone] gets the day she picked, or null for "I don't
 * remember", her two lengths, which the sliders keep within the lengths she can give, and her
 * method from its start day, or with no start for "I don't remember", or null for None or Skip.
 * [onRestore] is the welcome's "Restore from a Cycle export".
 */
@Composable
fun OnboardingScreen(
    today: LocalDate,
    onRestore: () -> Unit,
    onSkip: () -> Unit,
    onDone: (
        lastPeriodStart: LocalDate?,
        cycleLength: Int,
        periodLength: Int,
        contraception: ContraceptionStretch?
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    var step by rememberSaveable { mutableStateOf(OnboardingStep.Welcome) }
    var month by rememberSaveable { mutableStateOf(YearMonth.from(today)) }
    var start by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    var cycleLength by rememberSaveable { mutableIntStateOf(CycleRules.DEFAULT_CYCLE_LENGTH) }
    var periodLength by rememberSaveable { mutableIntStateOf(CycleRules.DEFAULT_PERIOD_LENGTH) }
    var choice by rememberSaveable(stateSaver = MethodChoiceSaver) { mutableStateOf<MethodChoice?>(null) }
    var sinceMonth by rememberSaveable { mutableStateOf(YearMonth.from(today)) }
    var started by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    var breaks by rememberSaveable { mutableStateOf(Breaks.MONTHLY) }
    val method = (choice as? MethodChoice.Method)?.method

    fun finish(contraception: ContraceptionStretch?) = onDone(start, cycleLength, periodLength, contraception)

    // After since when: the breaks on a combined method, else Done with the method from [from].
    fun afterSinceWhen(method: ContraceptionMethod, from: LocalDate?) {
        if (method.isCombined) {
            step = OnboardingStep.Breaks
        } else {
            finish(ContraceptionStretch(method, from))
        }
    }

    BackHandler(enabled = step != OnboardingStep.Welcome) {
        step = OnboardingStep.entries[step.ordinal - 1]
    }
    val motion = CycleTheme.motion
    AnimatedContent(
        targetState = step,
        modifier = modifier.fillMaxSize(),
        transitionSpec = { fadeIn(motion.defaultEffectsSpec()) togetherWith fadeOut(motion.fastEffectsSpec()) },
        label = "onboarding step"
    ) { shown ->
        when (shown) {
            OnboardingStep.Welcome -> WelcomeStep(
                onGetStarted = { step = OnboardingStep.LastPeriod },
                onRestore = onRestore,
                onSkip = onSkip
            )

            OnboardingStep.LastPeriod -> LastPeriodStep(
                today = today,
                month = month,
                picked = start,
                onPick = { start = it },
                onMonthChange = { month = it },
                onNext = { step = OnboardingStep.UsualLengths },
                onDontRemember = {
                    start = null
                    step = OnboardingStep.UsualLengths
                },
                onBack = { step = OnboardingStep.Welcome }
            )

            OnboardingStep.UsualLengths -> UsualLengthsStep(
                cycleLength = cycleLength,
                onCycleLengthChange = { cycleLength = it },
                periodLength = periodLength,
                onPeriodLengthChange = { periodLength = it },
                onNext = { step = OnboardingStep.Contraception },
                onBack = { step = OnboardingStep.LastPeriod }
            )

            OnboardingStep.Contraception -> ContraceptionStep(
                choice = choice,
                onChoose = { choice = it },
                onNext = { if (method == null) finish(null) else step = OnboardingStep.SinceWhen },
                onSkip = { finish(null) },
                onBack = { step = OnboardingStep.UsualLengths }
            )

            OnboardingStep.SinceWhen -> if (method != null) {
                SinceWhenStep(
                    method = method,
                    today = today,
                    month = sinceMonth,
                    picked = started,
                    onPick = { started = it },
                    onMonthChange = { sinceMonth = it },
                    onNext = { afterSinceWhen(method, started) },
                    onDontRemember = {
                        started = null
                        afterSinceWhen(method, from = null)
                    },
                    onBack = { step = OnboardingStep.Contraception }
                )
            }

            OnboardingStep.Breaks -> if (method != null) {
                BreaksStep(
                    method = method,
                    breaks = breaks,
                    onPick = { breaks = it },
                    onDone = { finish(ContraceptionStretch(method, started, breaks = breaks)) },
                    onBack = { step = OnboardingStep.SinceWhen }
                )
            }
        }
    }
}

/** "Hi! Let's get your cycle going", with Get started, Restore from a Cycle export and Skip for now. */
@Composable
internal fun WelcomeStep(
    onGetStarted: () -> Unit,
    onRestore: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        title = stringResource(R.string.welcome_title),
        body = stringResource(R.string.welcome_body),
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        illustration = { EmptyStateIcon(CycleIcons.WaterDrop) },
        action = EmptyStateAction(stringResource(R.string.welcome_get_started), onGetStarted),
        secondaryActions = listOf(
            EmptyStateAction(stringResource(R.string.welcome_restore), onRestore),
            EmptyStateAction(stringResource(R.string.welcome_skip), onSkip)
        )
    )
}

/** Step 1: the first day of her last period, up to today. Next waits for a day. */
@Composable
internal fun LastPeriodStep(
    today: LocalDate,
    month: YearMonth,
    picked: LocalDate?,
    onPick: (LocalDate) -> Unit,
    onMonthChange: (YearMonth) -> Unit,
    onNext: () -> Unit,
    onDontRemember: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    SetupStep(
        number = 1,
        title = stringResource(R.string.setup_last_period_title),
        body = stringResource(R.string.setup_last_period_body),
        onBack = onBack,
        modifier = modifier
    ) {
        MonthCalendar(
            month = month,
            stateOf = { CycleDayState.Plain },
            onDayClick = onPick,
            today = today,
            onPreviousMonth = { onMonthChange(month.minusMonths(1)) },
            onNextMonth = { onMonthChange(month.plusMonths(1)) },
            modifier = Modifier.padding(horizontal = CycleTheme.spacing.medium),
            selected = picked,
            isEnabled = { !it.isAfter(today) }
        )
        Actions {
            FilledButton(
                text = stringResource(R.string.setup_next),
                onClick = onNext,
                modifier = Modifier.fillMaxWidth(),
                enabled = picked != null
            )
            TextButton(
                text = stringResource(R.string.setup_dont_remember),
                onClick = onDontRemember,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** Step 2: her usual cycle and period lengths, on a slider each, starting at 28 and 5 days. */
@Composable
internal fun UsualLengthsStep(
    cycleLength: Int,
    onCycleLengthChange: (Int) -> Unit,
    periodLength: Int,
    onPeriodLengthChange: (Int) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    SetupStep(
        number = 2,
        title = stringResource(R.string.setup_lengths_title),
        body = stringResource(R.string.setup_lengths_body),
        onBack = onBack,
        modifier = modifier
    ) {
        UsualLengthSliders(cycleLength, onCycleLengthChange, periodLength, onPeriodLengthChange)
        Actions {
            FilledButton(
                text = stringResource(R.string.setup_next),
                onClick = onNext,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Step 3, optional: her method, from the same list as Settings, nothing chosen at first. Next waits
 * for a choice and reads Done on None; Skip leaves setup with no method.
 */
@Composable
internal fun ContraceptionStep(
    choice: MethodChoice?,
    onChoose: (MethodChoice) -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    SetupStep(
        number = 3,
        title = stringResource(R.string.setup_contraception_title),
        body = stringResource(R.string.setup_contraception_body),
        onBack = onBack,
        modifier = modifier
    ) {
        MethodList(choice, onChoose, Modifier.padding(horizontal = CycleTheme.spacing.large))
        Actions {
            FilledButton(
                text = stringResource(if (choice == MethodChoice.None) R.string.setup_done else R.string.setup_next),
                onClick = onNext,
                modifier = Modifier.fillMaxWidth(),
                enabled = choice != null
            )
            TextButton(
                text = stringResource(R.string.setup_skip),
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Step 3, since when: "When was your implant fitted?", a day up to today. Done waits for a day, and
 * reads Next on a combined [method], whose breaks follow. "I don't remember" goes on with no start.
 */
@Composable
internal fun SinceWhenStep(
    method: ContraceptionMethod,
    today: LocalDate,
    month: YearMonth,
    picked: LocalDate?,
    onPick: (LocalDate) -> Unit,
    onMonthChange: (YearMonth) -> Unit,
    onNext: () -> Unit,
    onDontRemember: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    SetupStep(
        number = 3,
        title = sinceWhenTitle(method),
        body = stringResource(CoreUiR.string.method_since_body),
        onBack = onBack,
        modifier = modifier
    ) {
        MonthCalendar(
            month = month,
            stateOf = { CycleDayState.Plain },
            onDayClick = onPick,
            today = today,
            onPreviousMonth = { onMonthChange(month.minusMonths(1)) },
            onNextMonth = { onMonthChange(month.plusMonths(1)) },
            modifier = Modifier.padding(horizontal = CycleTheme.spacing.medium),
            selected = picked,
            isEnabled = { !it.isAfter(today) }
        )
        Actions {
            FilledButton(
                text = stringResource(if (method.isCombined) R.string.setup_next else R.string.setup_done),
                onClick = onNext,
                modifier = Modifier.fillMaxWidth(),
                enabled = picked != null
            )
            TextButton(
                text = stringResource(R.string.setup_dont_remember),
                onClick = onDontRemember,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** Step 3 on a combined pill, patch or ring: "Do you take a break between packs?", then Done. */
@Composable
internal fun BreaksStep(
    method: ContraceptionMethod,
    breaks: Breaks,
    onPick: (Breaks) -> Unit,
    onDone: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    SetupStep(
        number = 3,
        title = breaksQuestion(method),
        body = null,
        onBack = onBack,
        modifier = modifier
    ) {
        BreaksList(method, breaks, onPick, Modifier.padding(horizontal = CycleTheme.spacing.large))
        Actions {
            FilledButton(
                text = stringResource(R.string.setup_done),
                onClick = onDone,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * A setup step: a bar with Back and "Step n of 3", the question as a heading, an optional line under
 * it, then [content]. Edge to edge, it scrolls, as at 200% font scale or on a phone on its side, and
 * pads itself above the system bars.
 */
@Composable
private fun SetupStep(
    number: Int,
    title: String,
    body: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val spacing = CycleTheme.spacing
    val typography = CycleTheme.typography
    val colors = CycleTheme.colors
    Column(modifier.fillMaxSize()) {
        TopAppBar(
            title = stringResource(R.string.setup_step, number, SETUP_STEPS),
            navigation = AppBarAction(CycleIcons.Back, stringResource(R.string.setup_back), onBack)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                )
                .verticalScroll(rememberScrollState())
                .padding(vertical = spacing.large),
            verticalArrangement = Arrangement.spacedBy(spacing.large)
        ) {
            val margin = Modifier.padding(horizontal = spacing.large)
            Column(margin, verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                BasicText(
                    title,
                    modifier = Modifier.semantics { heading() },
                    style = typography.headline.copy(color = colors.onSurface)
                )
                if (body != null) BasicText(body, style = typography.body.copy(color = colors.onSurfaceVariant))
            }
            content()
        }
    }
}

/** A step's buttons, full width one above the other, under its content. */
@Composable
private fun Actions(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = CycleTheme.spacing.large),
        verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small),
        content = content
    )
}

private const val SETUP_STEPS = 3

private const val NO_METHOD = "none"

// Keeps her choice in the method list through a configuration change: None, or the method's name.
private val MethodChoiceSaver = Saver<MethodChoice?, String>(
    save = { choice ->
        when (choice) {
            null -> null
            MethodChoice.None -> NO_METHOD
            is MethodChoice.Method -> choice.method.name
        }
    },
    restore = { saved ->
        if (saved ==
            NO_METHOD
        ) {
            MethodChoice.None
        } else {
            MethodChoice.Method(ContraceptionMethod.valueOf(saved))
        }
    }
)
