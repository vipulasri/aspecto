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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * A composable that arranges items in a grid layout with varying row heights based on item aspect ratios.
 * Each row is optimized to best utilize the available width while maintaining the aspect ratios of its items.
 *
 * The layout is recomputed from the current item list on every recomposition, so appending items
 * (e.g. pagination) never leaves stale or misplaced rows behind. Row keys are derived from item
 * keys, so growing the list does not invalidate already-rendered rows.
 *
 * Note: recomputation is O(n) in the number of items. This is negligible for typical paginated
 * feeds (thousands of items appended infrequently). If you observe jank from very large lists
 * under frequent parent recompositions, consider constraining the parent's recomposition scope.
 *
 * @param modifier The modifier to be applied to the grid
 * @param state The state object to be used to control or observe the list's state
 * @param contentPadding The padding around the content
 * @param maxRowHeight Maximum height allowed for any row
 * @param itemPadding Padding between items in a row. For horizontal spacing, only the start
 * padding value is used (consistent with [Arrangement.spacedBy]). For vertical spacing between
 * rows, only the top padding value is used. Asymmetric start/end or top/bottom values are
 * supported but only the start/top side is applied to inter-item and inter-row spacing.
 * @param content The grid content using [AspectoLayoutScope]
 *
 * Example usage:
 * ```
 * AspectoGrid(
 *     modifier = Modifier.fillMaxWidth(),
 *     contentPadding = PaddingValues(8.dp)
 * ) {
 *     items(
 *         items = imageList,
 *         key = { it.id },
 *         aspectRatio = { it.width / it.height.toFloat() }
 *     ) { image ->
 *         AsyncImage(
 *             model = image.url,
 *             contentDescription = null,
 *             modifier = Modifier.fillMaxSize()
 *         )
 *     }
 * }
 * ```
 */
@Composable
fun AspectoGrid(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    maxRowHeight: Dp = DEFAULT_MAX_ROW_HEIGHT_PX.dp,
    itemPadding: PaddingValues = PaddingValues(0.dp),
    content: AspectoLayoutScope.() -> Unit
) {
    val scope = AspectoLayoutScope().apply(content)
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    val maxRowHeightPx = with(density) { maxRowHeight.toPx().toInt() }
    val horizontalPaddingPx = with(density) {
        itemPadding.calculateStartPadding(layoutDirection).toPx().toInt()
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth()
    ) {
        check(constraints.hasBoundedWidth) {
            "AspectoGrid requires bounded width constraints to compute row layouts. " +
                "Ensure the parent container has bounded width, or use " +
                "Modifier.fillMaxWidth() with a bounded parent."
        }

        val availableWidth = (constraints.maxWidth - with(density) {
            (contentPadding.calculateStartPadding(layoutDirection) +
                contentPadding.calculateEndPadding(layoutDirection)).toPx()
        }.toInt()).coerceAtLeast(0)

        val rows = calculateRows(
            items = scope.items,
            availableWidth = availableWidth,
            maxRowHeight = maxRowHeightPx,
            horizontalPadding = horizontalPaddingPx
        )

        LazyColumn(
            state = state,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(itemPadding.calculateTopPadding())
        ) {
            items(
                items = rows,
                key = { row -> row.key },
                contentType = { row -> row.items.firstOrNull()?.contentType }
            ) { row ->
                AspectoRow(
                    row = row,
                    density = density,
                    itemPadding = itemPadding,
                    layoutDirection = layoutDirection
                )
            }
        }
    }
}

@Composable
internal fun AspectoRow(
    row: AspectoRow,
    density: Density,
    itemPadding: PaddingValues,
    layoutDirection: LayoutDirection
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(with(density) { (row.items.firstOrNull()?.height ?: 0).toDp() }),
        horizontalArrangement = Arrangement.spacedBy(
            itemPadding.calculateStartPadding(layoutDirection)
        )
    ) {
        for (item in row.items) {
            Box(
                modifier = Modifier
                    .width(with(density) { item.width.toDp() })
                    .fillMaxHeight()
            ) {
                item.content()
            }
        }
    }
}