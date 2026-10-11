/*
 * Copyright (C) 2026 VoltageOS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.appdatabackup.ui.backup.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// Wavy progress indicators drawn with stable Canvas APIs. The Material 3 Expressive
// LinearWavyProgressIndicator / CircularWavyProgressIndicator are still experimental and may be
// missing from the Compose Material 3 version in this tree, so these keep the same look
// (traveling wave, round caps, faint track, stop dot) without depending on them. If the tree
// later ships stable versions, these two functions are the only call sites to swap.

private val TWO_PI = (2.0 * PI).toFloat()
private val DEG_TO_RAD = (PI / 180.0).toFloat()

/**
 * Determinate linear progress whose filled part is a wave that travels along the bar.
 *
 * [progress] is a lambda so a changing value only redraws, it does not recompose. The wave is
 * never shorter than three quarters of one wavelength, so the bar still reads as "working"
 * while the first item has not finished and the real fraction is still 0.
 */
@Composable
fun WavyLinearProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = color.copy(alpha = 0.24f),
    strokeWidth: Dp = 5.dp,
    amplitude: Dp = 3.dp,
    wavelength: Dp = 32.dp,
) {
    val transition = rememberInfiniteTransition(label = "wavyLinear")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 1400, easing = LinearEasing)),
        label = "wavyLinearPhase",
    )

    Canvas(
        modifier = modifier
            .height(strokeWidth + amplitude * 2)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(progress().coerceIn(0f, 1f), 0f..1f)
            },
    ) {
        val stroke = strokeWidth.toPx()
        val amp = amplitude.toPx()
        val waveLength = wavelength.toPx()
        val startX = stroke / 2f
        val endX = size.width - stroke / 2f
        val centerY = size.height / 2f
        val available = endX - startX
        if (available <= 0f) return@Canvas

        val fraction = progress().coerceIn(0f, 1f)
        val activeLength = maxOf(fraction * available, minOf(waveLength * 0.75f, available))

        // Flat track, only for the part the wave has not reached yet. A small gap (the round
        // caps plus 4 dp) keeps the wave and the track from touching, so no flat line is ever
        // drawn underneath the wave.
        val trackStartX = startX + activeLength + stroke + 4.dp.toPx()
        val hasTrack = trackStartX < endX
        if (hasTrack) {
            drawLine(
                color = trackColor,
                start = Offset(trackStartX, centerY),
                end = Offset(endX, centerY),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }

        // Wavy active part.
        val shift = phase * TWO_PI
        val step = maxOf(1f, 1.5.dp.toPx())
        val path = Path()
        var x = 0f
        path.moveTo(startX, centerY + amp * sin(-shift))
        while (x < activeLength) {
            x = minOf(x + step, activeLength)
            path.lineTo(startX + x, centerY + amp * sin(TWO_PI * x / waveLength - shift))
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )

        // Stop dot at the end of the track (only while there is track left to show).
        if (hasTrack) {
            drawCircle(color = color, radius = stroke / 4f, center = Offset(endX, centerY))
        }
    }
}

/**
 * Indeterminate circular progress: a wavy arc that rotates around a faint ring while its
 * length breathes between roughly a quarter and three quarters of the circle, and the wave
 * itself keeps traveling along the arc. Fill the available space with [modifier] (it expects a
 * square area).
 */
@Composable
fun WavyCircularProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = color.copy(alpha = 0.24f),
    strokeWidth: Dp = 6.dp,
    amplitude: Dp = 2.5.dp,
    wavelength: Dp = 26.dp,
) {
    val transition = rememberInfiniteTransition(label = "wavyCircular")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 1800, easing = LinearEasing)),
        label = "wavyCircularRotation",
    )
    val sweep by transition.animateFloat(
        initialValue = 90f,
        targetValue = 270f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "wavyCircularSweep",
    )
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 1500, easing = LinearEasing)),
        label = "wavyCircularPhase",
    )

    Canvas(
        modifier = modifier.semantics { progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate },
    ) {
        val stroke = strokeWidth.toPx()
        val amp = amplitude.toPx()
        // Leave room for the wave's peaks and the stroke so nothing is clipped.
        val radius = size.minDimension / 2f - stroke / 2f - amp
        if (radius <= 0f) return@Canvas

        drawCircle(color = trackColor, radius = radius, style = Stroke(width = stroke))

        // A whole number of waves per full turn keeps the shape smooth at any arc length.
        val waves = maxOf(4, (TWO_PI * radius / wavelength.toPx()).roundToInt())
        val startRad = (rotation - 90f) * DEG_TO_RAD
        val sweepRad = sweep * DEG_TO_RAD
        val steps = maxOf(24, (sweep / 2f).roundToInt())

        val path = Path()
        for (i in 0..steps) {
            val a = sweepRad * i / steps
            val r = radius + amp * sin(waves * a - phase * TWO_PI)
            val t = startRad + a
            val px = center.x + r * cos(t)
            val py = center.y + r * sin(t)
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}
