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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.appdatabackup.R
import com.android.appdatabackup.data.SavedBackup
import com.android.appdatabackup.ui.backupContents
import com.android.appdatabackup.ui.formatBytes
import com.android.appdatabackup.ui.formatTimestamp

/** One saved backup, with real metadata from the engine's catalog and per-item actions. */
@Composable
fun BackupRecordItem(
    record: SavedBackup,
    selected: Boolean,
    verificationFailed: Boolean,
    onToggle: () -> Unit,
    onRestore: () -> Unit,
    onVerify: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val container by animateColorAsState(
        targetValue = if (selected) colors.primaryContainer else colors.surfaceContainer,
        label = "backupItemContainer",
    )
    val primaryText = if (selected) colors.onPrimaryContainer else colors.onSurface
    val secondaryText = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant
    var menuOpen by remember { mutableStateOf(false) }

    val meta = remember(record) {
        listOfNotNull(
            record.versionName.takeIf { it.isNotBlank() }?.let { context.getString(R.string.version_short, it) },
            formatTimestamp(context, record.timestampMs),
            formatBytes(record.totalSize),
        ).joinToString("  \u00b7  ")
    }
    val contents = remember(record.components, record.encrypted) {
        backupContents(context, record.components, record.encrypted)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = container,
        contentColor = primaryText,
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onToggle() })
                    .heightIn(min = 72.dp)
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AppIcon(packageName = record.packageName, label = record.label)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = primaryText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = record.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.labelMedium,
                        color = secondaryText,
                    )
                    Text(
                        text = contents,
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryText,
                    )
                    if (verificationFailed) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_clear_24),
                                contentDescription = null,
                                tint = colors.error,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = stringResource(R.string.verify_failed_badge),
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.error,
                            )
                        }
                    }
                }
                RoundCheckbox(checked = selected)
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_more_vert_24),
                        contentDescription = stringResource(R.string.cd_more_actions),
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    MenuEntry(R.string.restore, R.drawable.ic_restore_24) { menuOpen = false; onRestore() }
                    MenuEntry(R.string.verify, R.drawable.ic_verify_24) { menuOpen = false; onVerify() }
                    MenuEntry(R.string.export, R.drawable.ic_archive_24) { menuOpen = false; onExport() }
                    MenuEntry(R.string.delete, R.drawable.ic_delete_24) { menuOpen = false; onDelete() }
                }
            }
        }
    }
}

@Composable
private fun MenuEntry(textRes: Int, iconRes: Int, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(stringResource(textRes)) },
        onClick = onClick,
        leadingIcon = { Icon(painter = painterResource(iconRes), contentDescription = null) },
    )
}
