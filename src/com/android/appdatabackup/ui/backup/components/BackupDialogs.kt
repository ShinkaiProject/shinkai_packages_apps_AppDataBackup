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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.appdatabackup.R
import com.android.appdatabackup.data.BackupComponents
import com.android.appdatabackup.ui.BackupActions
import com.android.appdatabackup.ui.BackupDialog
import com.android.appdatabackup.ui.BackupUiState
import com.android.appdatabackup.ui.OpType
import com.android.appdatabackup.ui.ResultEntry
import com.android.appdatabackup.ui.backupContents
import com.android.appdatabackup.ui.formatBytes
import com.android.appdatabackup.ui.formatTimestamp
import com.android.appdatabackup.ui.valueOrNull
import com.android.appdatabackup.data.SavedBackup

/** Renders whichever dialog the ViewModel currently asks for. Dialog state survives rotation. */
@Composable
fun BackupDialogHost(state: BackupUiState, actions: BackupActions) {
    when (val dialog = state.dialog) {
        null -> Unit
        is BackupDialog.BackupOptions -> BackupOptionsDialog(
            onDismiss = actions::dismissDialog,
            onConfirm = actions::confirmBackup,
        )
        is BackupDialog.SpaceWarning -> SpaceWarningDialog(dialog, actions)
        is BackupDialog.RestoreOptions -> RestoreOptionsDialog(dialog, actions)
        is BackupDialog.VerifyPassphrase -> VerifyPassphraseDialog(dialog, actions)
        is BackupDialog.ConfirmDelete -> ConfirmDeleteDialog(dialog, actions)
        is BackupDialog.ConfirmExport -> ConfirmExportDialog(dialog, actions)
        is BackupDialog.ResultDetails -> ResultDetailsDialog(state, actions)
    }
}

// -------------------------------------------------------------------------------------
// Shared pieces
// -------------------------------------------------------------------------------------

private class ComponentOption(val labelRes: Int, val flag: Int)

private fun allComponents(): Int =
    BackupComponents.APK or BackupComponents.CE_DATA or BackupComponents.DE_DATA or BackupComponents.EXTERNAL

