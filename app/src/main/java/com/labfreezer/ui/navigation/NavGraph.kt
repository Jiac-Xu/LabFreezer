package com.labfreezer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.labfreezer.data.search.ScopeType
import com.labfreezer.data.search.SearchScope
import com.labfreezer.ui.screens.boxgrid.BoxGridScreen
import com.labfreezer.ui.screens.devices.DeviceDetailScreen
import com.labfreezer.ui.screens.devices.DeviceListScreen
import com.labfreezer.ui.screens.layers.LayerDetailScreen
import com.labfreezer.ui.screens.sample.SampleEditScreen
import com.labfreezer.ui.screens.search.SearchScreen
import com.labfreezer.ui.screens.tags.TagDetailScreen
import com.labfreezer.ui.screens.move.MoveBrowserScreen
import com.labfreezer.ui.screens.tags.DeviceTypeManageScreen
import com.labfreezer.ui.screens.tags.TagManageScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.DeviceList.route
    ) {
        composable(Screen.DeviceList.route) {
            DeviceListScreen(navController, showFabPadding = false)
        }
        composable(
            route = Screen.DeviceDetail.route,
            arguments = listOf(navArgument("deviceId") { type = NavType.LongType })
        ) { backStackEntry ->
            val deviceId = backStackEntry.arguments?.getLong("deviceId") ?: return@composable
            DeviceDetailScreen(navController, deviceId)
        }
        composable(
            route = Screen.LayerDetail.route,
            arguments = listOf(navArgument("layerId") { type = NavType.LongType })
        ) { backStackEntry ->
            val layerId = backStackEntry.arguments?.getLong("layerId") ?: return@composable
            LayerDetailScreen(navController, layerId)
        }
        composable(
            route = Screen.BoxGrid.route,
            arguments = listOf(navArgument("boxId") { type = NavType.LongType })
        ) { backStackEntry ->
            val boxId = backStackEntry.arguments?.getLong("boxId") ?: return@composable
            BoxGridScreen(navController, boxId)
        }
        composable(
            route = Screen.SampleEdit.route,
            arguments = listOf(
                navArgument("sampleId") { type = NavType.LongType },
                navArgument("browseCtx") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val sampleId = backStackEntry.arguments?.getLong("sampleId") ?: return@composable
            val browseCtxKey = backStackEntry.arguments?.getString("browseCtx")?.ifBlank { null }
            SampleEditScreen(navController, sampleId, browseCtxKey)
        }
        composable(
            route = Screen.SampleCreate.route,
            arguments = listOf(
                navArgument("boxId") { type = NavType.LongType },
                navArgument("row") { type = NavType.IntType },
                navArgument("col") { type = NavType.IntType }
            )
        ) { }
        composable(
            route = Screen.Search.route,
            arguments = listOf(
                navArgument("scope") { type = NavType.StringType; defaultValue = ScopeType.ALL.name },
                navArgument("scopeId") { type = NavType.LongType; defaultValue = -1L },
                navArgument("scopeName") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val scope = backStackEntry.arguments?.getString("scope") ?: ScopeType.ALL.name
            val scopeId = backStackEntry.arguments?.getLong("scopeId") ?: -1L
            val scopeName = backStackEntry.arguments?.getString("scopeName") ?: ""
            SearchScreen(
                navController = navController,
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
            TagManageScreen(navController, showFabPadding = false)
        }
        composable(
            route = Screen.TagDetail.route,
            arguments = listOf(navArgument("tagId") { type = NavType.LongType })
        ) { backStackEntry ->
            val tagId = backStackEntry.arguments?.getLong("tagId") ?: return@composable
            TagDetailScreen(navController, tagId)
        }
    }
}