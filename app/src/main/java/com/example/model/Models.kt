package com.example.model

import android.graphics.Bitmap
import androidx.compose.ui.graphics.vector.ImageVector

enum class FrameworkType(val displayName: String, val iconName: String, val description: String) {
    GOOGLE_AI_STUDIO("Google AI Studio", "code", "React + Vite project with metadata.json"),
    NATIVE_ANDROID("Native Android", "android", "Gradle Native Android with AndroidManifest.xml"),
    HTML5_HYBRID("HTML5 Web Hybrid", "html", "Web application with index.html"),
    REACT_NATIVE("React Native", "phone_android", "Cross-platform React Native app"),
    FLUTTER("Flutter", "flutter", "Dart & Flutter cross-platform project"),
    XAMARIN_MAUI("Xamarin / .NET MAUI", "memory", "C# .NET MAUI cross-platform app"),
    IONIC_CAPACITOR("Ionic / Capacitor", "web", "Capacitor web-native mobile app"),
    CORDOVA("Apache Cordova", "build", "Cordova web container app"),
    UNKNOWN("Generic Web / Zip", "folder_zip", "Standard zip archive project")
}

enum class OutputType(val displayName: String, val extension: String, val description: String) {
    APK("Standard APK", ".apk", "Standalone Android Package for direct installation"),
    XAPK("XAPK Bundle", ".xapk", "Package with APK + resources & split config for Play Store")
}

enum class SigningMode(val displayName: String, val description: String) {
    DEBUG_KEYSTORE(
        "Android Debug Keystore",
        "ลงชื่อด้วยคีย์มาตรฐาน Android (debug.keystore / androiddebugkey / android) ติดตั้งบนอุปกรณ์จริงและจำลองได้ทันที"
    ),
    AUTO_RELEASE(
        "คีย์อัตโนมัติ (Release / Production)",
        "คีย์อัตโนมัติสร้างครั้งแรกที่ build เก็บที่ ~/abp/abp.keystore สำรองไฟล์นี้ไว้ ถ้าหายจะอัปเดตแอปเดิมไม่ได้"
    ),
    CUSTOM_KEYSTORE(
        "ไฟล์ Keystore กำหนดเอง (.jks / .keystore / .p12)",
        "ใช้ไฟล์ Keystore ส่วนตัว พร้อม Alias และ Password ตามที่กำหนด"
    )
}

data class KeystoreConfig(
    val mode: SigningMode = SigningMode.DEBUG_KEYSTORE,
    val customFile: java.io.File? = null,
    val alias: String = "",
    val storePassword: String = "",
    val keyPassword: String = "",
    val isRemembered: Boolean = false
)

data class GradleRepairResult(
    val originalContent: String,
    val repairedContent: String,
    val fixes: List<String>,
    val isModified: Boolean
)

enum class CloudProvider(val displayName: String, val icon: String, val colorHex: String) {
    GOOGLE_DRIVE("Google Drive", "drive", "#4285F4"),
    DROPBOX("Dropbox", "cloud", "#0061FE"),
    ONEDRIVE("OneDrive", "cloud_queue", "#0078D4"),
    DIRECT_URL("Direct URL / GitHub", "link", "#10B981")
}

data class ProjectMetadata(
    val appName: String = "My App",
    val packageName: String = "com.app.generated",
    val versionName: String = "1.0.0",
    val versionCode: Int = 1,
    val minSdk: Int = 24,
    val targetSdk: Int = 36,
    val frameworkType: FrameworkType = FrameworkType.UNKNOWN,
    val appIconBitmap: Bitmap? = null,
    val rawIconBytes: ByteArray? = null,
    val editableGradleContent: String = "",
    val detectedPermissions: List<String> = listOf("android.permission.INTERNET"),
    val fileCount: Int = 0,
    val totalSizeBytes: Long = 0L,
    val sourceFileName: String = ""
)

enum class BuildStatus {
    IDLE,
    PREPARING,
    PARSING,
    COMPILING_ASSETS,
    GENERATING_DEX,
    PACKAGING,
    AUTO_SIGNING,
    VERIFYING,
    SUCCESS,
    FAILED
}

data class BuildStepProgress(
    val status: BuildStatus = BuildStatus.IDLE,
    val currentStepName: String = "Ready",
    val progressPercent: Float = 0f,
    val detailLog: List<String> = emptyList(),
    val errorMessage: String? = null,
    val errorLogPath: String? = null,
    val outputApkPath: String? = null,
    val outputFileSize: Long = 0L,
    val integrityResult: IntegrityCheckResult? = null
)

data class VerificationItem(
    val title: String,
    val isPassed: Boolean,
    val detailMessage: String
)

data class IntegrityCheckResult(
    val isValid: Boolean,
    val overallStatusText: String,
    val items: List<VerificationItem>,
    val signatureSHA256: String,
    val certIssuer: String,
    val totalEntriesChecked: Int,
    val dexHeaderValid: Boolean,
    val manifestValid: Boolean,
    val alignmentOk: Boolean
)

data class SampleProject(
    val id: String,
    val name: String,
    val description: String,
    val framework: FrameworkType,
    val defaultPackageName: String,
    val defaultVersion: String,
    val iconResId: Int? = null,
    val sampleZipUrl: String? = null
)
