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

package com.android.appdatabackup.ui.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.android.appdatabackup.R
import com.android.appdatabackup.ui.BackupActions
import com.android.appdatabackup.ui.BackupUiState
import com.android.appdatabackup.ui.Loadable
import com.android.appdatabackup.ui.ManageTab
import com.android.appdatabackup.ui.backup.components.BackupDetailItem
import com.android.appdatabackup.ui.backup.components.BackupDeviceCard
import com.android.appdatabackup.ui.backup.components.BackupTopBar
import com.android.appdatabackup.ui.backup.components.StatusPill
import com.android.appdatabackup.ui.formatTimestamp
import com.android.appdatabackup.ui.valueOrNull

/**
 * Backup dashboard. Everything shown here comes from the engine's catalog: the device name,
 * whether any backup exists, when the newest one was made, and whether a restore is possible.
 */
@Composable
fun BackupScreen(
    state: BackupUiState,
    actions: BackupActions,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val backups = state.backups
    val list = backups.valueOrNull().orEmpty()
    val latestMs = remember(list) { list.maxOfOrNull { it.timestampMs } }

    val hasBackup = latestMs != null
    val backupStatus = when {
        backups is Loadable.Loading -> stringResource(R.string.dash_status_loading)
        backups is Loadable.Error -> stringResource(R.string.dash_status_error)
        latestMs == null -> stringResource(R.string.dash_backup_none)
        else -> stringResource(R.string.dash_backup_active, formatTimestamp(context, latestMs))
    }
    val restoreStatus = when {
        backups is Loadable.Loading -> stringResource(R.string.dash_status_loading)
        backups is Loadable.Error -> stringResource(R.string.dash_status_error)
        list.isEmpty() -> stringResource(R.string.dash_restore_none)
        list.size == 1 -> stringResource(R.string.dash_restore_count_one)
        else -> stringResource(R.string.dash_restore_count, list.size)
    }

    Column(modifier = modifier.fillMaxSize()) {
        BackupTopBar(
            title = stringResource(R.string.name_dashboard),
            onBack = { actions.onBack() },
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BackupDeviceCard(
                deviceName = state.deviceName,
                onBackUpNow = { actions.openManage(ManageTab.APPS) },
            )
            Text(
                text = stringResource(R.string.backup_details),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .padding(start = 8.dp, top = 12.dp)
                    .semantics { heading() },
            )
            BackupDetailItem(
                iconRes = R.drawable.ic_save_24,
                title = stringResource(R.string.backup),
                supporting = backupStatus,
                // A failed read can be retried by tapping the row.
                onClick = if (backups is Loadable.Error) actions::reload else null,
                trailing = if (backups is Loadable.Ready) {
                    {
                        StatusPill(
                            text = stringResource(if (hasBackup) R.string.active else R.string.inactive),
                            iconRes = if (hasBackup) R.drawable.ic_check_24 else R.drawable.ic_pending_24,
                            active = hasBackup,
                        )
                    }
                } else {
                    null
                },
            )
            BackupDetailItem(
                iconRes = R.drawable.ic_restore_24,
                title = stringResource(R.string.dash_restore_title),
                supporting = restoreStatus,
                enabled = list.isNotEmpty() || backups is Loadable.Error,
                onClick = when {
                    backups is Loadable.Error -> actions::reload
                    list.isNotEmpty() -> ({ actions.openManage(ManageTab.BACKUPS) })
                    else -> null
                },
            )
        }
    }
}
