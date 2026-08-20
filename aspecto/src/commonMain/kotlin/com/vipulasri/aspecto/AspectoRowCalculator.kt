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

/** Default maximum row height in pixels (600px at 1.0 density). */
internal const val DEFAULT_MAX_ROW_HEIGHT_PX = 600

/**
 * Lays out [items] into rows that best utilize [availableWidth] while preserving each item's
 * aspect ratio.
 *
 * This is a pure function: it has no mutable state and always returns the complete layout for the
 * given inputs. Because [AspectoGrid] computes it with the current data on every recomposition,
 * the returned row content is never stale, and incremental/mid-list updates are correct by
 * construction (no cached state to desynchronize).
 *
 * @param items The items to lay out.
 * @param availableWidth Available width for the rows, in pixels.
 * @param maxRowHeight Maximum allowed height for any row, in pixels.
 * @param horizontalPadding Spacing between items within a row, in pixels.
 * @param decorations Full-width row decorations to insert at specific row positions.
 *   Each decoration is inserted before the regular row at its [RowDecoration.index].
 *   Sorted by index internally; empty list means no decorations.
 */
internal fun calculateRows(
    items: List<AspectoLayoutInfo>,
    availableWidth: Int,
    maxRowHeight: Int = DEFAULT_MAX_ROW_HEIGHT_PX,
    horizontalPadding: Int = 0,
    decorations: List<RowDecoration> = emptyList()
): List<AspectoRow> {
    val minRowHeight = (maxRowHeight * 0.5f).toInt()
    val rows = ArrayList<AspectoRow>(items.size / 2 + 1)
    val rowKeys = HashSet<Any>()
    var currentIndex = 0

    while (currentIndex < items.size) {
        val rowConfig = findBestRowConfiguration(
            items = items,
            startIndex = currentIndex,
            availableWidth = availableWidth,
            maxRowHeight = maxRowHeight,
            minRowHeight = minRowHeight,
            horizontalPadding = horizontalPadding
        )

        val rowItems = adjustItemDimensions(
            items = items,
            startIndex = rowConfig.startIndex,
            endIndex = rowConfig.endIndex,
            effectiveWidth = rowConfig.effectiveWidth,
            rowHeight = rowConfig.rowHeight
        )

        rows.add(
            AspectoRow(
                items = rowItems,
                // Stable identity for the lazy list: the first item's key, or its index when no
                // key is provided. Never derived from item hash codes, so row keys do not change
                // between recompositions and appended items do not disturb existing rows.
                key = rowKey(items, currentIndex, rowKeys)
            )
        )
        currentIndex = rowConfig.endIndex
    }

    return spliceDecorations(rows, decorations, rowKeys)
}

private fun rowKey(
    items: List<AspectoLayoutInfo>,
    index: Int,
    seen: HashSet<Any>
): Any {
    val key = items[index].key ?: index
    check(seen.add(key)) {
        "Duplicate row key '$key' detected for the item at index $index. Row keys are derived " +
            "from each row's first item key, so items with duplicate keys in different rows " +
            "produce conflicting keys for the underlying LazyColumn, which would crash. Use " +
            "unique keys in AspectoLayoutScope.items(key = ...) or omit them."
    }
    return key
}

private data class RowConfiguration(
    val startIndex: Int,
    val endIndex: Int,
    val effectiveWidth: Int,
    val rowHeight: Float
)

