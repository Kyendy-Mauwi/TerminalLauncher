package com.tty.launcher.presentation.components

import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tty.launcher.domain.models.CursorStyle
import com.tty.launcher.presentation.theme.LocalTerminalPalette
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun TerminalDivider(modifier: Modifier = Modifier, color: Color = LocalTerminalPalette.current.border) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color)
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    val palette = LocalTerminalPalette.current
    Text(
        text = text,
        modifier = modifier.padding(top = 18.dp, bottom = 8.dp),
        color = palette.accent,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
    )
}

@Composable
fun TerminalButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    enabled: Boolean = true,
    contentDescription: String? = null
) {
    val palette = LocalTerminalPalette.current
    val description = contentDescription ?: label
    Box(
        modifier = modifier
            .semantics { this.contentDescription = description }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .background(if (active) palette.primary.copy(alpha = 0.12f) else Color.Transparent)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = when {
                !enabled -> palette.secondary.copy(alpha = 0.4f)
                active -> palette.primary
                else -> palette.secondary
            },
            fontSize = 11.sp,
            letterSpacing = 1.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
fun TerminalChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = LocalTerminalPalette.current
    Box(
        modifier = modifier
            .background(
                color = if (selected) palette.primary.copy(alpha = 0.16f) else Color.Transparent,
                shape = RoundedCornerShape(4.dp)
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = if (selected) palette.primary else palette.secondary,
            fontSize = 11.sp,
            letterSpacing = 0.5.sp
        )
    }
}

/**
 * Terminal cursor. Blinks only when animations are enabled and the user has not asked for a
 * reduced-motion experience.
 */
@Composable
fun BlinkingCursor(
    style: CursorStyle,
    animations: Boolean,
    modifier: Modifier = Modifier
) {
    val palette = LocalTerminalPalette.current
    var visible by remember { mutableStateOf(true) }

    LaunchedEffect(animations) {
        if (!animations) {
            visible = true
            return@LaunchedEffect
        }
        while (true) {
            delay(530)
            visible = !visible
        }
    }

    val alpha = if (visible) 1f else 0f
    if (alpha == 0f) return
    when (style) {
        CursorStyle.BLOCK -> Box(
            modifier = modifier
                .padding(start = 2.dp)
                .size(width = 8.dp, height = 15.dp)
                .background(palette.primary)
        )

        CursorStyle.BAR -> Box(
            modifier = modifier
                .padding(start = 2.dp)
                .size(width = 2.dp, height = 16.dp)
                .background(palette.primary)
        )

        CursorStyle.UNDERLINE -> Box(
            modifier = modifier
                .padding(start = 2.dp)
                .size(width = 9.dp, height = 2.dp)
                .background(palette.primary)
        )
    }
}

/** Lightweight matrix rain used by the MATRIX theme; disabled when animations are off. */
@Composable
fun MatrixRain(enabled: Boolean, modifier: Modifier = Modifier) {
    if (!enabled) return
    val palette = LocalTerminalPalette.current
    val columns = remember {
        List(28) { index ->
            Drop(
                x = index / 28f + Random(index).nextFloat() * 0.02f,
                speed = 0.4f + Random(index + 41).nextFloat() * 0.9f,
                length = 0.12f + Random(index + 91).nextFloat() * 0.25f,
                start = Random(index + 7).nextFloat()
            )
        }
    }
    var elapsed by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        val started = SystemClock.elapsedRealtime()
        while (true) {
            elapsed = SystemClock.elapsedRealtime() - started
            delay(90)
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val seconds = elapsed / 1000f
        columns.forEach { drop ->
            val progress = (drop.start + seconds * drop.speed * 0.18f) % 1.2f
            val headY = progress * size.height
            val trailHeight = size.height * drop.length
            var offset = 0f
            while (offset < trailHeight) {
                val alpha = (1f - offset / trailHeight) * 0.10f
                drawCircle(
                    color = palette.primary.copy(alpha = alpha),
                    radius = 1.6f,
                    center = Offset(drop.x * size.width, headY - offset)
                )
                offset += 14f
            }
        }
    }
}

private data class Drop(val x: Float, val speed: Float, val length: Float, val start: Float)

@Composable
fun EmptyState(message: String, hint: String? = null, modifier: Modifier = Modifier) {
    val palette = LocalTerminalPalette.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(text = message, color = palette.warning, fontSize = 12.sp, letterSpacing = 0.5.sp)
        if (hint != null) {
            Text(text = hint, color = palette.secondary, fontSize = 12.sp)
        }
    }
}

@Composable
fun TerminalRow(label: String, value: String, modifier: Modifier = Modifier) {
    val palette = LocalTerminalPalette.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Text(
            text = label.uppercase(),
            color = palette.secondary,
            fontSize = 11.sp,
            letterSpacing = 0.8.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            color = palette.primary,
            fontSize = 11.sp,
            letterSpacing = 0.4.sp,
            textAlign = TextAlign.End
        )
    }
}
