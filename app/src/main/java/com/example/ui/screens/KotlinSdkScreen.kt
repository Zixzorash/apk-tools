package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentGreenBright
import com.example.ui.theme.DarkInputBg
import com.example.ui.theme.DarkInputBorder
import com.example.ui.theme.DarkNavyCard
import com.example.ui.theme.DarkNavyCardBorder
import com.example.ui.theme.TextDim
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

@Composable
fun KotlinSdkScreen() {
    val context = LocalContext.current

    val sampleKotlinCode = """
/*
class MainActivity : ComponentActivity() {
    private val viewModel: ApkBuilderViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            APKBuilderProTheme {
                val navController = rememberNavController()
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    AppNavigationHost(
                        navController = navController,
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
    """.trimIndent()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp)
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
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkNavyCard)
                    .border(1.dp, DarkNavyCardBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(AccentGreen))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Android 15 Native Environment", fontSize = 11.sp, color = TextWhite, fontWeight = FontWeight.SemiBold)
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkNavyCard)
                    .border(1.dp, DarkNavyCardBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Smartphone, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("โหมดมือถือ Android (Device Frame)", fontSize = 11.sp, color = TextWhite)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Hero Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkNavyCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkNavyCardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(AccentGreen.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Native Kotlin & Android SDK 35", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentGreen)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF3B82F6).copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Jetpack Compose", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF93C5FD))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "โครงสร้างแอพ Native Android (Kotlin & Android SDK)",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "สถาปัตยกรรมแอพพลิเคชัน Native แท้ 100% เขียนด้วยภาษา **Kotlin**, **Jetpack Compose Material 3**, และ **Android SDK (API Level 35 - Android 15)** พร้อมนำไปเปิดและคอมไพล์ใน **Android Studio** ได้ทันที",
                    fontSize = 12.sp,
                    color = TextMuted,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        Toast.makeText(context, "ดาวน์โหลดซอร์สโค้ดโปรเจกต์ Android Studio แล้ว", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ดาวน์โหลดโปรเจกต์ Android Studio (.zip)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // App Structure Settings Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkNavyCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkNavyCardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(AccentGreen))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("การปรับแต่งโครงสร้างแอพ (App Identity & Resources)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AccentGreen.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("ซิงค์ตรงกับ Header 100%", fontSize = 9.sp, color = AccentGreen, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Item 1: App Icon
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkInputBg)
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_app_logo),
                                contentDescription = "App Logo",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("1). App Icon", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentGreen)
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF1E293B))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Vector & Adaptive", fontSize = 9.sp, color = TextMuted)
                                }
                            }
                            Text("ใช้รูปเดียวกับโลโก้แอพบน Header", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                            Text("สร้างเป็น Vector Drawable (ic_app_logo.xml), Adaptive Icon (ic_launcher.xml) และไอคอนเว็บ (/icon.svg) ลวดลายหุ่นยนต์สายฟ้าความคมชัดสูง", fontSize = 10.sp, color = TextDim, lineHeight = 14.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Item 2: App Name
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkInputBg)
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1E293B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("APP\nNAME", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = AccentBlue, textAlign = TextAlign.Center)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("2). App Name", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentGreen)
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF1E293B))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("ตรงกับ Header", fontSize = 9.sp, color = AccentGreen)
                                }
                            }
                            Text("APK Builder Pro", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                            Text("กำหนดชื่อใน strings.xml, AndroidManifest.xml, Compose TopAppBar, index.html, และ metadata.json", fontSize = 10.sp, color = TextDim, lineHeight = 14.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4-Grid Metrics Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricBox(
                modifier = Modifier.weight(1f),
                label = "ภาษาหลัก (LANGUAGE)",
                title = "Kotlin 2.1",
                subtitle = "100% Type-safe",
                subtitleColor = AccentGreen
            )
            MetricBox(
                modifier = Modifier.weight(1f),
                label = "UI FRAMEWORK",
                title = "Jetpack Compose",
                subtitle = "Material 3 Modern",
                subtitleColor = TextWhite
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricBox(
                modifier = Modifier.weight(1f),
                label = "ANDROID SDK",
                title = "Target SDK 36",
                subtitle = "Android 16 (ล่าสุด)",
                subtitleColor = AccentGreen
            )
            MetricBox(
                modifier = Modifier.weight(1f),
                label = "BUILD SYSTEM",
                title = "Gradle 9 & gradlew",
                subtitle = "CI/CD GitHub Actions",
                subtitleColor = AccentAmber
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Source Code Viewer Box
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF030712)),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkNavyCardBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ไฟล์ซอร์สโค้ด:", fontSize = 11.sp, color = TextMuted)
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF1E293B))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("MainActivity.kt", fontSize = 11.sp, color = TextWhite, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Source", sampleKotlinCode)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "คัดลอกซอร์สโค้ดแล้ว", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("คัดลอกโค้ด", fontSize = 10.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0A0D14))
                        .padding(10.dp)
                ) {
                    Text(
                        text = sampleKotlinCode,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFF38BDF8),
                        lineHeight = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("> คำสั่งคอมไพล์ใน Android Studio Terminal:", fontSize = 11.sp, color = TextMuted)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF064E3B))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("./gradlew assembleDebug", fontSize = 11.sp, color = AccentGreenBright, fontFamily = FontFamily.Monospace)
                    }
                }

                Text("Output: app/build/outputs/apk/debug/app-debug.apk", fontSize = 10.sp, color = TextDim, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
fun MetricBox(
    modifier: Modifier = Modifier,
    label: String,
    title: String,
    subtitle: String,
    subtitleColor: Color
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkNavyCard)
            .border(1.dp, DarkNavyCardBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(label, fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            Text(subtitle, fontSize = 10.sp, color = subtitleColor)
        }
    }
}
