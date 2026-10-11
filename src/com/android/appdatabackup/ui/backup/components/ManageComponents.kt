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
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.android.appdatabackup.R
import com.android.appdatabackup.ui.ManageTab
import com.android.appdatabackup.ui.formatBytes

/** "No apps selected" / "N apps selected", with a small spring pop whenever it changes. */
@Composable
fun SelectionSummaryCard(
    count: Int,
    sizeBytes: Long,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(
        targetValue = if (count > 0) colors.primaryContainer else colors.surfaceContainerHigh,
        label = "summaryContainer",
    )
    val content = if (count > 0) colors.onPrimaryContainer else colors.onSurface
    val contentVariant = if (count > 0) colors.onPrimaryContainer else colors.onSurfaceVariant

    val scale = remember { Animatable(1f) }
    LaunchedEffect(count) {
        if (count > 0) {
            scale.snapTo(0.97f)
            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            // Announce changes to screen readers as the selection changes.
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.extraLarge,
        color = container,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (count) {
                        0 -> stringResource(R.string.summary_empty_count)
                        1 -> stringResource(R.string.summary_count_one)
                        else -> stringResource(R.string.summary_count, count)
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = content,
                )
                Text(
                    text = if (count == 0) {
                        stringResource(R.string.summary_empty_size)
                    } else if (sizeBytes > 0) {
                        stringResource(R.string.summary_size, formatBytes(sizeBytes))
                    } else {
                        ""
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentVariant,
                )
            }
            Icon(
                painter = painterResource(if (count > 0) R.drawable.ic_check_24 else R.drawable.ic_archive_24),
                contentDescription = null,
                tint = contentVariant,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

/** Pill-shaped Apps / Backups switcher. Selected tab is announced as selected, not just tinted. */
@Composable
fun BackupTabs(
    selected: ManageTab,
    onSelect: (ManageTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    // Opaque so list items scroll cleanly underneath it when it is pinned.
    Box(modifier = modifier.fillMaxWidth().background(colors.background).padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .background(colors.surfaceContainer)
                .padding(4.dp),
        ) {
            TabPill(R.string.tab_apps, selected == ManageTab.APPS, Modifier.weight(1f)) { onSelect(ManageTab.APPS) }
            TabPill(R.string.tab_backups, selected == ManageTab.BACKUPS, Modifier.weight(1f)) { onSelect(ManageTab.BACKUPS) }
        }
    }
}

@Composable
private fun TabPill(labelRes: Int, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val background by animateColorAsState(
        targetValue = if (selected) colors.primary else Color.Transparent,
        label = "tabBackground",
    )
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(CircleShape)
            .background(background)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) colors.onPrimary else colors.onSurfaceVariant,
        )
    }
}

/** Search field (label and package name) plus a sort menu. */
@Composable
fun AppSearchRow(
    query: String,
    sortBySize: Boolean,
    onQueryChange: (String) -> Unit,
    onSortBySizeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val focus: FocusManager = LocalFocusManager.current
    // Edited locally so the cursor never jumps; the ViewModel receives every change.
    var text by rememberSaveable { mutableStateOf(query) }
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                onQueryChange(it)
            },
            modifier = Modifier.weight(1f),
            singleLine = true,
            shape = CircleShape,
            placeholder = { Text(stringResource(R.string.hint_search)) },
            leadingIcon = {
                Icon(painter = painterResource(R.drawable.ic_search_24), contentDescription = null)
            },
            trailingIcon = if (text.isNotEmpty()) {
                {
                    IconButton(onClick = { text = ""; onQueryChange("") }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_clear_24),
                            contentDescription = stringResource(R.string.cd_clear_search),
                        )
                    }
                }
            } else {
                null
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.surfaceContainer,
                unfocusedContainerColor = colors.surfaceContainer,
                unfocusedBorderColor = Color.Transparent,
            ),
        )
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(
                    painter = painterResource(R.drawable.ic_sort_24),
                    contentDescription = stringResource(R.string.cd_sort),
                    // Same reason as the back arrow: this button is outside any Surface.
                    tint = colors.onSurfaceVariant,
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                SortEntry(R.string.sort_by_name, selected = !sortBySize) {
                    menuOpen = false
                    onSortBySizeChange(false)
                }
                SortEntry(R.string.sort_by_size, selected = sortBySize) {
                    menuOpen = false
                    onSortBySizeChange(true)
                }
            }
        }
    }
}

@Composable
private fun SortEntry(textRes: Int, selected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(stringResource(textRes)) },
        onClick = onClick,
        leadingIcon = {
            if (selected) {
                Icon(painter = painterResource(R.drawable.ic_check_24), contentDescription = null)
            } else {
                Spacer(Modifier.size(24.dp))
            }
        },
    )
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.dp, top = 12.dp, bottom = 4.dp)
            .semantics { heading() },
    )
}

/** Loading, empty and error placeholder shared by both tabs. */
@Composable
fun StateMessage(
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    loading: Boolean = false,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (loading) CircularProgressIndicator()
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        if (body != null) {
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (actionLabel != null && onAction != null) {
            FilledTonalButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}
