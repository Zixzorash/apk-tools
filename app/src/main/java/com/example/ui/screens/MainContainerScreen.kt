package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.LocalizationManager
import com.example.ui.MainViewModel
import com.example.ui.components.CloudImportDialog
import com.example.ui.components.CodeEditorModal
import com.example.ui.components.IntegrityReportDialog
import com.example.ui.components.TopHeaderBar
import com.example.ui.theme.DarkObsidianBg

enum class ScreenTab {
    HOME,
    GALLERY,
    FILES,
    KOTLIN_SDK,
    BUILD_PROGRESS
}

@Composable
fun MainContainerScreen(
    viewModel: MainViewModel
) {
    var currentTab by remember { mutableStateOf(ScreenTab.HOME) }

    val showCodeEditor by viewModel.showCodeEditor.collectAsState()
    val showCloudImport by viewModel.showCloudImport.collectAsState()
    val showIntegrityReport by viewModel.showIntegrityReport.collectAsState()
    val projectMetadata by viewModel.projectMetadata.collectAsState()
    val isDownloadingCloud by viewModel.isDownloadingCloud.collectAsState()
    val downloadError by viewModel.downloadError.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val strings = LocalizationManager.getStrings(currentLanguage)

    Scaffold(
        bottomBar = {
            if (currentTab != ScreenTab.BUILD_PROGRESS) {
                NavigationBar {
                    NavigationBarItem(
                        selected = (currentTab == ScreenTab.HOME),
                        onClick = { currentTab = ScreenTab.HOME },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("สร้าง APK") },
                        modifier = Modifier.testTag("nav_item_home")
                    )
                    NavigationBarItem(
                        selected = (currentTab == ScreenTab.GALLERY),
                        onClick = { currentTab = ScreenTab.GALLERY },
                        icon = { Icon(Icons.Default.Explore, contentDescription = "App Gallery") },
                        label = { Text("Gallery") },
                        modifier = Modifier.testTag("nav_item_gallery")
                    )
                    NavigationBarItem(
                        selected = (currentTab == ScreenTab.FILES),
                        onClick = { currentTab = ScreenTab.FILES },
                        icon = { Icon(Icons.Default.Folder, contentDescription = "Files") },
                        label = { Text("ไฟล์") },
                        modifier = Modifier.testTag("nav_item_files")
                    )
                    NavigationBarItem(
                        selected = (currentTab == ScreenTab.KOTLIN_SDK),
                        onClick = { currentTab = ScreenTab.KOTLIN_SDK },
                        icon = { Icon(Icons.Default.Code, contentDescription = "Kotlin SDK") },
                        label = { Text("Kotlin SDK") },
                        modifier = Modifier.testTag("nav_item_kotlin_sdk")
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkObsidianBg)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                if (currentTab != ScreenTab.BUILD_PROGRESS) {
                    TopHeaderBar(
                        currentTab = currentTab,
                        onTabSelected = { currentTab = it },
                        currentLanguage = currentLanguage,
                        onToggleLanguage = { viewModel.toggleLanguage() }
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (currentTab) {
                        ScreenTab.HOME -> {
                            HomeScreen(
                                viewModel = viewModel,
                                onStartBuild = { currentTab = ScreenTab.BUILD_PROGRESS },
                                onNavigateToTab = { currentTab = it }
                            )
                        }
                        ScreenTab.GALLERY -> {
                            GalleryScreen(
                                viewModel = viewModel,
                                onSelectSample = { currentTab = ScreenTab.HOME }
                            )
                        }
                        ScreenTab.FILES -> {
                            HistoryScreen(
                                viewModel = viewModel
                            )
                        }
                        ScreenTab.KOTLIN_SDK -> {
                            KotlinSdkScreen()
                        }
                        ScreenTab.BUILD_PROGRESS -> {
                            BuildProgressScreen(
                                viewModel = viewModel,
                                onBack = { currentTab = ScreenTab.HOME }
                            )
                        }
                    }
                }
            }
        }

        // Modals & Dialogs
        if (showCodeEditor && projectMetadata != null) {
            CodeEditorModal(
                initialContent = projectMetadata!!.editableGradleContent,
                strings = strings,
                onSave = { newContent ->
                    viewModel.updateMetadata(projectMetadata!!.copy(editableGradleContent = newContent))
                },
                onDismiss = { viewModel.closeCodeEditor() }
            )
        }

        if (showCloudImport) {
            CloudImportDialog(
                isDownloading = isDownloadingCloud,
                errorMessage = downloadError,
                strings = strings,
                onImport = { provider, url ->
                    viewModel.importFromCloudUrl(provider, url)
                },
                onDismiss = { viewModel.closeCloudImport() }
            )
        }

        showIntegrityReport?.let { result ->
            IntegrityReportDialog(
                result = result,
                strings = strings,
                onDismiss = { viewModel.closeIntegrityReport() }
            )
        }
    }
}
