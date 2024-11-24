package com.vipulasri.aspecto

import kotlin.math.abs

/**
 * Created by Vipul Asri on 23/11/24.
 */

internal class AspectoRowCalculator(
    private val maxRowHeight: Int = DEFAULT_MAX_ROW_HEIGHT,
    private val horizontalPadding: Int = 0
) {
    companion object {
        const val DEFAULT_MAX_ROW_HEIGHT = 600
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

    fun getRows(): List<List<LazyAspectoItem>> = rows

    private fun calculateRowScore(rowItems: List<LazyAspectoItem>): Float {
        val aspectRatioSum = rowItems.sumOf { it.aspectRatio.toDouble() }.toFloat()
        val effectiveWidth = availableWidth - (horizontalPadding * (rowItems.size - 1))

        // Calculate row height based on available width and aspect ratios
        val rowHeight = (effectiveWidth / aspectRatioSum).coerceIn(
            minRowHeight.toFloat(),
            maxRowHeight.toFloat()
        )

        // If we can't fit within height bounds, return infinity
        if (rowHeight > maxRowHeight || rowHeight < minRowHeight) {
            return Float.POSITIVE_INFINITY
        }

        // Calculate total width using the computed height
        val totalWidth = rowItems.sumOf { (rowHeight * it.aspectRatio).toDouble() }.toFloat()

        // Score based on how well we fill the available width
        return abs(totalWidth - effectiveWidth)
    }

    private fun calculateRowHeight(width: Int, aspectRatioSum: Float): Float {
        if (aspectRatioSum == 0f) return minRowHeight.toFloat()
        return (width / aspectRatioSum).coerceIn(minRowHeight.toFloat(), maxRowHeight.toFloat())
    }
}