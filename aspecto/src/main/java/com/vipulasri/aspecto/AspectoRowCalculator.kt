package com.vipulasri.aspecto

/**
 * Created by Vipul Asri on 23/11/24.
 */

class AspectoRowCalculator(
    private val maxRowHeight: Int = DEFAULT_MAX_ROW_HEIGHT,
    private val horizontalPadding: Int = 0,
    private val verticalPadding: Int = 0
) {
    companion object {
        const val DEFAULT_MAX_ROW_HEIGHT = 600
        const val VALID_ITEM_SLACK_THRESHOLD = 0.2f
        private const val MAX_ITEMS_PER_ROW = 3
    }

    private val minRowHeight = (maxRowHeight * 0.5f).toInt()
    private var availableWidth = 0
    private val rows = mutableListOf<List<LazyAspectoItem>>()

    fun setMaxRowWidth(maxWidth: Int) {
        availableWidth = maxWidth
    }
}