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

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.appdatabackup.R
import com.android.appdatabackup.ui.Operation
import com.android.appdatabackup.ui.PackageStatus

/**
 * Modal progress for backup, restore and verification. It sits above everything, swallows
 * touches so nothing underneath can start a second operation, and keeps an indeterminate ring
 * spinning around the current app even before the first package reports progress.
 */
@Composable
fun ProgressOverlay(
    operation: Operation,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    var expanded by remember { mutableStateOf(false) }
    val fraction = if (operation.total == 0) 0f else operation.finished.toFloat() / operation.total
    val animatedFraction by animateFloatAsState(targetValue = fraction, label = "progressFraction")
    val current = operation.packages.firstOrNull { it.packageName == operation.currentPackage }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.scrim.copy(alpha = 0.72f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            )
            .systemBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 420.dp)
                .fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = colors.surfaceContainerHigh,
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .animateContentSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.processing_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurfaceVariant,
                )
                Box(modifier = Modifier.size(112.dp), contentAlignment = Alignment.Center) {
                    if (current != null) {
                        AppIcon(packageName = current.packageName, label = current.label, size = 56.dp)
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.ic_verify_24),
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }
                Column(
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (current != null) {
                        Text(
                            text = current.label,
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.onSurface,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (operation.detail != null) {
                        Text(
                            text = operation.detail,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                if (operation.total > 0) {
                    WavyLinearProgressIndicator(
                        progress = { animatedFraction },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable { expanded = !expanded },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.progress_list_header, operation.finished, operation.total),
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            painter = painterResource(R.drawable.ic_expand_more_24),
                            contentDescription = null,
                            tint = colors.onSurfaceVariant,
                            modifier = Modifier.rotate(if (expanded) 180f else 0f),
                        )
                    }
                    if (expanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            operation.packages.forEach { pkg ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    AppIcon(packageName = pkg.packageName, label = pkg.label, size = 32.dp)
                                    Text(
                                        text = pkg.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colors.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                    PackageStatusIcon(pkg.status)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.size(4.dp))
                OutlinedButton(onClick = onCancel, enabled = !operation.cancelling) {
                    Text(stringResource(if (operation.cancelling) R.string.operation_cancelling else R.string.cancel))
                }
            }
        }
    }
}

/** Status glyph per package: the shape differs for every state, so color is only a hint. */
@Composable
private fun PackageStatusIcon(status: PackageStatus) {
    val colors = MaterialTheme.colorScheme
    when (status) {
        PackageStatus.SUCCESS -> Icon(
            painter = painterResource(R.drawable.ic_check_24),
            contentDescription = stringResource(R.string.status_succeeded),
            tint = colors.primary,
            modifier = Modifier.size(20.dp),
        )
        PackageStatus.FAILED -> Icon(
            painter = painterResource(R.drawable.ic_clear_24),
            contentDescription = stringResource(R.string.status_failed),
            tint = colors.error,
            modifier = Modifier.size(20.dp),
        )
        PackageStatus.RUNNING -> CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        PackageStatus.PENDING -> Icon(
            painter = painterResource(R.drawable.ic_pending_24),
            contentDescription = null,
            tint = colors.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}
