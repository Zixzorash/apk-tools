package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BuildStatus
import com.example.model.LocalizationManager
import com.example.ui.MainViewModel
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentGreenBright
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

@Composable
fun BuildProgressScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val buildProgress by viewModel.buildProgress.collectAsState()
    val projectMetadata by viewModel.projectMetadata.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val strings = LocalizationManager.getStrings(currentLanguage)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("back_from_build_progress_button")
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextWhite)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = strings.compilerTerminal,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Text(
                    text = projectMetadata?.appName ?: "Project Build",
                    style = MaterialTheme.typography.bodySmall,
                    color = AccentCyan
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Gauge Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { buildProgress.progressPercent / 100f },
                        modifier = Modifier.size(120.dp),
                        strokeWidth = 10.dp,
                        color = when (buildProgress.status) {
                            BuildStatus.SUCCESS -> AccentGreen
                            BuildStatus.FAILED -> MaterialTheme.colorScheme.error
                            else -> AccentBlue
                        }
                    )
                    Text(
                        text = "${buildProgress.progressPercent.toInt()}%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = buildProgress.currentStepName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = when (buildProgress.status) {
                        BuildStatus.SUCCESS -> AccentGreen
                        BuildStatus.FAILED -> MaterialTheme.colorScheme.error
                        else -> AccentCyan
                    }
                )

                if (buildProgress.status != BuildStatus.SUCCESS && buildProgress.status != BuildStatus.FAILED) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = AccentBlue
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Build Terminal Output
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = Color(0xFF030712)),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = "Build Log",
                        tint = AccentCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Build Output Stream Log",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = AccentCyan
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    Column {
                        buildProgress.detailLog.forEach { log ->
                            Text(
                                text = log,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                ),
                                color = Color(0xFF38BDF8)
                            )
                        }
                        if (buildProgress.errorMessage != null) {
                            Text(
                                text = "ERROR: ${buildProgress.errorMessage}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                ),
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Success Actions
        AnimatedVisibility(visible = buildProgress.status == BuildStatus.SUCCESS) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Verification Badge Button
                buildProgress.integrityResult?.let { result ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.openIntegrityReport(result) }
                            .testTag("view_integrity_report_card"),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = AccentGreen.copy(alpha = 0.15f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentGreen.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Verified",
                                tint = AccentGreen,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.integrityVerifiedTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentGreen
                                )
                                Text(
                                    text = strings.integrityTapDetails,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextWhite
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Install Button
                    Button(
                        onClick = {
                            buildProgress.outputApkPath?.let { viewModel.installApk(it) }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("install_apk_now_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Default.InstallMobile, contentDescription = null, tint = Color.White, modifier = Modifier.padding(end = 6.dp))
                        Text(strings.installApk, color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    // Share/Save Button
                    OutlinedButton(
                        onClick = {
                            buildProgress.outputApkPath?.let { viewModel.shareApk(it) }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("save_download_apk_button"),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = AccentCyan, modifier = Modifier.padding(end = 6.dp))
                        Text(strings.saveExport, color = AccentCyan, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Failure Actions & Error Log Card
        AnimatedVisibility(visible = buildProgress.status == BuildStatus.FAILED) {
            val viewingErrorLogContent by viewModel.viewingErrorLogContent.collectAsState()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF450A0A).copy(alpha = 0.6f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "การสร้าง APK ล้มเหลว",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFCA5A5)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = buildProgress.errorMessage ?: "เกิดข้อผิดพลาดในการประมวลผลไฟล์หรือการคอมไพล์",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFFEE2E2),
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Log File Info Box
                        if (buildProgress.errorLogPath != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1F1212))
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = androidx.compose.material.icons.Icons.Default.Terminal,
                                            contentDescription = null,
                                            tint = AccentCyan,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "บันทึกไฟล์ LOG อัตโนมัติแล้วที่จัดเก็บของอุปกรณ์:",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AccentCyan
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = buildProgress.errorLogPath!!,
                                        fontSize = 9.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = TextMuted,
                                        lineHeight = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Action buttons for failure
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (buildProgress.errorLogPath != null) {
                        OutlinedButton(
                            onClick = {
                                viewModel.openErrorLogViewer(buildProgress.errorLogPath!!)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentCyan)
                        ) {
                            Text("📄 ดูไฟล์ LOG", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                viewModel.shareErrorLog(buildProgress.errorLogPath!!)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF374151))
                        ) {
                            Text("📤 แชร์ไฟล์ LOG", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Auto-Repair Gradle Button
                Button(
                    onClick = {
                        viewModel.autoRepairGradleAndRebuild()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                ) {
                    Text("🛠️ ซ่อมแซม build.gradle อัตโนมัติและลองใหม่", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            // Error Log Viewer Dialog
            if (viewingErrorLogContent != null) {
                androidx.compose.ui.window.Dialog(
                    onDismissRequest = { viewModel.closeErrorLogViewer() },
                    properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    androidx.compose.material3.Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        color = Color(0xFF0F172A)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Default.Terminal,
                                        contentDescription = null,
                                        tint = AccentCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "รายงานข้อผิดพลาด (Build Error Log)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite
                                    )
                                }
                                IconButton(onClick = { viewModel.closeErrorLogViewer() }) {
                                    Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Default.ArrowBack,
                                        contentDescription = "Close",
                                        tint = TextWhite
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF030712)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = viewingErrorLogContent!!,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        color = Color(0xFFE2E8F0)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.closeErrorLogViewer() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("ปิด", color = TextWhite)
                                }
                                Button(
                                    onClick = {
                                        buildProgress.errorLogPath?.let { viewModel.shareErrorLog(it) }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                                ) {
                                    Text("แชร์ / ส่งออกไฟล์", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

