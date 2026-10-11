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

package com.android.appdatabackup.data

import android.app.appbackup.AppDataBackupRestoreManager
import android.app.appbackup.BackupResult
import android.app.appbackup.IBackupProgressCallback
import android.app.appbackup.IRestoreProgressCallback
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.StatFs
import android.os.UserHandle
import android.provider.Settings
import android.util.Log
import com.android.appdatabackup.R
import java.io.File
import java.util.Locale

private const val TAG = "AppDataBackupUI"

/** An installed app as reported by the backup engine. [dataSize] is negative when unknown. */
data class InstalledApp(
    val packageName: String,
    val label: String,
    val versionName: String,
    val dataSize: Long,
    val optsOutOfBackup: Boolean,
)

/** One saved backup as reported by the engine's catalog. */
data class SavedBackup(
    val id: String,
    val packageName: String,
    val label: String,
    val versionName: String,
    val versionCode: Long,
    val timestampMs: Long,
    val totalSize: Long,
    val backupDir: String,
    val encrypted: Boolean,
    val components: Int,
)

/** Coarse phase of the package that is currently being processed. */
enum class Phase { BACKING_UP, INSTALLING_APK, RESTORING_DATA }

/**
 * Framework-free view of the engine's progress callbacks. Methods are invoked on a binder
 * thread; implementers must hop to their own thread before touching UI state.
 */
interface OperationListener {
    fun onPackageStarted(packageName: String, phase: Phase)
    fun onPackageFinished(packageName: String, success: Boolean, message: String?)
    fun onFinished(success: Boolean, message: String?)
    fun onCancelled()
}

/** Component bit flags understood by the engine. */
object BackupComponents {
    val APK: Int = AppDataBackupRestoreManager.COMPONENT_APK
    val CE_DATA: Int = AppDataBackupRestoreManager.COMPONENT_CE_DATA
    val DE_DATA: Int = AppDataBackupRestoreManager.COMPONENT_DE_DATA
    val EXTERNAL: Int = AppDataBackupRestoreManager.COMPONENT_EXTERNAL
}

/**
 * The only class in the UI layer's dependency graph that touches
 * [AppDataBackupRestoreManager]. Every method is a blocking call that mirrors what the
 * previous View-based Activities did, with identical arguments, directories and flags, so
 * the on-disk backup format and engine behaviour are unchanged. Call from a background
 * thread.
 */
class BackupRepository(context: Context) {

    private val appContext = context.applicationContext
    private val manager: AppDataBackupRestoreManager? =
        appContext.getSystemService(Context.APP_DATA_BACKUP_SERVICE) as? AppDataBackupRestoreManager
    private val backupDir = File("/data/media/" + UserHandle.myUserId() + "/AppDataBackup")

    private fun requireManager(): AppDataBackupRestoreManager =
        manager ?: throw IllegalStateException(appContext.getString(R.string.backup_unavailable))

