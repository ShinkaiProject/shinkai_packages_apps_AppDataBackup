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

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.appdatabackup.ui.BackupViewModel
import com.android.appdatabackup.ui.Screen
import com.android.appdatabackup.ui.backup.components.BackupDialogHost
import com.android.appdatabackup.ui.backup.components.ProgressOverlay

/**
 * Root composable: owns navigation between the three screens, the progress overlay, dialogs,
 * back handling and one-shot messages. State is collected with lifecycle awareness, so
 * nothing is observed while the app is in the background.
 */
@Composable
fun BackupApp(
    viewModel: BackupViewModel,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { snackbar.showSnackbar(it.message) }
    }

    // Always intercepted: the ViewModel decides whether to go up a screen, ignore (busy) or leave.
    BackHandler {
        if (!viewModel.onBack()) onExit()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Crossfade(targetState = state.screen, label = "screen") { screen ->
            when (screen) {
                Screen.DASHBOARD -> BackupScreen(state = state, actions = viewModel)
                Screen.MANAGE -> AppDataBackupScreen(state = state, actions = viewModel)
                Screen.RESULT -> BackupSuccessScreen(state = state, actions = viewModel)
            }
        }

        BackupDialogHost(state = state, actions = viewModel)

        state.operation?.let { operation ->
            ProgressOverlay(operation = operation, onCancel = viewModel::cancelOperation)
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                // Clear of the floating action bar on the manage screen.
                .padding(bottom = if (state.screen == Screen.MANAGE) 128.dp else 16.dp),
        )
    }
}
