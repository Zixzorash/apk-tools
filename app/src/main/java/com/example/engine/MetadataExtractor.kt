package com.example.engine

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.example.model.FrameworkType
import com.example.model.ProjectMetadata
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

object MetadataExtractor {

    fun extractMetadataFromDirectory(
        extractedDir: File,
        sourceFileName: String
    ): ProjectMetadata {
        val allFiles = extractedDir.walkTopDown().filter { it.isFile }.toList()
        val relativePaths = allFiles.map { it.relativeTo(extractedDir).path.replace('\\', '/') }

        val frameworkType = FrameworkDetector.detectFrameworkFromEntries(relativePaths)

        var detectedAppName = "APK App"
        var detectedPackageName = "com.aistudio.app"
        var detectedVersionName = "1.0.0"
        var detectedVersionCode = 1
        var iconBitmap: Bitmap? = null
        var rawIconBytes: ByteArray? = null

        val permissionsList = mutableSetOf("android.permission.INTERNET")

        // 1. Try metadata.json (AI Studio)
        val metadataFile = allFiles.find { it.name.equals("metadata.json", ignoreCase = true) }
        if (metadataFile != null) {
            try {
                val json = JSONObject(metadataFile.readText(StandardCharsets.UTF_8))
                if (json.has("name") && json.getString("name").isNotBlank()) {
                    detectedAppName = json.getString("name")
                }
            } catch (_: Exception) {}
        }

        // 2. Try AndroidManifest.xml / strings.xml (Native Android)
        val stringsXmlFile = allFiles.find { it.path.replace('\\', '/').endsWith("res/values/strings.xml") }
        if (stringsXmlFile != null) {
            try {
                val text = stringsXmlFile.readText(StandardCharsets.UTF_8)
                val m = Pattern.compile("<string\\s+name=[\"']app_name[\"']>([^<]+)</string>").matcher(text)
                if (m.find()) {
                    m.group(1)?.trim()?.let { found ->
                        if (!found.isNull_BLANK_OR_GENERIC()) {
                            detectedAppName = found
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        val manifestFile = allFiles.find { it.name.equals("AndroidManifest.xml", ignoreCase = true) }
        if (manifestFile != null) {
            try {
                val text = manifestFile.readText(StandardCharsets.UTF_8)
                val pkgMatch = Pattern.compile("package=[\"']([^\"']+)[\"']").matcher(text)
                if (pkgMatch.find()) {
                    pkgMatch.group(1)?.let { detectedPackageName = it }
                }

                val permMatch = Pattern.compile("<uses-permission\\s+android:name=[\"']([^\"']+)[\"']").matcher(text)
                while (permMatch.find()) {
                    permMatch.group(1)?.let { permissionsList.add(it) }
                }
            } catch (_: Exception) {}
        }

        // 3. Try index.html <title> (HTML5 / Web Hybrid)
        if (detectedAppName == "APK App") {
            val indexHtmlFile = allFiles.find { it.name.equals("index.html", ignoreCase = true) }
            if (indexHtmlFile != null) {
                try {
                    val text = indexHtmlFile.readText(StandardCharsets.UTF_8)
                    val m = Pattern.compile("<title>([^<]+)</title>", Pattern.CASE_INSENSITIVE).matcher(text)
                    if (m.find()) {
                        m.group(1)?.trim()?.let { title ->
                            if (!title.isNull_BLANK_OR_GENERIC()) {
                                detectedAppName = title
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        // 4. Try package.json
        val packageJsonFile = allFiles.find { it.name.equals("package.json", ignoreCase = true) }
        if (packageJsonFile != null) {
            try {
                val json = JSONObject(packageJsonFile.readText(StandardCharsets.UTF_8))
                if (json.has("name") && (detectedAppName == "APK App" || detectedAppName.isBlank())) {
                    val rawName = json.getString("name")
                        .replace("-", " ")
                        .replace("_", " ")
                        .split(" ")
                        .joinToString(" ") { word -> word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } }
                    detectedAppName = rawName
                }
                if (json.has("version")) {
                    detectedVersionName = json.getString("version")
                }
            } catch (_: Exception) {}
        }

        // 5. Try pubspec.yaml (Flutter)
        val pubspecFile = allFiles.find { it.name.equals("pubspec.yaml", ignoreCase = true) }
        if (pubspecFile != null) {
            try {
                val lines = pubspecFile.readLines()
                lines.forEach { line ->
                    if (line.startsWith("name:")) {
                        val valName = line.substringAfter("name:").trim().replace("\"", "").replace("'", "")
                        if (detectedAppName == "APK App") {
                            detectedAppName = valName.replace("_", " ").capitalizeWords()
                        }
                    } else if (line.startsWith("version:")) {
                        detectedVersionName = line.substringAfter("version:").trim().substringBefore("+")
                    }
                }
            } catch (_: Exception) {}
        }

        // Clean & Sanitize App Name & Package Name
        if (detectedAppName.isBlank() || detectedAppName == "APK App") {
            val nameFromZip = sourceFileName.removeSuffix(".zip")
                .replace("-", " ")
                .replace("_", " ")
                .capitalizeWords()
            if (nameFromZip.isNotBlank()) {
                detectedAppName = nameFromZip
            } else {
                detectedAppName = "APK Builder Project"
            }
        }

        val sanitizedPkgSlug = detectedAppName.lowercase()
            .replace(Regex("[^a-z0-9]"), "")
            .take(20)
            .ifBlank { "app" }
        detectedPackageName = "com.aistudio.$sanitizedPkgSlug"

        // Search for App Icon
        val candidateIconFiles = allFiles.filter { file ->
            val n = file.name.lowercase()
            n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".ico") || n.endsWith(".webp")
        }.sortedByDescending { file ->
            val n = file.name.lowercase()
            var score = 0
            if (n.contains("ic_launcher")) score += 100
            if (n.contains("icon")) score += 50
            if (n.contains("logo")) score += 30
            if (n.contains("favicon")) score += 20
            if (file.path.contains("mipmap") || file.path.contains("drawable") || file.path.contains("assets")) score += 20
            score
        }

        val bestIconFile = candidateIconFiles.firstOrNull()
        if (bestIconFile != null) {
            try {
                val bytes = bestIconFile.readBytes()
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bmp != null) {
                    iconBitmap = bmp
                    rawIconBytes = bytes
                }
            } catch (_: Exception) {}
        }

        // Generate Editable build.gradle Content
        val gradleContent = generateEditableGradle(
            appName = detectedAppName,
            packageName = detectedPackageName,
            versionName = detectedVersionName,
            versionCode = detectedVersionCode,
            frameworkType = frameworkType,
            permissions = permissionsList.toList()
        )

        val totalSize = allFiles.sumOf { it.length() }

        return ProjectMetadata(
            appName = detectedAppName,
            packageName = detectedPackageName,
            versionName = detectedVersionName,
            versionCode = detectedVersionCode,
            minSdk = 24,
            targetSdk = 35,
            frameworkType = frameworkType,
            appIconBitmap = iconBitmap,
            rawIconBytes = rawIconBytes,
            editableGradleContent = gradleContent,
            detectedPermissions = permissionsList.toList(),
            fileCount = allFiles.size,
            totalSizeBytes = totalSize,
            sourceFileName = sourceFileName
        )
    }

    private fun generateEditableGradle(
        appName: String,
        packageName: String,
        versionName: String,
        versionCode: Int,
        frameworkType: FrameworkType,
        permissions: List<String>
    ): String {
        val sb = StringBuilder()
        sb.append("// ============================================================\n")
        sb.append("// APK Builder Pro - Editable Build Configuration\n")
        sb.append("// Project: $appName ($frameworkType)\n")
        sb.append("// Adjust application ID, SDK versions, or dependencies below.\n")
        sb.append("// ============================================================\n\n")
        sb.append("android {\n")
        sb.append("    namespace = \"com.example\"\n")
        sb.append("    defaultConfig {\n")
        sb.append("        applicationId = \"$packageName\"\n")
        sb.append("        minSdk = 24\n")
        sb.append("        targetSdk = 35\n")
        sb.append("        versionCode = $versionCode\n")
        sb.append("        versionName = \"$versionName\"\n")
        sb.append("    }\n")
        sb.append("    buildTypes {\n")
        sb.append("        release {\n")
        sb.append("            isMinifyEnabled = false\n")
        sb.append("            isShrinkResources = false\n")
        sb.append("        }\n")
        sb.append("    }\n")
        sb.append("}\n\n")
        sb.append("// Required Android Permissions\n")
        sb.append("permissions {\n")
        permissions.forEach { perm ->
            sb.append("    include(\"$perm\")\n")
        }
        sb.append("}\n\n")
        sb.append("dependencies {\n")
        sb.append("    implementation(\"androidx.core:core-ktx:1.15.0\")\n")
        sb.append("    implementation(\"androidx.appcompat:appcompat:1.7.0\")\n")
        sb.append("    implementation(\"com.google.android.material:material:1.12.0\")\n")
        sb.append("    implementation(\"androidx.webkit:webkit:1.12.1\")\n")
        sb.append("}\n")
        return sb.toString()
    }

    fun bitmapToBase64(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
        return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
    }

    fun base64ToBitmap(base64: String): Bitmap? {
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }

    private fun String?.isNull_BLANK_OR_GENERIC(): Boolean {
        if (this == null) return true
        val s = this.trim().lowercase()
        return s.isBlank() || s == "react app" || s == "vite + react" || s == "app" || s == "document" || s == "untitled"
    }

    private fun String.capitalizeWords(): String {
        return this.split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word -> word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } }
    }
}
