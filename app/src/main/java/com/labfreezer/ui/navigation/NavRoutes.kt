package com.labfreezer.ui.navigation

import android.net.Uri
import com.labfreezer.data.search.ScopeType

sealed class Screen(val route: String) {
    data object DeviceList : Screen("device_list")
    data object DeviceDetail : Screen("device_detail/{deviceId}") {
        fun createRoute(deviceId: Long) = "device_detail/$deviceId"
    }
    data object LayerDetail : Screen("layer_detail/{layerId}") {
        fun createRoute(layerId: Long) = "layer_detail/$layerId"
    }
    data object BoxGrid : Screen("box_grid/{boxId}") {
        fun createRoute(boxId: Long) = "box_grid/$boxId"
    }
    data object SampleEdit : Screen("sample_edit/{sampleId}?browseCtx={browseCtx}") {
        fun createRoute(sampleId: Long, browseCtx: String? = null): String {
            return if (browseCtx != null) "sample_edit/$sampleId?browseCtx=$browseCtx"
            else "sample_edit/$sampleId"
        }
    }
    data object SampleCreate : Screen("sample_create/{boxId}/{row}/{col}") {
        fun createRoute(boxId: Long, row: Int, col: Int) = "sample_create/$boxId/$row/$col"
    }
    data object Search : Screen("search?scope={scope}&scopeId={scopeId}&scopeName={scopeName}") {
        fun createRoute(scopeType: ScopeType = ScopeType.ALL, scopeId: Long? = null, scopeName: String? = null): String {
            val sb = StringBuilder("search")
            sb.append("?scope=${scopeType.name}")
            sb.append("&scopeId=${scopeId ?: -1L}")
            if (scopeName != null) {
                sb.append("&scopeName=${Uri.encode(scopeName)}")
            }
            return sb.toString()
        }
    }
    data object TagManage : Screen("tag_manage")
    data object TagDetail : Screen("tag_detail/{tagId}") {
        fun createRoute(tagId: Long) = "tag_detail/$tagId"
    }
    data object Settings : Screen("settings")
    data object StartPagePicker : Screen("start_page_picker")
    data object ImageCleanup : Screen("image_cleanup")
    data object OcrSettings : Screen("ocr_settings")
    data object MoveBrowser : Screen("move_browser")
    data object DeviceTypeManage : Screen("device_type_manage")
    data object About : Screen("about")
    data object BottomBarEdit : Screen("bottom_bar_edit")
    data object MainTabs : Screen("main_tabs")
}
