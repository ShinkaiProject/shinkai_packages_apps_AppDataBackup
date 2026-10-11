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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.android.appdatabackup.R

/**
 * Round selection indicator used by the app and backup rows.
 *
 * It is visual only: the row that contains it is the single `toggleable` (Role.Checkbox), which
 * owns the touch target and the accessibility state, exactly as before. The checked state is
 * shown by a filled disc and a check glyph (not by color alone); the glyph pops in with a short
 * spring and the fill and outline cross-fade.
 */
@Composable
fun RoundCheckbox(
    checked: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val fill by animateColorAsState(
        targetValue = if (checked) colors.primary else Color.Transparent,
        label = "roundCheckFill",
    )
    val outline by animateColorAsState(
        targetValue = if (checked) colors.primary else colors.onSurfaceVariant,
        label = "roundCheckOutline",
    )
    val mark by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "roundCheckMark",
    )

    // 48 dp slot (same footprint as the Material checkbox) around a 24 dp disc.
    Box(modifier = modifier.size(48.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(fill)
                .border(width = 2.dp, color = outline, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_check_24),
                contentDescription = null,
                tint = colors.onPrimary,
                modifier = Modifier
                    .size(16.dp)
                    .graphicsLayer {
                        scaleX = mark
                        scaleY = mark
                        alpha = mark.coerceIn(0f, 1f)
                    },
            )
        }
    }
}
