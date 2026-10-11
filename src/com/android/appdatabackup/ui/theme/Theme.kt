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

package com.android.appdatabackup.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Central theme for every screen.
 *
 * [dynamicColor] is off by default so the app keeps its mint-on-near-black identity; turn it
 * on to follow the wallpaper (Monet) palette instead. This theme deliberately uses only
 * stable Material 3 APIs: the Material 3 Expressive theme and components are still marked
 * experimental in the Compose Material 3 release this tree may carry, so the expressive look
 * comes from shapes, typography, color roles and spring motion instead.
 */
@Composable
fun BackupTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkBackupColors
        else -> LightBackupColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = BackupTypography,
        shapes = BackupShapes,
        content = content,
    )
}
