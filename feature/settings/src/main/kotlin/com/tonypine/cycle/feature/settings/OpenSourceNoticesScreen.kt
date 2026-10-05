package com.tonypine.cycle.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.tonypine.cycle.core.designsystem.AppBarAction
import com.tonypine.cycle.core.designsystem.ClickableCard
import com.tonypine.cycle.core.designsystem.CycleIcon
import com.tonypine.cycle.core.designsystem.CycleIcons
import com.tonypine.cycle.core.designsystem.CycleTheme
import com.tonypine.cycle.core.designsystem.OpenSourceNotice
import com.tonypine.cycle.core.designsystem.OpenSourceNotices
import com.tonypine.cycle.core.designsystem.TopAppBar

/**
 * Open-source notices: one row per asset bundled in the app ([OpenSourceNotices]), with its
 * licence, each opening its full text ([onOpen] gets its index).
 */
@Composable
fun OpenSourceNoticesScreen(
    onOpen: (index: Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    notices: List<OpenSourceNotice> = OpenSourceNotices
) {
    val colors = CycleTheme.colors
    val spacing = CycleTheme.spacing
    val typography = CycleTheme.typography
    Column(modifier.fillMaxSize()) {
        TopAppBar(
            title = stringResource(R.string.notices_title),
            navigation = AppBarAction(CycleIcons.Back, stringResource(R.string.settings_back), onBack)
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = spacing.large)
                .padding(top = spacing.small, bottom = spacing.extraLarge),
            verticalArrangement = Arrangement.spacedBy(spacing.small)
        ) {
            notices.forEachIndexed { index, notice ->
                ClickableCard(onClick = { onOpen(index) }, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing.large),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            BasicText(notice.name, style = typography.titleSmall.copy(color = colors.onSurface))
                            BasicText(
                                notice.license,
                                style = typography.bodySmall.copy(color = colors.onSurfaceVariant)
                            )
                        }
                        CycleIcon(CycleIcons.ChevronEnd, contentDescription = null, tint = colors.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

/** The full licence text and copyright notice of [notice], as it ships in the app. */
@Composable
fun OpenSourceNoticeScreen(notice: OpenSourceNotice, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val resources = LocalContext.current.resources
    // A few kilobytes from the APK: read once, on the first frame.
    val text = remember(notice) { resources.openRawResource(notice.text).bufferedReader().use { it.readText() } }
    val spacing = CycleTheme.spacing
    Column(modifier.fillMaxSize()) {
        TopAppBar(
            title = notice.name,
            navigation = AppBarAction(CycleIcons.Back, stringResource(R.string.settings_back), onBack)
        )
        BasicText(
            text,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = spacing.large)
                .padding(top = spacing.small, bottom = spacing.extraLarge),
            style = CycleTheme.typography.bodySmall.copy(color = CycleTheme.colors.onSurfaceVariant)
        )
    }
}

@Preview
@Composable
private fun OpenSourceNoticesPreview() {
    CycleTheme { OpenSourceNoticesScreen(onOpen = {}, onBack = {}) }
}
