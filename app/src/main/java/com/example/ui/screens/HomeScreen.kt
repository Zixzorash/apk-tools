package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.model.OutputType
import com.example.ui.MainViewModel
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentGreenBright
import com.example.ui.theme.AccentViolet
import com.example.ui.theme.DarkInputBg
import com.example.ui.theme.DarkInputBorder
import com.example.ui.theme.DarkNavyCard
import com.example.ui.theme.DarkNavyCardBorder
import com.example.ui.theme.DarkObsidianBg
import com.example.ui.theme.TextDim
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onStartBuild: () -> Unit,
    onNavigateToTab: (ScreenTab) -> Unit = {}
) {
    val projectMetadata by viewModel.projectMetadata.collectAsState()
    val selectedOutputType by viewModel.selectedOutputType.collectAsState()

    val sourceTab by viewModel.sourceTab.collectAsState()
    val configSubTab by viewModel.configSubTab.collectAsState()

    val appNameInput by viewModel.appNameInput.collectAsState()
    val packageNameInput by viewModel.packageNameInput.collectAsState()
    val versionNameInput by viewModel.versionNameInput.collectAsState()
    val versionCodeInput by viewModel.versionCodeInput.collectAsState()
    val targetSdkInput by viewModel.targetSdkInput.collectAsState()
    val hardwareAccelEnabled by viewModel.hardwareAccelEnabled.collectAsState()

    val selectedSigningMode by viewModel.selectedSigningMode.collectAsState()
    val keystoreFileName by viewModel.keystoreFileName.collectAsState()
    val keystoreAliasInput by viewModel.keystoreAliasInput.collectAsState()
    val keystoreStorePasswordInput by viewModel.keystoreStorePasswordInput.collectAsState()
    val keystoreKeyPasswordInput by viewModel.keystoreKeyPasswordInput.collectAsState()
    val isUsingCustomKeystore by viewModel.isUsingCustomKeystore.collectAsState()
    val rememberKeystoreChecked by viewModel.rememberKeystoreChecked.collectAsState()
    val rememberedKeystoreStatus by viewModel.rememberedKeystoreStatus.collectAsState()
    val gradleRepairReport by viewModel.gradleRepairReport.collectAsState()

    val signV1 by viewModel.signV1.collectAsState()
    val signV2 by viewModel.signV2.collectAsState()
    val signV3 by viewModel.signV3.collectAsState()
    val signV4 by viewModel.signV4.collectAsState()
    val showValidationDetails by viewModel.showValidationDetails.collectAsState()

    val installerPackageNameInput by viewModel.installerPackageNameInput.collectAsState()
    val isInstallerSpoofEnabled by viewModel.isInstallerSpoofEnabled.collectAsState()

    val selectedPermissions by viewModel.selectedPermissions.collectAsState()

    var showSdkDropdown by remember { mutableStateOf(false) }

    val sdkOptions = listOf(
        "API Level 36 (Android 16 - ล่าสุด)",
        "API Level 35 (Android 15)",
        "API Level 34 (Android 14)",
        "API Level 33 (Android 13)",
        "API Level 31 (Android 12)",
        "API Level 24 (Android 7.0 - Minimum)"
    )

    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = uri.path?.substringAfterLast('/') ?: "project.zip"
            viewModel.processSelectedZipUri(uri, fileName)
        }
    }

    val keystorePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = uri.path?.substringAfterLast('/') ?: "keystore.jks"
            viewModel.selectKeystoreUri(uri, fileName)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkObsidianBg)
            .padding(6.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Environment Status Pills Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF111827))
                    .border(1.dp, Color(0xFF1F2937), RoundedCornerShape(12.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(AccentGreen))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Android 16 Native Environment", fontSize = 9.5.sp, color = TextWhite, fontWeight = FontWeight.SemiBold)
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF111827))
                    .border(1.dp, Color(0xFF1F2937), RoundedCornerShape(12.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(10.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("ระบบปฏิบัติการจริง (Production Mode)", fontSize = 9.5.sp, color = TextWhite)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // -------------------------------------------------------------
        // DEVICE PERMISSION STATUS CARD
        // -------------------------------------------------------------
        val context = androidx.compose.ui.platform.LocalContext.current
        var hasStoragePerm by remember {
            mutableStateOf(
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    android.os.Environment.isExternalStorageManager() ||
                            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
                } else {
                    androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
                }
            )
        }
        var hasGalleryPerm by remember {
            mutableStateOf(
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_IMAGES) == android.content.pm.PackageManager.PERMISSION_GRANTED
                } else {
                    androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
                }
            )
        }
        var hasOverlayPerm by remember {
            mutableStateOf(android.provider.Settings.canDrawOverlays(context))
        }
        var hasNotificationPerm by remember {
            mutableStateOf(
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
                } else true
            )
        }

        val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            hasStoragePerm = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                android.os.Environment.isExternalStorageManager() ||
                        androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
            hasGalleryPerm = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_IMAGES) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
            hasOverlayPerm = android.provider.Settings.canDrawOverlays(context)
            hasNotificationPerm = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else true
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF0F172A))
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                .padding(8.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("สถานะการอนุญาตสิทธิ์อุปกรณ์ (Device Permissions)", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                    }

                    Text(
                        text = "กดเพื่อขอสิทธิ์ทั้งหมด",
                        fontSize = 9.sp,
                        color = AccentBlue,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            val perms = mutableListOf<String>()
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                perms.add(android.Manifest.permission.POST_NOTIFICATIONS)
                                perms.add(android.Manifest.permission.READ_MEDIA_IMAGES)
                                perms.add(android.Manifest.permission.READ_MEDIA_VIDEO)
                            } else {
                                perms.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                                perms.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            }
                            permLauncher.launch(perms.toTypedArray())

                            if (!hasOverlayPerm) {
                                try {
                                    val intent = android.content.Intent(
                                        android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        android.net.Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PermissionPill("💾 Storage", hasStoragePerm)
                    PermissionPill("🖼️ Gallery", hasGalleryPerm)
                    PermissionPill("🪟 Overlay", hasOverlayPerm)
                    PermissionPill("🔔 Notification", hasNotificationPerm)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // -------------------------------------------------------------
        // HERO BANNER CARD
        // -------------------------------------------------------------
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = DarkNavyCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkNavyCardBorder)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                // Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(AccentGreen.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(AccentGreen))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ระบบปฏิบัติการจริง · Production Engine Kotlin 2.1 & SDK 36", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AccentGreen)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "APK Builder Pro (Production Engine)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "ระบบปฏิบัติการจริงสำหรับแปลงไฟล์ ZIP โปรเจกต์เป็น APK/XAPK พร้อมลงชื่อ SHA-256 และส่งออกไปใช้งานจริงบน Android Device ได้ 100%",
                    fontSize = 10.5.sp,
                    color = TextMuted,
                    lineHeight = 14.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(
                        onClick = { onNavigateToTab(ScreenTab.KOTLIN_SDK) },
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentGreen)
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("ดูโค้ด Native Kotlin", fontSize = 9.5.sp)
                    }

                    OutlinedButton(
                        onClick = { onNavigateToTab(ScreenTab.GALLERY) },
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentAmber)
                    ) {
                        Text("App Gallery", fontSize = 9.5.sp)
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(12.dp))
                    }

                    OutlinedButton(
                        onClick = {},
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite)
                    ) {
                        Text("GitHub", fontSize = 9.5.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // -------------------------------------------------------------
        // STEP 1 CARD: นำเข้าไฟล์ซอร์สโค้ด
        // -------------------------------------------------------------
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = DarkNavyCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkNavyCardBorder)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(AccentGreen))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ขั้นตอนที่ 1 : นำเข้าไฟล์ซอร์สโค้ด", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = AccentGreen)
                }

                Text("เลือกไฟล์ ZIP โปรเจกต์", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextWhite)

                Spacer(modifier = Modifier.height(6.dp))

                // Source Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkInputBg)
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    listOf("อุปกรณ์", "คลาวด์ (Drive/Dropbox)", "โปรเจกต์อ้างอิง").forEachIndexed { index, title ->
                        val isSelected = sourceTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) Color(0xFF1E293B) else Color.Transparent)
                                .clickable {
                                    viewModel.sourceTab.value = index
                                    if (index == 1) viewModel.openCloudImport()
                                    if (index == 2) viewModel.loadSampleProject(viewModel.sampleProjects.first())
                                }
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                fontSize = 9.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) TextWhite else TextMuted,
                                textAlign = TextAlign.Center,
                                lineHeight = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Drag & Drop Box (Dashed Area)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0D1322))
                        .border(1.dp, DarkInputBorder, RoundedCornerShape(10.dp))
                        .clickable { zipPickerLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed")) }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1A2338)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = "Upload", tint = AccentBlue, modifier = Modifier.size(20.dp))
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        val annotatedText = buildAnnotatedString {
                            append("ลากไฟล์ ZIP มาวางที่นี่ หรือ ")
                            withStyle(style = SpanStyle(color = AccentBlue, textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Bold)) {
                                append("คลิกเลือกจากอุปกรณ์")
                            }
                        }
                        Text(text = annotatedText, fontSize = 11.sp, color = TextWhite)

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = "รองรับ React Native, Flutter, Xamarin, .NET MAUI, Ionic, Capacitor, Native Android (Gradle) และ AI Studio",
                            fontSize = 9.5.sp,
                            color = TextDim,
                            textAlign = TextAlign.Center,
                            lineHeight = 13.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Framework Badges
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                FrameworkChip("🦋 Flutter & ⚛️ React Native")
                                FrameworkChip("🟣 .NET MAUI & 📱 Xamarin")
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                FrameworkChip("⚡ Ionic / Capacitor / 📦 Cordova")
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                FrameworkChip("🤖 Native Android Gradle", isActive = true)
                                FrameworkChip("✨ Google AI Studio")
                            }
                        }
                    }
                }

                // Project Validation Summary Card
                val report = projectMetadata?.validationReport
                if (report != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F172A))
                            .border(
                                1.dp,
                                if (report.isValid) AccentGreen.copy(alpha = 0.5f) else AccentAmber.copy(alpha = 0.5f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (report.isValid) Icons.Default.CheckCircle else Icons.Default.HelpOutline,
                                        contentDescription = null,
                                        tint = if (report.isValid) AccentGreen else AccentAmber,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "การตรวจสอบโปรเจกต์ (Score: ${if (report.isValid) (100 - report.errorsCount * 30 - report.warningsCount * 10).coerceIn(0, 100) else 50}/100)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite
                                    )
                                }

                                Text(
                                    text = "ดูรายละเอียด",
                                    fontSize = 9.5.sp,
                                    color = AccentBlue,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { viewModel.showValidationDetails.value = true }
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "กรอบงาน: ${report.framework.displayName} • Summary: ${report.summary}",
                                fontSize = 9.5.sp,
                                color = TextDim
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // -------------------------------------------------------------
        // STEP 2 CARD: กำหนดค่าการคอมไพล์และแพ็กเกจ
        // -------------------------------------------------------------
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = DarkNavyCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkNavyCardBorder)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(AccentGreen))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ขั้นตอนที่ 2 : กำหนดค่าการคอมไพล์และแพ็กเกจ", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = AccentGreen)
                }

                Text("การตั้งค่า APK & สถาปัตยกรรม Android", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextWhite)

                Spacer(modifier = Modifier.height(6.dp))

                // Config Sub-Tabs
                val subTabTitles = listOf(
                    "ทั่วไป & รูปแบบ",
                    "การลงชื่อ (Keystore)",
                    "สิทธิ์ของแอพ (${selectedPermissions.size})",
                    "แก้ไข build.gradle"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkInputBg)
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    subTabTitles.forEachIndexed { index, title ->
                        val isSelected = configSubTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) Color(0xFF1E293B) else Color.Transparent)
                                .clickable {
                                    viewModel.configSubTab.value = index
                                    if (index == 3) viewModel.openCodeEditor()
                                }
                                .padding(vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) TextWhite else TextMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // -------------------------------------------------------------
                // SUB-TAB CONTENTS
                // -------------------------------------------------------------
                when (configSubTab) {
                    0 -> {
                        // 1. ทั่วไป & รูปแบบ (General Format)
                        Column {
                            // Output Format Radio Cards
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("ตัวเลือก OUTPUT : รูปแบบไฟล์ผลลัพธ์", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                                Text("พร้อมติดตั้งและส่งขึ้นสโตร์", fontSize = 9.5.sp, color = TextMuted)
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            OutputRadioCard(
                                title = "APK Standalone",
                                badge = "ยอดนิยม",
                                badgeColor = AccentGreen,
                                subtitle = "ไฟล์ติดตั้งลงบนมือถือ Android ได้ทันที",
                                icon = Icons.Default.PhoneAndroid,
                                isSelected = selectedOutputType == OutputType.APK,
                                onSelect = { viewModel.selectOutputType(OutputType.APK) }
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            OutputRadioCard(
                                title = "XAPK (Bundle)",
                                badge = null,
                                badgeColor = AccentBlue,
                                subtitle = "รวม Split APKs, OBB & Manifest สำหรับสโตร์",
                                icon = Icons.Default.Layers,
                                isSelected = selectedOutputType == OutputType.XAPK,
                                onSelect = { viewModel.selectOutputType(OutputType.XAPK) }
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            OutputRadioCard(
                                title = "AAB (Play Store)",
                                badge = "Play Store",
                                badgeColor = AccentBlue,
                                subtitle = "Android App Bundle สำหรับอัปโหลดขึ้น Play Console",
                                icon = Icons.Default.Layers,
                                isSelected = false,
                                onSelect = { viewModel.selectOutputType(OutputType.XAPK) }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // App Icon Settings Box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(DarkInputBg)
                                    .border(1.dp, DarkInputBorder, RoundedCornerShape(10.dp))
                                    .padding(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF1E293B)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (projectMetadata?.appIconBitmap != null) {
                                            Image(
                                                bitmap = projectMetadata!!.appIconBitmap!!.asImageBitmap(),
                                                contentDescription = "Icon",
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Image(
                                                painter = painterResource(id = R.drawable.ic_app_logo),
                                                contentDescription = "App Logo",
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("ไอคอนแอพพลิเคชัน (APP ICON)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(AccentBlue.copy(alpha = 0.2f))
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Text("ใช้โลโก้เดียวกับ Header", fontSize = 8.5.sp, color = AccentBlue)
                                            }
                                        }
                                        Text("ใช้รูปเดียวกับโลโก้บน Header (Adaptive Icon)", fontSize = 9.5.sp, color = TextDim)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            OutlinedButton(onClick = {}, shape = RoundedCornerShape(4.dp)) {
                                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(10.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("ใช้โลโก้ Header", fontSize = 9.sp)
                                            }
                                            OutlinedButton(onClick = {}, shape = RoundedCornerShape(4.dp)) {
                                                Text("เลือกรูปใหม่", fontSize = 9.sp)
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            InputFieldWithNote(
                                label = "ชื่อแอพพลิเคชัน (App Name)",
                                note = "ชื่อไอคอนบนหน้าจอ",
                                value = appNameInput,
                                onValueChange = { viewModel.appNameInput.value = it }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            InputFieldWithNote(
                                label = "แพ็กเกจ (Package Name / ApplicationId)",
                                note = "ห้ามซ้ำบนสโตร์",
                                value = packageNameInput,
                                onValueChange = { viewModel.packageNameInput.value = it }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            InputFieldWithNote(
                                label = "เวอร์ชัน (Version Name)",
                                note = "เช่น 1.0.0",
                                value = versionNameInput,
                                onValueChange = { viewModel.versionNameInput.value = it }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            InputFieldWithNote(
                                label = "รหัสเวอร์ชัน (Version Code)",
                                note = "ตัวเลขจำนวนเต็ม",
                                value = versionCodeInput,
                                onValueChange = { viewModel.versionCodeInput.value = it }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Target SDK Dropdown (With API 36)
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Target SDK (เป้าหมายระบบ Android)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                                    Text("Android 16 (ล่าสุด)", fontSize = 9.5.sp, color = AccentGreen)
                                }

                                Spacer(modifier = Modifier.height(3.dp))

                                Box {
                                    OutlinedTextField(
                                        value = targetSdkInput,
                                        onValueChange = {},
                                        readOnly = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showSdkDropdown = true },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = DarkInputBg,
                                            unfocusedContainerColor = DarkInputBg,
                                            focusedBorderColor = AccentBlue,
                                            unfocusedBorderColor = DarkInputBorder,
                                            focusedTextColor = TextWhite,
                                            unfocusedTextColor = TextWhite
                                        )
                                    )

                                    DropdownMenu(
                                        expanded = showSdkDropdown,
                                        onDismissRequest = { showSdkDropdown = false }
                                    ) {
                                        sdkOptions.forEach { option ->
                                            DropdownMenuItem(
                                                text = { Text(option, fontSize = 11.sp) },
                                                onClick = {
                                                    viewModel.targetSdkInput.value = option
                                                    showSdkDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Hardware Acceleration Toggle
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkInputBg)
                                    .border(1.dp, DarkInputBorder, RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("⚙️ เปิดใช้ Hardware Acceleration", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                                        Text("เร่งความเร็วการเรนเดอร์ GPU บนมือถือ", fontSize = 9.5.sp, color = TextDim)
                                    }
                                    Checkbox(
                                        checked = hardwareAccelEnabled,
                                        onCheckedChange = { viewModel.hardwareAccelEnabled.value = it },
                                        colors = CheckboxDefaults.colors(checkedColor = AccentBlue)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Preview & Edit build.gradle Card
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0F172A))
                                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Description, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("พรีวิวและปรับแต่ง build.gradle", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(AccentGreen.copy(alpha = 0.2f))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text("พร้อมใช้งาน", fontSize = 8.5.sp, color = AccentGreen)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(3.dp))

                                    Text("ปรับเปลี่ยน dependencies หรือ SDK versions ด้วยตนเอง หากระบบตรวจจับโครงสร้างไม่สมบูรณ์", fontSize = 9.5.sp, color = TextDim)

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { viewModel.openCodeEditor() },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(6.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentBlue)
                                        ) {
                                            Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("เปิดตัวแก้ไข", fontSize = 10.sp)
                                        }

                                        Button(
                                            onClick = { viewModel.autoRepairGradle() },
                                            modifier = Modifier.weight(1.2f),
                                            shape = RoundedCornerShape(6.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                                        ) {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("✨ ซ่อมแซมอัตโนมัติ", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // 2. การลงชื่อ (Keystore) - With Android Debug Keystore & Screenshot 1
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Signing Mode Selector Chips
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF0F172A))
                                    .padding(3.dp),
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                val modes = listOf(
                                    Triple(com.example.model.SigningMode.DEBUG_KEYSTORE, "🛡️ Debug Keystore", "Android Debug"),
                                    Triple(com.example.model.SigningMode.AUTO_RELEASE, "🔑 คีย์อัตโนมัติ", "Auto Release"),
                                    Triple(com.example.model.SigningMode.CUSTOM_KEYSTORE, "📁 คีย์กำหนดเอง", "Custom .jks")
                                )

                                modes.forEach { (mode, label, _) ->
                                    val isSelected = selectedSigningMode == mode
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) Color(0xFF2563EB) else Color.Transparent)
                                            .clickable { viewModel.selectSigningMode(mode) }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 9.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else TextMuted,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            // Dynamic details based on selected mode
                            when (selectedSigningMode) {
                                com.example.model.SigningMode.DEBUG_KEYSTORE -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF1E293B).copy(alpha = 0.5f))
                                            .border(1.dp, Color(0xFF3B82F6).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                            .padding(8.dp)
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(AccentGreen))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "การลงชื่อด้วย Android Debug Keystore",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = TextWhite
                                                    )
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(AccentGreen.copy(alpha = 0.2f))
                                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                                ) {
                                                    Text("พร้อมใช้งานทันที", fontSize = 8.5.sp, color = AccentGreen)
                                                }
                                            }

                                            Text(
                                                text = "ใช้คีย์มาตรฐานทางการของ Android (CN=Android Debug, O=Android, C=US) ป้องกันปัญหาแยกวิเคราะห์แพ็กเกจ และสามารถติดตั้งทดสอบบนอุปกรณ์ Android จริงได้ทันที",
                                                fontSize = 9.5.sp,
                                                color = TextDim,
                                                lineHeight = 13.5.sp
                                            )

                                            Spacer(modifier = Modifier.height(2.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(DarkInputBg)
                                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                                ) {
                                                    Column {
                                                        Text("Key Alias", fontSize = 8.sp, color = TextDim)
                                                        Text("androiddebugkey", fontSize = 9.5.sp, color = AccentBlue, fontWeight = FontWeight.SemiBold)
                                                    }
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(DarkInputBg)
                                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                                ) {
                                                    Column {
                                                        Text("Keystore Password", fontSize = 8.sp, color = TextDim)
                                                        Text("android", fontSize = 9.5.sp, color = AccentGreen, fontWeight = FontWeight.SemiBold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                com.example.model.SigningMode.AUTO_RELEASE -> {
                                    Column {
                                        Text(
                                            text = "ใช้คีย์อัตโนมัติ (สร้างแล้ว)",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextWhite
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "คีย์อัตโนมัติสร้างครั้งแรกที่ build เก็บที่ ~/abp/abp.keystore สำรองไฟล์นี้ไว้ ถ้าหายจะอัปเดตแอปเดิมไม่ได้",
                                            fontSize = 9.5.sp,
                                            color = TextDim,
                                            lineHeight = 13.5.sp
                                        )
                                    }
                                }

                                com.example.model.SigningMode.CUSTOM_KEYSTORE -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "ไฟล์ keystore กำหนดเอง",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextWhite
                                        )
                                        Text(
                                            text = "ใช้คีย์ส่วนตัวสำหรับการเผยแพร่ Production บนสโตร์",
                                            fontSize = 9.5.sp,
                                            color = TextDim
                                        )
                                    }
                                }
                            }

                            // Keystore file chooser (Screenshots 1 UI)
                            Column {
                                Text(
                                    text = "ไฟล์ keystore (.jks / .keystore / .p12)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            keystorePickerLauncher.launch(arrayOf("*/*"))
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF8FAFC), contentColor = Color(0xFF0F172A)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text("เลือกไฟล์", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text(
                                        text = keystoreFileName,
                                        fontSize = 10.5.sp,
                                        color = TextMuted
                                    )
                                }
                            }

                            // Key alias
                            InputFieldWithNote(
                                label = "Key alias",
                                note = if (selectedSigningMode == com.example.model.SigningMode.DEBUG_KEYSTORE) "androiddebugkey" else "",
                                value = if (selectedSigningMode == com.example.model.SigningMode.DEBUG_KEYSTORE && keystoreAliasInput.isBlank()) "androiddebugkey" else keystoreAliasInput,
                                onValueChange = { viewModel.keystoreAliasInput.value = it }
                            )

                            // Store password
                            InputFieldWithNote(
                                label = "Store password",
                                note = if (selectedSigningMode == com.example.model.SigningMode.DEBUG_KEYSTORE) "android" else "",
                                value = if (selectedSigningMode == com.example.model.SigningMode.DEBUG_KEYSTORE && keystoreStorePasswordInput.isBlank()) "android" else keystoreStorePasswordInput,
                                onValueChange = { viewModel.keystoreStorePasswordInput.value = it }
                            )

                            // Key password
                            InputFieldWithNote(
                                label = "Key password",
                                note = "เว้นว่างถ้าเหมือน store password",
                                value = keystoreKeyPasswordInput,
                                onValueChange = { viewModel.keystoreKeyPasswordInput.value = it }
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            // Remember Keystore Status Badge
                            if (rememberedKeystoreStatus != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF064E3B).copy(alpha = 0.4f))
                                        .border(1.dp, AccentGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = AccentGreen,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = rememberedKeystoreStatus!!,
                                                fontSize = 9.5.sp,
                                                color = AccentGreenBright,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Text(
                                            text = "ลืมคีย์นี้",
                                            fontSize = 9.sp,
                                            color = TextDim,
                                            textDecoration = TextDecoration.Underline,
                                            modifier = Modifier
                                                .clickable { viewModel.clearRememberedKeystore() }
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            // Remember Checkbox
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(DarkInputBg)
                                    .clickable { viewModel.rememberKeystoreChecked.value = !rememberKeystoreChecked }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("💾 บันทึกและจดจำ Keystore นี้สำหรับการสร้างครั้งต่อไป", fontSize = 9.5.sp, color = TextWhite)
                                    Checkbox(
                                        checked = rememberKeystoreChecked,
                                        onCheckedChange = { viewModel.rememberKeystoreChecked.value = it },
                                        colors = CheckboxDefaults.colors(checkedColor = AccentBlue)
                                    )
                                }
                            }

                            // Signature Schemes Options (V1, V2, V3, V4)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkInputBg)
                                    .border(1.dp, DarkInputBorder, RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text("รูปแบบการลงชื่อดิจิทัล (APK Signature Schemes)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                                    Text("เลือกรุ่น V1, V2, V3, V4 เพื่อให้รองรับ Android ทุกเวอร์ชันและ V4 Streaming", fontSize = 9.sp, color = TextDim)
                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.clickable { viewModel.signV1.value = !signV1 }
                                        ) {
                                            Checkbox(
                                                checked = signV1,
                                                onCheckedChange = { viewModel.signV1.value = it },
                                                colors = CheckboxDefaults.colors(checkedColor = AccentBlue)
                                            )
                                            Text("V1 (JAR)", fontSize = 9.5.sp, color = TextWhite)
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.clickable { viewModel.signV2.value = !signV2 }
                                        ) {
                                            Checkbox(
                                                checked = signV2,
                                                onCheckedChange = { viewModel.signV2.value = it },
                                                colors = CheckboxDefaults.colors(checkedColor = AccentBlue)
                                            )
                                            Text("V2 (APK Sig v2)", fontSize = 9.5.sp, color = TextWhite)
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.clickable { viewModel.signV3.value = !signV3 }
                                        ) {
                                            Checkbox(
                                                checked = signV3,
                                                onCheckedChange = { viewModel.signV3.value = it },
                                                colors = CheckboxDefaults.colors(checkedColor = AccentBlue)
                                            )
                                            Text("V3 (Key Rotation)", fontSize = 9.5.sp, color = TextWhite)
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.clickable { viewModel.signV4.value = !signV4 }
                                        ) {
                                            Checkbox(
                                                checked = signV4,
                                                onCheckedChange = { viewModel.signV4.value = it },
                                                colors = CheckboxDefaults.colors(checkedColor = AccentBlue)
                                            )
                                            Text("V4 (.idsig)", fontSize = 9.5.sp, color = TextWhite)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Installer Name Spoofing Section
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkInputBg)
                                    .border(1.dp, DarkInputBorder, RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("🛡️ Installer Name Spoofing (ปลอมแปลงผู้ติดตั้ง)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                                            Text("ระบุชื่อ Package Name ของตัวติดตั้งด้วยตัวเอง (Default = com.android.vending)", fontSize = 9.sp, color = TextDim)
                                        }
                                        Checkbox(
                                            checked = isInstallerSpoofEnabled,
                                            onCheckedChange = { viewModel.isInstallerSpoofEnabled.value = it },
                                            colors = CheckboxDefaults.colors(checkedColor = AccentBlue)
                                        )
                                    }

                                    if (isInstallerSpoofEnabled) {
                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text("ตัวเลือกพรีเซ็ตด่วน:", fontSize = 9.sp, color = TextMuted)
                                        Spacer(modifier = Modifier.height(3.dp))

                                        // Quick Presets
                                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                            listOf(
                                                "com.android.vending" to "Google Play Store (แนะนำ/Default)",
                                                "com.sec.android.app.samsungapps" to "Samsung Galaxy Store",
                                                "com.amazon.venezia" to "Amazon Appstore"
                                            ).forEach { (pkg, label) ->
                                                val isSelected = installerPackageNameInput == pkg
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(if (isSelected) Color(0xFF1E293B) else Color(0xFF0F172A))
                                                        .border(1.dp, if (isSelected) AccentBlue else Color(0xFF334155), RoundedCornerShape(6.dp))
                                                        .clickable { viewModel.installerPackageNameInput.value = pkg }
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(pkg, fontSize = 9.5.sp, color = if (isSelected) AccentGreenBright else TextWhite, fontWeight = FontWeight.SemiBold)
                                                        Text(label, fontSize = 8.5.sp, color = TextMuted)
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        InputFieldWithNote(
                                            label = "Installer Package Name (กำหนดเอง)",
                                            note = "ระบุ package name ที่ต้องการ",
                                            value = installerPackageNameInput,
                                            onValueChange = { viewModel.installerPackageNameInput.value = it }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            // Action buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.applyCustomKeystore() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1), contentColor = Color.White),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    Text("อัปโหลดและใช้ keystore นี้", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { viewModel.useDebugKeystore() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentGreen),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    Text("ใช้ Debug Keystore", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                }

                                OutlinedButton(
                                    onClick = { viewModel.resetToAutoKeystore() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    Text("กลับไปใช้คีย์อัตโนมัติ", fontSize = 10.sp)
                                }
                            }
                        }
                    }

                    2 -> {
                        // 3. สิทธิ์ของแอพ - Screenshots 2 Exactly
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "ประกาศสิทธิ์ใน manifest ของแอปที่ห่อด้วย WebViewเท่านั้น ยังไม่มีโค้ดขอสิทธิ์ตอนใช้งาน (runtime) ให้หน้าเว็บ",
                                fontSize = 10.sp,
                                color = TextDim,
                                lineHeight = 14.sp
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            viewModel.availablePermissions.forEach { item ->
                                val isChecked = selectedPermissions.contains(item.key)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = !item.isRequired) { viewModel.togglePermission(item.key) }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { if (!item.isRequired) viewModel.togglePermission(item.key) },
                                        enabled = !item.isRequired,
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = AccentBlue,
                                            uncheckedColor = DarkInputBorder,
                                            disabledCheckedColor = Color(0xFF334155)
                                        )
                                    )

                                    Spacer(modifier = Modifier.width(6.dp))

                                    Column {
                                        Text(
                                            text = item.titleTh,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isChecked) TextWhite else TextMuted
                                        )
                                        Text(
                                            text = item.subtitleEn,
                                            fontSize = 9.sp,
                                            color = TextDim
                                        )
                                    }
                                }
                                HorizontalDivider(
                                    color = Color(0xFF1E293B),
                                    thickness = 0.5.dp
                                )
                            }
                        }
                    }

                    3 -> {
                        // 4. แก้ไข build.gradle
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0F172A))
                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Description, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("พรีวิวและปรับแต่ง build.gradle", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(AccentGreen.copy(alpha = 0.2f))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text("พร้อมใช้งาน", fontSize = 8.5.sp, color = AccentGreen)
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text("ปรับเปลี่ยน dependencies หรือ SDK versions ด้วยตนเอง หากระบบตรวจจับโครงสร้างไม่สมบูรณ์", fontSize = 9.5.sp, color = TextDim)

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedButton(
                                    onClick = { viewModel.openCodeEditor() },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentBlue)
                                ) {
                                    Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("เปิดตัวแก้ไข build.gradle", fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 1-CLICK AUTO DECOMPILE & AUTO BUILD APK BUTTON
                Button(
                    onClick = {
                        viewModel.runOneClickAutoPipeline()
                        onStartBuild()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("one_click_auto_build_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "⚡ Auto Decompile & Auto Build (1-Click อัตโนมัติ)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // CONVERT ACTION BUTTON
                Button(
                    onClick = {
                        viewModel.startBuildProcess()
                        onStartBuild()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("start_apk_build_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    val buildButtonTitle = when (selectedSigningMode) {
                        com.example.model.SigningMode.DEBUG_KEYSTORE -> "เริ่มแปลงเป็น APK (ลงชื่อ Debug Keystore)"
                        com.example.model.SigningMode.AUTO_RELEASE -> "เริ่มแปลงเป็น APK และลงชื่ออัตโนมัติ"
                        com.example.model.SigningMode.CUSTOM_KEYSTORE -> "เริ่มแปลงเป็น APK (ลงชื่อด้วย Keystore ของคุณ)"
                    }
                    Text(
                        text = buildButtonTitle,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "พร้อมแล้ว กดเพื่อเริ่มสร้าง (คอมไพล์ไบนารีและลงชื่อจริง)",
                    fontSize = 10.5.sp,
                    color = AccentGreen,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // FOOTER TEXT
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "APK Builder Pro (Production Engine) • สถาปัตยกรรม Kotlin 2.1 & Android SDK 36",
                fontSize = 9.5.sp,
                color = TextDim,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Native Android Architecture • รองรับการดาวน์โหลดและเปิดใน Android Studio 100%",
                fontSize = 9.5.sp,
                color = TextDim,
                textAlign = TextAlign.Center
            )
        }

        // Gradle Repair Report Dialog
        if (gradleRepairReport != null) {
            val report = gradleRepairReport!!
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { viewModel.dismissGradleRepairReport() },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                androidx.compose.material3.Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF0F172A),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFF818CF8),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ผลการซ่อมแซม build.gradle อัตโนมัติ",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (report.isModified) "ตรวจพบปัญหาและทำการแก้ไข build.gradle ให้พร้อมใช้งานทันที:" else "โครงสร้าง build.gradle ถูกต้องและมีความเข้ากันได้สูง:",
                            fontSize = 11.sp,
                            color = TextDim
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF030712))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            report.fixes.forEach { fix ->
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = AccentGreen,
                                        modifier = Modifier.size(16.dp).padding(top = 1.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = fix,
                                        fontSize = 11.sp,
                                        color = TextWhite,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.dismissGradleRepairReport()
                                    viewModel.openCodeEditor()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("ดูโค้ดที่แก้ไข", fontSize = 11.sp, color = AccentBlue)
                            }

                            Button(
                                onClick = { viewModel.dismissGradleRepairReport() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                            ) {
                                Text("ตกลง / ใช้งาน", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Project Validation Details Dialog
        if (showValidationDetails && projectMetadata?.validationReport != null) {
            val report = projectMetadata!!.validationReport!!
            val score = if (report.isValid) (100 - report.errorsCount * 30 - report.warningsCount * 10).coerceIn(0, 100) else 50
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { viewModel.showValidationDetails.value = false },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF0F172A),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("รายงานการตรวจสอบโครงสร้างโปรเจกต์", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                            Text("คะแนน: $score/100", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentGreen)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            report.checks.forEach { item ->
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(DarkInputBg)
                                        .padding(8.dp)
                                ) {
                                    Icon(
                                        imageVector = when (item.status) {
                                            com.example.model.CheckStatus.PASS -> Icons.Default.CheckCircle
                                            com.example.model.CheckStatus.WARNING -> Icons.Default.HelpOutline
                                            com.example.model.CheckStatus.ERROR -> Icons.Default.Security
                                            com.example.model.CheckStatus.INFO -> Icons.Default.CheckCircle
                                        },
                                        contentDescription = null,
                                        tint = when (item.status) {
                                            com.example.model.CheckStatus.PASS -> AccentGreen
                                            com.example.model.CheckStatus.WARNING -> AccentAmber
                                            com.example.model.CheckStatus.ERROR -> Color(0xFFEF4444)
                                            com.example.model.CheckStatus.INFO -> AccentBlue
                                        },
                                        modifier = Modifier.size(16.dp).padding(top = 1.dp)
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Column {
                                        Text(item.title, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                                        Text(item.description, fontSize = 9.5.sp, color = TextDim)
                                        if (item.suggestion != null) {
                                            Text("แนะนำ: ${item.suggestion}", fontSize = 9.sp, color = AccentAmber)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.showValidationDetails.value = false },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                        ) {
                            Text("ปิด", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FrameworkChip(title: String, isActive: Boolean = false) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isActive) AccentGreen.copy(alpha = 0.2f) else Color(0xFF1A2338))
            .border(1.dp, if (isActive) AccentGreen else Color(0xFF243048), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(title, fontSize = 9.5.sp, color = if (isActive) AccentGreenBright else TextMuted)
    }
}

@Composable
fun OutputRadioCard(
    title: String,
    badge: String?,
    badgeColor: Color,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkInputBg)
            .border(1.dp, if (isSelected) AccentBlue else DarkInputBorder, RoundedCornerShape(10.dp))
            .clickable { onSelect() }
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(16.dp))
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                    if (badge != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(badgeColor.copy(alpha = 0.2f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(badge, fontSize = 8.5.sp, color = badgeColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text(subtitle, fontSize = 9.5.sp, color = TextDim)
            }

            RadioButton(
                selected = isSelected,
                onClick = onSelect,
                colors = RadioButtonDefaults.colors(selectedColor = AccentBlue)
            )
        }
    }
}

@Composable
fun InputFieldWithNote(
    label: String,
    note: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            if (note.isNotBlank()) {
                Text(note, fontSize = 9.5.sp, color = TextMuted)
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkInputBg,
                unfocusedContainerColor = DarkInputBg,
                focusedBorderColor = AccentBlue,
                unfocusedBorderColor = DarkInputBorder,
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite
            ),
            singleLine = true
        )
    }
}

@Composable
fun PermissionPill(label: String, isGranted: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isGranted) AccentGreen.copy(alpha = 0.2f) else Color(0xFF1E293B))
            .border(1.dp, if (isGranted) AccentGreen else Color(0xFF334155), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(if (isGranted) AccentGreen else Color(0xFFEF4444))
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "$label ${if (isGranted) "✓" else "✕"}",
                fontSize = 8.5.sp,
                color = if (isGranted) AccentGreenBright else TextMuted,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
