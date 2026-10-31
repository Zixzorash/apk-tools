package com.example.engine

import android.content.Context
import android.content.SharedPreferences
import com.example.model.KeystoreConfig
import com.example.model.SigningMode
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object KeystorePersistenceManager {

    private const val PREFS_NAME = "apk_builder_keystore_prefs"
    private const val KEY_IS_REMEMBERED = "is_remembered"
    private const val KEY_MODE = "signing_mode"
    private const val KEY_ALIAS = "key_alias"
    private const val KEY_STORE_PASS = "store_password"
    private const val KEY_KEY_PASS = "key_password"
    private const val KEY_FILE_NAME = "file_name"
    private const val KEY_HAS_FILE = "has_custom_file"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveKeystoreConfig(context: Context, config: KeystoreConfig, fileName: String? = null) {
        val prefs = getPrefs(context)
        val editor = prefs.edit()

        editor.putBoolean(KEY_IS_REMEMBERED, true)
        editor.putString(KEY_MODE, config.mode.name)
        editor.putString(KEY_ALIAS, config.alias)
        editor.putString(KEY_STORE_PASS, config.storePassword)
        editor.putString(KEY_KEY_PASS, config.keyPassword)

        if (!fileName.isNullOrBlank()) {
            editor.putString(KEY_FILE_NAME, fileName)
        }

        // If custom keystore file exists, copy it to private internal filesDir
        if (config.customFile != null && config.customFile.exists()) {
            try {
                val destDir = File(context.filesDir, "saved_keystores").apply { mkdirs() }
                val persistentFile = File(destDir, "remembered_keystore.bin")
                FileInputStream(config.customFile).use { input ->
                    FileOutputStream(persistentFile).use { output -> input.copyTo(output) }
                }
                editor.putBoolean(KEY_HAS_FILE, true)
            } catch (_: Exception) {
                editor.putBoolean(KEY_HAS_FILE, false)
            }
        }

        editor.apply()
    }

    fun loadRememberedKeystore(context: Context): RememberedKeystoreData? {
        val prefs = getPrefs(context)
        val isRemembered = prefs.getBoolean(KEY_IS_REMEMBERED, false)
        if (!isRemembered) return null

        val modeName = prefs.getString(KEY_MODE, SigningMode.DEBUG_KEYSTORE.name) ?: SigningMode.DEBUG_KEYSTORE.name
        val mode = try {
            SigningMode.valueOf(modeName)
        } catch (_: Exception) {
            SigningMode.DEBUG_KEYSTORE
        }

        val alias = prefs.getString(KEY_ALIAS, "") ?: ""
        val storePass = prefs.getString(KEY_STORE_PASS, "") ?: ""
        val keyPass = prefs.getString(KEY_KEY_PASS, "") ?: ""
        val fileName = prefs.getString(KEY_FILE_NAME, "remembered.keystore") ?: "remembered.keystore"
        val hasFile = prefs.getBoolean(KEY_HAS_FILE, false)

        var file: File? = null
        if (hasFile) {
            val destDir = File(context.filesDir, "saved_keystores")
            val persistentFile = File(destDir, "remembered_keystore.bin")
            if (persistentFile.exists()) {
                file = persistentFile
            }
        }

        return RememberedKeystoreData(
            config = KeystoreConfig(
                mode = mode,
                customFile = file,
                alias = alias,
                storePassword = storePass,
                keyPassword = keyPass,
                isRemembered = true
            ),
            fileName = fileName
        )
    }

    fun clearRememberedKeystore(context: Context) {
        val prefs = getPrefs(context)
        prefs.edit().clear().apply()

        // Also delete saved keystore file
        try {
            val destDir = File(context.filesDir, "saved_keystores")
            if (destDir.exists()) destDir.deleteRecursively()
        } catch (_: Exception) {}
    }

    data class RememberedKeystoreData(
        val config: KeystoreConfig,
        val fileName: String
    )
}