    fun installedApps(): List<InstalledApp> {
        val engine = requireManager()
        val apps = engine.getInstalledApps().orEmpty()
        val optOut = HashSet<String>()
        for (ai in appContext.packageManager.getInstalledApplications(0)) {
            if ((ai.flags and ApplicationInfo.FLAG_ALLOW_BACKUP) == 0) optOut.add(ai.packageName)
        }
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "Loaded ${apps.size} apps for backup UI")
        return apps.map { info ->
            val pkg: String = info.getPackageName()
            InstalledApp(
                packageName = pkg,
                label = info.getLabel() ?: pkg,
                versionName = info.getVersionName() ?: "",
                dataSize = info.getDataSize().toLong(),
                optsOutOfBackup = pkg in optOut,
            )
        }
    }

    fun availableBackups(): List<SavedBackup> {
        val engine = requireManager()
        backupDir.mkdirs()
        val records = engine.getAvailableBackups(backupDir.absolutePath).orEmpty()
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "Loaded ${records.size} backups from $backupDir")
        return records.map { r ->
            val pkg: String = r.getPackageName()
            SavedBackup(
                id = r.getId(),
                packageName = pkg,
                label = r.getLabel() ?: pkg,
                versionName = r.getVersionName() ?: "",
                versionCode = r.getVersionCode().toLong(),
                timestampMs = r.getTimestampMs().toLong(),
                totalSize = r.getTotalSize().toLong(),
                backupDir = r.getBackupDir(),
                encrypted = r.isEncrypted(),
                components = r.getComponents(),
            )
        }
    }

    /** Starts an asynchronous backup. Returns the operation token used for [cancel]. */
    fun backup(
        packages: List<String>,
        excludeCache: Boolean,
        passphrase: String?,
        components: Int,
        keepVersions: Int,
        listener: OperationListener,
    ): String? = requireManager().backupPackages(
        packages,
        backupDir.absolutePath,
        excludeCache,
        object : IBackupProgressCallback.Stub() {
            override fun onBackupStarted(token: String?, total: Int) {}

            override fun onPackageBackupStarted(token: String?, pkg: String?, idx: Int, total: Int) {
                if (pkg != null) listener.onPackageStarted(pkg, Phase.BACKING_UP)
            }

            override fun onPackageBackupFinished(token: String?, pkg: String?, result: BackupResult?) {
                if (pkg == null) return
                val ok = result?.isSuccess() ?: false
                if (!ok) Log.w(TAG, "Backup failed for $pkg: ${result?.getMessage()}")
                listener.onPackageFinished(pkg, ok, result?.getMessage())
            }

            override fun onBackupFinished(token: String?, result: BackupResult?) {
                listener.onFinished(result?.isSuccess() ?: false, result?.getMessage())
            }

            override fun onBackupCancelled(token: String?) {
                listener.onCancelled()
            }
        },
        passphrase,
        components,
        keepVersions,
    )

    /** Starts an asynchronous restore of the given backup ids. Returns the operation token. */
    fun restore(
        ids: List<String>,
        backupDir: String,
        passphrase: String?,
        components: Int,
        listener: OperationListener,
    ): String? = requireManager().restorePackages(
        ids,
        backupDir,
        object : IRestoreProgressCallback.Stub() {
            override fun onRestoreStarted(token: String?, total: Int) {}

            override fun onPackageRestoreStarted(token: String?, pkg: String?, idx: Int, total: Int) {
                if (pkg != null) listener.onPackageStarted(pkg, Phase.INSTALLING_APK)
            }

            override fun onPackageDataRestoring(token: String?, pkg: String?) {
                if (pkg != null) listener.onPackageStarted(pkg, Phase.RESTORING_DATA)
            }

            override fun onPackageRestoreFinished(token: String?, pkg: String?, result: BackupResult?) {
                if (pkg == null) return
                listener.onPackageFinished(pkg, result?.isSuccess() ?: false, result?.getMessage())
            }

            override fun onRestoreFinished(token: String?, result: BackupResult?) {
                listener.onFinished(result?.isSuccess() ?: false, result?.getMessage())
            }

            override fun onRestoreCancelled(token: String?) {
                listener.onCancelled()
            }
        },
        passphrase,
        components,
    )

    fun cancel(token: String) {
        requireManager().cancelOperation(token)
    }

    /** Returns null when the backup is intact, otherwise the engine's error message. */
    fun verify(id: String, backupDir: String, passphrase: String?): String? =
        requireManager().verifyBackup(id, backupDir, passphrase)

    fun delete(id: String, backupDir: String) {
        requireManager().deleteBackup(id, backupDir)
    }

    /** Returns the exported path, or null on failure. */
    fun export(id: String): String? = requireManager().exportBackup(id)

    fun freeBytes(): Long = try {
        StatFs("/data").availableBytes
    } catch (e: Exception) {
        Log.w(TAG, "statfs failed", e)
        0L
    }

    fun installedVersionCode(packageName: String): Long? = try {
        appContext.packageManager.getPackageInfo(packageName, 0).longVersionCode
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }

    /** Same lookup order as the previous dashboard: user-set name, market name, model. */
    fun deviceName(): String {
        val deviceName = Settings.Global.getString(appContext.contentResolver, Settings.Global.DEVICE_NAME)
        if (!deviceName.isNullOrBlank()) return deviceName

        val marketName = systemProperty("ro.product.marketname", "")
        if (marketName.isNotBlank()) return marketName

        val model = systemProperty("ro.product.model", Build.MODEL)
        val manufacturer = Build.MANUFACTURER
        return if (!model.lowercase(Locale.getDefault()).startsWith(manufacturer.lowercase(Locale.getDefault()))) {
            manufacturer.replaceFirstChar { it.uppercaseChar() } + " " + model
        } else {
            model
        }
    }

    private fun systemProperty(key: String, default: String): String = try {
        val cls = Class.forName("android.os.SystemProperties")
        val get = cls.getMethod("get", String::class.java, String::class.java)
        get.invoke(cls, key, default) as String
    } catch (e: Exception) {
        default
    }
}
