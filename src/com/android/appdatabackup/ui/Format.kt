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

import android.content.Context
import android.text.format.DateUtils
import com.android.appdatabackup.R
import com.android.appdatabackup.data.BackupComponents
import java.util.Locale

/** Same thresholds and rounding as the previous UI. Negative means "unknown". */
fun formatBytes(bytes: Long): String {
    if (bytes < 0) return "?"
    if (bytes < 1024) return "$bytes B"
    if (bytes < 1024 * 1024) return String.format(Locale.US, "%.1f KB", bytes / 1024.0)
    if (bytes < 1024L * 1024 * 1024) return String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024))
    return String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024 * 1024))
}

/** Locale-aware short date and time, e.g. "Oct 3, 14:20". */
fun formatTimestamp(context: Context, timestampMs: Long): String = DateUtils.formatDateTime(
    context,
    timestampMs,
    DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH or DateUtils.FORMAT_SHOW_TIME,
)

/** "Includes App (APK) · App data · ... · Encrypted", as the old Backups list showed it. */
fun backupContents(context: Context, components: Int, encrypted: Boolean): String {
    val parts = ArrayList<String>()
    if ((components and BackupComponents.APK) != 0) parts.add(context.getString(R.string.content_apk))
    if ((components and BackupComponents.CE_DATA) != 0) parts.add(context.getString(R.string.content_app_data))
    if ((components and BackupComponents.DE_DATA) != 0) parts.add(context.getString(R.string.content_device_data))
    if ((components and BackupComponents.EXTERNAL) != 0) parts.add(context.getString(R.string.content_external))
    parts.add(context.getString(R.string.content_permissions))
    var includes = context.getString(R.string.content_includes, parts.joinToString("  \u00b7  "))
    if (encrypted) includes += "  \u00b7  " + context.getString(R.string.content_encrypted)
    return includes
}
