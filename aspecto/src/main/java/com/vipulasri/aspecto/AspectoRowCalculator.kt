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
    }

    private val minRowHeight = (maxRowHeight * 0.5f).toInt()
    private var availableWidth = 0
    private val rows = mutableListOf<AspectoRow>()
    private var lastProcessedItems = listOf<AspectoLayoutInfo>()

    fun setMaxRowWidth(maxWidth: Int) {
        availableWidth = maxWidth
    }

    fun addItems(items: List<AspectoLayoutInfo>) {
        if (items == lastProcessedItems) return

        val firstDiffIndex = findFirstDifferenceIndex(items, lastProcessedItems)

        if (rows.isNotEmpty() && firstDiffIndex > 0) {
            var itemCount = 0
            val rowsToKeep = rows.takeWhile { row ->
                itemCount += row.items.size
                itemCount <= firstDiffIndex
            }

            rows.clear()
            rows.addAll(rowsToKeep)

            processItems(items.subList(itemCount, items.size))
        } else {
            rows.clear()
            processItems(items)
        }

        lastProcessedItems = items
    }

    private fun findFirstDifferenceIndex(
        newItems: List<AspectoLayoutInfo>,
        oldItems: List<AspectoLayoutInfo>
    ): Int = newItems.zip(oldItems)
        .withIndex()
        .firstOrNull { (_, pair) -> pair.first != pair.second }
        ?.index ?: minOf(newItems.size, oldItems.size)

    private fun calculateEffectiveWidth(itemCount: Int): Int {
        return availableWidth - (horizontalPadding * (itemCount - 1))
    }

    private fun processItems(items: List<AspectoLayoutInfo>) {
        var currentIndex = 0

        while (currentIndex < items.size) {
            var bestRow: List<AspectoLayoutInfo>? = null
            var bestScore = Float.POSITIVE_INFINITY

            // Try different row sizes
            for (numItems in 1..items.size - currentIndex) {
                val candidateRow = items.subList(currentIndex, currentIndex + numItems)
                val score = calculateRowScore(candidateRow)

                if (score > bestScore) {
                    // If score is getting worse, no point trying larger rows
                    break
                }

                bestScore = score
                bestRow = candidateRow
            }

            val rowItems = bestRow ?: run {
                val remainingItems = items.size - currentIndex
                items.subList(currentIndex, currentIndex + remainingItems)
            }

            // Calculate dimensions and add row
            val effectiveWidth = calculateEffectiveWidth(rowItems.size)
            val aspectRatioSum = rowItems.sumOf { it.aspectRatio.toDouble() }.toFloat()
            val rowHeight = calculateRowHeight(effectiveWidth, aspectRatioSum)
                .coerceIn(minRowHeight.toFloat(), maxRowHeight.toFloat())

            val updatedItems = rowItems.map { item ->
                val itemWidth = (rowHeight * item.aspectRatio).toInt()
                    .coerceAtMost(effectiveWidth)
                item.copy(
                    width = itemWidth,
                    height = rowHeight.toInt()
                )
            }
            
            rows.add(AspectoRow(updatedItems))
            currentIndex += rowItems.size
        }
    }

    fun getRows(): List<AspectoRow> = rows

    private fun calculateRowScore(rowItems: List<AspectoLayoutInfo>): Float {
        val aspectRatioSum = rowItems.sumOf { it.aspectRatio.toDouble() }.toFloat()
        val effectiveWidth = calculateEffectiveWidth(rowItems.size)

        // Calculate row height based on available width and aspect ratios
        val rowHeight = calculateRowHeight(effectiveWidth, aspectRatioSum)
            .coerceIn(minRowHeight.toFloat(), maxRowHeight.toFloat())

        // Calculate widths using the same logic as processItems
        val widths = rowItems.map { item ->
            (rowHeight * item.aspectRatio).toInt()
                .coerceAtMost(effectiveWidth)
        }
        val totalWidth = widths.sum()

        // Score based on how well we fill the available width
        val score = abs(totalWidth - effectiveWidth) / rowItems.size.toFloat()
        
        return score
    }

    private fun calculateRowHeight(width: Int, aspectRatioSum: Float): Float {
        if (aspectRatioSum == 0f) return minRowHeight.toFloat()

        // Calculate the base row height
        val rowHeight = width / aspectRatioSum

        // If there's only one item and it would result in a very tall row,
        // limit it to 75% of maxRowHeight
        if (rowHeight > maxRowHeight) {
            return (maxRowHeight * 0.75f)
        }

        return rowHeight.coerceIn(minRowHeight.toFloat(), maxRowHeight.toFloat())
    }
}