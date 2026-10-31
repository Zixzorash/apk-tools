package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AppLanguage
import com.example.ui.screens.ScreenTab
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.DarkNavyCard
import com.example.ui.theme.DarkNavyCardBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

@Composable
fun TopHeaderBar(
    currentTab: ScreenTab,
    onTabSelected: (ScreenTab) -> Unit,
    currentLanguage: AppLanguage,
    onToggleLanguage: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = DarkNavyCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkNavyCardBorder)
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            // Main Top Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Logo + Compact Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onTabSelected(ScreenTab.HOME) }
                        .padding(end = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_app_logo),
                            contentDescription = "App Logo",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Column(verticalArrangement = Arrangement.Center) {
                        Text(
                            text = "APK",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextWhite,
                            lineHeight = 13.sp
                        )
                        Text(
                            text = buildAnnotatedString {
                                append("Builder ")
                                withStyle(
                                    style = SpanStyle(
                                        color = Color(0xFF2DD4BF),
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                ) {
                                    append("Pro")
                                }
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextWhite,
                            lineHeight = 13.sp
                        )
                    }
                }

                // 2. Compact Navigation Capsule Pill Bar
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF131726))
                        .border(1.dp, Color(0xFF22283E), RoundedCornerShape(16.dp))
                        .padding(1.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HeaderTabPill(
                        line1 = "สร้าง",
                        line2 = "APK",
                        isSelected = currentTab == ScreenTab.HOME,
                        onClick = { onTabSelected(ScreenTab.HOME) },
                        testTag = "header_tab_build"
                    )
                    HeaderTabPill(
                        line1 = "Gallery",
                        line2 = null,
                        isSelected = currentTab == ScreenTab.GALLERY,
                        onClick = { onTabSelected(ScreenTab.GALLERY) },
                        testTag = "header_tab_gallery"
                    )
                    HeaderTabPill(
                        line1 = "ไฟล์",
                        line2 = null,
                        isSelected = currentTab == ScreenTab.FILES,
                        onClick = { onTabSelected(ScreenTab.FILES) },
                        testTag = "header_tab_files"
                    )
                    HeaderTabPill(
                        line1 = "Kotlin",
                        line2 = "SDK",
                        isSelected = currentTab == ScreenTab.KOTLIN_SDK,
                        onClick = { onTabSelected(ScreenTab.KOTLIN_SDK) },
                        testTag = "header_tab_kotlin_sdk"
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Action Icons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Language Switcher Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1B2032))
                        .border(1.dp, Color(0xFF2D354E), RoundedCornerShape(10.dp))
                        .clickable { onToggleLanguage() }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .testTag("language_switch_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(11.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (currentLanguage == AppLanguage.TH) "TH" else "EN",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // GitHub Cat / Code Icon
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1B2032))
                        .border(1.dp, Color(0xFF2D354E), CircleShape)
                        .clickable { onTabSelected(ScreenTab.KOTLIN_SDK) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Code, contentDescription = "Git", tint = TextWhite, modifier = Modifier.size(13.dp))
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Security Shield Icon
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF261D12))
                        .border(1.dp, Color(0xFFD97706), CircleShape)
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Shield, contentDescription = "Security", tint = Color(0xFFFBBF24), modifier = Modifier.size(13.dp))
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Help Icon
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1B2032))
                        .border(1.dp, Color(0xFF2D354E), CircleShape)
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.HelpOutline, contentDescription = "Help", tint = Color(0xFF2DD4BF), modifier = Modifier.size(13.dp))
                }
            }
        }
    }
}

@Composable
private fun HeaderTabPill(
    line1: String,
    line2: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Color(0xFF23293E) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = line1,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) TextWhite else TextMuted,
                textAlign = TextAlign.Center
            )
            if (line2 != null) {
                Text(
                    text = line2,
                    fontSize = 9.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) TextWhite else TextMuted,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
