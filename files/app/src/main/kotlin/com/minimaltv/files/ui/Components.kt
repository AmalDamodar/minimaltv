package com.minimaltv.files.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.minimaltv.files.R

/** One selectable action at the end of a row: a round icon button, or a pill with a label. */
@Composable
internal fun RowAction(label: String, icon: ImageVector, showLabel: Boolean = false, onClick: () -> Unit) {
    val g = LocalGlass.current
    Row(
        Modifier
            .dpadFocus(CircleShape, focusScale = 1f)
            .clip(CircleShape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .heightIn(min = 44.dp)
            .widthIn(min = 44.dp)
            .padding(horizontal = if (showLabel) 14.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        if (showLabel) Text(label, style = MaterialTheme.typography.labelLarge, color = g.textDim)
        Icon(icon, contentDescription = label, tint = g.textDim)
    }
}

// the TV's built-in stub only says "no app"; it never counts as a handler
private const val STUB_PACKAGE = "com.google.android.tv.frameworkpackagestubs"

/** Hands a file to an app that can show it; says so if there is none. An APK goes to the system installer. */
internal fun openContent(ctx: Context, uri: Uri?, mime: String, name: String) {
    val view = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mime)
        clipData = uri?.let { ClipData.newRawUri("", it) }
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val handlers = ctx.packageManager.queryIntentActivities(view, PackageManager.MATCH_DEFAULT_ONLY)
        .filter { it.activityInfo.packageName != STUB_PACKAGE }
    if (uri == null || handlers.isEmpty()) {
        Toast.makeText(ctx, R.string.files_cannot_open, Toast.LENGTH_LONG).show()
        return
    }
    try {
        if (handlers.size == 1) ctx.startActivity(view)
        else ctx.startActivity(Intent.createChooser(view, name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
        Toast.makeText(ctx, R.string.files_cannot_open, Toast.LENGTH_LONG).show()
    }
}

internal fun sizeText(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var v = bytes.toDouble()
    var i = -1
    do { v /= 1024; i++ } while (v >= 1024 && i < units.size - 1)
    return if (v >= 100 || i == 0) "%.0f %s".format(v, units[i]) else "%.1f %s".format(v, units[i])
}

@Composable
private fun glassFieldColors(): TextFieldColors {
    val g = LocalGlass.current
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = g.text, unfocusedTextColor = g.text,
        focusedBorderColor = g.text, unfocusedBorderColor = g.textDim.copy(alpha = 0.45f),
        focusedLabelColor = g.text, unfocusedLabelColor = g.textDim,
        cursorColor = g.text,
        focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
        focusedSupportingTextColor = g.textDim, unfocusedSupportingTextColor = g.textDim,
    )
}

/** A small dialog with one text box (used to rename). Confirm is disabled while [validate] says no. */
@Composable
internal fun TextEditDialog(
    label: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    validate: (String) -> Boolean = { true },
) {
    var text by remember { mutableStateOf(initial) }
    val valid = text.isNotBlank() && validate(text)
    val requester = remember { FocusRequester() }
    fun confirm() {
        if (!valid) return
        if (text != initial) onConfirm(text)
        onDismiss()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = { Text(label) },
        text = {
            // the dialog is its own window with its own focus manager, so ask for it here
            val focus = LocalFocusManager.current
            OutlinedTextField(
                colors = glassFieldColors(),
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                isError = text.isNotEmpty() && !valid,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { confirm() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(requester)
                    // Down leaves the box for the buttons instead of moving the cursor
                    .onPreviewKeyEvent { e ->
                        if (e.type == KeyEventType.KeyDown && e.key == Key.DirectionDown) {
                            focus.moveFocus(FocusDirection.Down); true
                        } else false
                    },
            )
        },
        confirmButton = {
            TextButton(onClick = ::confirm, enabled = valid, modifier = Modifier.dpadFocus(CircleShape)) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.dpadFocus(CircleShape)) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
    LaunchedEffect(Unit) { requester.requestFocusUntilLanded() }
}
