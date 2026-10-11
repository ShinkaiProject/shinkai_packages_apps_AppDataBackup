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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.Alignment
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.android.appdatabackup.R
import com.android.appdatabackup.data.InstalledApp
import com.android.appdatabackup.ui.BackupActions
import com.android.appdatabackup.ui.BackupUiState
import com.android.appdatabackup.ui.Loadable
import com.android.appdatabackup.ui.ManageTab
import com.android.appdatabackup.ui.backup.components.AppSearchRow
import com.android.appdatabackup.ui.backup.components.AppsBottomActions
import com.android.appdatabackup.ui.backup.components.BackupAppItem
import com.android.appdatabackup.ui.backup.components.BackupRecordItem
import com.android.appdatabackup.ui.backup.components.BackupTabs
import com.android.appdatabackup.ui.backup.components.BackupTopBar
import com.android.appdatabackup.ui.backup.components.BackupsBottomActions
import com.android.appdatabackup.ui.backup.components.SectionHeader
import com.android.appdatabackup.ui.backup.components.SelectionSummaryCard
import com.android.appdatabackup.ui.backup.components.StateMessage
import com.android.appdatabackup.ui.valueOrNull

/**
 * "App Data Backup": heading, live selection summary, pinned Apps / Backups tabs, the
 * searchable app list or the saved-backup list, and floating bottom actions. The lists are
 * LazyColumns with stable keys (package name / backup id), so selection, scroll position and
 * animations survive refreshes and package changes.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppDataBackupScreen(
    state: BackupUiState,
    actions: BackupActions,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val installed = state.apps.valueOrNull()
    val selectedCount = state.selectedPackages.size
    val selectedSize = remember(state.selectedPackages, installed) {
        installed.orEmpty()
            .filter { it.packageName in state.selectedPackages }
            .sumOf { maxOf(0L, it.dataSize) }
    }
    // Split once per change of the visible list, not on every selection toggle.
    val sections = remember(state.visibleApps, state.lastBackupByPackage) {
        state.visibleApps.partition { it.packageName !in state.lastBackupByPackage }
    }
    val notBackedUp = sections.first
    val backedUp = sections.second

    val density = LocalDensity.current
    // Height of the floating action bar (including the navigation-bar inset), measured so the
    // list can scroll underneath it yet still stop above it at the very end.
    var actionBarHeightPx by remember { mutableStateOf(0) }
    val actionBarHeight = with(density) { actionBarHeightPx.toDp() }

    Column(modifier = modifier.fillMaxSize().imePadding()) {
        BackupTopBar(
            title = stringResource(R.string.app_name),
            onBack = { actions.onBack() },
        )
        // The list fills the whole area and the action bar floats above it, so no opaque
        // background sits behind the bar: list items stay visible around and below it.
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 4.dp,
                    bottom = 16.dp + actionBarHeight,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "header", contentType = "header") {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        // The heading's tight line height let its descenders touch the next line.
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.backup_headline),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text(
                            text = stringResource(R.string.backup_subheadline),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                item(key = "summary", contentType = "summary") {
                    SelectionSummaryCard(count = selectedCount, sizeBytes = selectedSize)
                }
                stickyHeader(key = "tabs") {
                    BackupTabs(selected = state.manageTab, onSelect = actions::selectTab)
                }
                when (state.manageTab) {
                    ManageTab.APPS -> appsTab(state, actions, notBackedUp, backedUp)
                    ManageTab.BACKUPS -> backupsTab(state, actions)
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { actionBarHeightPx = it.height },
            ) {
                when (state.manageTab) {
                    ManageTab.APPS -> AppsBottomActions(
                        selectedCount = selectedCount,
                        excludeCache = state.excludeCache,
                        canSelectAll = state.visibleApps.isNotEmpty(),
                        enabled = !state.isBusy && state.apps is Loadable.Ready,
                        onToggleExcludeCache = actions::toggleExcludeCache,
                        onSelectAll = actions::selectAllApps,
                        onClear = actions::clearApps,
                        onBackUp = actions::backUp,
                    )
                    ManageTab.BACKUPS -> BackupsBottomActions(
                        selectedCount = state.selectedBackupIds.size,
                        hasBackups = state.backups.valueOrNull().orEmpty().isNotEmpty(),
                        enabled = !state.isBusy,
                        onSelectAll = actions::selectAllBackups,
                        onVerifyAll = actions::verifyAll,
                        onClear = actions::clearBackups,
                        onDeleteSelected = actions::deleteSelected,
                        onRestore = actions::restoreSelected,
                    )
                }
            }
        }
    }
}

private fun LazyListScope.appsTab(
    state: BackupUiState,
    actions: BackupActions,
    notBackedUp: List<InstalledApp>,
    backedUp: List<InstalledApp>,
) {
    item(key = "search", contentType = "search") {
        AppSearchRow(
            query = state.query,
            sortBySize = state.sortBySize,
            onQueryChange = actions::setQuery,
            onSortBySizeChange = actions::setSortBySize,
        )
    }
    when (val apps = state.apps) {
        is Loadable.Loading -> item(key = "apps-loading", contentType = "state") {
            StateMessage(title = stringResource(R.string.processing_title), loading = true)
        }
        is Loadable.Error -> item(key = "apps-error", contentType = "state") {
            StateMessage(
                title = stringResource(R.string.state_error_title),
                body = apps.message,
                actionLabel = stringResource(R.string.action_retry),
                onAction = actions::reload,
            )
        }
        is Loadable.Ready -> {
            if (state.visibleApps.isEmpty()) {
                item(key = "apps-empty", contentType = "state") {
                    StateMessage(
                        title = if (state.query.isBlank()) {
                            stringResource(R.string.apps_empty)
                        } else {
                            stringResource(R.string.apps_empty_search, state.query.trim())
                        },
                    )
                }
            } else {
                appSection(state, actions, "not-backed-up", R.string.section_not_backed_up, notBackedUp)
                appSection(state, actions, "backed-up", R.string.section_already_backed_up, backedUp)
            }
        }
    }
}

private fun LazyListScope.appSection(
    state: BackupUiState,
    actions: BackupActions,
    id: String,
    titleRes: Int,
    apps: List<InstalledApp>,
) {
    if (apps.isEmpty()) return
    item(key = "header-$id", contentType = "section") {
        SectionHeader(stringResource(titleRes, apps.size))
    }
    items(apps, key = { it.packageName }, contentType = { "app" }) { app ->
        BackupAppItem(
            app = app,
            selected = app.packageName in state.selectedPackages,
            lastBackupMs = state.lastBackupByPackage[app.packageName],
            onToggle = { actions.toggleApp(app.packageName) },
        )
    }
}

private fun LazyListScope.backupsTab(state: BackupUiState, actions: BackupActions) {
    when (val backups = state.backups) {
        is Loadable.Loading -> item(key = "backups-loading", contentType = "state") {
            StateMessage(title = stringResource(R.string.processing_title), loading = true)
        }
        is Loadable.Error -> item(key = "backups-error", contentType = "state") {
            StateMessage(
                title = stringResource(R.string.state_error_title),
                body = backups.message,
                actionLabel = stringResource(R.string.action_retry),
                onAction = actions::reload,
            )
        }
        is Loadable.Ready -> {
            if (backups.value.isEmpty()) {
                item(key = "backups-empty", contentType = "state") {
                    StateMessage(
                        title = stringResource(R.string.backups_empty_title),
                        body = stringResource(R.string.backups_empty_body),
                    )
                }
            } else {
                items(backups.value, key = { it.id }, contentType = { "backup" }) { record ->
                    BackupRecordItem(
                        record = record,
                        selected = record.id in state.selectedBackupIds,
                        verificationFailed = record.id in state.failedVerificationIds,
                        onToggle = { actions.toggleBackup(record.id) },
                        onRestore = { actions.restore(record) },
                        onVerify = { actions.verify(record) },
                        onExport = { actions.export(record) },
                        onDelete = { actions.delete(record) },
                    )
                }
            }
        }
    }
}
