package com.example.engine

import com.example.model.GradleRepairResult
import com.example.model.ProjectMetadata

object GradleAutoRepairHelper {

    fun autoRepairGradle(currentContent: String, metadata: ProjectMetadata): GradleRepairResult {
        val fixes = mutableListOf<String>()
        var content = currentContent.trim()

        // If content is empty or obviously invalid, generate a pristine modern build.gradle
        if (content.isBlank() || content.length < 20 || (!content.contains("android") && !content.contains("dependencies"))) {
            fixes.add("สร้างโครงสร้าง build.gradle ใหม่ที่สมบูรณ์และถูกต้องตามมาตรฐาน Android")
            val generated = generatePristineGradle(metadata)
            return GradleRepairResult(
                originalContent = currentContent,
                repairedContent = generated,
                fixes = fixes,
                isModified = true
            )
        }

        // 1. Check and repair namespace
        if (!content.contains("namespace")) {
            val targetNamespace = metadata.packageName.ifBlank { "com.example.app" }
            if (content.contains("android {")) {
                content = content.replaceFirst("android {", "android {\n    namespace = \"$targetNamespace\"")
                fixes.add("เพิ่มการกำหนดค่า namespace = \"$targetNamespace\" ในบล็อก android")
            } else if (content.contains("android{")) {
                content = content.replaceFirst("android{", "android {\n    namespace = \"$targetNamespace\"")
                fixes.add("เพิ่มการกำหนดค่า namespace = \"$targetNamespace\" ในบล็อก android")
            }
        }

        // 2. Check and harmonize compileSdk & targetSdk
        val targetSdk = metadata.targetSdk.coerceAtLeast(34)
        val compileSdkRegexKts = Regex("""compileSdk\s*=\s*\d+""")
        val compileSdkRegexGroovy = Regex("""compileSdkVersion\s*\d+""")

        if (compileSdkRegexKts.containsMatchIn(content)) {
            val currentVal = compileSdkRegexKts.find(content)?.value
            if (currentVal != null && !currentVal.contains("$targetSdk")) {
                content = content.replace(compileSdkRegexKts, "compileSdk = $targetSdk")
                fixes.add("ปรับปรุง compileSdk เป็น $targetSdk เพื่อรองรับ Android ล่าสุด")
            }
        } else if (compileSdkRegexGroovy.containsMatchIn(content)) {
            content = content.replace(compileSdkRegexGroovy, "compileSdkVersion $targetSdk")
            fixes.add("ปรับปรุง compileSdkVersion เป็น $targetSdk เพื่อรองรับ Android ล่าสุด")
        } else if (content.contains("android {")) {
            content = content.replaceFirst("android {", "android {\n    compileSdk = $targetSdk")
            fixes.add("เพิ่มการกำหนดค่า compileSdk = $targetSdk ในบล็อก android")
        }

        // 3. Check and repair Java 17 sourceCompatibility & targetCompatibility
        if (content.contains("VERSION_1_8") || content.contains("VERSION_11")) {
            content = content.replace("JavaVersion.VERSION_1_8", "JavaVersion.VERSION_17")
                .replace("JavaVersion.VERSION_11", "JavaVersion.VERSION_17")
                .replace("jvmTarget = \"1.8\"", "jvmTarget = \"17\"")
                .replace("jvmTarget = \"11\"", "jvmTarget = \"17\"")
            fixes.add("อัปเกรด Java Source & Target Compatibility เป็น JavaVersion.VERSION_17 (JDK 17)")
        } else if (!content.contains("compileOptions")) {
            val compileOptionsBlock = """
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
            """.trimIndent()
            if (content.contains("android {")) {
                content = content.replaceFirst("android {", "android {\n$compileOptionsBlock\n")
                fixes.add("เพิ่มการกำหนดค่า compileOptions สำหรับ JavaVersion.VERSION_17")
            }
        }

        // 4. Ensure repositories are present (google and mavenCentral)
        if (!content.contains("google()") || !content.contains("mavenCentral()")) {
            if (content.contains("repositories {")) {
                if (!content.contains("google()")) {
                    content = content.replaceFirst("repositories {", "repositories {\n        google()")
                    fixes.add("เพิ่ม google() repository สำหรับดาวน์โหลด AndroidX และ Material")
                }
                if (!content.contains("mavenCentral()")) {
                    content = content.replaceFirst("repositories {", "repositories {\n        mavenCentral()")
                    fixes.add("เพิ่ม mavenCentral() repository สำหรับดาวน์โหลด dependencies")
                }
            }
        }

        // 5. Check and repair applicationId
        val targetAppId = metadata.packageName.ifBlank { "com.example.app" }
        if (!content.contains("applicationId")) {
            if (content.contains("defaultConfig {")) {
                content = content.replaceFirst("defaultConfig {", "defaultConfig {\n        applicationId = \"$targetAppId\"")
                fixes.add("เพิ่ม applicationId = \"$targetAppId\" ใน defaultConfig")
            }
        }

        // 6. Fix bracket balancing if syntax corrupted
        val openBraces = content.count { it == '{' }
        val closeBraces = content.count { it == '}' }
        if (openBraces > closeBraces) {
            val missing = openBraces - closeBraces
            content += "\n" + "}".repeat(missing)
            fixes.add("ปิดปีกกา '}' ที่ขาดหายไปจำนวน $missing ตัว")
        }

        if (fixes.isEmpty()) {
            fixes.add("ตรวจสอบแล้ว โครงสร้าง build.gradle ถูกต้องและมีความเข้ากันได้สูง")
        }

        return GradleRepairResult(
            originalContent = currentContent,
            repairedContent = content,
            fixes = fixes,
            isModified = fixes.any { !it.startsWith("ตรวจสอบแล้ว") }
        )
    }

    private fun generatePristineGradle(metadata: ProjectMetadata): String {
        val targetSdk = metadata.targetSdk.coerceAtLeast(34)
        val minSdk = metadata.minSdk.coerceAtLeast(24)
        val pkg = metadata.packageName.ifBlank { "com.example.app" }
        val verCode = metadata.versionCode.coerceAtLeast(1)
        val verName = metadata.versionName.ifBlank { "1.0.0" }

        return """
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "$pkg"
    compileSdk = $targetSdk

    defaultConfig {
        applicationId = "$pkg"
        minSdk = $minSdk
        targetSdk = $targetSdk
        versionCode = $verCode
        versionName = "$verName"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.webkit:webkit:1.11.0")
}
        """.trimIndent()
    }
}
