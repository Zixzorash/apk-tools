package com.example.engine

import com.example.model.FrameworkType
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object FrameworkDetector {

    fun detectFrameworkFromEntries(entryNames: List<String>): FrameworkType {
        val namesLower = entryNames.map { it.lowercase() }

        // 1. Google AI Studio (metadata.json + package.json / vite)
        val hasMetadataJson = namesLower.any { it.endsWith("metadata.json") }
        val hasPackageJson = namesLower.any { it.endsWith("package.json") }
        if (hasMetadataJson && hasPackageJson) {
            return FrameworkType.GOOGLE_AI_STUDIO
        }

        // 2. Native Android (build.gradle/kts + AndroidManifest.xml)
        val hasGradle = namesLower.any { it.endsWith("build.gradle") || it.endsWith("build.gradle.kts") }
        val hasManifest = namesLower.any { it.endsWith("androidmanifest.xml") }
        if (hasGradle && hasManifest) {
            return FrameworkType.NATIVE_ANDROID
        }

        // 3. Flutter
        val hasPubspec = namesLower.any { it.endsWith("pubspec.yaml") || it.endsWith("pubspec.yml") }
        if (hasPubspec) {
            return FrameworkType.FLUTTER
        }

        // 4. React Native
        val hasReactNative = namesLower.any { entry ->
            entry.contains("react-native") || (hasPackageJson && entry.contains("android/app/build.gradle"))
        }
        if (hasReactNative) {
            return FrameworkType.REACT_NATIVE
        }

        // 5. Ionic / Capacitor
        val hasCapacitor = namesLower.any { it.endsWith("capacitor.config.json") || it.endsWith("capacitor.config.ts") || it.endsWith("ionic.config.json") }
        if (hasCapacitor) {
            return FrameworkType.IONIC_CAPACITOR
        }

        // 6. Cordova
        val hasCordova = namesLower.any { it.endsWith("config.xml") && namesLower.any { e -> e.contains("www/index.html") } }
        if (hasCordova) {
            return FrameworkType.CORDOVA
        }

        // 7. Xamarin / .NET MAUI
        val hasMaui = namesLower.any { it.endsWith(".csproj") || it.contains("mauiprogram.cs") }
        if (hasMaui) {
            return FrameworkType.XAMARIN_MAUI
        }

        // 8. HTML5 Web Hybrid
        val hasIndexHtml = namesLower.any { it.endsWith("index.html") }
        if (hasIndexHtml) {
            return FrameworkType.HTML5_HYBRID
        }

        return FrameworkType.UNKNOWN
    }
}
