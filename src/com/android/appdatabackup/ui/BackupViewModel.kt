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

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.android.appdatabackup.R
import com.android.appdatabackup.data.BackupRepository
import com.android.appdatabackup.data.InstalledApp
import com.android.appdatabackup.data.OperationListener
import com.android.appdatabackup.data.Phase
import com.android.appdatabackup.data.SavedBackup
import java.util.Locale
import java.util.concurrent.Executors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "AppDataBackupUI"

/**
 * Owns all UI state and is the only thing that talks to [BackupRepository].
 *
 * Threading model: every state mutation happens on the main thread. Blocking engine calls
 * run on a single dedicated thread (so calls are serialised exactly as they were with the
 * old single-thread executor), and the engine's binder callbacks hop to the main thread
 * before touching state. That makes the "is an operation already running?" checks race-free
 * and prevents duplicate operations.
 */
class BackupViewModel(application: Application) : AndroidViewModel(application), BackupActions {

    private val repo = BackupRepository(application)
    private val engineExecutor = Executors.newSingleThreadExecutor { r -> Thread(r, "AppDataBackupEngine") }
    private val engine = engineExecutor.asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(BackupUiState(deviceName = repo.deviceName()))
    val state: StateFlow<BackupUiState> = _state.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events: Flow<UiEvent> = _events.receiveAsFlow()

    private var operationToken: String? = null
    private var cancelRequested = false
    private var verifyJob: Job? = null

    override fun onCleared() {
        scope.cancel()
        engineExecutor.shutdown()
    }

    // ---------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------

    private fun str(id: Int, vararg args: Any): String =
        getApplication<Application>().getString(id, *args)

    private fun emit(id: Int, vararg args: Any) {
        _events.trySend(UiEvent(str(id, *args)))
    }

    private fun errorText(e: Throwable): String =
        e.message?.takeIf { it.isNotBlank() } ?: str(R.string.state_error_title)

