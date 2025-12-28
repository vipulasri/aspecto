/*
 * Copyright 2024 Vipul Asri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.vipulasri.aspecto

import kotlin.math.abs

/**
 * Calculator for optimizing row layouts in AspectGrid.
 * 
 * This class handles:
 * - Row height and width calculations
 * - Item distribution across rows
 * - Height constraints enforcement
 * - Incremental layout updates
 *
 * @param maxRowHeight Maximum allowed height for any row
 * @param horizontalPadding Padding between items in a row
 */
internal class AspectoRowCalculator(
    private val maxRowHeight: Int = DEFAULT_MAX_ROW_HEIGHT,
    private val horizontalPadding: Int = 0
) {
    companion object {
        /** Default maximum row height in pixels */
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
        if (items === lastProcessedItems) return // Use referential equality first
        if (items.isEmpty()) {
            rows.clear()
            lastProcessedItems = items
            return
        }

        val firstDiffIndex = findFirstDifferenceIndex(items, lastProcessedItems)

        if (rows.isNotEmpty() && firstDiffIndex > 0) {
            var itemCount = 0
            // Optimization: Calculate rows to keep more efficiently
            var keepUpToRow = 0
            for (i in rows.indices) {
                val nextItemCount = itemCount + rows[i].items.size
                if (nextItemCount > firstDiffIndex) break
                itemCount = nextItemCount
                keepUpToRow = i + 1
            }

            // Optimization: Use subList view and avoid intermediate list
            if (keepUpToRow < rows.size) {
                val rowsToRemove = rows.size - keepUpToRow
                repeat(rowsToRemove) { rows.removeAt(rows.lastIndex) }
            }

            if (itemCount < items.size) {
                processItems(items.subList(itemCount, items.size))
            }
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
        if (items.isEmpty()) return
        
        // Optimization: Pre-allocate capacity for rows list
        // Estimate: average 2-3 items per row
        val estimatedRowCount = (items.size / 2.5).toInt().coerceAtLeast(1)
        rows.ensureCapacity(rows.size + estimatedRowCount)
        
        var currentIndex = 0

        while (currentIndex < items.size) {
            // Find best configuration for next row
            val rowConfig = findBestRowConfiguration(
                items = items,
                startIndex = currentIndex
            )

            // Calculate and add row with adjusted dimensions
            val rowItems = adjustItemDimensions(
                items = items,
                startIndex = rowConfig.startIndex,
                endIndex = rowConfig.endIndex,
                effectiveWidth = rowConfig.effectiveWidth,
                rowHeight = rowConfig.rowHeight
            )
            
            rows.add(AspectoRow(rowItems))
            currentIndex = rowConfig.endIndex
        }
    }

    private data class RowConfiguration(
        val startIndex: Int,
        val endIndex: Int,
        val effectiveWidth: Int,
        val rowHeight: Float
    )

    private fun findBestRowConfiguration(
        items: List<AspectoLayoutInfo>,
        startIndex: Int
    ): RowConfiguration {
        var bestStartIndex = startIndex
        var bestEndIndex = startIndex + 1
        var bestScore = Float.POSITIVE_INFINITY
        var bestEffectiveWidth = 0
        var bestRowHeight = 0f
        
        // Optimization: Track aspect ratio sum incrementally
        var aspectRatioSum = 0f
        
        // Optimization: Early termination threshold - if score increases significantly, stop
        val earlyTerminationThreshold = 1.5f

        for (numItems in 1..items.size - startIndex) {
            val endIndex = startIndex + numItems
            val effectiveWidth = calculateEffectiveWidth(numItems)
            
            // Incrementally calculate aspect ratio sum
            aspectRatioSum += items[endIndex - 1].aspectRatio
            
            val rowHeight = calculateRowHeight(effectiveWidth, aspectRatioSum)
                .coerceIn(minRowHeight.toFloat(), maxRowHeight.toFloat())
            
            val score = calculateRowScore(items, startIndex, endIndex, effectiveWidth, rowHeight, aspectRatioSum)

            // Early termination: if score is getting worse significantly, stop
            if (score > bestScore * earlyTerminationThreshold) break

            bestScore = score
            bestStartIndex = startIndex
            bestEndIndex = endIndex
            bestEffectiveWidth = effectiveWidth
            bestRowHeight = rowHeight
        }

        return RowConfiguration(
            startIndex = bestStartIndex,
            endIndex = bestEndIndex,
            effectiveWidth = bestEffectiveWidth,
            rowHeight = bestRowHeight
        )
    }

    private fun adjustItemDimensions(
        items: List<AspectoLayoutInfo>,
        startIndex: Int,
        endIndex: Int,
        effectiveWidth: Int,
        rowHeight: Float
    ): List<AspectoLayoutInfo> {
        // Optimization: Pre-allocate ArrayList with exact capacity
        val count = endIndex - startIndex
        val result = ArrayList<AspectoLayoutInfo>(count)
        
        for (i in startIndex until endIndex) {
            val item = items[i]
            val itemWidth = (rowHeight * item.aspectRatio).toInt()
                .coerceAtMost(effectiveWidth)
            result.add(
                item.copy(
                    width = itemWidth,
                    height = rowHeight.toInt()
                )
            )
        }
        
        return result
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

    private fun calculateRowScore(
        items: List<AspectoLayoutInfo>,
        startIndex: Int,
        endIndex: Int,
        effectiveWidth: Int,
        rowHeight: Float,
        aspectRatioSum: Float
    ): Float {
        // Optimization: Calculate total width more efficiently
        // Total width is approximately rowHeight * aspectRatioSum, but we need to account for integer rounding
        var totalWidth = 0
        for (i in startIndex until endIndex) {
            val itemWidth = (rowHeight * items[i].aspectRatio).toInt()
                .coerceAtMost(effectiveWidth)
            totalWidth += itemWidth
        }

        return abs(totalWidth - effectiveWidth) / (endIndex - startIndex).toFloat()
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