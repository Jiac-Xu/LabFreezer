package com.labfreezer.ui.screens.settings

import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var versionName by remember { mutableStateOf("") }
    var updateInfo by remember { mutableStateOf("检查中...") }

    LaunchedEffect(Unit) {
        // Read current version
        versionName = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "未知"
        } catch (e: Exception) { "未知" }

        // Check for updates silently
        try {
            val latestVersion = withContext(Dispatchers.IO) {
                val url = URL("https://api.github.com/repos/xujiacheng8511/LabFreezer/releases/latest")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                val body = conn.inputStream.bufferedReader().readText()
                conn.disconnect()
                // Parse tag_name field
                val tagMatch = Regex(""""tag_name"\s*:\s*"([^"]+)"""").find(body)
                tagMatch?.groupValues?.getOrNull(1)?.removePrefix("v") ?: ""
            }
            updateInfo = if (latestVersion.isBlank()) {
                "无法获取版本信息"
            } else {
                val current = versionName
                if (latestVersion == current) {
                    "已是最新版本 ($latestVersion)"
                } else {
                    "发现新版本 $latestVersion"
                }
            }
        } catch (e: Exception) {
            updateInfo = "检查更新失败"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("关于", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            Icon(
                Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(16.dp))

            Text(
                "LabFreezer",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "版本 $versionName",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(Modifier.height(32.dp))

            Text(
                updateInfo,
                style = MaterialTheme.typography.bodyMedium,
                color = if (updateInfo.startsWith("发现")) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )
        }
    }
}
