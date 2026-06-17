package com.labfreezer.ui.screens.move

enum class MoveTarget {
    DEVICE, LAYER, BOX
}

enum class MoveLevel {
    DEVICE, LAYER, BOX, GRID
}

data class BreadcrumbItem(
    val label: String,
    val level: MoveLevel,
    val id: Long
)

data class GridCellInfo(
    val row: Int,
    val col: Int,
    val label: String,
    val occupied: Boolean,
    val occupiedBySampleId: Long? = null
)

object MoveState {
    var selectedItemIds: Set<Long> = emptySet()
    var moveTarget: MoveTarget = MoveTarget.BOX
    var sourceBoxId: Long? = null
    var sourceLayerId: Long? = null
    var sourceDeviceId: Long? = null

    var selectMode: Boolean = false
    var resultDeviceId: Long? = null
    var resultLayerId: Long? = null
    var resultBoxId: Long? = null
    var resultGridRow: Int? = null
    var resultGridCol: Int? = null

    fun clear() {
        selectedItemIds = emptySet()
        moveTarget = MoveTarget.BOX
        sourceBoxId = null
        sourceLayerId = null
        sourceDeviceId = null
        selectMode = false
        resultDeviceId = null
        resultLayerId = null
        resultBoxId = null
        resultGridRow = null
        resultGridCol = null
    }
}
