package com.labfreezer.ui

import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.labfreezer.R
import com.labfreezer.export.ZipAnalysis
import com.labfreezer.export.ZipType
import com.labfreezer.data.search.ScopeType
import com.labfreezer.data.search.SearchScope
import com.labfreezer.ui.glass.LiquidBottomTab
import com.labfreezer.ui.glass.LiquidBottomTabs
import com.labfreezer.ui.glass.LocalGlassBackdrop
import com.labfreezer.ui.navigation.Screen
import com.labfreezer.ui.screens.boxgrid.BoxGridScreen
import com.labfreezer.ui.screens.devices.DeviceDetailScreen
import com.labfreezer.ui.screens.devices.DeviceListScreen
import com.labfreezer.ui.screens.export.ExportViewModel
import com.labfreezer.ui.screens.layers.LayerDetailScreen
import com.labfreezer.ui.screens.sample.SampleEditScreen
import com.labfreezer.ui.screens.search.SearchScreen
import com.labfreezer.ui.screens.move.MoveBrowserScreen
import com.labfreezer.ui.screens.tags.DeviceTypeManageScreen
import com.labfreezer.ui.screens.tags.TagDetailScreen
import com.labfreezer.ui.screens.tags.TagManageScreen
import com.labfreezer.ui.screens.settings.ImageCleanupScreen
import com.labfreezer.ui.screens.settings.OcrSettingsScreen
import com.labfreezer.ui.screens.settings.PersonalizationScreen
import com.labfreezer.ui.screens.settings.SettingsScreen
import com.labfreezer.ui.screens.settings.StartPagePickerScreen
import com.labfreezer.ui.screens.settings.StartPagePickerViewModel
import com.labfreezer.ui.screens.settings.AboutScreen
import com.labfreezer.ui.screens.settings.BottomBarEditScreen
import com.labfreezer.ui.screens.settings.BottomTab
import com.labfreezer.ui.screens.settings.BottomTabPreference
import com.labfreezer.ui.screens.settings.StartPagePreference
import com.labfreezer.FairMemoryReceiver
import com.labfreezer.ui.theme.LabFreezerTheme
import com.labfreezer.ui.theme.LocalThemeMode
import com.labfreezer.ui.theme.ThemeMode
import com.labfreezer.ui.theme.ThemePreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object NavAnimState {
    var isSwipePrevious = false
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    pendingImportUri: androidx.compose.runtime.MutableState<android.net.Uri?>? = null
) {
    val activity = LocalContext.current as Activity
    var themeModeOrdinal by remember { mutableIntStateOf(ThemePreferences.getMode(activity).ordinal) }
    val themeMode = ThemeMode.entries[themeModeOrdinal]

    CompositionLocalProvider(LocalThemeMode provides themeMode) {
        LabFreezerTheme {
        val navController = rememberNavController()
        val startPage = remember { StartPagePreference.get(activity) }
        var currentTabIndex by remember { mutableIntStateOf(0) }

        LaunchedEffect(Unit) {
            when (startPage.route) {
                Screen.Search.route -> navController.navigate(Screen.Search.createRoute(ScopeType.ALL))
                Screen.DeviceDetail.route -> navController.navigate(Screen.DeviceDetail.createRoute(startPage.id))
                Screen.LayerDetail.route -> navController.navigate(Screen.LayerDetail.createRoute(startPage.id))
                Screen.BoxGrid.route -> navController.navigate(Screen.BoxGrid.createRoute(startPage.id))
            }
        }

        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        // ── 公平运行内存：跟踪当前导航路由 ──
        LaunchedEffect(navBackStackEntry) {
            val entry = navBackStackEntry
            if (entry != null) {
                FairMemoryReceiver.currentRoute = entry.destination.route
                val args = entry.arguments
                if (args != null && !args.isEmpty) {
                    val json = org.json.JSONObject()
                    for (key in args.keySet()) {
                        when (val v = args.get(key)) {
                            is Long -> json.put(key, v)
                            is Int -> json.put(key, v)
                            is String -> json.put(key, v)
                            is Boolean -> json.put(key, v)
                        }
                    }
                    FairMemoryReceiver.currentRouteArgsJson = json.toString()
                } else {
                    FairMemoryReceiver.currentRouteArgsJson = null
                }
            }
        }
        // ────────────────────────────────

        // 每次回到 MainTabs 时重新读取底栏配置（确保底栏编辑后立即生效）
        val tabConfig = remember(currentRoute) { BottomTabPreference.get(activity) }

        val initialTabIndex = remember(startPage, tabConfig) {
            when (startPage.route) {
                Screen.TagManage.route -> tabConfig.indexOf(BottomTab.TAG_MANAGE).let { if (it < 0) 0 else it }
                Screen.Settings.route -> tabConfig.indexOf(BottomTab.SETTINGS).let { if (it < 0) 0 else it }
                else -> 0
            }
        }
        // ★ wrap在remember(Unit)中，仅在首帧执行一次，后续用户点击底栏不再干预
        remember(Unit) {
            if (currentTabIndex != initialTabIndex && currentTabIndex == 0) {
                currentTabIndex = initialTabIndex
            }
        }

        val showBottomBar = currentRoute == Screen.MainTabs.route

        // 当前 tab 是否有 FAB：有则底栏向左避让，FAB 位于右侧同一行
        val hasFab = showBottomBar && tabConfig.getOrNull(currentTabIndex)?.let {
            it == BottomTab.DEVICE_LIST || it == BottomTab.TAG_MANAGE
        } ?: false

        // ── 公平运行内存：冷启动时恢复被查杀前的导航现场 ──
        LaunchedEffect(Unit) {
            val saved = com.labfreezer.FairMemoryReceiver.run {
                val appCtx = activity.applicationContext
                val prefs = appCtx.getSharedPreferences("fair_memory_prefs", android.content.Context.MODE_PRIVATE)
                val route = prefs.getString("saved_route", null) ?: return@LaunchedEffect
                val argsJson = prefs.getString("saved_args", null)
                prefs.edit().clear().apply()
                Pair(route, argsJson)
            }
            val (savedRoute, savedArgsJson) = saved
            if (savedRoute.contains("{")) {
                // 参数化路由：用 JSON 恢复参数值拼出实际路由字符串
                val argsObj = try {
                    org.json.JSONObject(savedArgsJson ?: "")
                } catch (_: Exception) { null }
                if (argsObj != null && argsObj.length() > 0) {
                    val actualRoute = buildString {
                        var remaining = savedRoute
                        while (true) {
                            val start = remaining.indexOf('{')
                            if (start == -1) { append(remaining); break }
                            append(remaining.substring(0, start))
                            val end = remaining.indexOf('}', start)
                            if (end == -1) break
                            val key = remaining.substring(start + 1, end)
                            val value = argsObj.opt(key)
                            if (value == null || value == org.json.JSONObject.NULL) break
                            append(value.toString())
                            remaining = remaining.substring(end + 1)
                        }
                    }
                    navController.navigate(actualRoute) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            } else {
                navController.navigate(savedRoute) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
        // ──────────────────────────────────────────────

        Box(modifier = Modifier.fillMaxSize()) {
            // 液态玻璃：记录 NavHost 内容层，悬浮底栏作为兄弟节点从中采样折射
            val appBackdrop = rememberLayerBackdrop()

            // 全局玻璃采样层：NavHost 内容 + 悬浮底栏（非屏幕内，取不到屏幕的 GlassFabScaffold 层）
            CompositionLocalProvider(LocalGlassBackdrop provides appBackdrop) {
            NavHost(
                navController = navController,
                startDestination = Screen.MainTabs.route,
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(appBackdrop),
                enterTransition = {
                    val isSampleToSample = initialState.destination.route?.startsWith("sample_edit") == true &&
                            targetState.destination.route?.startsWith("sample_edit") == true
                    if (isSampleToSample && NavAnimState.isSwipePrevious) {
                        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, animationSpec = tween(350, easing = FastOutSlowInEasing))
                    } else {
                        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, animationSpec = tween(350, easing = FastOutSlowInEasing))
                    }
                },
                exitTransition = {
                    val isSampleToSample = initialState.destination.route?.startsWith("sample_edit") == true &&
                            targetState.destination.route?.startsWith("sample_edit") == true
                    if (isSampleToSample && NavAnimState.isSwipePrevious) {
                        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, animationSpec = tween(350, easing = FastOutSlowInEasing))
                    } else {
                        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, animationSpec = tween(350, easing = FastOutSlowInEasing))
                    }
                },
                popEnterTransition = {
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, animationSpec = tween(350, easing = FastOutSlowInEasing))
                },
                popExitTransition = {
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, animationSpec = tween(350, easing = FastOutSlowInEasing))
                }
            ) {
                composable(Screen.MainTabs.route) {
                    MainTabPager(
                        currentTabIndex = currentTabIndex,
                        tabConfig = tabConfig,
                        onTabChange = { currentTabIndex = it },
                        navController = navController,
                        onNavigateToPersonalization = { navController.navigate(Screen.Personalization.route) },
                        onNavigateToCheckUpdate = { navController.navigate(Screen.About.route) },
                        onNavigateToImageCleanup = { navController.navigate(Screen.ImageCleanup.route) },
                        onNavigateToOcrSettings = { navController.navigate(Screen.OcrSettings.route) },
                        onImportFileSelected = { uri -> pendingImportUri?.value = uri }
                    )
                }
                composable(
                    route = Screen.DeviceDetail.route,
                    arguments = listOf(androidx.navigation.navArgument("deviceId") { type = androidx.navigation.NavType.LongType })
                ) { backStackEntry ->
                    val deviceId = backStackEntry.arguments?.getLong("deviceId") ?: return@composable
                    DeviceDetailScreen(navController, deviceId)
                }
                composable(
                    route = Screen.LayerDetail.route,
                    arguments = listOf(androidx.navigation.navArgument("layerId") { type = androidx.navigation.NavType.LongType })
                ) { backStackEntry ->
                    val layerId = backStackEntry.arguments?.getLong("layerId") ?: return@composable
                    LayerDetailScreen(navController, layerId)
                }
                composable(
                    route = Screen.BoxGrid.route,
                    arguments = listOf(androidx.navigation.navArgument("boxId") { type = androidx.navigation.NavType.LongType })
                ) { backStackEntry ->
                    val boxId = backStackEntry.arguments?.getLong("boxId") ?: return@composable
                    BoxGridScreen(navController, boxId)
                }
                composable(
                    route = Screen.SampleEdit.route,
                    arguments = listOf(
                        androidx.navigation.navArgument("sampleId") { type = androidx.navigation.NavType.LongType },
                        androidx.navigation.navArgument("browseCtx") { type = androidx.navigation.NavType.StringType; defaultValue = "" }
                    )
                ) { backStackEntry ->
                    val sampleId = backStackEntry.arguments?.getLong("sampleId") ?: return@composable
                    val browseCtxKey = backStackEntry.arguments?.getString("browseCtx")?.ifBlank { null }
                    SampleEditScreen(navController, sampleId, browseCtxKey)
                }
                composable(
                    route = Screen.Search.route,
                    arguments = listOf(
                        androidx.navigation.navArgument("scope") { type = androidx.navigation.NavType.StringType; defaultValue = ScopeType.ALL.name },
                        androidx.navigation.navArgument("scopeId") { type = androidx.navigation.NavType.LongType; defaultValue = -1L },
                        androidx.navigation.navArgument("scopeName") { type = androidx.navigation.NavType.StringType; defaultValue = "" }
                    )
                ) { backStackEntry ->
                    val scope = backStackEntry.arguments?.getString("scope") ?: ScopeType.ALL.name
                    val scopeId = backStackEntry.arguments?.getLong("scopeId") ?: -1L
                    val scopeName = backStackEntry.arguments?.getString("scopeName") ?: ""
                    SearchScreen(
                        navController = navController,
                        showBackButton = true,
                        scope = SearchScope(
                            type = ScopeType.valueOf(scope),
                            id = scopeId.takeIf { it != -1L },
                            name = scopeName.ifBlank { null }
                        )
                    )
                }
                composable(Screen.MoveBrowser.route) {
                    MoveBrowserScreen(onBack = { navController.popBackStack() })
                }
                composable(Screen.DeviceTypeManage.route) {
                    DeviceTypeManageScreen(onBack = { navController.popBackStack() })
                }
                composable(Screen.TagManage.route) {
                    TagManageScreen(navController, showBackButton = true, alignFabWithBar = false)
                }
                composable(
                    route = Screen.TagDetail.route,
                    arguments = listOf(androidx.navigation.navArgument("tagId") { type = androidx.navigation.NavType.LongType })
                ) { backStackEntry ->
                    val tagId = backStackEntry.arguments?.getLong("tagId") ?: return@composable
                    TagDetailScreen(navController, tagId)
                }
                composable(Screen.StartPagePicker.route) {
                    val vm: StartPagePickerViewModel = hiltViewModel()
                    StartPagePickerScreen(
                        onBack = { navController.popBackStack() },
                        deviceRepo = vm.deviceRepo,
                        layerRepo = vm.layerRepo,
                        boxRepo = vm.boxRepo,
                        sampleRepo = vm.sampleRepo
                    )
                }
                composable(Screen.ImageCleanup.route) {
                    ImageCleanupScreen(onBack = { navController.popBackStack() })
                }
                composable(Screen.OcrSettings.route) {
                    OcrSettingsScreen(onBack = { navController.popBackStack() })
                }
                composable(Screen.About.route) {
                    AboutScreen(onBack = { navController.popBackStack() })
                }
                composable(Screen.Personalization.route) {
                    PersonalizationScreen(
                        onBack = { navController.popBackStack() },
                        onNavigateToStartPagePicker = { navController.navigate(Screen.StartPagePicker.route) },
                        onNavigateToBottomBarEdit = { navController.navigate(Screen.BottomBarEdit.route) },
                        onThemeChanged = { mode ->
                            themeModeOrdinal = mode.ordinal
                            ThemePreferences.setMode(activity, mode)
                        }
                    )
                }
                composable(Screen.BottomBarEdit.route) {
                    BottomBarEditScreen(onBack = { navController.popBackStack() })
                }
            }
            if (showBottomBar) {
                FloatingBottomNav(
                    currentIndex = currentTabIndex,
                    tabList = tabConfig,
                    onTabSelected = { currentTabIndex = it },
                    hasFab = hasFab,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
            pendingImportUri?.value?.let { uri ->
                ImportPreviewDialog(
                    uri = uri,
                    onDismiss = { pendingImportUri.value = null }
                )
            }
            }
        }
        }
    }
}

