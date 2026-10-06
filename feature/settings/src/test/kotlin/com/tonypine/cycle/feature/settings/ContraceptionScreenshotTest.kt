package com.tonypine.cycle.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.tonypine.cycle.core.model.Breaks
import com.tonypine.cycle.core.model.ContraceptionMethod
import com.tonypine.cycle.core.model.ContraceptionStretch
import com.tonypine.cycle.core.model.StretchMove
import com.tonypine.cycle.core.model.StretchRefusal
import com.tonypine.cycle.core.ui.MethodChoice
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Settings › Contraception in each state, in light, dark and at 200% font scale: on none, on the pill
 * (E1), on none after the implant (D4), on the hormonal IUD after the pill (E4) and on the injection
 * in its 13 weeks; then the steps of adding a method (B3 to B5), "Mark as stopped" (D3), a method's
 * page (E5), its date sheet (E6), and its dialogs: moving the pill's end (E7), a refusal and deleting dates. Each records
 * `src/test/screenshots/contraception_<screen>_<appearance>.png`, on a screen tall enough for the
 * whole page. Synthetic dates only.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w360dp-h800dp-mdpi")
class ContraceptionScreenshotTest(private val screen: Screen, private val appearance: Appearance) {
    @get:Rule
    val composeRule = createComposeRule()

    @OptIn(ExperimentalRoborazziApi::class)
    @Test
    fun capture() {
        if (screen.tall) RuntimeEnvironment.setQualifiers(if (appearance == Appearance.FontScale200) TALLER else TALL)
        composeRule.setContent {
            val density = LocalDensity.current
            val fontScale = if (appearance == Appearance.FontScale200) 2f else 1f
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                Themed(darkTheme = appearance == Appearance.Dark) { screen.content() }
            }
        }
        val path = "src/test/screenshots/contraception_${screen.fileName}_${appearance.fileName}.png"
        when (screen) {
            Screen.DeleteDialog -> {
                composeRule.onNodeWithText("Delete these dates").performClick()
                composeRule.waitForIdle()
                captureScreenRoboImage(path)
            }

            Screen.StartSheet -> {
                composeRule.onNodeWithText("Fitted").performClick()
                composeRule.waitForIdle()
                captureScreenRoboImage(path)
            }

            Screen.MoveDialog, Screen.RefusedDialog -> captureScreenRoboImage(path)

            else -> composeRule.onRoot().captureRoboImage(path)
        }
    }

    enum class Screen(val fileName: String, val tall: Boolean = false, val content: @Composable () -> Unit) {
        None("none", content = { Page(current = null) }),
        Current("current", tall = true, content = { Page(current = Pill, Pill) }),
        Stopped("stopped", content = { Page(current = null, StoppedImplant, today = LocalDate.of(2027, 11, 15)) }),
        PastStretches("past_stretches", tall = true, content = { Page(current = Iud, Iud, PillUntil12) }),
        Injection("injection", tall = true, content = {
            Page(current = OnInjection, OnInjection, today = LocalDate.of(2027, 8, 20))
        }),
        AddMethod("add_method", tall = true, content = {
            Add(AddMethodStep.Method, MethodChoice.Method(ContraceptionMethod.COMBINED_PILL))
        }),
        AddSince("add_since", content = {
            Add(AddMethodStep.Since, MethodChoice.Method(ContraceptionMethod.COMBINED_PILL), started = PillStart)
        }),
        AddBreaks("add_breaks", content = {
            Add(AddMethodStep.Breaks, MethodChoice.Method(ContraceptionMethod.COMBINED_PILL), started = PillStart)
        }),
        Stop("stop", content = {
            StopMethodScreen(StretchUiState.Ready(Implant, ImplantToday, ImplantToday), {}, {}, {}, {})
        }),
        Stretch("stretch", content = { StretchPage(Iud) }),
        PillStretch("pill_stretch", content = { StretchPage(PillUntil12) }),
        StartSheet("start_sheet", content = { StretchPage(Iud) }),
        MoveDialog("move_dialog", content = {
            StretchPage(
                Iud,
                StretchDialog.ConfirmMoves(listOf(StretchMove(PillUntil12, PillUntil12.copy(stopped = PillEnd))))
            )
        }),
        RefusedDialog("refused_dialog", content = {
            StretchPage(Iud, StretchDialog.Refused(StretchRefusal.CoversWhole(PillUntil12)))
        }),
        DeleteDialog("delete_dialog", content = { StretchPage(PillUntil12) })
    }

    enum class Appearance(val fileName: String) {
        Light("light"),
        Dark("dark"),
        FontScale200("font_scale_200")
    }

    companion object {
        private const val TALL = "+h1100dp"
        private const val TALLER = "+h1800dp"

        // Each state of the page in every appearance; the steps and dialogs in light and at 200%.
        private val pageStates = listOf(Screen.None, Screen.Current, Screen.Stopped, Screen.PastStretches)

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}")
        fun cases(): List<Array<Any>> = Screen.entries.flatMap { screen ->
            val appearances = if (screen in pageStates) {
                Appearance.entries
            } else {
                listOf(Appearance.Light, Appearance.FontScale200)
            }
            appearances.map { arrayOf<Any>(screen, it) }
        }
    }
}

private val Today = LocalDate.of(2027, 9, 17)
private val PillStart = LocalDate.of(2027, 5, 3)
private val PillEnd = LocalDate.of(2027, 9, 5)
private val ImplantToday = LocalDate.of(2027, 11, 15)
private val Pill = ContraceptionStretch(ContraceptionMethod.COMBINED_PILL, PillStart, breaks = Breaks.MONTHLY, id = 1)
private val PillUntil12 = Pill.copy(stopped = LocalDate.of(2027, 9, 12))
private val Iud = ContraceptionStretch(ContraceptionMethod.HORMONAL_IUD, LocalDate.of(2027, 9, 13), id = 2)
private val Implant = ContraceptionStretch(ContraceptionMethod.IMPLANT, LocalDate.of(2026, 11, 9), id = 3)
private val StoppedImplant = Implant.copy(stopped = LocalDate.of(2027, 11, 3))
private val OnInjection = ContraceptionStretch(
    ContraceptionMethod.INJECTION,
    LocalDate.of(2026, 11, 9),
    stopped = LocalDate.of(2027, 11, 2),
    id = 4
)

@Composable
private fun Page(current: ContraceptionStretch?, vararg stretches: ContraceptionStretch, today: LocalDate = Today) =
    ContraceptionScreen(
        ContraceptionUiState(today, current, stretches.toList()),
        onBack = {},
        onAdd = {},
        onStop = {},
        onOpen = {}
    )

@Composable
private fun Add(step: AddMethodStep, choice: MethodChoice?, started: LocalDate? = null) = AddMethodScreen(
    AddMethodUiState(
        today = LocalDate.of(2027, 5, 10),
        current = null,
        step = step,
        choice = choice,
        started = started,
        breaks = Breaks.MONTHLY
    ),
    onChoose = {},
    onMethodNext = {},
    onStop = {},
    onDone = {},
    onPickStart = {},
    onSinceNext = {},
    onPickBreaks = {},
    onSave = {},
    onBack = {},
    onMove = {},
    onDismissDialog = {}
)

@Composable
private fun StretchPage(stretch: ContraceptionStretch, dialog: StretchDialog? = null) = StretchScreen(
    StretchUiState.Ready(stretch, Today, Today, dialog),
    onEdit = {},
    onDelete = {},
    onBack = {},
    onMove = {},
    onDismissDialog = {}
)
