package com.minimaltv.files.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.remember
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

fun Modifier.dpadFocus(shape: Shape = RoundedCornerShape(12.dp), focusScale: Float = 1f): Modifier = composed {
    focusRing(shape = shape, focusScale = focusScale)
}

/**
 * A tile or row that opens on OK and offers its menu on a long press of OK or on the Menu key (the remote has no right
 * click). Taps and long taps work the same for touch. Place it before the visual modifiers that should look focused.
 */
fun Modifier.pressable(onClick: () -> Unit, onMenu: (() -> Unit)?): Modifier = composed {
    val state = remember { booleanArrayOf(false) } // [0]: a long press already opened the menu for this key press
    this
        .onKeyEvent { k ->
            val select = k.key == Key.DirectionCenter || k.key == Key.Enter || k.key == Key.NumPadEnter
            when {
                select && k.type == KeyEventType.KeyDown -> {
                    if (k.nativeKeyEvent.repeatCount == 1 && onMenu != null && !state[0]) { state[0] = true; onMenu() }
                    true
                }
                select && k.type == KeyEventType.KeyUp -> {
                    if (state[0]) state[0] = false else onClick()
                    true
                }
                onMenu != null && k.key == Key.Menu && k.type == KeyEventType.KeyUp -> { onMenu(); true }
                else -> false
            }
        }
        .focusable()
        .pointerInput(onClick, onMenu) { detectTapGestures(onTap = { onClick() }, onLongPress = { onMenu?.invoke() }) }
}
