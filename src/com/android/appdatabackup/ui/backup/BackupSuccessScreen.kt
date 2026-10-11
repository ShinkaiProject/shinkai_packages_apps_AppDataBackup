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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.android.appdatabackup.R
import com.android.appdatabackup.ui.BackupActions
import com.android.appdatabackup.ui.BackupUiState
import com.android.appdatabackup.ui.OpType
import com.android.appdatabackup.ui.Outcome
import com.android.appdatabackup.ui.OperationResult
import com.android.appdatabackup.ui.backup.components.BackupTopBar
import com.android.appdatabackup.ui.backup.components.ResultEmblem
import com.android.appdatabackup.ui.backup.components.ResultEntryRow
import com.android.appdatabackup.ui.backup.components.StateMessage
import com.android.appdatabackup.ui.backup.components.groupedShape

/**
 * Result of the backup or restore that just ran. Everything shown is what the engine reported:
 * success is only claimed when every reported package and the engine's overall verdict agree,
 * and missing result data is shown as such, never as success.
 */
@Composable
fun BackupSuccessScreen(
    state: BackupUiState,
    actions: BackupActions,
    modifier: Modifier = Modifier,
) {
    val result = state.result
    val title = if (result != null) resultTitle(result) else stringResource(R.string.result_details_title)

    Column(modifier = modifier.fillMaxSize()) {
        BackupTopBar(title = title, onBack = actions::resultDone)

        if (result == null) {
            StateMessage(
                title = stringResource(R.string.result_no_data),
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                item(key = "emblem", contentType = "emblem") {
                    Column(
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        ResultEmblem(outcome = result.outcome)
                    }
                }
                item(key = "heading", contentType = "heading") {
                    Column(
                        modifier = Modifier.padding(bottom = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text(
                            text = resultSupporting(result),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        if (result.entries.isNotEmpty() &&
                            result.outcome != Outcome.SUCCESS &&
                            result.overallMessage.isNotBlank()
                        ) {
                            Text(
                                text = result.overallMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
                itemsIndexed(result.entries, key = { _, entry -> entry.packageName }) { index, entry ->
                    ResultEntryRow(entry = entry, shape = groupedShape(index, result.entries.size))
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (result != null) {
                OutlinedButton(
                    onClick = actions::showResultDetails,
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                ) {
                    Text(stringResource(R.string.result_view_details))
                }
            }
            Button(
                onClick = actions::resultDone,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
            ) {
                Text(
                    text = stringResource(R.string.result_done),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun resultTitle(result: OperationResult): String {
    val restore = result.type == OpType.RESTORE
    val res = when (result.outcome) {
        Outcome.SUCCESS ->
            if (restore) R.string.result_title_restore_success else R.string.result_title_backup_success
        Outcome.PARTIAL ->
            if (restore) R.string.result_title_restore_partial else R.string.result_title_backup_partial
        Outcome.FAILED ->
            if (restore) R.string.result_title_restore_failed else R.string.result_title_backup_failed
        Outcome.UNKNOWN ->
            if (restore) R.string.result_title_restore_done else R.string.result_title_backup_done
    }
    return stringResource(res)
}

@Composable
private fun resultSupporting(result: OperationResult): String {
    val restore = result.type == OpType.RESTORE
    return when {
        result.entries.isEmpty() -> result.overallMessage.ifBlank { stringResource(R.string.result_no_data) }
        result.failCount > 0 -> stringResource(R.string.result_subtitle_with_failures, result.okCount, result.failCount)
        restore && result.okCount == 1 -> stringResource(R.string.result_subtitle_restore_one)
        restore -> stringResource(R.string.result_subtitle_restore, result.okCount)
        result.okCount == 1 -> stringResource(R.string.result_subtitle_backup_one)
        else -> stringResource(R.string.result_subtitle_backup, result.okCount)
    }
}
