package com.swordfish.lemuroid.lib.storage

import android.content.Context
import android.os.Environment
import java.io.File

class DirectoriesManager(private val appContext: Context) {
    // Prefer a folder in public storage (Download/EmuSaves) so saves and states
    // can be synced with standard tools (Syncthing, adb, MTP, file managers).
    // Writing there requires legacy WRITE_EXTERNAL_STORAGE (up to Android 10)
    // or "All files access" (Android 11+, MANAGE_EXTERNAL_STORAGE appop); when
    // neither is granted we silently fall back to the app-private directory,
    // so the app keeps working on unmodified devices.
    private fun getSyncableDirectory(name: String): File {
        val publicDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "EmuSaves/$name",
        )
        if (publicDir.isDirectory || publicDir.mkdirs()) {
            val writable = runCatching {
                val probe = File(publicDir, ".write_probe")
                probe.createNewFile() && probe.delete()
            }.getOrDefault(false)
            if (writable) return publicDir
        }
        return File(appContext.getExternalFilesDir(null), name).apply { mkdirs() }
    }

    @Deprecated("Use the external states directory")
    fun getInternalStatesDirectory(): File =
        File(appContext.filesDir, "states").apply {
            mkdirs()
        }

    fun getCoresDirectory(): File =
        File(appContext.filesDir, "cores").apply {
            mkdirs()
        }

    fun getSystemDirectory(): File =
        File(appContext.filesDir, "system").apply {
            mkdirs()
        }

    fun getStatesDirectory(): File = getSyncableDirectory("states")

    fun getStatesPreviewDirectory(): File = getSyncableDirectory("state-previews")

    fun getSavesDirectory(): File = getSyncableDirectory("saves")

    fun getInternalRomsDirectory(): File =
        File(appContext.getExternalFilesDir(null), "roms").apply {
            mkdirs()
        }
}
