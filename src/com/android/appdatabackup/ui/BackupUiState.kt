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

package com.android.appdatabackup.ui

import com.android.appdatabackup.data.InstalledApp
import com.android.appdatabackup.data.SavedBackup

enum class Screen { DASHBOARD, MANAGE, RESULT }

enum class ManageTab { APPS, BACKUPS }

enum class OpType { BACKUP, RESTORE, VERIFY }

enum class PackageStatus { PENDING, RUNNING, SUCCESS, FAILED }

enum class Outcome { SUCCESS, PARTIAL, FAILED, UNKNOWN }

/** Loading / error / content wrapper for anything that comes from the engine. */
sealed interface Loadable<out T> {
    object Loading : Loadable<Nothing>
    data class Error(val message: String) : Loadable<Nothing>
    data class Ready<out T>(val value: T) : Loadable<T>
}

fun <T> Loadable<T>.valueOrNull(): T? = (this as? Loadable.Ready<T>)?.value

/** One package inside a running backup / restore. */
data class OpPackage(
    val packageName: String,
    val label: String,
    val status: PackageStatus = PackageStatus.PENDING,
    val message: String = "",
    /** The backup id being restored, when this is a restore. */
    val backupId: String? = null,
)

/** A running backup, restore or verification. Non-null in the UI state means "busy". */
data class Operation(
    val type: OpType,
    val packages: List<OpPackage> = emptyList(),
    val currentPackage: String? = null,
    val detail: String? = null,
    val cancelling: Boolean = false,
) {
    val total: Int get() = packages.size
    val finished: Int
        get() = packages.count { it.status == PackageStatus.SUCCESS || it.status == PackageStatus.FAILED }
}

data class ResultEntry(
    val packageName: String,
    val label: String,
    val success: Boolean,
    val message: String,
    val backupId: String?,
)

/** What the engine actually reported for the operation that just finished. */
data class OperationResult(
    val type: OpType,
    val entries: List<ResultEntry>,
    /** The engine's own overall verdict (or the per-package verdict if it sent none). */
    val overallSuccess: Boolean,
    val overallMessage: String,
) {
    val okCount: Int = entries.count { it.success }
    val failCount: Int = entries.size - okCount

    /** Never [Outcome.SUCCESS] unless every reported package and the engine agree. */
    val outcome: Outcome
        get() = when {
            entries.isEmpty() -> if (overallSuccess) Outcome.UNKNOWN else Outcome.FAILED
            failCount == 0 && overallSuccess -> Outcome.SUCCESS
            okCount == 0 -> Outcome.FAILED
            else -> Outcome.PARTIAL
        }
}

/**
 * Everything needed to start a backup. Holds the passphrase in memory only; [toString] is
 * deliberately redacted so it can never leak through logging.
 */
data class BackupRequest(
    val packages: List<String>,
    val components: Int,
    val passphrase: String?,
    val keepVersions: Int,
    val excludeCache: Boolean,
) {
    override fun toString(): String = "BackupRequest(packages=$packages)"
}

sealed interface BackupDialog {
    object BackupOptions : BackupDialog
    data class SpaceWarning(val needed: Long, val free: Long, val request: BackupRequest) : BackupDialog
    data class RestoreOptions(val records: List<SavedBackup>, val downgradeWarning: String?) : BackupDialog
    data class VerifyPassphrase(val records: List<SavedBackup>) : BackupDialog
    data class ConfirmDelete(val records: List<SavedBackup>) : BackupDialog
    data class ConfirmExport(val record: SavedBackup) : BackupDialog
    object ResultDetails : BackupDialog
}

/** A one-shot message for the snackbar. */
data class UiEvent(val message: String)

data class BackupUiState(
    val screen: Screen = Screen.DASHBOARD,
    val manageTab: ManageTab = ManageTab.APPS,
    val deviceName: String = "",

    val apps: Loadable<List<InstalledApp>> = Loadable.Loading,
    val backups: Loadable<List<SavedBackup>> = Loadable.Loading,

    /** [apps] after search and sort; recomputed only when one of those inputs changes. */
    val visibleApps: List<InstalledApp> = emptyList(),
    val lastBackupByPackage: Map<String, Long> = emptyMap(),

    val query: String = "",
    val sortBySize: Boolean = false,
    val excludeCache: Boolean = false,

    val selectedPackages: Set<String> = emptySet(),
    val selectedBackupIds: Set<String> = emptySet(),
    val failedVerificationIds: Set<String> = emptySet(),

    val operation: Operation? = null,
    val result: OperationResult? = null,
    val dialog: BackupDialog? = null,
) {
    val isBusy: Boolean get() = operation != null
}
