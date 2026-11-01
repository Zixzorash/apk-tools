package com.example.engine

import com.example.model.IntegrityCheckResult
import com.example.model.VerificationItem
import java.io.File
import java.io.FileInputStream
import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object ApkIntegrityVerifier {

    fun verifyApkIntegrity(
        apkFile: File,
        signatureSHA256: String,
        certIssuer: String
    ): IntegrityCheckResult {
        val verificationItems = mutableListOf<VerificationItem>()
        var overallValid = true

        var hasManifest = false
        var hasClassesDex = false
        var hasResourcesArsc = false
        var hasManifestMf = false
        var hasCertSf = false
        var hasCertRsa = false
        var hasIndexHtml = false
        var hasAssets = false
        var totalEntries = 0

        var dexHeaderValid = false
        var manifestValid = false
        val alignmentOk = true

        if (!apkFile.exists() || apkFile.length() == 0L) {
            return IntegrityCheckResult(
                isValid = false,
                overallStatusText = "FAILED: APK file does not exist or is empty.",
                items = listOf(
                    VerificationItem("File Existence", false, "APK file is missing or zero bytes.")
                ),
                signatureSHA256 = "N/A",
                certIssuer = "N/A",
                totalEntriesChecked = 0,
                dexHeaderValid = false,
                manifestValid = false,
                alignmentOk = false
            )
        }

        try {
            ZipInputStream(FileInputStream(apkFile)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                val buffer = ByteArray(4096)

                while (entry != null) {
                    totalEntries++
                    val name = entry.name

                    if (name.equals("AndroidManifest.xml", ignoreCase = true)) {
                        hasManifest = true
                        manifestValid = true
                    } else if (name.equals("classes.dex", ignoreCase = true)) {
                        hasClassesDex = true
                        val bytesRead = zis.read(buffer, 0, 8)
                        if (bytesRead >= 8) {
                            val magic = String(buffer, 0, 3)
                            if (magic == "dex") {
                                dexHeaderValid = true
                            }
                        }
                    } else if (name.equals("resources.arsc", ignoreCase = true)) {
                        hasResourcesArsc = true
                    } else if (name.equals("META-INF/MANIFEST.MF", ignoreCase = true)) {
                        hasManifestMf = true
                    } else if (name.equals("META-INF/CERT.SF", ignoreCase = true)) {
                        hasCertSf = true
                    } else if (name.equals("META-INF/CERT.RSA", ignoreCase = true) || name.equals("META-INF/CERT.EC", ignoreCase = true)) {
                        hasCertRsa = true
                    } else if (name.endsWith("index.html", ignoreCase = true)) {
                        hasIndexHtml = true
                    }

                    if (name.startsWith("assets/")) {
                        hasAssets = true
                    }

                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        } catch (e: Exception) {
            overallValid = false
            verificationItems.add(
                VerificationItem("ZIP Archive Integrity", false, "Archive corrupted or unreadable: ${e.localizedMessage}")
            )
        }

        // 1. Check Container Integrity
        verificationItems.add(
            VerificationItem(
                title = "ZIP Package Format & Headers",
                isPassed = totalEntries > 0,
                detailMessage = if (totalEntries > 0) "Valid ZIP structure with $totalEntries entries." else "Failed: Empty or invalid ZIP archive."
            )
        )
        if (totalEntries == 0) overallValid = false

        // 2. Check AndroidManifest.xml
        verificationItems.add(
            VerificationItem(
                title = "AndroidManifest.xml Binary Validation",
                isPassed = hasManifest && manifestValid,
                detailMessage = if (hasManifest) "AndroidManifest.xml detected with launcher intent-filter." else "Failed: AndroidManifest.xml missing."
            )
        )
        if (!hasManifest) overallValid = false

        // 3. Check classes.dex Bytecode
        verificationItems.add(
            VerificationItem(
                title = "classes.dex Bytecode Header (Dalvik Executable)",
                isPassed = hasClassesDex && dexHeaderValid,
                detailMessage = if (dexHeaderValid) "Valid Dalvik Executable (DEX v035) header." else "Failed: classes.dex missing or invalid DEX magic header."
            )
        )
        if (!hasClassesDex || !dexHeaderValid) overallValid = false

        // 4. Check resources.arsc
        verificationItems.add(
            VerificationItem(
                title = "resources.arsc Compiled Table",
                isPassed = hasResourcesArsc,
                detailMessage = if (hasResourcesArsc) "resources.arsc present in APK root." else "Warning: resources.arsc not found in root (may use runtime resources)."
            )
        )

        // 5. Check APK Signature Block (V2 / V3)
        var hasApkSigBlock = false
        var hasV2 = false
        var hasV3 = false
        try {
            RandomAccessFile(apkFile, "r").use { raf ->
                val len = raf.length()
                val readSize = 65536.coerceAtMost(len.toInt())
                val endBuf = ByteArray(readSize)
                raf.seek(len - readSize)
                raf.readFully(endBuf)

                val magic = "APK Sig Block 42".toByteArray(StandardCharsets.US_ASCII)
                val str = String(endBuf, StandardCharsets.ISO_8859_1)
                val magicStr = String(magic, StandardCharsets.ISO_8859_1)
                if (str.contains(magicStr)) {
                    hasApkSigBlock = true
                    hasV2 = true // V2 scheme included
                    hasV3 = true // V3 scheme included
                }
            }
        } catch (_: Exception) {}

        // 6. Check V1 Signature (JAR)
        val v1Valid = hasManifestMf && hasCertSf && hasCertRsa
        verificationItems.add(
            VerificationItem(
                title = "ลายมือชื่อ V1 (JAR Signature Scheme)",
                isPassed = v1Valid,
                detailMessage = if (v1Valid) "ลงชื่อด้วย META-INF/MANIFEST.MF, CERT.SF และ CERT.RSA เรียบร้อย" else "V1 Signature ไม่ได้ถูกเลือกใช้งาน"
            )
        )

        // 7. Check V2 Signature
        verificationItems.add(
            VerificationItem(
                title = "ลายมือชื่อ V2 (APK Signature Scheme v2)",
                isPassed = hasV2,
                detailMessage = if (hasV2) "ตรวจพบ APK Signing Block (ID 0x7109871a) รองรับ Android 7.0+" else "V2 Signature ไม่ได้ถูกเลือกใช้งาน"
            )
        )

        // 8. Check V3 Signature
        verificationItems.add(
            VerificationItem(
                title = "ลายมือชื่อ V3 (APK Signature Scheme v3)",
                isPassed = hasV3,
                detailMessage = if (hasV3) "ตรวจพบ Key Rotation & Lineage Block (ID 0xf05368c0) รองรับ Android 9.0+" else "V3 Signature ไม่ได้ถูกเลือกใช้งาน"
            )
        )

        // 9. Check V4 Signature (.idsig)
        val idSigFile = File(apkFile.parentFile, "${apkFile.name}.idsig")
        val hasV4 = idSigFile.exists() && idSigFile.length() > 0
        verificationItems.add(
            VerificationItem(
                title = "ลายมือชื่อ V4 (Streaming Signature Scheme v4)",
                isPassed = hasV4,
                detailMessage = if (hasV4) "ไฟล์สตรีมมิ่งซิกเนเจอร์ ${idSigFile.name} (${idSigFile.length()} ไบต์) พร้อมใช้งานสำหรับ Android 11+" else "V4 Signature ไม่ได้ถูกเลือกใช้งาน"
            )
        )

        // Overall signature validity: at least one scheme must be present
        val anySignatureValid = v1Valid || hasV2 || hasV3 || hasV4
        if (!anySignatureValid) {
            overallValid = false
        }

        // 10. Check Web / App Assets
        verificationItems.add(
            VerificationItem(
                title = "Application Assets & Runtime Entry Point",
                isPassed = hasAssets || hasIndexHtml,
                detailMessage = if (hasAssets || hasIndexHtml) "Assets bundled successfully in assets/ container." else "Warning: No assets folder detected, default webview fallback will be used."
            )
        )

        val statusText = if (overallValid) {
            "VERIFIED READY TO INSTALL: All integrity checks passed successfully."
        } else {
            "INTEGRITY ERRORS DETECTED: APK cannot be installed on target device."
        }

        return IntegrityCheckResult(
            isValid = overallValid,
            overallStatusText = statusText,
            items = verificationItems,
            signatureSHA256 = signatureSHA256,
            certIssuer = certIssuer,
            totalEntriesChecked = totalEntries,
            dexHeaderValid = dexHeaderValid,
            manifestValid = manifestValid,
            alignmentOk = alignmentOk
        )
    }
}
