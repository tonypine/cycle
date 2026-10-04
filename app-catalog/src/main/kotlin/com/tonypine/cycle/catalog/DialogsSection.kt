package com.tonypine.cycle.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.tonypine.cycle.core.designsystem.CycleAlertDialog
import com.tonypine.cycle.core.designsystem.CycleDestructiveDialog
import com.tonypine.cycle.core.designsystem.CycleDialog
import com.tonypine.cycle.core.designsystem.CycleIcons
import com.tonypine.cycle.core.designsystem.CycleTextField
import com.tonypine.cycle.core.designsystem.CycleTheme
import com.tonypine.cycle.core.designsystem.FilterChip
import com.tonypine.cycle.core.designsystem.TonalButton

private enum class SampleDialog { Alert, Destructive, Note }

@Composable
internal fun DialogsSection() {
    var open by rememberSaveable { mutableStateOf<SampleDialog?>(null) }
    var dismissible by rememberSaveable { mutableStateOf(true) }
    var outcome by rememberSaveable { mutableStateOf("Nothing chosen yet.") }
    val close = { choice: String ->
        outcome = choice
        open = null
    }

    SubsectionTitle("Try it")
    CatalogText(
        "Open a dialog. Back and a tap on the scrim close it, unless you turn that off below. Each " +
            "dialog fades and scales in, and snaps under reduce motion.",
        CycleTheme.typography.bodySmall,
        color = CycleTheme.colors.onSurfaceVariant
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small),
        verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small)
    ) {
        TonalButton("Alert", { open = SampleDialog.Alert })
        TonalButton("Destructive", { open = SampleDialog.Destructive })
        TonalButton("Text field", { open = SampleDialog.Note })
    }
    FilterChip("Close on back and scrim tap", selected = dismissible, onClick = { dismissible = !dismissible })
    CatalogText(outcome, CycleTheme.typography.body)

    val dismissed = { close("Dismissed.") }
    CycleAlertDialog(
        visible = open == SampleDialog.Alert,
        onDismissRequest = dismissed,
        title = "Turn on reminders?",
        text = "Cycle can nudge you a day before your period is due. Reminders stay on this phone.",
        confirmText = "Remind me",
        onConfirm = { close("Remind me.") },
        dismissText = "Not now",
        onDismiss = { close("Not now.") },
        icon = CycleIcons.Today,
        dismissOnBackPress = dismissible,
        dismissOnScrimTap = dismissible
    )
    CycleDestructiveDialog(
        visible = open == SampleDialog.Destructive,
        onDismissRequest = dismissed,
        title = "Delete this day?",
        text = "This removes the period and notes logged for 14 March. You can't undo it.",
        confirmText = "Delete",
        onConfirm = { close("Deleted (nothing was, it's a sample).") },
        dismissText = "Keep it",
        onDismiss = { close("Kept it.") },
        dismissOnBackPress = dismissible,
        dismissOnScrimTap = dismissible
    )
    val note = rememberTextFieldState()
    CycleDialog(
        visible = open == SampleDialog.Note,
        onDismissRequest = dismissed,
        title = "Add a note",
        confirmText = "Save",
        onConfirm = {
            close("Saved: ${note.text}")
            note.clearText()
        },
        text = "A few words about the day. The keyboard opens, and the field and actions stay above it.",
        dismissText = "Cancel",
        onDismiss = { close("Cancelled.") },
        confirmEnabled = note.text.isNotBlank(),
        dismissOnBackPress = dismissible,
        dismissOnScrimTap = dismissible
    ) {
        CycleTextField(note, label = "Note", supportingText = "Only on this phone.", maxLength = 200)
    }
}
