package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ApkRepository
import com.example.data.ConvertedApkEntity
import com.example.engine.ApkBuilderEngine
import com.example.engine.MetadataExtractor
import com.example.model.BuildStatus
import com.example.model.BuildStepProgress
import com.example.model.CloudProvider
import com.example.model.FrameworkType
import com.example.model.IntegrityCheckResult
import com.example.model.OutputType
import com.example.model.ProjectMetadata
import com.example.model.SampleProject
import com.example.service.BuildNotificationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ApkRepository
    val historyList: StateFlow<List<ConvertedApkEntity>>

    init {
        val db = AppDatabase.getInstance(application)
        repository = ApkRepository(db.apkDao(), application)
        historyList = repository.allConvertedApks.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        BuildNotificationManager.createNotificationChannel(application)
        loadSavedKeystoreIfExists()
    }

    // Active zip file & metadata
    private val _selectedZipFile = MutableStateFlow<File?>(null)
    val selectedZipFile: StateFlow<File?> = _selectedZipFile.asStateFlow()

    private val _projectMetadata = MutableStateFlow<ProjectMetadata?>(null)
    val projectMetadata: StateFlow<ProjectMetadata?> = _projectMetadata.asStateFlow()

    private val _selectedOutputType = MutableStateFlow(OutputType.APK)
    val selectedOutputType: StateFlow<OutputType> = _selectedOutputType.asStateFlow()

    // Language State
    private val _currentLanguage = MutableStateFlow(com.example.model.AppLanguage.TH)
    val currentLanguage: StateFlow<com.example.model.AppLanguage> = _currentLanguage.asStateFlow()

    // Form inputs matching reference UI
    val sourceTab = MutableStateFlow(0)
    val configSubTab = MutableStateFlow(0)
    val appNameInput = MutableStateFlow("APK Builder Pro")
    val packageNameInput = MutableStateFlow("com.apkbuilder.pro")
    val versionNameInput = MutableStateFlow("1.0.0")
    val versionCodeInput = MutableStateFlow("1")
    val targetSdkInput = MutableStateFlow("API Level 36 (Android 16 - ล่าสุด)")
    val hardwareAccelEnabled = MutableStateFlow(true)

    // Keystore / Signing State
    val selectedSigningMode = MutableStateFlow(com.example.model.SigningMode.DEBUG_KEYSTORE)
    val keystoreFileName = MutableStateFlow("ไม่ได้เลือกไฟล์ใด")
    val keystoreAliasInput = MutableStateFlow("")
    val keystoreStorePasswordInput = MutableStateFlow("")
    val keystoreKeyPasswordInput = MutableStateFlow("")
    val isUsingCustomKeystore = MutableStateFlow(false)
    val rememberKeystoreChecked = MutableStateFlow(true)
    val rememberedKeystoreStatus = MutableStateFlow<String?>(null)
    private val customKeystoreFile = MutableStateFlow<File?>(null)

    // APK Signature Schemes V1, V2, V3, V4
    val signV1 = MutableStateFlow(true)
    val signV2 = MutableStateFlow(true)
    val signV3 = MutableStateFlow(true)
    val signV4 = MutableStateFlow(true)

    // Installer Name Spoofing (Default: com.android.vending)
    val installerPackageNameInput = MutableStateFlow("com.android.vending")
    val isInstallerSpoofEnabled = MutableStateFlow(true)

    // Project Validation Inspection
    val showValidationDetails = MutableStateFlow(false)

    fun toggleSignV1(enabled: Boolean) { signV1.value = enabled }
    fun toggleSignV2(enabled: Boolean) { signV2.value = enabled }
    fun toggleSignV3(enabled: Boolean) { signV3.value = enabled }
    fun toggleSignV4(enabled: Boolean) { signV4.value = enabled }

    private fun loadSavedKeystoreIfExists() {
        val context = getApplication<Application>()
        val remembered = com.example.engine.KeystorePersistenceManager.loadRememberedKeystore(context)
        if (remembered != null) {
            val cfg = remembered.config
            selectedSigningMode.value = cfg.mode
            isUsingCustomKeystore.value = (cfg.mode == com.example.model.SigningMode.CUSTOM_KEYSTORE)
            keystoreFileName.value = remembered.fileName
            keystoreAliasInput.value = cfg.alias
            keystoreStorePasswordInput.value = cfg.storePassword
            keystoreKeyPasswordInput.value = cfg.keyPassword
            customKeystoreFile.value = cfg.customFile
            rememberKeystoreChecked.value = true
            rememberedKeystoreStatus.value = "จดจำ Keystore แล้ว: ${cfg.alias.ifBlank { remembered.fileName }}"
        }
    }

    fun selectSigningMode(mode: com.example.model.SigningMode) {
        selectedSigningMode.value = mode
        isUsingCustomKeystore.value = (mode == com.example.model.SigningMode.CUSTOM_KEYSTORE)
    }

    fun selectKeystoreUri(uri: Uri, fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val cacheKeystore = File(context.cacheDir, "keystore_${System.currentTimeMillis()}_$fileName")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(cacheKeystore).use { output -> input.copyTo(output) }
                }
                customKeystoreFile.value = cacheKeystore
                keystoreFileName.value = fileName
                selectedSigningMode.value = com.example.model.SigningMode.CUSTOM_KEYSTORE
                isUsingCustomKeystore.value = true

                if (rememberKeystoreChecked.value) {
                    val config = com.example.model.KeystoreConfig(
                        mode = com.example.model.SigningMode.CUSTOM_KEYSTORE,
                        customFile = cacheKeystore,
                        alias = keystoreAliasInput.value,
                        storePassword = keystoreStorePasswordInput.value,
                        keyPassword = keystoreKeyPasswordInput.value,
                        isRemembered = true
                    )
                    com.example.engine.KeystorePersistenceManager.saveKeystoreConfig(context, config, fileName)
                    rememberedKeystoreStatus.value = "จดจำ Keystore แล้ว: $fileName"
                }
            } catch (e: Exception) {
                _downloadError.value = "Failed to load keystore: ${e.localizedMessage}"
            }
        }
    }

    fun applyCustomKeystore() {
        selectedSigningMode.value = com.example.model.SigningMode.CUSTOM_KEYSTORE
        isUsingCustomKeystore.value = true
        val context = getApplication<Application>()
        if (rememberKeystoreChecked.value) {
            val config = com.example.model.KeystoreConfig(
                mode = com.example.model.SigningMode.CUSTOM_KEYSTORE,
                customFile = customKeystoreFile.value,
                alias = keystoreAliasInput.value,
                storePassword = keystoreStorePasswordInput.value,
                keyPassword = keystoreKeyPasswordInput.value,
                isRemembered = true
            )
            com.example.engine.KeystorePersistenceManager.saveKeystoreConfig(context, config, keystoreFileName.value)
            rememberedKeystoreStatus.value = "บันทึกและจดจำ Keystore แล้ว (${keystoreAliasInput.value.ifBlank { keystoreFileName.value }})"
        }
    }

    fun clearRememberedKeystore() {
        val context = getApplication<Application>()
        com.example.engine.KeystorePersistenceManager.clearRememberedKeystore(context)
        rememberedKeystoreStatus.value = null
        resetToAutoKeystore()
    }

    fun resetToAutoKeystore() {
        selectedSigningMode.value = com.example.model.SigningMode.AUTO_RELEASE
        isUsingCustomKeystore.value = false
        customKeystoreFile.value = null
        keystoreFileName.value = "ไม่ได้เลือกไฟล์ใด"
        keystoreAliasInput.value = ""
        keystoreStorePasswordInput.value = ""
        keystoreKeyPasswordInput.value = ""
    }

    fun useDebugKeystore() {
        selectedSigningMode.value = com.example.model.SigningMode.DEBUG_KEYSTORE
        isUsingCustomKeystore.value = false
        if (rememberKeystoreChecked.value) {
            val context = getApplication<Application>()
            val config = com.example.model.KeystoreConfig(
                mode = com.example.model.SigningMode.DEBUG_KEYSTORE,
                alias = "androiddebugkey",
                storePassword = "android",
                isRemembered = true
            )
            com.example.engine.KeystorePersistenceManager.saveKeystoreConfig(context, config, "debug.keystore")
            rememberedKeystoreStatus.value = "จดจำ Android Debug Keystore แล้ว"
        }
    }

    // App Permissions State
    data class AppPermissionItem(
        val key: String,
        val titleTh: String,
        val subtitleEn: String,
        val isRequired: Boolean = false
    )

    val availablePermissions = listOf(
        AppPermissionItem("android.permission.INTERNET", "อินเทอร์เน็ต (จำเป็น)", "INTERNET", isRequired = true),
        AppPermissionItem("android.permission.ACCESS_NETWORK_STATE", "ดูสถานะเครือข่าย", "ACCESS_NETWORK_STATE"),
        AppPermissionItem("android.permission.CAMERA", "กล้อง", "CAMERA"),
        AppPermissionItem("android.permission.RECORD_AUDIO", "ไมโครโฟน", "RECORD_AUDIO"),
        AppPermissionItem("android.permission.ACCESS_FINE_LOCATION", "ตำแหน่งที่แม่นยำ", "ACCESS_FINE_LOCATION"),
        AppPermissionItem("android.permission.VIBRATE", "การสั่น", "VIBRATE"),
        AppPermissionItem("android.permission.READ_MEDIA_IMAGES", "อ่านรูปภาพ", "READ_MEDIA_IMAGES")
    )

    val selectedPermissions = MutableStateFlow<Set<String>>(
        setOf("android.permission.INTERNET")
    )

    fun togglePermission(key: String) {
        val current = selectedPermissions.value.toMutableSet()
        val item = availablePermissions.find { it.key == key }
        if (item?.isRequired == true) return // Required permissions cannot be unchecked

        if (current.contains(key)) {
            current.remove(key)
        } else {
            current.add(key)
        }
        selectedPermissions.value = current
    }

    fun toggleLanguage() {
        _currentLanguage.value = if (_currentLanguage.value == com.example.model.AppLanguage.TH) com.example.model.AppLanguage.EN else com.example.model.AppLanguage.TH
    }

    fun setLanguage(language: com.example.model.AppLanguage) {
        _currentLanguage.value = language
    }

    // Build Progress State
    private val _buildProgress = MutableStateFlow(BuildStepProgress())
    val buildProgress: StateFlow<BuildStepProgress> = _buildProgress.asStateFlow()

    // UI Dialog Toggles
    private val _showCodeEditor = MutableStateFlow(false)
    val showCodeEditor: StateFlow<Boolean> = _showCodeEditor.asStateFlow()

    private val _showCloudImport = MutableStateFlow(false)
    val showCloudImport: StateFlow<Boolean> = _showCloudImport.asStateFlow()

    private val _showIntegrityReport = MutableStateFlow<IntegrityCheckResult?>(null)
    val showIntegrityReport: StateFlow<IntegrityCheckResult?> = _showIntegrityReport.asStateFlow()

    private val _isDownloadingCloud = MutableStateFlow(false)
    val isDownloadingCloud: StateFlow<Boolean> = _isDownloadingCloud.asStateFlow()

    private val _downloadError = MutableStateFlow<String?>(null)
    val downloadError: StateFlow<String?> = _downloadError.asStateFlow()

    // App Gallery Samples
    val sampleProjects = listOf(
        SampleProject(
            id = "ai_studio_react",
            name = "AI Studio React App",
            description = "Complete React + Vite app with metadata.json, Material UI, and responsive layout",
            framework = FrameworkType.GOOGLE_AI_STUDIO,
            defaultPackageName = "com.aistudio.reactapp",
            defaultVersion = "1.0.0"
        ),
        SampleProject(
            id = "native_android_starter",
            name = "Native Android Starter",
            description = "Standard Gradle Kotlin Android project with AndroidManifest.xml and material components",
            framework = FrameworkType.NATIVE_ANDROID,
            defaultPackageName = "com.aistudio.nativeapp",
            defaultVersion = "1.2.0"
        ),
        SampleProject(
            id = "html5_pwa_hybrid",
            name = "HTML5 PWA Hybrid Game",
            description = "Canvas-based web application with index.html, local offline assets, and sounds",
            framework = FrameworkType.HTML5_HYBRID,
            defaultPackageName = "com.aistudio.html5game",
            defaultVersion = "2.0.0"
        ),
        SampleProject(
            id = "flutter_counter",
            name = "Flutter Cross-Platform",
            description = "Flutter Dart project with pubspec.yaml and cross-platform native widgets",
            framework = FrameworkType.FLUTTER,
            defaultPackageName = "com.aistudio.flutterapp",
            defaultVersion = "1.0.1"
        ),
        SampleProject(
            id = "capacitor_mobile",
            name = "Ionic / Capacitor Mobile",
            description = "Capacitor web-native mobile shell with capacitor.config.json and native plugins",
            framework = FrameworkType.IONIC_CAPACITOR,
            defaultPackageName = "com.aistudio.ionicapp",
            defaultVersion = "3.1.0"
        )
    )

    fun selectOutputType(type: OutputType) {
        _selectedOutputType.value = type
    }

    fun openCodeEditor() {
        _showCodeEditor.value = true
    }

    fun closeCodeEditor() {
        _showCodeEditor.value = false
    }

    fun openCloudImport() {
        _downloadError.value = null
        _showCloudImport.value = true
    }

    fun closeCloudImport() {
        _showCloudImport.value = false
    }

    fun openIntegrityReport(result: IntegrityCheckResult) {
        _showIntegrityReport.value = result
    }

    fun closeIntegrityReport() {
        _showIntegrityReport.value = null
    }

    fun updateMetadata(newMetadata: ProjectMetadata) {
        _projectMetadata.value = newMetadata
    }

    fun processSelectedZipUri(uri: Uri, fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val cacheZip = File(context.cacheDir, "imported_${System.currentTimeMillis()}_$fileName")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(cacheZip).use { output -> input.copyTo(output) }
                }
                processZipFile(cacheZip, fileName)
            } catch (e: Exception) {
                _downloadError.value = "Failed to load zip file: ${e.localizedMessage}"
            }
        }
    }

    fun loadSampleProject(sample: SampleProject) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val cacheZip = File(context.cacheDir, "${sample.id}_sample.zip")

                // Create synthetic zip project for sample
                val buildDir = File(context.cacheDir, "sample_staging_${sample.id}")
                if (buildDir.exists()) buildDir.deleteRecursively()
                buildDir.mkdirs()

                when (sample.framework) {
                    FrameworkType.GOOGLE_AI_STUDIO -> {
                        File(buildDir, "metadata.json").writeText("""
                            {
                              "name": "${sample.name}",
                              "description": "${sample.description}",
                              "majorCapabilities": ["MAJOR_CAPABILITY_SERVER_SIDE_GEMINI_API"]
                            }
                        """.trimIndent())
                        File(buildDir, "package.json").writeText("""
                            {
                              "name": "${sample.id}",
                              "version": "${sample.defaultVersion}",
                              "dependencies": { "react": "^18.2.0" }
                            }
                        """.trimIndent())
                        File(buildDir, "index.html").writeText("""
                            <!DOCTYPE html>
                            <html><head><title>${sample.name}</title></head>
                            <body style="background:#0F172A;color:white;text-align:center;padding:50px;font-family:sans-serif;">
                            <h1>${sample.name}</h1>
                            <p>${sample.description}</p>
                            <p style="color:#10B981;">Converted with APK Builder Pro</p>
                            </body></html>
                        """.trimIndent())
                    }
                    FrameworkType.NATIVE_ANDROID -> {
                        File(buildDir, "build.gradle.kts").writeText("// Native Android Gradle")
                        File(buildDir, "AndroidManifest.xml").writeText("""
                            <manifest package="${sample.defaultPackageName}">
                                <application android:label="${sample.name}"/>
                            </manifest>
                        """.trimIndent())
                        File(buildDir, "index.html").writeText("<html><body><h1>Native Android App</h1></body></html>")
                    }
                    else -> {
                        File(buildDir, "index.html").writeText("""
                            <!DOCTYPE html>
                            <html><head><title>${sample.name}</title></head>
                            <body style="background:#1E1B4B;color:white;text-align:center;padding:40px;">
                            <h2>${sample.name}</h2>
                            <p>${sample.description}</p>
                            </body></html>
                        """.trimIndent())
                    }
                }

                // Compress staging to zip
                val zos = java.util.zip.ZipOutputStream(FileOutputStream(cacheZip))
                buildDir.walkTopDown().filter { it.isFile }.forEach { f ->
                    val rel = f.relativeTo(buildDir).path.replace('\\', '/')
                    zos.putNextEntry(java.util.zip.ZipEntry(rel))
                    f.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
                zos.close()
                buildDir.deleteRecursively()

                processZipFile(cacheZip, "${sample.id}.zip")

            } catch (e: Exception) {
                _downloadError.value = "Failed to load sample: ${e.localizedMessage}"
            }
        }
    }

    fun importFromCloudUrl(provider: CloudProvider, urlString: String) {
        if (urlString.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            _isDownloadingCloud.value = true
            _downloadError.value = null
            try {
                val context = getApplication<Application>()
                val directDownloadUrl = parseCloudDirectUrl(provider, urlString)
                val targetZip = File(context.cacheDir, "cloud_import_${System.currentTimeMillis()}.zip")

                val connection = URL(directDownloadUrl).openConnection()
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                connection.getInputStream().use { input ->
                    FileOutputStream(targetZip).use { output -> input.copyTo(output) }
                }

                _isDownloadingCloud.value = false
                _showCloudImport.value = false
                processZipFile(targetZip, "cloud_import.zip")

            } catch (e: Exception) {
                _isDownloadingCloud.value = false
                _downloadError.value = "Cloud download failed: ${e.localizedMessage}. Check URL or permissions."
            }
        }
    }

    private fun parseCloudDirectUrl(provider: CloudProvider, originalUrl: String): String {
        return when (provider) {
            CloudProvider.GOOGLE_DRIVE -> {
                if (originalUrl.contains("drive.google.com") && originalUrl.contains("/d/")) {
                    val fileId = originalUrl.substringAfter("/d/").substringBefore("/")
                    "https://drive.google.com/uc?export=download&id=$fileId"
                } else originalUrl
            }
            CloudProvider.DROPBOX -> {
                originalUrl.replace("www.dropbox.com", "dl.dropboxusercontent.com")
                    .replace("dl=0", "dl=1")
            }
            CloudProvider.ONEDRIVE -> {
                if (originalUrl.contains("1drv.ms") || originalUrl.contains("onedrive.live.com")) {
                    "$originalUrl&download=1"
                } else originalUrl
            }
            CloudProvider.DIRECT_URL -> originalUrl
        }
    }

    private suspend fun processZipFile(zipFile: File, fileName: String) {
        val context = getApplication<Application>()
        _selectedZipFile.value = zipFile

        val tempExtractDir = File(context.cacheDir, "temp_inspect_${System.currentTimeMillis()}")
        tempExtractDir.mkdirs()

        java.util.zip.ZipInputStream(java.io.FileInputStream(zipFile)).use { zis ->
            var entry = zis.nextEntry
            val buf = ByteArray(8192)
            while (entry != null) {
                val f = File(tempExtractDir, entry.name)
                if (entry.isDirectory) {
                    f.mkdirs()
                } else {
                    f.parentFile?.mkdirs()
                    FileOutputStream(f).use { fos ->
                        var len: Int
                        while (zis.read(buf).also { len = it } != -1) {
                            fos.write(buf, 0, len)
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        val metadata = MetadataExtractor.extractMetadataFromDirectory(tempExtractDir, fileName)
        tempExtractDir.deleteRecursively()

        withContext(Dispatchers.Main) {
            _projectMetadata.value = metadata
            appNameInput.value = metadata.appName
            packageNameInput.value = metadata.packageName
            versionNameInput.value = metadata.versionName
            versionCodeInput.value = metadata.versionCode.toString()
        }
    }

    fun startBuildProcess() {
        val zipFile = _selectedZipFile.value ?: return
        val rawMetadata = _projectMetadata.value ?: return
        val metadata = rawMetadata.copy(
            appName = appNameInput.value.ifBlank { rawMetadata.appName },
            packageName = packageNameInput.value.ifBlank { rawMetadata.packageName },
            versionName = versionNameInput.value.ifBlank { rawMetadata.versionName },
            versionCode = versionCodeInput.value.toIntOrNull() ?: rawMetadata.versionCode
        )
        val outputType = _selectedOutputType.value
        val context = getApplication<Application>()

        val keystoreConfig = com.example.model.KeystoreConfig(
            mode = selectedSigningMode.value,
            customFile = customKeystoreFile.value,
            alias = keystoreAliasInput.value,
            storePassword = keystoreStorePasswordInput.value,
            keyPassword = keystoreKeyPasswordInput.value,
            signV1 = signV1.value,
            signV2 = signV2.value,
            signV3 = signV3.value,
            signV4 = signV4.value,
            installerPackageName = installerPackageNameInput.value.ifBlank { "com.android.vending" },
            isInstallerSpoofEnabled = isInstallerSpoofEnabled.value
        )

        viewModelScope.launch {
            ApkBuilderEngine.buildApkFromZip(
                context = context,
                zipFile = zipFile,
                customMetadata = metadata,
                outputType = outputType,
                selectedPermissions = selectedPermissions.value,
                keystoreConfig = keystoreConfig,
                onNotificationProgress = { stepName, percent ->
                    BuildNotificationManager.showBuildProgressNotification(
                        context, metadata.appName, stepName, percent
                    )
                }
            ).collect { stepProgress ->
                _buildProgress.value = stepProgress

                if (stepProgress.status == BuildStatus.SUCCESS && stepProgress.outputApkPath != null) {
                    BuildNotificationManager.showBuildSuccessNotification(
                        context, metadata.appName, stepProgress.outputApkPath
                    )
                    // Save to Room Database history
                    val iconB64 = metadata.appIconBitmap?.let { MetadataExtractor.bitmapToBase64(it) }
                    val entity = ConvertedApkEntity(
                        appName = metadata.appName,
                        packageName = metadata.packageName,
                        versionName = metadata.versionName,
                        versionCode = metadata.versionCode,
                        frameworkType = metadata.frameworkType.displayName,
                        outputType = outputType.displayName,
                        filePath = stepProgress.outputApkPath,
                        fileSize = stepProgress.outputFileSize,
                        verificationPassed = stepProgress.integrityResult?.isValid ?: true,
                        signatureSHA256 = stepProgress.integrityResult?.signatureSHA256 ?: "SHA256:RSA2048",
                        sourceFileName = metadata.sourceFileName,
                        iconBase64 = iconB64
                    )
                    repository.saveConvertedApk(entity)
                } else if (stepProgress.status == BuildStatus.FAILED) {
                    BuildNotificationManager.showBuildErrorNotification(
                        context, metadata.appName, stepProgress.errorMessage ?: "Build error"
                    )
                }
            }
        }
    }

    fun runOneClickAutoPipeline() {
        viewModelScope.launch {
            // 1. Ensure a project is loaded or load default sample
            if (_selectedZipFile.value == null) {
                loadSampleProject(sampleProjects.first())
                kotlinx.coroutines.delay(600)
            }

            // 2. Auto repair build.gradle and config
            autoRepairGradle()

            // 3. Set signature schemes & installer spoof defaults
            signV1.value = true
            signV2.value = true
            signV3.value = true
            signV4.value = true
            isInstallerSpoofEnabled.value = true
            if (installerPackageNameInput.value.isBlank()) {
                installerPackageNameInput.value = "com.android.vending"
            }

            // 4. Trigger build process
            startBuildProcess()
        }
    }

    fun installApk(filePath: String) {
        repository.installApk(filePath)
    }

    fun shareApk(filePath: String) {
        repository.shareApk(filePath)
    }

    fun deleteHistoryItem(id: Long, filePath: String) {
        viewModelScope.launch {
            repository.deleteApk(id, filePath)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    // Gradle Auto-Repair State & Logic
    val gradleRepairReport = MutableStateFlow<com.example.model.GradleRepairResult?>(null)

    fun autoRepairGradle(): com.example.model.GradleRepairResult {
        val currentMeta = _projectMetadata.value ?: ProjectMetadata(
            appName = appNameInput.value,
            packageName = packageNameInput.value,
            versionName = versionNameInput.value,
            versionCode = versionCodeInput.value.toIntOrNull() ?: 1,
            targetSdk = 36
        )
        val currentGradle = currentMeta.editableGradleContent
        val repairResult = com.example.engine.GradleAutoRepairHelper.autoRepairGradle(currentGradle, currentMeta)
        val updatedMeta = currentMeta.copy(editableGradleContent = repairResult.repairedContent)
        _projectMetadata.value = updatedMeta
        gradleRepairReport.value = repairResult
        return repairResult
    }

    fun autoRepairGradleAndRebuild() {
        autoRepairGradle()
        startBuildProcess()
    }

    fun dismissGradleRepairReport() {
        gradleRepairReport.value = null
    }

    // Error Log Viewer & Share Logic
    val viewingErrorLogContent = MutableStateFlow<String?>(null)

    fun openErrorLogViewer(logPath: String) {
        val content = com.example.engine.ErrorLogHelper.readLogContent(logPath)
        viewingErrorLogContent.value = content
    }

    fun closeErrorLogViewer() {
        viewingErrorLogContent.value = null
    }

    fun shareErrorLog(logPath: String) {
        val context = getApplication<Application>()
        com.example.engine.ErrorLogHelper.shareLogFile(context, logPath)
    }
}