@Composable
private fun FloatingBottomNav(
    currentIndex: Int,
    tabList: List<BottomTab>,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    hasFab: Boolean = false
) {
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current

    // 玻璃采样层：MainScreen 提供全局内容层，无则退化画布底色
    val ambientBackdrop = LocalGlassBackdrop.current
    val fallbackBackdrop = rememberCanvasBackdrop {
        drawRect(colorScheme.surface.copy(alpha = 0.4f))
    }
    val backdrop = ambientBackdrop ?: fallbackBackdrop

    // 有 FAB 时底栏向右避让，给右侧 FAB（56dp + 边距 16dp + 间距 16dp）留出 88dp
    val endPadding by animateDpAsState(
        targetValue = if (hasFab) 88.dp else 16.dp,
        label = "bottom_nav_end_padding"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = endPadding, bottom = 16.dp)
    ) {
        if (tabList.isNotEmpty()) {
            LiquidBottomTabs(
                selectedTabIndex = { currentIndex },
                onTabSelected = onTabSelected,
                backdrop = backdrop,
                tabsCount = tabList.size,
                accentColor = colorScheme.primary,
                containerColor = colorScheme.surface.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                tabList.forEach { item ->
                    LiquidBottomTab(onClick = { onTabSelected(tabList.indexOf(item)) }) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = colorScheme.onSurfaceVariant
                        )
                        Text(
                            item.getLabel(context),
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MainTabPager(
    currentTabIndex: Int,
    tabConfig: List<BottomTab>,
    onTabChange: (Int) -> Unit,
    navController: NavController,
    onNavigateToPersonalization: () -> Unit = {},
    onNavigateToCheckUpdate: () -> Unit = {},
    onNavigateToImageCleanup: () -> Unit = {},
    onNavigateToOcrSettings: () -> Unit = {},
    onImportFileSelected: (Uri) -> Unit = {},
) {
    // 🎯 使用 AnimatedContent 彻底重构，零预加载，完美复刻 MomentLog 动效
    val colorScheme = MaterialTheme.colorScheme
    AnimatedContent(
        targetState = currentTabIndex,
        transitionSpec = {
            val direction = targetState - initialState
            (slideInHorizontally(
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                initialOffsetX = { if (direction > 0) it else -it }
            ) + fadeIn(
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            )) togetherWith (
            slideOutHorizontally(
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                targetOffsetX = { if (direction > 0) -it else it }
            ) + fadeOut(
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            ))
        },
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .pointerInput(currentTabIndex, tabConfig.size) {
                var totalDragX = 0f
                detectHorizontalDragGestures(
                    onDragStart = { totalDragX = 0f },
                    onDragEnd = {
                        val threshold = 150f
                        if (totalDragX > threshold && currentTabIndex > 0) {
                            onTabChange(currentTabIndex - 1)
                        } else if (totalDragX < -threshold && currentTabIndex < tabConfig.lastIndex) {
                            onTabChange(currentTabIndex + 1)
                        }
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        totalDragX += dragAmount
                    }
                )
            },
        label = "MainTabTransition"
    ) { targetPage ->
        when (tabConfig.getOrNull(targetPage)) {
            BottomTab.DEVICE_LIST -> DeviceListScreen(navController)
            BottomTab.TAG_MANAGE -> TagManageScreen(navController, showBackButton = false)
            BottomTab.SEARCH -> SearchScreen(navController, showBackButton = false, scope = SearchScope(ScopeType.ALL))
            BottomTab.SETTINGS -> SettingsScreen(
                onNavigateToPersonalization = onNavigateToPersonalization,
                onNavigateToCheckUpdate = onNavigateToCheckUpdate,
                onNavigateToImageCleanup = onNavigateToImageCleanup,
                onNavigateToOcrSettings = onNavigateToOcrSettings,
                onImportFileSelected = onImportFileSelected
            )
            null -> Box(modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun ImportPreviewDialog(
    uri: Uri,
    onDismiss: () -> Unit,
    exportViewModel: ExportViewModel = hiltViewModel()
) {
    var analysis by remember { mutableStateOf<ZipAnalysis?>(null) }
    var currentCount by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var isImporting by remember { mutableStateOf(false) }
    var showSecondConfirm by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uri) {
        exportViewModel.inspectImportPackage(uri) { result, count ->
            analysis = result
            currentCount = count
            isLoading = false
        }
    }

    val context = LocalContext.current

    // 导入中
    if (isImporting) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.import_loading), fontWeight = FontWeight.SemiBold) },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
        return
    }

    // 分析中
    if (isLoading) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.import_analyzing), fontWeight = FontWeight.SemiBold) },
            text = {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.import_reading), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.import_cancel)) }
            }
        )
        return
    }

    val result = analysis

    // INVALID
    if (result == null || result.type == ZipType.INVALID) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.import_failed_title), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error) },
            text = { Text(stringResource(R.string.import_invalid_hint)) },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.import_ok)) }
            }
        )
        return
    }

    // UNSUPPORTED
    if (result.type == ZipType.UNSUPPORTED) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.import_failed_title), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error) },
            text = { Text(stringResource(R.string.import_unknown_hint)) },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.import_ok)) }
            }
        )
        return
    }

    // 二次确认：数据库覆盖
    if (showSecondConfirm && result.type == ZipType.DATABASE_BACKUP) {
        AlertDialog(
            onDismissRequest = { showSecondConfirm = false },
            title = { Text(stringResource(R.string.import_overwrite_title), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error) },
            text = {
                Column {
                    Text(
                        stringResource(R.string.import_overwrite_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.import_current_count_label), style = MaterialTheme.typography.bodyMedium)
                        Text("$currentCount", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.import_import_count_label), style = MaterialTheme.typography.bodyMedium)
                        Text("${result.sampleCount}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isImporting = true
                        showSecondConfirm = false
                        errorMessage = null
                        scope.launch {
                            val ok = exportViewModel.importDatabase(uri)
                            if (ok) {
                                Toast.makeText(context, context.getString(R.string.import_success_restart), Toast.LENGTH_LONG).show()
                                delay(600)
                                onDismiss()
                            } else {
                                isImporting = false
                                errorMessage = context.getString(R.string.import_operation_failed)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.import_overwrite_confirm), color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSecondConfirm = false }) { Text(stringResource(R.string.import_cancel)) }
            }
        )
        return
    }

    // 预览
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.import_preview_title), fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                // 类型标识
                val typeLabel = when (result.type) {
                    ZipType.DATABASE_BACKUP -> stringResource(R.string.import_type_database)
                    ZipType.MARKDOWN_EXPORT -> stringResource(R.string.import_type_markdown)
                    else -> ""
                }
                Text(
                    text = typeLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                if (result.version != null || result.exportTime != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        buildString {
                            if (result.version != null) {
                                append(stringResource(R.string.import_version_prefix))
                                append(result.version)
                            }
                            if (result.version != null && result.exportTime != null) append(" · ")
                            if (result.exportTime != null) {
                                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                                append(stringResource(R.string.import_export_time_prefix))
                                append(sdf.format(java.util.Date(result.exportTime)))
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.import_current_samples), style = MaterialTheme.typography.bodyMedium)
                    Text("$currentCount", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.import_import_samples), style = MaterialTheme.typography.bodyMedium)
                    Text("${result.sampleCount}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                if (result.type == ZipType.MARKDOWN_EXPORT) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.import_markdown_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                if (result.type == ZipType.DATABASE_BACKUP) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.import_database_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                errorMessage?.let { msg ->
                    Spacer(Modifier.height(8.dp))
                    Text(msg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when (result.type) {
                        ZipType.MARKDOWN_EXPORT -> {
                            isImporting = true
                            errorMessage = null
                            scope.launch {
                                val ok = exportViewModel.importMarkdown(uri)
                                if (ok) {
                                    Toast.makeText(context, context.getString(R.string.import_success), Toast.LENGTH_LONG).show()
                                    delay(600)
                                    onDismiss()
                                } else {
                                    isImporting = false
                                    errorMessage = context.getString(R.string.import_operation_failed)
                                }
                            }
                        }
                        ZipType.DATABASE_BACKUP -> showSecondConfirm = true
                        else -> {}
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(stringResource(R.string.import_confirm), color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.import_cancel)) }
        }
    )
}
