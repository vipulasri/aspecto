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
    ): Int  {
        val minLength = minOf(oldItems.size, newItems.size)

        // Compare elements up to the length of the shorter list
        for (i in 0 until minLength) {
            if (oldItems[i] != newItems[i]) {
                return i
            }
        }

        // If no difference was found, check for length mismatch
        return if (oldItems.size != newItems.size) minLength else -1
    }

    private fun calculateEffectiveWidth(itemCount: Int): Int {
        return availableWidth - (horizontalPadding * (itemCount - 1))
    }

    private fun processItems(items: List<AspectoLayoutInfo>) {
        var currentIndex = 0

        while (currentIndex < items.size) {
            var bestStartIndex = currentIndex
            var bestEndIndex = currentIndex + 1
            var bestScore = Float.POSITIVE_INFINITY

            // Try different row sizes using indices
            for (numItems in 1..items.size - currentIndex) {
                val endIndex = currentIndex + numItems
                val score = calculateRowScore(items, currentIndex, endIndex)

                if (score > bestScore) {
                    break
                }

                bestScore = score
                bestStartIndex = currentIndex
                bestEndIndex = endIndex
            }

            // Calculate dimensions and add row
            val effectiveWidth = calculateEffectiveWidth(bestEndIndex - bestStartIndex)
            val aspectRatioSum = calculateAspectRatioSum(items, bestStartIndex, bestEndIndex)
            val rowHeight = calculateRowHeight(effectiveWidth, aspectRatioSum)
                .coerceIn(minRowHeight.toFloat(), maxRowHeight.toFloat())

            val updatedItems = createRowItems(
                items, 
                bestStartIndex, 
                bestEndIndex, 
                rowHeight, 
                effectiveWidth
            )
            
            rows.add(AspectoRow(updatedItems))
            currentIndex = bestEndIndex
        }
    }

    private fun calculateAspectRatioSum(
        items: List<AspectoLayoutInfo>, 
        startIndex: Int, 
        endIndex: Int
    ): Float {
        var sum = 0f
        for (i in startIndex until endIndex) {
            sum += items[i].aspectRatio
        }
        return sum
    }

    private fun createRowItems(
        items: List<AspectoLayoutInfo>,
        startIndex: Int,
        endIndex: Int,
        rowHeight: Float,
        effectiveWidth: Int
    ): List<AspectoLayoutInfo> {
        return List(endIndex - startIndex) { index ->
            val item = items[startIndex + index]
            val itemWidth = (rowHeight * item.aspectRatio).toInt()
                .coerceAtMost(effectiveWidth)
            item.copy(
                width = itemWidth,
                height = rowHeight.toInt()
            )
        }
    }

    private fun calculateRowScore(
        items: List<AspectoLayoutInfo>, 
        startIndex: Int, 
        endIndex: Int
    ): Float {
        val itemCount = endIndex - startIndex
        val effectiveWidth = calculateEffectiveWidth(itemCount)
        val aspectRatioSum = calculateAspectRatioSum(items, startIndex, endIndex)
        val rowHeight = calculateRowHeight(effectiveWidth, aspectRatioSum)
            .coerceIn(minRowHeight.toFloat(), maxRowHeight.toFloat())

        var totalWidth = 0
        for (i in startIndex until endIndex) {
            val itemWidth = (rowHeight * items[i].aspectRatio).toInt()
                .coerceAtMost(effectiveWidth)
            totalWidth += itemWidth
        }

        return abs(totalWidth - effectiveWidth) / itemCount.toFloat()
    }

    fun getRows(): List<AspectoRow> = rows

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