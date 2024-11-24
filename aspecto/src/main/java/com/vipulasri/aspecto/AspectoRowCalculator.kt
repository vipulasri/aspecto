package com.vipulasri.aspecto

import kotlin.math.abs

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

    fun addItems(items: List<LazyAspectoItem>) {
        rows.clear()
        var currentIndex = 0

        while (currentIndex < items.size) {
            var bestRow: List<LazyAspectoItem>? = null
            var bestScore = Float.POSITIVE_INFINITY

            for (numItems in 1..MAX_ITEMS_PER_ROW) {
                if (currentIndex + numItems > items.size) break

                val candidateRow = items.subList(currentIndex, currentIndex + numItems)
                val score = calculateRowScore(candidateRow)

                if (score < bestScore) {
                    bestScore = score
                    bestRow = candidateRow
                }
            }

            bestRow?.let {
                rows.add(it)
                currentIndex += it.size
            } ?: run {
                val remainingItems = items.size - currentIndex
                val rowSize = minOf(remainingItems, MAX_ITEMS_PER_ROW)
                rows.add(items.subList(currentIndex, currentIndex + rowSize))
                currentIndex += rowSize
            }
        }
    }

    fun computeLayout() {
        rows.forEach { rowItems ->
            val effectiveWidth = availableWidth - (horizontalPadding * (rowItems.size - 1))
            val aspectRatioSum = rowItems.sumOf { it.aspectRatio.toDouble() }.toFloat()
            val rowHeight = calculateRowHeight(effectiveWidth, aspectRatioSum)
                .coerceIn(minRowHeight.toFloat(), maxRowHeight.toFloat())

            rowItems.forEach { item ->
                item.height = rowHeight.toInt()
            }

            var remainingWidth = effectiveWidth
            rowItems.forEachIndexed { index, item ->
                item.width = if (index == rowItems.lastIndex) {
                    remainingWidth
                } else {
                    (effectiveWidth * (item.aspectRatio / aspectRatioSum)).toInt().also {
                        remainingWidth -= it
                    }
                }
            }
        }
    }

    private fun calculateRowScore(rowItems: List<LazyAspectoItem>): Float {
        val aspectRatioSum = rowItems.sumOf { it.aspectRatio.toDouble() }.toFloat()
        val effectiveWidth = availableWidth - (horizontalPadding * (rowItems.size - 1))
        val rowHeight = calculateRowHeight(effectiveWidth, aspectRatioSum)

        if (rowHeight > maxRowHeight || rowHeight < minRowHeight) {
            return Float.POSITIVE_INFINITY
        }

        val itemSlacks = rowItems.map { item ->
            val actualWidth = rowHeight * item.aspectRatio
            val expectedWidth = effectiveWidth * (item.aspectRatio / aspectRatioSum)
            abs(actualWidth - expectedWidth) / expectedWidth
        }

        if (itemSlacks.any { it > VALID_ITEM_SLACK_THRESHOLD }) {
            return Float.POSITIVE_INFINITY
        }

        val targetHeight = maxRowHeight * 0.8f
        return abs(rowHeight - targetHeight) / targetHeight
    }

    private fun calculateRowHeight(width: Int, aspectRatioSum: Float): Float {
        if (aspectRatioSum == 0f) return minRowHeight.toFloat()
        val desiredHeight = width / aspectRatioSum

        val minAllowedHeight = (minRowHeight * (1 - VALID_ITEM_SLACK_THRESHOLD))
            .coerceAtLeast(50f)

        return desiredHeight.coerceIn(minAllowedHeight, maxRowHeight.toFloat())
    }
}