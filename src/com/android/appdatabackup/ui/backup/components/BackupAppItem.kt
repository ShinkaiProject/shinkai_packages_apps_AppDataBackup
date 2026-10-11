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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.appdatabackup.R
import com.android.appdatabackup.data.InstalledApp
import com.android.appdatabackup.ui.formatBytes
import com.android.appdatabackup.ui.formatTimestamp

/**
 * One installed app. The whole row is a single checkbox for TalkBack and touch; selection is
 * shown by the checkbox glyph and an animated container color, and backup status by an icon
 * plus text, so nothing depends on color alone.
 */
@Composable
fun BackupAppItem(
    app: InstalledApp,
    selected: Boolean,
    lastBackupMs: Long?,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val container by animateColorAsState(
        targetValue = if (selected) colors.primaryContainer else colors.surfaceContainer,
        label = "appItemContainer",
    )
    val primaryText = if (selected) colors.onPrimaryContainer else colors.onSurface
    val secondaryText = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant

    val meta = remember(app.versionName, app.dataSize) {
        listOfNotNull(
            app.versionName.takeIf { it.isNotBlank() }?.let { context.getString(R.string.version_short, it) },
            app.dataSize.takeIf { it >= 0 }?.let { formatBytes(it) },
        ).joinToString("  \u00b7  ")
    }
    val backedUpText = if (lastBackupMs != null) {
        stringResource(R.string.status_backed_up_on, remember(lastBackupMs) { formatTimestamp(context, lastBackupMs) })
    } else {
        stringResource(R.string.status_not_backed_up)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = container,
        contentColor = primaryText,
    ) {
        Row(
            modifier = Modifier
                .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onToggle() })
                .heightIn(min = 72.dp)
                .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppIcon(packageName = app.packageName, label = app.label)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = primaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (meta.isNotEmpty()) {
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.labelMedium,
                        color = secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        painter = painterResource(
                            if (lastBackupMs != null) R.drawable.ic_check_24 else R.drawable.ic_pending_24,
                        ),
                        contentDescription = null,
                        tint = secondaryText,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = backedUpText,
                        style = MaterialTheme.typography.labelMedium,
                        color = secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (app.optsOutOfBackup) {
                    Text(
                        text = stringResource(R.string.backup_opt_out),
                        style = MaterialTheme.typography.labelMedium,
                        color = secondaryText,
                    )
                }
            }
            // Visual only: the toggleable row above owns the interaction and the semantics.
            RoundCheckbox(checked = selected)
        }
    }
}
