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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.appdatabackup.R

/** Floating action surface that stays above the navigation bar and the keyboard. */
@Composable
private fun ActionSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            content()
        }
    }
}

/** Apps tab: Exclude cache, Select all, Clear, Back up. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppsBottomActions(
    selectedCount: Int,
    excludeCache: Boolean,
    canSelectAll: Boolean,
    enabled: Boolean,
    onToggleExcludeCache: () -> Unit,
    onSelectAll: () -> Unit,
    onClear: () -> Unit,
    onBackUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ActionSurface(modifier) {
        // FlowRow keeps every control reachable at large font scales by wrapping to a new line.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = excludeCache,
                onClick = onToggleExcludeCache,
                enabled = enabled,
                label = { Text(stringResource(R.string.chip_exclude_cache)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(
                            if (excludeCache) R.drawable.ic_check_24 else R.drawable.ic_cache_24,
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
            TextButton(onClick = onSelectAll, enabled = enabled && canSelectAll) {
                Text(stringResource(R.string.action_select_all))
            }
            TextButton(onClick = onClear, enabled = enabled && selectedCount > 0) {
                Text(stringResource(R.string.action_clear))
            }
        }
        Button(
            onClick = onBackUp,
            enabled = enabled && selectedCount > 0,
            shape = CircleShape,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
        ) {
            Text(
                text = if (selectedCount > 0) {
                    stringResource(R.string.fab_backup_count, selectedCount)
                } else {
                    stringResource(R.string.action_back_up)
                },
            )
        }
    }
}

/** Backups tab: Select all, Verify all, Clear, Delete selected, Restore. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BackupsBottomActions(
    selectedCount: Int,
    hasBackups: Boolean,
    enabled: Boolean,
    onSelectAll: () -> Unit,
    onVerifyAll: () -> Unit,
    onClear: () -> Unit,
    onDeleteSelected: () -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ActionSurface(modifier) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = onSelectAll, enabled = enabled && hasBackups) {
                Text(stringResource(R.string.action_select_all))
            }
            TextButton(onClick = onVerifyAll, enabled = enabled && hasBackups) {
                Text(stringResource(R.string.verify_all))
            }
            TextButton(onClick = onClear, enabled = enabled && selectedCount > 0) {
                Text(stringResource(R.string.action_clear))
            }
            TextButton(
                onClick = onDeleteSelected,
                enabled = enabled && selectedCount > 0,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text(stringResource(R.string.action_delete_selected))
            }
        }
        Button(
            onClick = onRestore,
            enabled = enabled && selectedCount > 0,
            shape = CircleShape,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
        ) {
            Text(
                text = if (selectedCount > 0) {
                    stringResource(R.string.fab_restore_count, selectedCount)
                } else {
                    stringResource(R.string.restore)
                },
            )
        }
    }
}
