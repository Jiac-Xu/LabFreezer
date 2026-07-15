package com.labfreezer.ui.screens.settings
import com.labfreezer.R

import android.content.Context
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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

    val checkFailedStr = context.getString(R.string.about_check_update_failed)
    val newVersionPrefix = context.getString(R.string.about_new_version_found)

    LaunchedEffect(Unit) {
        val unknownStr = context.getString(R.string.fallback_unknown)
        versionName = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: unknownStr
        } catch (e: Exception) { unknownStr }

        // 从 SharedPreferences 读取上次保存的蒲公英自增 build 号
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val savedBuildVersion = prefs.getInt("pgyer_build_build_version", -1)
        val buildBuildVersionParam = if (savedBuildVersion >= 0) savedBuildVersion else null

        // Silent update check via PGYER AppUpdateChecker
        updateInfo = try {
            val result = withContext(Dispatchers.IO) {
                suspendCancellableCoroutine { cont ->
                    UpdateChecker(API_KEY).check(
                        APP_KEY,
                        versionName,
                        buildBuildVersionParam,  // 传入上次保存的 buildBuildVersion，初次为 null
                        null,
                        object : UpdateChecker.Callback {
                            override fun result(updateInfo: UpdateChecker.UpdateInfo) {
                                // 保存本次返回的自增 build 号，供下次检查使用
                                prefs.edit().putInt("pgyer_build_build_version", updateInfo.buildBuildVersion).apply()

                                if (updateInfo.buildHaveNewVersion) {
                                    val desc = if (updateInfo.buildUpdateDescription.isNullOrBlank()) ""
                                        else ": ${updateInfo.buildUpdateDescription.replace("\\n", "\n")}"
                                    cont.resume("$newVersionPrefix ${updateInfo.buildVersion}$desc")
                                } else {
                                    cont.resume(context.getString(R.string.about_up_to_date, versionName))
                                }
                            }

                            override fun error(message: String) {
                                cont.resume(checkFailedStr)
                            }
                        }
                    )
                }
            }
            result as String
        } catch (e: Exception) {
            checkFailedStr
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.content_description_back)) } },
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
                stringResource(R.string.about_version, versionName),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(Modifier.height(32.dp))

            Text(
                updateInfo,
                style = MaterialTheme.typography.bodyMedium,
                color = if (updateInfo.startsWith(newVersionPrefix)) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )

            if (updateInfo.isNotEmpty() && updateInfo != checkFailedStr) {
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
                    Text(
                        if (updateInfo.startsWith(newVersionPrefix))
                            stringResource(R.string.about_download)
                        else
                            stringResource(R.string.about_open_website),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}
