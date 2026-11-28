package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Ensures the focused input (plus [extraBottomDp] of breathing room below it) scrolls smoothly
 * above the software keyboard once the IME opens or resizes.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.bringIntoViewOnFocus(extraBottomDp: Dp = 28.dp): Modifier = composed {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val extraBottomPx = with(density) { extraBottomDp.toPx() }
    var isFocused by remember { mutableStateOf(false) }
    var fieldSize by remember { mutableStateOf(IntSize.Zero) }
    val imeBottom = WindowInsets.ime.getBottom(density)

    fun targetRect(): Rect? {
        if (fieldSize.width <= 0 || fieldSize.height <= 0) return null
        return Rect(
            left = 0f,
            top = 0f,
            right = fieldSize.width.toFloat(),
            bottom = fieldSize.height.toFloat() + extraBottomPx
        )
    }

    LaunchedEffect(isFocused, imeBottom) {
        if (isFocused && imeBottom > 0) {
            delay(90)
            bringIntoViewRequester.bringIntoView(targetRect())
        }
    }

    this
        .onSizeChanged { fieldSize = it }
        .bringIntoViewRequester(bringIntoViewRequester)
        .onFocusEvent { state ->
            val wasFocused = isFocused
            isFocused = state.isFocused
            if (state.isFocused && !wasFocused) {
                coroutineScope.launch {
                    delay(140)
                    bringIntoViewRequester.bringIntoView(targetRect())
                }
            }
        }
}