private fun findBestRowConfiguration(
    items: List<AspectoLayoutInfo>,
    startIndex: Int,
    availableWidth: Int,
    maxRowHeight: Int,
    minRowHeight: Int,
    horizontalPadding: Int
): RowConfiguration {
    var bestEndIndex = startIndex + 1
    var bestScore = Float.POSITIVE_INFINITY
    var bestEffectiveWidth = 0
    var bestRowHeight = 0f

    for (numItems in 1..items.size - startIndex) {
        val endIndex = startIndex + numItems
        val effectiveWidth = calculateEffectiveWidth(availableWidth, horizontalPadding, numItems)
        val aspectRatioSum = calculateAspectRatioSum(items, startIndex, endIndex)
        val rowHeight = calculateRowHeight(
            width = effectiveWidth,
            aspectRatioSum = aspectRatioSum,
            minRowHeight = minRowHeight,
            maxRowHeight = maxRowHeight
        )
        val score = calculateRowScore(
            items = items,
            startIndex = startIndex,
            endIndex = endIndex,
            effectiveWidth = effectiveWidth,
            rowHeight = rowHeight
        )

        if (score > bestScore) break

        bestScore = score
        bestEndIndex = endIndex
        bestEffectiveWidth = effectiveWidth
        bestRowHeight = rowHeight
    }

    return RowConfiguration(
        startIndex = startIndex,
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

private fun calculateEffectiveWidth(
    availableWidth: Int,
    horizontalPadding: Int,
    itemCount: Int
): Int {
    return availableWidth - (horizontalPadding * (itemCount - 1))
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
    rowHeight: Float
): Float {
    var totalWidth = 0
    for (i in startIndex until endIndex) {
        val itemWidth = (rowHeight * items[i].aspectRatio).toInt()
            .coerceAtMost(effectiveWidth)
        totalWidth += itemWidth
    }

    return abs(totalWidth - effectiveWidth) / (endIndex - startIndex).toFloat()
}

private fun calculateRowHeight(
    width: Int,
    aspectRatioSum: Float,
    minRowHeight: Int,
    maxRowHeight: Int
): Float {
    if (aspectRatioSum == 0f) return minRowHeight.toFloat()

    val rowHeight = width / aspectRatioSum

    // If the computed height exceeds maxRowHeight (e.g. a very wide item or
    // several wide items in one row), cap at 75% of maxRowHeight so rows don't
    // dominate the screen.
    if (rowHeight > maxRowHeight) {
        return (maxRowHeight * 0.75f)
    }

    return rowHeight.coerceIn(minRowHeight.toFloat(), maxRowHeight.toFloat())
}

/**
 * Inserts [RowDecoration] rows into the computed regular rows at the specified positions.
 *
 * Each decoration is placed before the regular row at its [RowDecoration.index]. Decorations
 * with indices beyond the last regular row are appended at the end.
 *
 * @param rows The computed regular rows.
 * @param decorations The decorations to splice in, sorted by [RowDecoration.index].
 * @param existingKeys The set of keys already used by regular rows, used to detect collisions.
 * @return A new list with decorations inserted at the correct positions.
 */
private fun spliceDecorations(
    rows: List<AspectoRow>,
    decorations: List<RowDecoration>,
    existingKeys: HashSet<Any>
): List<AspectoRow> {
    if (decorations.isEmpty()) return rows

    val sorted = decorations.sortedBy { it.index }
    val result = ArrayList<AspectoRow>(rows.size + sorted.size)
    var regularRowCounter = 0
    var decorationIdx = 0

    for (row in rows) {
        while (decorationIdx < sorted.size && sorted[decorationIdx].index == regularRowCounter) {
            val decKey = sorted[decorationIdx].key
            check(existingKeys.add(decKey)) {
                "Duplicate row key '$decKey' detected for decoration at index " +
                    "${sorted[decorationIdx].index}. Decoration keys must be unique and must " +
                    "not collide with item-derived row keys."
            }
            result.add(
                AspectoRow(
                    items = emptyList(),
                    key = decKey,
                    isFullWidth = true
                )
            )
            decorationIdx++
        }
        result.add(row)
        regularRowCounter++
    }

    // Append remaining decorations (index >= regular row count)
    while (decorationIdx < sorted.size) {
        val decKey = sorted[decorationIdx].key
        check(existingKeys.add(decKey)) {
            "Duplicate row key '$decKey' detected for decoration at index " +
                "${sorted[decorationIdx].index}. Decoration keys must be unique and must " +
                "not collide with item-derived row keys."
        }
        result.add(
            AspectoRow(
                items = emptyList(),
                key = decKey,
                isFullWidth = true
            )
        )
        decorationIdx++
    }

    return result
}