/** Same four components, all selected by default, exactly as the previous dialog behaved. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ComponentChips(mask: Int, onMaskChange: (Int) -> Unit) {
    val options = remember {
        listOf(
            ComponentOption(R.string.comp_apk, BackupComponents.APK),
            ComponentOption(R.string.comp_app_data, BackupComponents.CE_DATA),
            ComponentOption(R.string.comp_de_data, BackupComponents.DE_DATA),
            ComponentOption(R.string.comp_external, BackupComponents.EXTERNAL),
        )
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            val on = (mask and option.flag) != 0
            FilterChip(
                selected = on,
                onClick = { onMaskChange(mask xor option.flag) },
                label = { Text(stringResource(option.labelRes)) },
                leadingIcon = if (on) {
                    {
                        Icon(
                            painter = painterResource(R.drawable.ic_check_24),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                } else {
                    null
                },
            )
        }
    }
}

@Composable
private fun DialogSectionLabel(textRes: Int) {
    Text(
        text = stringResource(textRes),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun PassphraseField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean = false,
    supporting: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text(label) },
        isError = isError,
        supportingText = if (supporting != null) {
            { Text(supporting) }
        } else {
            null
        },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
    )
}

@Composable
private fun ErrorLine(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
}

// -------------------------------------------------------------------------------------
// Backup options
// -------------------------------------------------------------------------------------

@Composable
private fun BackupOptionsDialog(
    onDismiss: () -> Unit,
    onConfirm: (components: Int, passphrase: String?, keepVersions: Int) -> Unit,
) {
    var mask by remember { mutableStateOf(allComponents()) }
    // Secrets are intentionally kept in plain `remember`, never in saved instance state.
    var pass by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var keep by remember { mutableStateOf("") }
    val mismatch = pass != confirm
    val noComponent = mask == 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.backup_options_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DialogSectionLabel(R.string.section_include)
                ComponentChips(mask) { mask = it }
                if (noComponent) ErrorLine(stringResource(R.string.select_one_component))

                DialogSectionLabel(R.string.section_encryption)
                PassphraseField(pass, { pass = it }, stringResource(R.string.hint_passphrase))
                PassphraseField(
                    value = confirm,
                    onValueChange = { confirm = it },
                    label = stringResource(R.string.hint_passphrase_confirm),
                    isError = mismatch,
                    supporting = if (mismatch) stringResource(R.string.passphrase_mismatch) else null,
                )

                DialogSectionLabel(R.string.section_retention)
                OutlinedTextField(
                    value = keep,
                    onValueChange = { keep = it.filter(Char::isDigit).take(4) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.hint_keep)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !mismatch && !noComponent,
                onClick = { onConfirm(mask, pass.ifEmpty { null }, keep.toIntOrNull() ?: 0) },
            ) { Text(stringResource(R.string.action_back_up)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun SpaceWarningDialog(dialog: BackupDialog.SpaceWarning, actions: BackupActions) {
    AlertDialog(
        onDismissRequest = actions::dismissDialog,
        title = { Text(stringResource(R.string.not_enough_space_title)) },
        text = {
            Text(
                stringResource(
                    R.string.not_enough_space_message,
                    formatBytes(dialog.needed),
                    formatBytes(dialog.free),
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = actions::proceedDespiteLowSpace) { Text(stringResource(R.string.backup_anyway)) }
        },
        dismissButton = {
            TextButton(onClick = actions::dismissDialog) { Text(stringResource(R.string.cancel)) }
        },
    )
}

// -------------------------------------------------------------------------------------
// Restore options
// -------------------------------------------------------------------------------------

@Composable
private fun RestoreOptionsDialog(dialog: BackupDialog.RestoreOptions, actions: BackupActions) {
    val records = dialog.records
    val encrypted = records.any { it.encrypted }
    var mask by remember { mutableStateOf(allComponents()) }
    var pass by remember { mutableStateOf("") }
    val noComponent = mask == 0
    val missingPassphrase = encrypted && pass.isEmpty()

    val baseMessage = stringResource(R.string.confirm_restore_message)
    val message = if (dialog.downgradeWarning != null) {
        baseMessage + "\n\n" + stringResource(R.string.restore_downgrade_warning, dialog.downgradeWarning)
    } else {
        baseMessage
    }
    val title = if (records.size == 1) {
        stringResource(R.string.confirm_restore_title, records[0].label)
    } else {
        stringResource(R.string.confirm_restore_selected_title, records.size)
    }

    AlertDialog(
        onDismissRequest = actions::dismissDialog,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(message, style = MaterialTheme.typography.bodyMedium)
                DialogSectionLabel(R.string.section_include)
                ComponentChips(mask) { mask = it }
                if (noComponent) ErrorLine(stringResource(R.string.select_one_component))
                if (encrypted) {
                    DialogSectionLabel(R.string.section_encryption)
                    PassphraseField(
                        value = pass,
                        onValueChange = { pass = it },
                        label = stringResource(R.string.hint_passphrase),
                        isError = missingPassphrase,
                        supporting = if (missingPassphrase) stringResource(R.string.passphrase_required) else null,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !noComponent && !missingPassphrase,
                onClick = { actions.confirmRestore(mask, pass.ifEmpty { null }) },
            ) { Text(stringResource(R.string.restore)) }
        },
        dismissButton = {
            TextButton(onClick = actions::dismissDialog) { Text(stringResource(R.string.cancel)) }
        },
    )
}

// -------------------------------------------------------------------------------------
// Verify, delete, export
// -------------------------------------------------------------------------------------

@Composable
private fun VerifyPassphraseDialog(dialog: BackupDialog.VerifyPassphrase, actions: BackupActions) {
    val bulk = dialog.records.size > 1
    var pass by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = actions::dismissDialog,
        title = { Text(stringResource(if (bulk) R.string.verify_all else R.string.verify_encrypted_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(if (bulk) R.string.verify_all_message else R.string.verify_encrypted_message),
                    style = MaterialTheme.typography.bodyMedium,
                )
                PassphraseField(pass, { pass = it }, stringResource(R.string.hint_passphrase))
            }
        },
        confirmButton = {
            TextButton(onClick = { actions.confirmVerify(pass.ifEmpty { null }) }) {
                Text(stringResource(R.string.verify))
            }
        },
        dismissButton = {
            TextButton(onClick = actions::dismissDialog) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun ConfirmDeleteDialog(dialog: BackupDialog.ConfirmDelete, actions: BackupActions) {
    val single = dialog.records.size == 1
    AlertDialog(
        onDismissRequest = actions::dismissDialog,
        title = {
            Text(
                if (single) {
                    stringResource(R.string.confirm_delete_title)
                } else {
                    stringResource(R.string.confirm_delete_selected_title, dialog.records.size)
                },
            )
        },
        text = {
            Text(
                if (single) {
                    stringResource(R.string.confirm_delete_message, dialog.records[0].label)
                } else {
                    stringResource(R.string.confirm_delete_selected_message)
                },
            )
        },
        confirmButton = {
            TextButton(
                onClick = actions::confirmDelete,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text(stringResource(R.string.delete)) }
        },
        dismissButton = {
            TextButton(onClick = actions::dismissDialog) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun ConfirmExportDialog(dialog: BackupDialog.ConfirmExport, actions: BackupActions) {
    AlertDialog(
        onDismissRequest = actions::dismissDialog,
        title = { Text(stringResource(R.string.export)) },
        text = { Text(stringResource(R.string.export_message, dialog.record.label)) },
        confirmButton = {
            TextButton(onClick = actions::confirmExport) { Text(stringResource(R.string.export)) }
        },
        dismissButton = {
            TextButton(onClick = actions::dismissDialog) { Text(stringResource(R.string.cancel)) }
        },
    )
}

// -------------------------------------------------------------------------------------
// "View details" on the result screen
// -------------------------------------------------------------------------------------

@Composable
private fun ResultDetailsDialog(state: BackupUiState, actions: BackupActions) {
    val result = state.result ?: return
    val records = state.backups.valueOrNull().orEmpty()

    AlertDialog(
        onDismissRequest = actions::dismissDialog,
        title = { Text(stringResource(R.string.result_details_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (result.overallMessage.isNotBlank()) {
                    Text(result.overallMessage, style = MaterialTheme.typography.bodyMedium)
                }
                if (result.entries.isEmpty()) {
                    Text(stringResource(R.string.result_no_data), style = MaterialTheme.typography.bodyMedium)
                }
                result.entries.forEach { entry ->
                    // Backed-up apps show the catalog's real metadata; failures never borrow an older backup's.
                    val record = if (entry.success) {
                        records.firstOrNull { entry.backupId != null && it.id == entry.backupId }
                            ?: records.firstOrNull { it.packageName == entry.packageName }
                    } else {
                        null
                    }
                    ResultDetailEntry(entry, record)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = actions::showFromResult) {
                Text(stringResource(if (result.type == OpType.BACKUP) R.string.result_show_backups else R.string.result_show_apps))
            }
        },
        dismissButton = {
            TextButton(onClick = actions::dismissDialog) { Text(stringResource(R.string.dialog_close)) }
        },
    )
}

@Composable
private fun ResultDetailEntry(entry: ResultEntry, record: SavedBackup?) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AppIcon(packageName = entry.packageName, label = entry.label, size = 36.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.label, style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
                Text(
                    text = entry.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                painter = painterResource(if (entry.success) R.drawable.ic_check_24 else R.drawable.ic_clear_24),
                contentDescription = stringResource(if (entry.success) R.string.status_succeeded else R.string.status_failed),
                tint = if (entry.success) colors.primary else colors.error,
            )
        }
        if (!entry.success && entry.message.isNotBlank()) ErrorLine(entry.message)
        if (entry.success) {
            if (record != null) {
                DetailLine(R.string.details_version, record.versionName.ifBlank { "\u2013" })
                DetailLine(R.string.details_saved, formatTimestamp(context, record.timestampMs))
                DetailLine(R.string.details_size, formatBytes(record.totalSize))
                Text(
                    text = backupContents(context, record.components, record.encrypted),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
                DetailLine(R.string.details_location, record.backupDir)
            } else {
                Text(
                    text = stringResource(R.string.details_no_metadata),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun DetailLine(labelRes: Int, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(min = 72.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}
