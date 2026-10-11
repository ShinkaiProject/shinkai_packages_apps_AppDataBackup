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

import com.android.appdatabackup.data.SavedBackup

/**
 * Everything the composables can ask for. Screens depend on this interface rather than on
 * the ViewModel, so they can be previewed and tested with a fake.
 */
interface BackupActions {
    /** Handles a system/app back press. Returns false when the host should leave the app. */
    fun onBack(): Boolean
    fun reload()

    // Navigation
    fun openManage(tab: ManageTab)
    fun selectTab(tab: ManageTab)

    // Apps tab
    fun setQuery(query: String)
    fun setSortBySize(bySize: Boolean)
    fun toggleExcludeCache()
    fun toggleApp(packageName: String)
    fun selectAllApps()
    fun clearApps()
    fun backUp()

    // Backups tab
    fun toggleBackup(id: String)
    fun selectAllBackups()
    fun clearBackups()
    fun verifyAll()
    fun restoreSelected()
    fun deleteSelected()
    fun restore(record: SavedBackup)
    fun verify(record: SavedBackup)
    fun export(record: SavedBackup)
    fun delete(record: SavedBackup)

    // Running operation and its result
    fun cancelOperation()
    fun resultDone()
    fun showResultDetails()
    fun showFromResult()

    // Dialog callbacks
    fun dismissDialog()
    fun confirmBackup(components: Int, passphrase: String?, keepVersions: Int)
    fun proceedDespiteLowSpace()
    fun confirmRestore(components: Int, passphrase: String?)
    fun confirmVerify(passphrase: String?)
    fun confirmDelete()
    fun confirmExport()
}
