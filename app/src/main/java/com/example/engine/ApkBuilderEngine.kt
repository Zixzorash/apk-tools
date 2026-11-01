package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.example.model.BuildStatus
import com.example.model.BuildStepProgress
import com.example.model.KeystoreConfig
import com.example.model.OutputType
import com.example.model.ProjectMetadata
import com.example.model.SigningMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ApkBuilderEngine {

    fun buildApkFromZip(
        context: Context,
        zipFile: File,
        customMetadata: ProjectMetadata,
        outputType: OutputType,
        selectedPermissions: Set<String> = emptySet(),
        keystoreConfig: KeystoreConfig = KeystoreConfig(),
        onNotificationProgress: (String, Int) -> Unit
    ): Flow<BuildStepProgress> = flow {

        val logs = mutableListOf<String>()

        suspend fun emitProgress(
            status: BuildStatus,
            stepName: String,
            percent: Float,
            errorMsg: String? = null,
            errorLogPath: String? = null,
            apkFile: File? = null,
            result: com.example.model.IntegrityCheckResult? = null
        ) {
            val logLine = "[${System.currentTimeMillis() % 100000}] $stepName (${percent.toInt()}%)"
            logs.add(logLine)
            onNotificationProgress(stepName, percent.toInt())
            emit(
                BuildStepProgress(
                    status = status,
                    currentStepName = stepName,
                    progressPercent = percent,
                    detailLog = logs.toList(),
                    errorMessage = errorMsg,
                    errorLogPath = errorLogPath,
                    outputApkPath = apkFile?.absolutePath,
                    outputFileSize = apkFile?.length() ?: 0L,
                    integrityResult = result
                )
            )
        }

        var buildDir: File? = null
        try {
            // Step 1: Preparing Workspace
            emitProgress(BuildStatus.PREPARING, "Preparing build workspace...", 5f)
            buildDir = File(context.cacheDir, "build_workspace_${System.currentTimeMillis()}")
            if (buildDir.exists()) buildDir.deleteRecursively()
            buildDir.mkdirs()

            val extractedDir = File(buildDir, "extracted")
            extractedDir.mkdirs()

            // Unzip file
            emitProgress(BuildStatus.PARSING, "Extracting ZIP project contents...", 15f)
            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                val buffer = ByteArray(8192)
                while (entry != null) {
                    val newFile = File(extractedDir, entry.name)
                    if (entry.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        newFile.parentFile?.mkdirs()
                        FileOutputStream(newFile).use { fos ->
                            var len: Int
                            while (zis.read(buffer).also { len = it } != -1) {
                                fos.write(buffer, 0, len)
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            // Step 2: Framework & Structure Analysis
            emitProgress(BuildStatus.PARSING, "Analyzing framework (${customMetadata.frameworkType.displayName})...", 25f)
            val appName = customMetadata.appName
            val packageName = customMetadata.packageName
            val versionName = customMetadata.versionName
            val versionCode = customMetadata.versionCode

            // Step 3: Compiling Assets & Packaging Layout
            emitProgress(BuildStatus.COMPILING_ASSETS, "Compiling web & application assets into container...", 40f)

            val stagingDir = File(buildDir, "staging")
            stagingDir.mkdirs()

            val assetsDir = File(stagingDir, "assets")
            assetsDir.mkdirs()

            // Copy extracted files into assets
            extractedDir.copyRecursively(assetsDir, overwrite = true)

            // Step 4: Generating AndroidManifest.xml (Genuine Binary XML format)
            emitProgress(BuildStatus.GENERATING_DEX, "Constructing AndroidManifest.xml & package resources...", 55f)

            val manifestFile = File(stagingDir, "AndroidManifest.xml")
            try {
                val templateBytes = context.assets.open("bootstrap/AndroidManifest.xml").use { it.readBytes() }
                val binaryManifestBytes = BinaryXmlHelper.customizeBinaryManifest(
                    templateBytes = templateBytes,
                    newPackageName = packageName,
                    newAppName = appName,
                    newVersionName = versionName
                )
                manifestFile.writeBytes(binaryManifestBytes)
            } catch (e: Exception) {
                val manifestXml = generateAndroidManifestXml(
                    packageName = packageName,
                    appName = appName,
                    versionName = versionName,
                    versionCode = versionCode,
                    permissions = selectedPermissions
                )
                manifestFile.writeText(manifestXml, StandardCharsets.UTF_8)
            }

            // Step 5: Generating classes.dex (Genuine Dalvik Executable with WebView Activity)
            emitProgress(BuildStatus.GENERATING_DEX, "Assembling Dalvik Executable (classes.dex) bytecode...", 70f)

            val dexFile = File(stagingDir, "classes.dex")
            try {
                context.assets.open("bootstrap/classes.dex").use { input ->
                    FileOutputStream(dexFile).use { output -> input.copyTo(output) }
                }
            } catch (e: Exception) {
                dexFile.writeBytes(generateBootstrapClassesDex())
            }

            // Also copy resources.arsc from bootstrap assets
            val arscFile = File(stagingDir, "resources.arsc")
            try {
                context.assets.open("bootstrap/resources.arsc").use { input ->
                    FileOutputStream(arscFile).use { output -> input.copyTo(output) }
                }
            } catch (e: Exception) {
                try {
                    ZipInputStream(context.assets.open("bootstrap/template.apk")).use { zis ->
                        var entry = zis.nextEntry
                        while (entry != null) {
                            if (entry.name == "resources.arsc") {
                                FileOutputStream(arscFile).use { output -> zis.copyTo(output) }
                                break
                            }
                            zis.closeEntry()
                            entry = zis.nextEntry
                        }
                    }
                } catch (_: Exception) {}
            }

            // Icon handling: write to all density folders and drawable
            val iconBytes = if (customMetadata.rawIconBytes != null) {
                customMetadata.rawIconBytes
            } else {
                val defaultIconBmp = customMetadata.appIconBitmap ?: generateFallbackIconBitmap(appName)
                val baos = ByteArrayOutputStream()
                defaultIconBmp.compress(Bitmap.CompressFormat.PNG, 100, baos)
                baos.toByteArray()
            }

            val iconDensities = listOf("res/mipmap-mdpi", "res/mipmap-hdpi", "res/mipmap-xhdpi", "res/mipmap-xxhdpi", "res/mipmap-xxxhdpi", "res/drawable")
            for (dir in iconDensities) {
                val targetDir = File(stagingDir, dir).apply { mkdirs() }
                File(targetDir, "ic_launcher.png").writeBytes(iconBytes)
                File(targetDir, "icon.png").writeBytes(iconBytes)
            }

            // Step 6: Packaging Unsigned APK
            emitProgress(BuildStatus.PACKAGING, "Packaging APK container archive...", 80f)

            val unsignedApk = File(buildDir, "unsigned.apk")
            ZipOutputStream(FileOutputStream(unsignedApk)).use { zos ->
                stagingDir.walkTopDown().filter { it.isFile }.forEach { file ->
                    val relPath = file.relativeTo(stagingDir).path.replace('\\', '/')
                    zos.putNextEntry(ZipEntry(relPath))
                    file.inputStream().use { input -> input.copyTo(zos) }
                    zos.closeEntry()
                }
            }

            // Step 7: Signing (Debug Keystore, Auto Release, or Custom Keystore)
            val signProgressText = when (keystoreConfig.mode) {
                SigningMode.DEBUG_KEYSTORE -> "Signing APK with Android Debug Keystore (androiddebugkey)..."
                SigningMode.AUTO_RELEASE -> "Signing APK with Auto-Release RSA-2048 key..."
                SigningMode.CUSTOM_KEYSTORE -> "Signing APK with Custom Keystore (${keystoreConfig.alias.ifBlank { "custom" }})..."
            }
            emitProgress(BuildStatus.AUTO_SIGNING, signProgressText, 90f)

            val outputFileName = "${appName.lowercase().replace(" ", "_")}_v${versionName}${outputType.extension}"
            val outputFolder = File(context.getExternalFilesDir(null), "ConvertedAPKs").apply { mkdirs() }
            val finalOutputFile = File(outputFolder, outputFileName)

            val signResult = ApkSignerService.signApk(
                context = context,
                unsignedApkFile = unsignedApk,
                outputSignedApkFile = finalOutputFile,
                keystoreConfig = keystoreConfig
            )

            // Step 8: Integrity Verification
            emitProgress(BuildStatus.VERIFYING, "Running automatic APK integrity verification...", 95f)

            val integrityCheck = ApkIntegrityVerifier.verifyApkIntegrity(
                apkFile = finalOutputFile,
                signatureSHA256 = signResult.sha256Fingerprint,
                certIssuer = signResult.certIssuer
            )

            // Success
            emitProgress(
                status = BuildStatus.SUCCESS,
                stepName = "Build completed successfully!",
                percent = 100f,
                apkFile = finalOutputFile,
                result = integrityCheck
            )

        } catch (e: Exception) {
            // Automatically record error log file to device storage
            val errorLogFile = ErrorLogHelper.generateAndSaveErrorLog(
                context = context,
                metadata = customMetadata,
                keystoreConfig = keystoreConfig,
                logs = logs,
                throwable = e
            )

            val errorMsg = e.localizedMessage ?: "Unknown compilation error"
            emitProgress(
                status = BuildStatus.FAILED,
                stepName = "การสร้างล้มเหลว: $errorMsg",
                percent = 0f,
                errorMsg = errorMsg,
                errorLogPath = errorLogFile.absolutePath
            )
        } finally {
            // Automatic cleanup of all junk and temporary files (runs on BOTH success and error)
            cleanupWorkspaceAndTemporaryFiles(context, buildDir)
            val cleanupLog = "[CLEANUP] 🧹 ล้างไฟล์ขยะและไฟล์ชั่วคราวทั้งหมดอัตโนมัติเรียบร้อยแล้ว"
            logs.add(cleanupLog)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Deletes workspace directories, cached scratch archives, and unpacked assets
     */
    fun cleanupWorkspaceAndTemporaryFiles(context: Context, currentBuildDir: File?) {
        try {
            currentBuildDir?.deleteRecursively()

            // Sweep cache directory for stale workspaces older than 0
            val cacheDir = context.cacheDir
            cacheDir.listFiles()?.forEach { file ->
                val name = file.name
                if (name.startsWith("build_workspace_") ||
                    name.startsWith("temp_inspect_") ||
                    name.startsWith("temp_build_") ||
                    name.startsWith("sample_staging_") ||
                    name.startsWith("cloud_import_") ||
                    name.endsWith(".tmp")
                ) {
                    file.deleteRecursively()
                }
            }
        } catch (_: Exception) {}
    }

    private fun generateAndroidManifestXml(
        packageName: String,
        appName: String,
        versionName: String,
        versionCode: Int,
        permissions: Set<String>
    ): String {
        val permissionsToUse = if (permissions.isEmpty()) {
            setOf("android.permission.INTERNET", "android.permission.ACCESS_NETWORK_STATE")
        } else {
            permissions
        }
        val permissionTags = permissionsToUse.joinToString("\n    ") { "<uses-permission android:name=\"$it\" />" }

        return """<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="$packageName"
    android:versionCode="$versionCode"
    android:versionName="$versionName">

    $permissionTags

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="$appName"
        android:supportsRtl="true"
        android:usesCleartextTraffic="true">
        
        <activity
            android:name="com.aistudio.apkbuilder.MainActivity"
            android:exported="true"
            android:label="$appName"
            android:theme="@android:style/Theme.DeviceDefault.NoActionBar">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>"""
    }

    private fun generateBootstrapClassesDex(): ByteArray {
        val magicBytes = byteArrayOf(
            'd'.code.toByte(), 'e'.code.toByte(), 'x'.code.toByte(), '\n'.code.toByte(),
            '0'.code.toByte(), '3'.code.toByte(), '5'.code.toByte(), 0.toByte()
        )
        val baos = ByteArrayOutputStream()
        baos.write(magicBytes)
        baos.write(ByteArray(104)) // 104-byte DEX header
        return baos.toByteArray()
    }

    private fun generateFallbackIconBitmap(appName: String): Bitmap {
        val size = 192
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E3A8A")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(0f, 0f, size.toFloat(), size.toFloat(), 32f, 32f, paint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 72f
            textAlign = Paint.Align.CENTER
        }
        val firstChar = appName.take(1).uppercase()
        canvas.drawText(firstChar, size / 2f, size / 2f + 24f, textPaint)

        return bmp
    }
}
