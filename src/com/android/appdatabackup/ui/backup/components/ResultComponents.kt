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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.appdatabackup.R
import com.android.appdatabackup.ui.Outcome
import com.android.appdatabackup.ui.ResultEntry

/**
 * Large emblem for the result screen. The glyph is drawn as it appears: a check for success,
 * a cross for failure, an exclamation mark for partial or unknown results. The heading next
 * to it always states the outcome in words.
 */
@Composable
fun ResultEmblem(outcome: Outcome, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val container: Color
    val glyph: Color
    when (outcome) {
        Outcome.SUCCESS -> { container = colors.primaryContainer; glyph = colors.onPrimaryContainer }
        Outcome.PARTIAL -> { container = colors.tertiaryContainer; glyph = colors.onTertiaryContainer }
        Outcome.FAILED -> { container = colors.errorContainer; glyph = colors.onErrorContainer }
        Outcome.UNKNOWN -> { container = colors.secondaryContainer; glyph = colors.onSecondaryContainer }
    }

    val scale = remember { Animatable(0f) }
    val draw = remember { Animatable(0f) }
    LaunchedEffect(outcome) {
        scale.snapTo(0f)
        draw.snapTo(0f)
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
        draw.animateTo(1f, tween(durationMillis = 380))
    }

    Box(
        modifier = modifier
            .size(132.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .clip(CircleShape)
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(64.dp)) {
            drawGlyph(outcome, draw.value, glyph)
        }
    }
}

private fun DrawScope.drawGlyph(outcome: Outcome, progress: Float, color: Color) {
    if (progress <= 0f) return
    val w = size.width
    val h = size.height
    val stroke = Stroke(width = w * 0.11f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    when (outcome) {
        Outcome.SUCCESS -> strokeLine(
            listOf(Offset(w * 0.12f, h * 0.54f), Offset(w * 0.40f, h * 0.80f), Offset(w * 0.88f, h * 0.22f)),
            progress, color, stroke,
        )
        Outcome.FAILED -> {
            strokeLine(listOf(Offset(w * 0.2f, h * 0.2f), Offset(w * 0.8f, h * 0.8f)), progress, color, stroke)
            strokeLine(listOf(Offset(w * 0.8f, h * 0.2f), Offset(w * 0.2f, h * 0.8f)), progress, color, stroke)
        }
        Outcome.PARTIAL, Outcome.UNKNOWN -> {
            strokeLine(listOf(Offset(w * 0.5f, h * 0.12f), Offset(w * 0.5f, h * 0.58f)), progress, color, stroke)
            drawCircle(color = color.copy(alpha = progress), radius = w * 0.07f, center = Offset(w * 0.5f, h * 0.84f))
        }
    }
}

/** Draws the first [progress] fraction of a poly-line, so the stroke appears to be written. */
private fun DrawScope.strokeLine(points: List<Offset>, progress: Float, color: Color, stroke: Stroke) {
    val path = Path().apply {
        moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
    }
    val measure = PathMeasure()
    measure.setPath(path, false)
    val partial = Path()
    measure.getSegment(0f, measure.length * progress, partial, true)
    drawPath(partial, color, style = stroke)
}

/** Rounded top and bottom corners on the first and last row, tight corners in between. */
fun groupedShape(index: Int, count: Int): Shape {
    val big = 28.dp
    val small = 6.dp
    val top = if (index == 0) big else small
    val bottom = if (index == count - 1) big else small
    return RoundedCornerShape(topStart = top, topEnd = top, bottomEnd = bottom, bottomStart = bottom)
}

/** One line of the result list: real icon and name, outcome icon, and the error if any. */
@Composable
fun ResultEntryRow(
    entry: ResultEntry,
    shape: Shape,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        shape = shape,
        color = colors.surfaceContainer,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 64.dp)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppIcon(packageName = entry.packageName, label = entry.label, size = 44.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!entry.success && entry.message.isNotBlank()) {
                    Text(
                        text = entry.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.error,
                    )
                }
            }
            Icon(
                painter = painterResource(if (entry.success) R.drawable.ic_check_24 else R.drawable.ic_clear_24),
                contentDescription = stringResource(if (entry.success) R.string.status_succeeded else R.string.status_failed),
                tint = if (entry.success) colors.primary else colors.error,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
