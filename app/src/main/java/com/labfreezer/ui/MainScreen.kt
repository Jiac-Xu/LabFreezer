package com.labfreezer.ui

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.labfreezer.ui.navigation.Screen
import com.labfreezer.ui.screens.boxgrid.BoxGridScreen
import com.labfreezer.ui.screens.devices.DeviceDetailScreen
import com.labfreezer.ui.screens.devices.DeviceListScreen
import com.labfreezer.ui.screens.layers.LayerDetailScreen
import com.labfreezer.ui.screens.sample.SampleEditScreen
import com.labfreezer.ui.screens.search.SearchScreen
import com.labfreezer.ui.screens.move.MoveBrowserScreen
import com.labfreezer.ui.screens.tags.DeviceTypeManageScreen
import com.labfreezer.ui.screens.tags.TagDetailScreen
import com.labfreezer.ui.screens.tags.TagManageScreen
import com.labfreezer.ui.screens.settings.ImageCleanupScreen
import com.labfreezer.ui.screens.settings.OcrSettingsScreen
import com.labfreezer.ui.screens.settings.SettingsScreen
import com.labfreezer.ui.screens.settings.StartPagePickerScreen
import com.labfreezer.ui.screens.settings.StartPagePickerViewModel
import com.labfreezer.ui.screens.settings.AboutScreen
import com.labfreezer.ui.screens.settings.StartPagePreference
import com.labfreezer.ui.theme.LabFreezerTheme
import com.labfreezer.ui.theme.LocalThemeMode
import com.labfreezer.ui.theme.ThemeMode
import com.labfreezer.ui.theme.ThemePreferences

object NavAnimState {
    var isSwipePrevious = false
}

private data class BottomNavItem(val label: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
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
                Screen.Search.route -> navController.navigate(Screen.Search.route)
                Screen.DeviceDetail.route -> navController.navigate(Screen.DeviceDetail.createRoute(startPage.id))
                Screen.LayerDetail.route -> navController.navigate(Screen.LayerDetail.createRoute(startPage.id))
                Screen.BoxGrid.route -> navController.navigate(Screen.BoxGrid.createRoute(startPage.id))
            }
        }

        val initialTabIndex = remember(startPage) {
            when (startPage.route) {
                Screen.TagManage.route -> 1
                Screen.Settings.route -> 2
                else -> 0
            }
        }
        // ★ wrap在remember(Unit)中，仅在首帧执行一次，后续用户点击底栏不再干预
        remember(Unit) {
            if (currentTabIndex != initialTabIndex && currentTabIndex == 0) {
                currentTabIndex = initialTabIndex
            }
        }

        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val showBottomBar = currentRoute == Screen.MainTabs.route

        val bottomTabs = listOf(
            BottomNavItem("\u5e93", Icons.Default.Home),
            BottomNavItem("\u6807\u7b7e", Icons.Default.Tag),
            BottomNavItem("\u8bbe\u7f6e", Icons.Default.Settings),
        )

        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = Screen.MainTabs.route,
                modifier = Modifier.fillMaxSize(),
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
                        onTabChange = { currentTabIndex = it },
                        navController = navController,
                        onThemeChanged = { mode ->
                            themeModeOrdinal = mode.ordinal
                            ThemePreferences.setMode(activity, mode)
                        },
                        onNavigateToStartPagePicker = { navController.navigate(Screen.StartPagePicker.route) },
                        onNavigateToImageCleanup = { navController.navigate(Screen.ImageCleanup.route) },
                        onNavigateToOcrSettings = { navController.navigate(Screen.OcrSettings.route) },
                        onNavigateToAbout = { navController.navigate(Screen.About.route) }
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
                    arguments = listOf(androidx.navigation.navArgument("sampleId") { type = androidx.navigation.NavType.LongType })
                ) { backStackEntry ->
                    val sampleId = backStackEntry.arguments?.getLong("sampleId") ?: return@composable
                    SampleEditScreen(navController, sampleId)
                }
                composable(Screen.Search.route) {
                    SearchScreen(navController)
                }
                composable(Screen.MoveBrowser.route) {
                    MoveBrowserScreen(onBack = { navController.popBackStack() })
                }
                composable(Screen.DeviceTypeManage.route) {
                    DeviceTypeManageScreen(onBack = { navController.popBackStack() })
                }
                composable(Screen.TagManage.route) {
                    TagManageScreen(navController)
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
            }
            if (showBottomBar) {
                FloatingBottomNav(
                    currentIndex = currentTabIndex,
                    items = bottomTabs,
                    onTabSelected = { currentTabIndex = it },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
        }
    }
}

@Composable
private fun FloatingBottomNav(
    currentIndex: Int,
    items: List<BottomNavItem>,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
    ) {
        Surface(
            color = colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            shape = RoundedCornerShape(24.dp)
        ) {
            Box {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    colorScheme.surface.copy(alpha = 0.9f),
                                    colorScheme.surface.copy(alpha = 0.7f)
                                )
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEachIndexed { index, item ->
                        val selected = index == currentIndex
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onTabSelected(index) }
                        ) {
                            if (selected) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(colorScheme.primaryContainer.copy(alpha = 0.5f))
                                )
                            }
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Spacer(Modifier.weight(1f))
                                Icon(
                                    item.icon,
                                    contentDescription = item.label,
                                    modifier = Modifier.size(24.dp),
                                    tint = if (selected) colorScheme.primary
                                           else colorScheme.outline
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    item.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (selected) colorScheme.primary else colorScheme.outline
                                )
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MainTabPager(
    currentTabIndex: Int,
    onTabChange: (Int) -> Unit,
    navController: NavController,
    onThemeChanged: (ThemeMode) -> Unit,
    onNavigateToStartPagePicker: () -> Unit,
    onNavigateToImageCleanup: () -> Unit,
    onNavigateToOcrSettings: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
) {
    // 🎯 使用 AnimatedContent 彻底重构，零预加载，完美复刻 MomentLog 动效
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
            .pointerInput(currentTabIndex) {
                var totalDragX = 0f
                detectHorizontalDragGestures(
                    onDragStart = { totalDragX = 0f },
                    onDragEnd = {
                        val threshold = 150f
                        if (totalDragX > threshold && currentTabIndex > 0) {
                            onTabChange(currentTabIndex - 1)
                        } else if (totalDragX < -threshold && currentTabIndex < 2) {
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
        when (targetPage) {
            0 -> DeviceListScreen(navController)
            1 -> TagManageScreen(navController)
            2 -> SettingsScreen(
                onThemeChanged = onThemeChanged,
                onNavigateToStartPagePicker = onNavigateToStartPagePicker,
                onNavigateToImageCleanup = onNavigateToImageCleanup,
                onNavigateToOcrSettings = onNavigateToOcrSettings,
                onNavigateToAbout = onNavigateToAbout
            )
        }
    }
}
