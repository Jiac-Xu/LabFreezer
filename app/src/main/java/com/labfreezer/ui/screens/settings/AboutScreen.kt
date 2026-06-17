package com.labfreezer.ui.screens.settings

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.labfreezer.util.UpdateChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

private const val API_KEY = "ac51db043f72100c2a4ba0eca0e13282"
private const val APP_KEY = "1691ea53a9fc98f9fa65d05c76c37bbd"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    var versionName by remember { mutableStateOf("") }
    var updateInfo by remember { mutableStateOf("") }

    val appName = if (configuration.locales[0].language == "zh") {
        "冰盒"
    } else {
        "LabFreezer"
    }

    LaunchedEffect(Unit) {
        versionName = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "未知"
        } catch (e: Exception) { "未知" }

        // Silent update check via PGYER AppUpdateChecker
        updateInfo = try {
            val result = withContext(Dispatchers.IO) {
                suspendCancellableCoroutine { cont ->
                    UpdateChecker(API_KEY).check(
                        APP_KEY,
                        versionName,
                        null,  // buildBuildVersion
                        null,  // channelKey
                        object : UpdateChecker.Callback {
                            override fun result(updateInfo: UpdateChecker.UpdateInfo) {
                                if (updateInfo.buildHaveNewVersion) {
                                    val desc = if (updateInfo.buildUpdateDescription.isNullOrBlank()) ""
                                        else ": ${updateInfo.buildUpdateDescription}"
                                    cont.resume("发现新版本 ${updateInfo.buildVersion}$desc")
                                } else {
                                    cont.resume("已是最新版本 ($versionName)")
                                }
                            }

                            override fun error(message: String) {
                                cont.resume("检查更新失败")
                            }
                        }
                    )
                }
            }
            result as String
        } catch (e: Exception) {
            "检查更新失败"
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

            if (updateInfo.startsWith("发现")) {
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.pgyer.com/labfreezer"))
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("去下载", modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}
