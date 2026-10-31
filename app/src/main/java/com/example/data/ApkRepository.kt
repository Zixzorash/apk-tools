package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.flow.Flow
import java.io.File

class ApkRepository(
    private val apkDao: ApkDao,
    private val context: Context
) {
    val allConvertedApks: Flow<List<ConvertedApkEntity>> = apkDao.getAllConvertedApks()

    suspend fun saveConvertedApk(entity: ConvertedApkEntity): Long {
        return apkDao.insertApk(entity)
    }

    suspend fun deleteApk(id: Long, filePath: String) {
        apkDao.deleteApkById(id)
        val file = File(filePath)
        if (file.exists()) {
            file.delete()
        }
    }

    suspend fun clearHistory() {
        apkDao.clearAllHistory()
    }

    fun installApk(filePath: String) {
        val file = File(filePath)
        if (!file.exists()) return

        val apkUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun shareApk(filePath: String) {
        val file = File(filePath)
        if (!file.exists()) return

        val apkUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.android.package-archive"
            putExtra(Intent.EXTRA_STREAM, apkUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share APK / XAPK Package"))
    }
}
