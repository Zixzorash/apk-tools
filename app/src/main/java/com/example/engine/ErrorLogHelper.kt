package com.example.engine

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.example.model.KeystoreConfig
import com.example.model.ProjectMetadata
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ErrorLogHelper {

    fun generateAndSaveErrorLog(
        context: Context,
        metadata: ProjectMetadata,
        keystoreConfig: KeystoreConfig,
        logs: List<String>,
        throwable: Throwable
    ): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val readableDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss (z)", Locale.getDefault()).format(Date())

        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val stackTraceString = sw.toString()

        val sb = StringBuilder()
        sb.appendLine("================================================================================")
        sb.appendLine("                 APK BUILDER PRO - AUTOMATIC BUILD ERROR LOG")
        sb.appendLine("================================================================================")
        sb.appendLine("บันทึกเมื่อ (Timestamp) : $readableDate")
        sb.appendLine("ชื่อแอพ (App Name)     : ${metadata.appName}")
        sb.appendLine("แพ็กเกจ (Package)      : ${metadata.packageName}")
        sb.appendLine("เวอร์ชัน (Version)     : ${metadata.versionName} (Code: ${metadata.versionCode})")
        sb.appendLine("Target SDK             : API Level ${metadata.targetSdk} (Min SDK: ${metadata.minSdk})")
        sb.appendLine("ประเภทโปรเจกต์          : ${metadata.frameworkType.displayName}")
        sb.appendLine("โหมดการลงชื่อ (Signing) : ${keystoreConfig.mode.displayName}")
        sb.appendLine("สิทธิ์ที่ขอ (Permissions): ${metadata.detectedPermissions.joinToString(", ")}")
        sb.appendLine("--------------------------------------------------------------------------------")
        sb.appendLine("ข้อมูลอุปกรณ์และระบบ (Device & Environment):")
        sb.appendLine("  Manufacturer : ${Build.MANUFACTURER}")
        sb.appendLine("  Model        : ${Build.MODEL}")
        sb.appendLine("  Device       : ${Build.DEVICE}")
        sb.appendLine("  Android OS   : Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        sb.appendLine("  Supported ABIs: ${Build.SUPPORTED_ABIS.joinToString(", ")}")
        sb.appendLine("--------------------------------------------------------------------------------")
        sb.appendLine("สรุปข้อผิดพลาด (ERROR SUMMARY):")
        sb.appendLine(throwable.localizedMessage ?: throwable.message ?: "Unknown Exception")
        sb.appendLine("--------------------------------------------------------------------------------")
        sb.appendLine("ลำดับขั้นตอนการทำงาน (EXECUTION TERMINAL LOGS):")
        if (logs.isEmpty()) {
            sb.appendLine("  [No execution logs recorded before error occurred]")
        } else {
            logs.forEach { log -> sb.appendLine("  $log") }
        }
        sb.appendLine("--------------------------------------------------------------------------------")
        sb.appendLine("STACK TRACE รายละเอียดทางเทคนิค:")
        sb.appendLine(stackTraceString)
        sb.appendLine("================================================================================")
        sb.appendLine("คำแนะนำการแก้ไขอัตโนมัติ:")
        sb.appendLine("1. ตรวจสอบ syntax ใน build.gradle หรือใช้ฟีเจอร์ 'ซ่อมแซม build.gradle อัตโนมัติ'")
        sb.appendLine("2. ตรวจสอบว่าไฟล์ Keystore มี Alias และรหัสผ่านที่ถูกต้อง")
        sb.appendLine("3. ไฟล์ชั่วคราวและแคชทั้งหมดถูกล้างอัตโนมัติเพื่อป้องกันไฟล์ตกค้าง")
        sb.appendLine("================================================================================")

        // Save to device storage: getExternalFilesDir("logs")
        val logsDir = File(context.getExternalFilesDir(null), "logs").apply { mkdirs() }
        val logFile = File(logsDir, "build_error_${timeStamp}.log")
        logFile.writeText(sb.toString(), Charsets.UTF_8)

        return logFile
    }

    fun shareLogFile(context: Context, logFilePath: String) {
        val file = File(logFilePath)
        if (!file.exists()) return

        val uri: Uri = try {
            FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        } catch (_: Exception) {
            Uri.fromFile(file)
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "APK Builder Pro Error Log - ${file.name}")
            putExtra(Intent.EXTRA_TEXT, "รายงานข้อผิดพลาดจากการสร้าง APK (${file.name}):\n\n${file.readText().take(500)}...")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(shareIntent, "แชร์ไฟล์ Error Log").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun readLogContent(logFilePath: String): String {
        val file = File(logFilePath)
        return if (file.exists()) {
            file.readText(Charsets.UTF_8)
        } else {
            "ไม่พบไฟล์ log ที่ตำแหน่ง: $logFilePath"
        }
    }
}