    /** Runs a blocking engine call off the main thread and never throws (except cancellation). */
    private suspend fun <T> onEngine(block: () -> T): Result<T> = withContext(engine) {
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Engine call failed", e)
            Result.failure(e)
        }
    }

    private fun BackupUiState.appList(): List<InstalledApp> = apps.valueOrNull().orEmpty()

    private fun BackupUiState.backupList(): List<SavedBackup> = backups.valueOrNull().orEmpty()

    private fun BackupUiState.labels(): Map<String, String> {
        val map = HashMap<String, String>()
        backupList().forEach { map[it.packageName] = it.label }
        appList().forEach { map[it.packageName] = it.label }
        return map
    }

    // ---------------------------------------------------------------------------------
    // Loading
    // ---------------------------------------------------------------------------------

    /** Called when the Activity resumes. Never reloads underneath a running operation. */
    fun refresh() {
        if (_state.value.operation == null) reload()
    }

    override fun reload() {
        loadApps()
        loadBackups()
    }

    private fun loadApps() {
        // Keep showing the current list while refreshing, so selection and scroll survive.
        _state.update { s -> if (s.apps is Loadable.Ready) s else s.copy(apps = Loadable.Loading) }
        scope.launch {
            onEngine { repo.installedApps() }
                .onSuccess { list -> _state.update { it.withApps(list) } }
                .onFailure { e ->
                    _state.update { s ->
                        if (s.apps is Loadable.Ready) s else s.copy(apps = Loadable.Error(errorText(e)))
                    }
                }
        }
    }

    private fun loadBackups() {
        _state.update { s -> if (s.backups is Loadable.Ready) s else s.copy(backups = Loadable.Loading) }
        scope.launch {
            onEngine { repo.availableBackups() }
                .onSuccess { list -> _state.update { it.withBackups(list) } }
                .onFailure { e ->
                    _state.update { s ->
                        if (s.backups is Loadable.Ready) s else s.copy(backups = Loadable.Error(errorText(e)))
                    }
                }
        }
    }

    private fun BackupUiState.withApps(list: List<InstalledApp>): BackupUiState {
        val installed = HashSet<String>()
        list.forEach { installed.add(it.packageName) }
        return copy(
            apps = Loadable.Ready(list),
            // A package that was uninstalled since the last refresh can no longer be selected.
            selectedPackages = selectedPackages.filterTo(HashSet()) { it in installed },
        ).withVisible()
    }

    private fun BackupUiState.withBackups(list: List<SavedBackup>): BackupUiState {
        val sorted = list.sortedByDescending { it.timestampMs }
        val ids = HashSet<String>()
        val latest = HashMap<String, Long>()
        for (b in sorted) {
            ids.add(b.id)
            val prev = latest[b.packageName]
            if (prev == null || b.timestampMs > prev) latest[b.packageName] = b.timestampMs
        }
        return copy(
            backups = Loadable.Ready(sorted),
            lastBackupByPackage = latest,
            selectedBackupIds = selectedBackupIds.filterTo(HashSet()) { it in ids },
            failedVerificationIds = failedVerificationIds.filterTo(HashSet()) { it in ids },
        )
    }

    /** Applies search (label or package name) and sort to the app list. */
    private fun BackupUiState.withVisible(): BackupUiState {
        val all = appList()
        val q = query.trim().lowercase(Locale.getDefault())
        val filtered = if (q.isEmpty()) {
            all
        } else {
            all.filter {
                it.label.lowercase(Locale.getDefault()).contains(q) ||
                    it.packageName.lowercase(Locale.getDefault()).contains(q)
            }
        }
        val sorted = if (sortBySize) {
            filtered.sortedByDescending { it.dataSize }
        } else {
            filtered.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
        }
        return copy(visibleApps = sorted)
    }

    // ---------------------------------------------------------------------------------
    // Navigation
    // ---------------------------------------------------------------------------------

    override fun onBack(): Boolean {
        val s = _state.value
        if (s.operation != null) return true // swallow: use the Cancel button
        return when (s.screen) {
            Screen.DASHBOARD -> false
            Screen.MANAGE -> {
                _state.update { it.copy(screen = Screen.DASHBOARD) }
                true
            }
            Screen.RESULT -> {
                resultDone()
                true
            }
        }
    }

    override fun openManage(tab: ManageTab) {
        _state.update { it.copy(screen = Screen.MANAGE, manageTab = tab) }
    }

    override fun selectTab(tab: ManageTab) {
        _state.update { it.copy(manageTab = tab) }
    }

    override fun resultDone() {
        _state.update { it.copy(result = null, screen = Screen.MANAGE, dialog = null) }
    }

    override fun showResultDetails() {
        _state.update { if (it.result != null) it.copy(dialog = BackupDialog.ResultDetails) else it }
    }

    override fun showFromResult() {
        _state.update { s ->
            val tab = if (s.result?.type == OpType.BACKUP) ManageTab.BACKUPS else ManageTab.APPS
            s.copy(result = null, screen = Screen.MANAGE, manageTab = tab, dialog = null)
        }
    }

    override fun dismissDialog() {
        _state.update { it.copy(dialog = null) }
    }

    // ---------------------------------------------------------------------------------
    // Apps tab: search, sort, selection
    // ---------------------------------------------------------------------------------

    override fun setQuery(query: String) {
        _state.update { it.copy(query = query).withVisible() }
    }

    override fun setSortBySize(bySize: Boolean) {
        _state.update { it.copy(sortBySize = bySize).withVisible() }
    }

    override fun toggleExcludeCache() {
        _state.update { it.copy(excludeCache = !it.excludeCache) }
    }

    override fun toggleApp(packageName: String) {
        _state.update { s ->
            val next = if (packageName in s.selectedPackages) {
                s.selectedPackages - packageName
            } else {
                s.selectedPackages + packageName
            }
            s.copy(selectedPackages = next)
        }
    }

    /** Selects every app in the current search scope; apps hidden by the search stay as they were. */
    override fun selectAllApps() {
        _state.update { s -> s.copy(selectedPackages = s.selectedPackages + s.visibleApps.map { it.packageName }) }
    }

    override fun clearApps() {
        _state.update { it.copy(selectedPackages = emptySet()) }
    }

    // ---------------------------------------------------------------------------------
    // Backup
    // ---------------------------------------------------------------------------------

    override fun backUp() {
        val s = _state.value
        if (s.operation != null || s.apps !is Loadable.Ready) return
        if (s.selectedPackages.isEmpty()) {
            emit(R.string.select_at_least_one)
            return
        }
        _state.update { it.copy(dialog = BackupDialog.BackupOptions) }
    }

    override fun confirmBackup(components: Int, passphrase: String?, keepVersions: Int) {
        val s = _state.value
        if (s.operation != null) return
        val selected = s.appList().filter { it.packageName in s.selectedPackages }
        if (selected.isEmpty()) {
            _state.update { it.copy(dialog = null) }
            emit(R.string.select_at_least_one)
            return
        }
        val request = BackupRequest(
            packages = selected.map { it.packageName },
            components = components,
            passphrase = passphrase,
            keepVersions = keepVersions,
            excludeCache = s.excludeCache,
        )
        val needed = selected.sumOf { maxOf(0L, it.dataSize) }
        _state.update { it.copy(dialog = null) }
        scope.launch {
            val free = onEngine { repo.freeBytes() }.getOrDefault(0L)
            if (free > 0 && needed > free) {
                _state.update { it.copy(dialog = BackupDialog.SpaceWarning(needed, free, request)) }
            } else {
                startBackup(request)
            }
        }
    }

    override fun proceedDespiteLowSpace() {
        val d = _state.value.dialog as? BackupDialog.SpaceWarning ?: return
        startBackup(d.request)
    }

    private fun startBackup(request: BackupRequest) {
        val s = _state.value
        if (s.operation != null) return
        val labels = s.labels()
        val operation = Operation(
            type = OpType.BACKUP,
            packages = request.packages.map { OpPackage(it, labels[it] ?: it) },
        )
        _state.update { it.copy(dialog = null, result = null, operation = operation) }
        resetOperationHandles()
        scope.launch {
            onEngine {
                repo.backup(
                    request.packages,
                    request.excludeCache,
                    request.passphrase,
                    request.components,
                    request.keepVersions,
                    listenerFor(OpType.BACKUP),
                )
            }
                .onSuccess { token -> registerToken(token) }
                .onFailure { e -> failOperation(OpType.BACKUP, e) }
        }
    }

    // ---------------------------------------------------------------------------------
    // Restore
    // ---------------------------------------------------------------------------------

    override fun restore(record: SavedBackup) = requestRestore(listOf(record))

    override fun restoreSelected() {
        val s = _state.value
        requestRestore(s.backupList().filter { it.id in s.selectedBackupIds })
    }

    private fun requestRestore(records: List<SavedBackup>) {
        if (records.isEmpty()) {
            emit(R.string.select_at_least_one_backup)
            return
        }
        if (_state.value.operation != null) return
        scope.launch {
            // Same rule as before: warn when an installed app is newer than the backup.
            val downgrades = onEngine {
                records
                    .filter { r -> repo.installedVersionCode(r.packageName)?.let { it > r.versionCode } == true }
                    .joinToString(", ") { it.label }
            }.getOrDefault("")
            _state.update {
                it.copy(dialog = BackupDialog.RestoreOptions(records, downgrades.ifEmpty { null }))
            }
        }
    }

    override fun confirmRestore(components: Int, passphrase: String?) {
        val d = _state.value.dialog as? BackupDialog.RestoreOptions ?: return
        if (_state.value.operation != null) return
        val records = d.records
        val operation = Operation(
            type = OpType.RESTORE,
            packages = records.map { OpPackage(it.packageName, it.label, backupId = it.id) },
        )
        _state.update { it.copy(dialog = null, result = null, operation = operation) }
        resetOperationHandles()
        scope.launch {
            onEngine {
                repo.restore(
                    records.map { it.id },
                    records.first().backupDir,
                    passphrase,
                    components,
                    listenerFor(OpType.RESTORE),
                )
            }
                .onSuccess { token -> registerToken(token) }
                .onFailure { e -> failOperation(OpType.RESTORE, e) }
        }
    }

    // ---------------------------------------------------------------------------------
    // Operation progress (backup + restore)
    // ---------------------------------------------------------------------------------

    private fun listenerFor(type: OpType) = object : OperationListener {
        override fun onPackageStarted(packageName: String, phase: Phase) {
            scope.launch { markStarted(packageName, phase) }
        }

        override fun onPackageFinished(packageName: String, success: Boolean, message: String?) {
            scope.launch { markFinished(packageName, success, message) }
        }

        override fun onFinished(success: Boolean, message: String?) {
            scope.launch { finishOperation(type, success, message) }
        }

        override fun onCancelled() {
            scope.launch { cancelled(type) }
        }
    }

    private fun resetOperationHandles() {
        operationToken = null
        cancelRequested = false
    }

    private fun registerToken(token: String?) {
        if (_state.value.operation == null) return // already finished before we got the token
        operationToken = token
        if (cancelRequested && token != null) {
            cancelRequested = false
            scope.launch { onEngine { repo.cancel(token) } }
        }
    }

    private fun updateOperation(transform: (Operation) -> Operation) {
        _state.update { s -> s.operation?.let { s.copy(operation = transform(it)) } ?: s }
    }

    private fun phaseText(phase: Phase): String = str(
        when (phase) {
            Phase.BACKING_UP -> R.string.progress_backing_up_data
            Phase.INSTALLING_APK -> R.string.progress_installing_apk
            Phase.RESTORING_DATA -> R.string.progress_restoring_data
        },
    )

    private fun markStarted(packageName: String, phase: Phase) {
        updateOperation { op ->
            op.copy(
                packages = op.packages.map {
                    if (it.packageName == packageName && it.status == PackageStatus.PENDING) {
                        it.copy(status = PackageStatus.RUNNING)
                    } else {
                        it
                    }
                },
                currentPackage = packageName,
                detail = phaseText(phase),
            )
        }
    }

    private fun markFinished(packageName: String, success: Boolean, message: String?) {
        updateOperation { op ->
            op.copy(
                packages = op.packages.map {
                    if (it.packageName == packageName) {
                        it.copy(
                            status = if (success) PackageStatus.SUCCESS else PackageStatus.FAILED,
                            message = message.orEmpty(),
                        )
                    } else {
                        it
                    }
                },
            )
        }
    }

    private fun finishOperation(type: OpType, success: Boolean, message: String?) {
        val op = _state.value.operation ?: return
        if (op.type != type) return
        resetOperationHandles()
        val entries = op.packages
            .filter { it.status == PackageStatus.SUCCESS || it.status == PackageStatus.FAILED }
            .map { ResultEntry(it.packageName, it.label, it.status == PackageStatus.SUCCESS, it.message, it.backupId) }
        val result = OperationResult(type, entries, success, message.orEmpty())
        _state.update {
            it.copy(
                operation = null,
                result = result,
                screen = Screen.RESULT,
                dialog = null,
                selectedBackupIds = if (type == OpType.RESTORE) emptySet() else it.selectedBackupIds,
            )
        }
        loadBackups()
        if (type == OpType.RESTORE) loadApps()
    }

    private fun cancelled(type: OpType) {
        val op = _state.value.operation ?: return
        if (op.type != type) return
        resetOperationHandles()
        _state.update { it.copy(operation = null) }
        emit(if (type == OpType.BACKUP) R.string.backup_cancelled else R.string.restore_cancelled)
        loadBackups()
    }

    /** The engine could not even start the operation: surface that as a failed result. */
    private fun failOperation(type: OpType, e: Throwable) {
        resetOperationHandles()
        _state.update {
            it.copy(
                operation = null,
                result = OperationResult(type, emptyList(), false, errorText(e)),
                screen = Screen.RESULT,
                dialog = null,
            )
        }
    }

    override fun cancelOperation() {
        val op = _state.value.operation ?: return
        if (op.cancelling) return
        if (op.type == OpType.VERIFY) {
            verifyJob?.cancel()
            return
        }
        updateOperation { it.copy(cancelling = true, detail = str(R.string.operation_cancelling)) }
        val token = operationToken
        if (token == null) {
            cancelRequested = true // the token has not come back from the engine yet
        } else {
            scope.launch { onEngine { repo.cancel(token) } }
        }
    }

    // ---------------------------------------------------------------------------------
    // Backups tab: selection, verify, delete, export
    // ---------------------------------------------------------------------------------

    override fun toggleBackup(id: String) {
        _state.update { s ->
            val next = if (id in s.selectedBackupIds) s.selectedBackupIds - id else s.selectedBackupIds + id
            s.copy(selectedBackupIds = next)
        }
    }

    override fun selectAllBackups() {
        _state.update { s -> s.copy(selectedBackupIds = s.backupList().mapTo(HashSet()) { it.id }) }
    }

    override fun clearBackups() {
        _state.update { it.copy(selectedBackupIds = emptySet()) }
    }

    override fun verify(record: SavedBackup) = requestVerify(listOf(record))

    override fun verifyAll() {
        val all = _state.value.backupList()
        if (all.isEmpty()) {
            emit(R.string.no_backups_to_verify)
            return
        }
        requestVerify(all)
    }

    private fun requestVerify(records: List<SavedBackup>) {
        if (_state.value.operation != null) return
        if (records.any { it.encrypted }) {
            _state.update { it.copy(dialog = BackupDialog.VerifyPassphrase(records)) }
        } else {
            startVerify(records, null)
        }
    }

    override fun confirmVerify(passphrase: String?) {
        val d = _state.value.dialog as? BackupDialog.VerifyPassphrase ?: return
        startVerify(d.records, passphrase)
    }

    private fun startVerify(records: List<SavedBackup>, passphrase: String?) {
        if (_state.value.operation != null || records.isEmpty()) return
        val bulk = records.size > 1
        _state.update {
            it.copy(
                dialog = null,
                operation = Operation(
                    type = OpType.VERIFY,
                    detail = str(R.string.progress_verifying, records.first().label),
                ),
            )
        }
        verifyJob = scope.launch {
            var valid = 0
            var failed = 0
            var skipped = 0
            var lastError: String? = null
            val good = HashSet<String>()
            val bad = HashSet<String>()
            try {
                for (r in records) {
                    ensureActive()
                    if (r.encrypted && passphrase == null) {
                        skipped++
                        continue
                    }
                    updateOperation { it.copy(detail = str(R.string.progress_verifying, r.label)) }
                    val error: String? = onEngine {
                        repo.verify(r.id, r.backupDir, if (r.encrypted) passphrase else null)
                    }.getOrElse { e -> errorText(e) }
                    if (error == null) {
                        valid++
                        good.add(r.id)
                    } else {
                        failed++
                        bad.add(r.id)
                        lastError = error
                    }
                }
            } finally {
                _state.update { it.copy(operation = null) }
            }
            _state.update { s -> s.copy(failedVerificationIds = (s.failedVerificationIds - good) + bad) }
            when {
                bulk -> emit(R.string.verify_all_result, valid, failed, skipped)
                failed == 0 && valid == 1 -> emit(R.string.verify_ok)
                failed > 0 -> emit(R.string.verify_failed, lastError.orEmpty())
                else -> Unit // everything was skipped (encrypted, no passphrase)
            }
        }
    }

    override fun delete(record: SavedBackup) {
        _state.update { it.copy(dialog = BackupDialog.ConfirmDelete(listOf(record))) }
    }

    override fun deleteSelected() {
        val s = _state.value
        val records = s.backupList().filter { it.id in s.selectedBackupIds }
        if (records.isEmpty()) {
            emit(R.string.select_at_least_one_backup)
            return
        }
        _state.update { it.copy(dialog = BackupDialog.ConfirmDelete(records)) }
    }

    override fun confirmDelete() {
        val d = _state.value.dialog as? BackupDialog.ConfirmDelete ?: return
        _state.update { it.copy(dialog = null) }
        scope.launch {
            val ids = d.records.mapTo(HashSet()) { it.id }
            onEngine { d.records.forEach { repo.delete(it.id, it.backupDir) } }
                .onFailure { e -> _events.trySend(UiEvent(errorText(e))) }
            _state.update { it.copy(selectedBackupIds = it.selectedBackupIds - ids) }
            loadBackups()
        }
    }

    override fun export(record: SavedBackup) {
        _state.update { it.copy(dialog = BackupDialog.ConfirmExport(record)) }
    }

    override fun confirmExport() {
        val d = _state.value.dialog as? BackupDialog.ConfirmExport ?: return
        _state.update { it.copy(dialog = null) }
        scope.launch {
            val path = onEngine { repo.export(d.record.id) }.getOrNull()
            if (path != null) emit(R.string.export_done, path) else emit(R.string.export_failed)
        }
    }
}
