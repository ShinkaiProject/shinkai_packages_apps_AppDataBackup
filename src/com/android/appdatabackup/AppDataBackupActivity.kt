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

package com.android.appdatabackup

import android.os.Bundle
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.lifecycle.ViewModelProvider
import com.android.appdatabackup.ui.BackupViewModel
import com.android.appdatabackup.ui.backup.BackupApp
import com.android.appdatabackup.ui.theme.BackupTheme

/**
 * Single entry point of the app (the launcher / Settings intent filters in the manifest still
 * point here). It hosts the whole Compose UI; the former BackupManageActivity and
 * OperationResultActivity are now destinations inside [BackupApp].
 */
class AppDataBackupActivity : ComponentActivity() {

    private lateinit var viewModel: BackupViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge: content draws under transparent system bars and every screen applies
        // the status/navigation/IME insets itself.
        window.setDecorFitsSystemWindows(false)
        window.isStatusBarContrastEnforced = false
        window.isNavigationBarContrastEnforced = false

        viewModel = ViewModelProvider(this)[BackupViewModel::class.java]

        setContent {
            val darkTheme = isSystemInDarkTheme()
            SideEffect { updateSystemBarIcons(darkTheme) }
            BackupTheme(darkTheme = darkTheme) {
                BackupApp(viewModel = viewModel, onExit = { finish() })
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Same refresh points as before: the dashboard re-read the catalog on every resume.
        viewModel.refresh()
    }

    private fun updateSystemBarIcons(darkTheme: Boolean) {
        val controller = window.insetsController ?: return
        val mask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
            WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
        controller.setSystemBarsAppearance(if (darkTheme) 0 else mask, mask)
    }
}
