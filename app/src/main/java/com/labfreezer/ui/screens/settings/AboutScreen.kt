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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.labfreezer.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStreamWriter

private const val API_KEY = "ac51db043f72100c2a4ba0eca0e13282"
private const val USER_KEY = "47af7f598157a74533e25b2a37d9858e"
private const val APP_KEY = "1691ea53a9fc98f9fa65d05c76c37bbd"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    var versionName by remember { mutableStateOf("") }
    var updateInfo by remember { mutableStateOf("") }

    // Use "冰盒" in Chinese, "LabFreezer" otherwise
    val appName = if (configuration.locales[0].language == "zh") {
        "冰盒"
    } else {
        "LabFreezer"
    }

    LaunchedEffect(Unit) {
        versionName = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "未知"
        } catch (e: Exception) { "未知" }

        // Silent update check via AppUpdateChecker API
        try {
            val result = withContext(Dispatchers.IO) {
                val url = URL("https://api.appupdatechecker.com/v1/check")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 8000
                conn.readTimeout = 8000

                val body = JSONObject().apply {
                    put("api_key", API_KEY)
                    put("user_key", USER_KEY)
                    put("app_key", APP_KEY)
                    put("version_name", versionName)
                    put("package_name", "com.labfreezer")
                }.toString()

                OutputStreamWriter(conn.outputStream).use { it.write(body) }

                val responseCode = conn.responseCode
                val responseBody = if (responseCode in 200..299) {
                    conn.inputStream.bufferedReader().readText()
                } else {
                    ""
                }
                conn.disconnect()
                Pair(responseCode, responseBody)
            }

            val (code, body) = result
            if (code in 200..299 && body.isNotBlank()) {
                val json = JSONObject(body)
                val data = json.optJSONObject("data")
                if (data != null) {
                    val latestVersion = data.optString("version_name", "")
                    val updateDesc = data.optString("update_description", "")
                    when {
                        latestVersion.isBlank() -> "检查失败：无版本信息"
                        latestVersion == versionName -> "已是最新版本 ($latestVersion)"
                        else -> "发现新版本 $latestVersion${if (updateDesc.isNotBlank()) ": $updateDesc" else ""}"
                    }
                } else {
                    json.optString("message", "检查失败")
                }
            } else {
                // Fallback to GitHub API
                try {
                    val ghUrl = URL("https://api.github.com/repos/xujiacheng8511/LabFreezer/releases/latest")
                    val ghConn = ghUrl.openConnection() as HttpURLConnection
                    ghConn.requestMethod = "GET"
                    ghConn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                    ghConn.connectTimeout = 8000
                    ghConn.readTimeout = 8000
                    val ghBody = ghConn.inputStream.bufferedReader().readText()
                    ghConn.disconnect()
                    val tagMatch = Regex("\"tag_name\"\\s*:\\s*\"([^\"]+)\"").find(ghBody)
                    val latest = tagMatch?.groupValues?.getOrNull(1)?.removePrefix("v") ?: ""
                    if (latest.isBlank()) "检查失败"
                    else if (latest == versionName) "已是最新版本 ($latest)"
                    else "发现新版本 $latest"
                } catch (_: Exception) {
                    "检查更新失败"
                }
            }
        } catch (e: Exception) {
            updateInfo = "检查更新失败"
        }

        if (updateInfo.isEmpty()) {
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
                painter = painterResource(R.mipmap.ic_launcher),
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Color.Unspecified
            )

            Spacer(Modifier.height(16.dp))

            Text(
                appName,
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
