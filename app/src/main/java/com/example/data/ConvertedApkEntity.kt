package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "converted_apks")
data class ConvertedApkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Int,
    val frameworkType: String,
    val outputType: String,
    val filePath: String,
    val fileSize: Long,
    val buildTimestamp: Long = System.currentTimeMillis(),
    val verificationPassed: Boolean,
    val signatureSHA256: String,
    val sourceFileName: String,
    val iconBase64: String? = null
)
