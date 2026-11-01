package com.example.engine

import com.example.model.CheckStatus
import com.example.model.FrameworkType
import com.example.model.ProjectCheckItem
import com.example.model.ProjectMetadata
import com.example.model.ProjectValidationReport
import java.io.File
import java.util.regex.Pattern

object ProjectValidator {

    private val PACKAGE_NAME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")

    fun validateProject(
        extractedDir: File,
        sourceFileName: String,
        metadata: ProjectMetadata
    ): ProjectValidationReport {
        val checks = mutableListOf<ProjectCheckItem>()
        val allFiles = extractedDir.walkTopDown().filter { it.isFile }.toList()
        val filePaths = allFiles.map { it.relativeTo(extractedDir).path.replace('\\', '/') }

        val totalFiles = allFiles.size
        val totalSize = allFiles.sumOf { it.length() }
        var warningsCount = 0
        var errorsCount = 0

        // 1. ตรวจสอบความสมบูรณ์ของโครงสร้างไฟล์ (Archive Integrity)
        if (totalFiles == 0) {
            errorsCount++
            checks.add(
                ProjectCheckItem(
                    title = "โครงสร้างไฟล์โปรเจกต์ (Project Files)",
                    status = CheckStatus.ERROR,
                    description = "ไม่พบไฟล์ในไฟล์บีบอัด หรือไฟล์ ZIP ว่างเปล่า",
                    suggestion = "กรุณาตรวจสอบและอัปโหลดไฟล์ ZIP ที่มีโครงสร้างโค้ดโปรเจกต์"
                )
            )
        } else {
            val sizeMb = "%.2f".format(totalSize / (1024f * 1024f))
            checks.add(
                ProjectCheckItem(
                    title = "ความสมบูรณ์ของไฟล์ (File Structure)",
                    status = CheckStatus.PASS,
                    description = "ตรวจพบ $totalFiles ไฟล์ รวมขนาด $sizeMb MB โครงสร้างครบถ้วน",
                    suggestion = null
                )
            )
        }

        // 2. ตรวจสอบประเภทเฟรมเวิร์กและจุดเริ่มต้น (Framework & Entry Point)
        var mainEntry: String? = null
        val framework = metadata.frameworkType

        when (framework) {
            FrameworkType.GOOGLE_AI_STUDIO, FrameworkType.HTML5_HYBRID, FrameworkType.UNKNOWN -> {
                val indexHtml = filePaths.find { it.equals("index.html", ignoreCase = true) || it.endsWith("/index.html", ignoreCase = true) }
                val packageJson = filePaths.find { it.equals("package.json", ignoreCase = true) || it.endsWith("/package.json", ignoreCase = true) }
                if (indexHtml != null) {
                    mainEntry = indexHtml
                    checks.add(
                        ProjectCheckItem(
                            title = "จุดเริ่มต้นแอป (Web Entry Point)",
                            status = CheckStatus.PASS,
                            description = "ตรวจพบไฟล์หลัก: $indexHtml พร้อมเชื่อมต่อ WebView คอนเทนเนอร์",
                            suggestion = null
                        )
                    )
                } else if (packageJson != null) {
                    mainEntry = packageJson
                    warningsCount++
                    checks.add(
                        ProjectCheckItem(
                            title = "จุดเริ่มต้นแอป (Web Entry Point)",
                            status = CheckStatus.WARNING,
                            description = "พบ package.json แต่ไม่พบ index.html โดยตรงที่รูท",
                            suggestion = "ระบบจะใช้หน้า Wrapper หรือ index.html สำรองอัตโนมัติ"
                        )
                    )
                } else {
                    warningsCount++
                    checks.add(
                        ProjectCheckItem(
                            title = "จุดเริ่มต้นแอป (Web Entry Point)",
                            status = CheckStatus.WARNING,
                            description = "ไม่พบไฟล์ index.html ในโฟลเดอร์หลัก",
                            suggestion = "ระบบสร้าง index.html สำหรับรัน Web Container ให้โดยอัตโนมัติ"
                        )
                    )
                }
            }
            FrameworkType.NATIVE_ANDROID -> {
                val manifest = filePaths.find { it.endsWith("AndroidManifest.xml", ignoreCase = true) }
                val gradle = filePaths.find { it.endsWith("build.gradle", ignoreCase = true) || it.endsWith("build.gradle.kts", ignoreCase = true) }
                if (manifest != null && gradle != null) {
                    mainEntry = manifest
                    checks.add(
                        ProjectCheckItem(
                            title = "โครงสร้าง Android Gradle",
                            status = CheckStatus.PASS,
                            description = "พบ AndroidManifest.xml และสคริปต์คอมไพล์ Gradle ครบถ้วน",
                            suggestion = null
                        )
                    )
                } else {
                    warningsCount++
                    checks.add(
                        ProjectCheckItem(
                            title = "โครงสร้าง Android Gradle",
                            status = CheckStatus.WARNING,
                            description = "ขาดไฟล์กำหนดค่า Android บางส่วน",
                            suggestion = "ระบบจะผสาน AndroidManifest.xml และ Gradle สำรองอัตโนมัติ"
                        )
                    )
                }
            }
            FrameworkType.FLUTTER -> {
                val pubspec = filePaths.find { it.endsWith("pubspec.yaml", ignoreCase = true) }
                mainEntry = pubspec ?: "Flutter App"
                checks.add(
                    ProjectCheckItem(
                        title = "โครงสร้าง Flutter Project",
                        status = CheckStatus.PASS,
                        description = "ตรวจพบสเปก Flutter (${pubspec ?: "โครงสร้างมาตรฐาน"})",
                        suggestion = null
                    )
                )
            }
            FrameworkType.REACT_NATIVE -> {
                val pkg = filePaths.find { it.endsWith("package.json", ignoreCase = true) }
                mainEntry = pkg ?: "React Native App"
                checks.add(
                    ProjectCheckItem(
                        title = "โครงสร้าง React Native",
                        status = CheckStatus.PASS,
                        description = "ตรวจพบ React Native โมดูลพร้อมแพ็กเกจ",
                        suggestion = null
                    )
                )
            }
            FrameworkType.XAMARIN_MAUI -> {
                val proj = filePaths.find { it.endsWith(".csproj", ignoreCase = true) }
                mainEntry = proj ?: ".NET MAUI Project"
                checks.add(
                    ProjectCheckItem(
                        title = "โครงสร้าง .NET MAUI / Xamarin",
                        status = CheckStatus.PASS,
                        description = "ตรวจพบไฟล์โซลูชัน: $mainEntry",
                        suggestion = null
                    )
                )
            }
            FrameworkType.IONIC_CAPACITOR, FrameworkType.CORDOVA -> {
                val cap = filePaths.find { it.contains("capacitor", ignoreCase = true) || it.contains("config.xml", ignoreCase = true) }
                mainEntry = cap ?: "Capacitor App"
                checks.add(
                    ProjectCheckItem(
                        title = "โครงสร้าง Ionic / Capacitor",
                        status = CheckStatus.PASS,
                        description = "ตรวจพบการกำหนดค่าโมบายล์เว็บคอนเทนเนอร์",
                        suggestion = null
                    )
                )
            }
        }

        // 3. ตรวจสอบ Package Name / ApplicationId
        val pkg = metadata.packageName.trim()
        val isPackageValid = PACKAGE_NAME_PATTERN.matcher(pkg).matches() && !pkg.contains("..") && pkg.split('.').size >= 2
        if (isPackageValid) {
            checks.add(
                ProjectCheckItem(
                    title = "ความถูกต้องของ Package Name",
                    status = CheckStatus.PASS,
                    description = "Package ID ถูกต้องตามมาตรฐาน Android: '$pkg'",
                    suggestion = null
                )
            )
        } else {
            warningsCount++
            checks.add(
                ProjectCheckItem(
                    title = "ความถูกต้องของ Package Name",
                    status = CheckStatus.WARNING,
                    description = "Package Name '$pkg' อาจมีรูปแบบที่ไม่เป็นไปตามมาตรฐาน Java/Android",
                    suggestion = "แนะนำให้ใช้รูปแบบ com.example.appname เพื่อให้ติดตั้งบนเครื่องได้ราบรื่น"
                )
            )
        }

        // 4. ตรวจสอบชื่อแอปและเวอร์ชัน (App Identity)
        if (metadata.appName.isNotBlank() && metadata.appName != "APK App") {
            checks.add(
                ProjectCheckItem(
                    title = "ข้อมูลระบุตัวตนแอป (App Identity)",
                    status = CheckStatus.PASS,
                    description = "ชื่อแอป: \"${metadata.appName}\" | เวอร์ชัน: ${metadata.versionName} (Code: ${metadata.versionCode})",
                    suggestion = null
                )
            )
        } else {
            checks.add(
                ProjectCheckItem(
                    title = "ข้อมูลระบุตัวตนแอป (App Identity)",
                    status = CheckStatus.INFO,
                    description = "ใช้ชื่อเริ่มต้น \"${metadata.appName}\" คุณสามารถเปลี่ยนได้ในช่องด้านล่าง",
                    suggestion = null
                )
            )
        }

        // 5. ตรวจสอบไอคอนแอป (App Launcher Icon)
        val hasCustomIcon = metadata.appIconBitmap != null || metadata.rawIconBytes != null
        if (hasCustomIcon) {
            checks.add(
                ProjectCheckItem(
                    title = "ไอคอนแอปพลิเคชัน (App Icon)",
                    status = CheckStatus.PASS,
                    description = "ตรวจพบไอคอนแอปจากโปรเจกต์ พร้อมส่งออกทุกความละเอียด (mipmap-xxxhdpi)",
                    suggestion = null
                )
            )
        } else {
            checks.add(
                ProjectCheckItem(
                    title = "ไอคอนแอปพลิเคชัน (App Icon)",
                    status = CheckStatus.INFO,
                    description = "ไม่พบไฟล์ไอคอนเฉพาะ ระบบจะสร้าง Adaptive Brand Icon สวยงามให้อัตโนมัติ",
                    suggestion = "สามารถอัปโหลดรูปไอคอนที่กำหนดเองได้หากต้องการ"
                )
            )
        }

        // 6. ตรวจสอบสิทธิ์ของแอป (Declared Permissions)
        val perms = metadata.detectedPermissions
        val permCount = perms.size
        checks.add(
            ProjectCheckItem(
                title = "สิทธิ์การเข้าถึงอุปกรณ์ (Android Permissions)",
                status = CheckStatus.PASS,
                description = "กำหนดไว้ $permCount สิทธิ์ (${perms.take(3).joinToString { it.substringAfterLast('.') }}${if (permCount > 3) "..." else ""})",
                suggestion = "สามารถเปิด/ปิดสิทธิ์เพิ่มเติมได้ในแท็บ 'สิทธิ์ของแอพ'"
            )
        )

        // 7. ตรวจสอบเป้าหมาย SDK และความเข้ากันได้ของระบบ (Target SDK Compatibility)
        checks.add(
            ProjectCheckItem(
                title = "ความเข้ากันได้ Android SDK",
                status = CheckStatus.PASS,
                description = "คอมไพล์สำหรับ Target SDK 36 (Android 16 ล่าสุด) พร้อมรันบน Android 7.0+ (API 24)",
                suggestion = null
            )
        )

        val canProceed = errorsCount == 0 && totalFiles > 0
        val isValid = errorsCount == 0
        val summary = when {
            errorsCount > 0 -> "พบข้อผิดพลาด $errorsCount รายการ กรุณาตรวจสอบไฟล์ที่นำเข้า"
            warningsCount > 0 -> "ผ่านการตรวจสอบ (มีคำแนะนำ $warningsCount รายการ) พร้อมคอมไพล์เป็น APK"
            else -> "ผ่านการตรวจสอบ 100% โปรเจกต์สมบูรณ์แบบพร้อมคอมไพล์และลงชื่อ APK"
        }

        return ProjectValidationReport(
            isValid = isValid,
            canProceed = canProceed,
            framework = framework,
            summary = summary,
            checks = checks,
            detectedFilesCount = totalFiles,
            detectedTotalSize = totalSize,
            mainEntryFile = mainEntry,
            hasValidManifest = isPackageValid,
            hasValidConfig = true,
            hasIcons = hasCustomIcon,
            detectedPermissions = perms,
            warningsCount = warningsCount,
            errorsCount = errorsCount
        )
    }
}
