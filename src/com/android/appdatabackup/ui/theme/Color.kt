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

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

internal val AmoledBackupColors = darkColorScheme(
    primary = Color(0xFF7CE3B0),
    onPrimary = Color(0xFF00382A),
    primaryContainer = Color(0xFF1E4D38),
    onPrimaryContainer = Color(0xFFB5F2D2),
    inversePrimary = Color(0xFF0B6B49),
    secondary = Color(0xFF9FCBB2),
    onSecondary = Color(0xFF06372A),
    secondaryContainer = Color(0xFF27402F),
    onSecondaryContainer = Color(0xFFCDE9D8),
    tertiary = Color(0xFFE6C36A),
    onTertiary = Color(0xFF3F2E00),
    tertiaryContainer = Color(0xFF5A4400),
    onTertiaryContainer = Color(0xFFFFE08E),
    background = Color(0xFF0A0E0C),
    onBackground = Color(0xFFE1E9E4),
    surface = Color(0xFF0A0E0C),
    onSurface = Color(0xFFE1E9E4),
    surfaceVariant = Color(0xFF25332B),
    onSurfaceVariant = Color(0xFFB0C0B6),
    surfaceTint = Color(0xFF7CE3B0),
    inverseSurface = Color(0xFFE1E9E4),
    inverseOnSurface = Color(0xFF2A332E),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF5C1A15),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF7A8D82),
    outlineVariant = Color(0xFF33453B),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF2A3A31),
    surfaceDim = Color(0xFF0A0E0C),
    surfaceContainerLowest = Color(0xFF060907),
    surfaceContainerLow = Color(0xFF101813),
    surfaceContainer = Color(0xFF15211A),
    surfaceContainerHigh = Color(0xFF1B2A21),
    surfaceContainerHighest = Color(0xFF22342A),
)

/**
 * Default dark scheme: the same mint-on-green identity as [AmoledBackupColors], but the canvas
 * and card surfaces are lifted from near-black to a dark green-tinted gray, so it no longer
 * reads as pure AMOLED. Only the neutral surface roles (plus the two container roles that sit
 * on them) differ; accents and text colors are shared.
 */
internal val DarkBackupColors = AmoledBackupColors.copy(
    background = Color(0xFF131A16),
    surface = Color(0xFF131A16),
    surfaceDim = Color(0xFF131A16),
    surfaceBright = Color(0xFF34443B),
    surfaceVariant = Color(0xFF33443A),
    surfaceContainerLowest = Color(0xFF0E1411),
    surfaceContainerLow = Color(0xFF18211C),
    surfaceContainer = Color(0xFF1E2923),
    surfaceContainerHigh = Color(0xFF26332C),
    surfaceContainerHighest = Color(0xFF2E3D35),
    secondaryContainer = Color(0xFF2C4A38),
    outlineVariant = Color(0xFF435A4C),
)

internal val LightBackupColors = lightColorScheme(
    primary = Color(0xFF0B6B49),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFA6F5CC),
    onPrimaryContainer = Color(0xFF002115),
    inversePrimary = Color(0xFF7CE3B0),
    secondary = Color(0xFF4D6357),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD0E8D8),
    onSecondaryContainer = Color(0xFF0A1F14),
    tertiary = Color(0xFF7A5900),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDF9A),
    onTertiaryContainer = Color(0xFF251A00),
    background = Color(0xFFF5FBF6),
    onBackground = Color(0xFF171D19),
    surface = Color(0xFFF5FBF6),
    onSurface = Color(0xFF171D19),
    surfaceVariant = Color(0xFFDBE5DD),
    onSurfaceVariant = Color(0xFF404943),
    surfaceTint = Color(0xFF0B6B49),
    inverseSurface = Color(0xFF2C322E),
    inverseOnSurface = Color(0xFFECF2ED),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF707972),
    outlineVariant = Color(0xFFBFC9C1),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFF5FBF6),
    surfaceDim = Color(0xFFD5DCD6),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEFF5F0),
    surfaceContainer = Color(0xFFE9F0EA),
    surfaceContainerHigh = Color(0xFFDEE9E1),
    surfaceContainerHighest = Color(0xFFD8E3DB),
)
