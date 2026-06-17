package com.labfreezer.ui.navigation

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
    data object SampleEdit : Screen("sample_edit/{sampleId}") {
        fun createRoute(sampleId: Long) = "sample_edit/$sampleId"
    }
    data object SampleCreate : Screen("sample_create/{boxId}/{row}/{col}") {
        fun createRoute(boxId: Long, row: Int, col: Int) = "sample_create/$boxId/$row/$col"
    }
    data object Search : Screen("search")
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
    data object MainTabs : Screen("main_tabs")
}
