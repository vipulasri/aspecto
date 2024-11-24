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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Created by Vipul Asri on 23/11/24.
 */

@Composable
fun AspectoGrid(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    maxRowHeight: Dp = AspectoRowCalculator.DEFAULT_MAX_ROW_HEIGHT.dp,
    itemPadding: PaddingValues = PaddingValues(0.dp),
    content: LazyAspectoLayoutScope.() -> Unit
) {
    val scope = remember { LazyAspectoLayoutScope().apply(content) }
    val density = LocalDensity.current

    val layoutInfo = remember(scope.items, maxRowHeight, itemPadding) {
        AspectoRowCalculator(
            maxRowHeight = with(density) { maxRowHeight.toPx().toInt() },
            horizontalPadding = with(density) {
                itemPadding.calculateStartPadding(LayoutDirection.Ltr).toPx().toInt()
            }
        )
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth()
    ) {
        val rows by remember(layoutInfo, scope.items, constraints.maxWidth) {
            derivedStateOf {
                val availableWidth = constraints.maxWidth - with(density) {
                    (contentPadding.calculateStartPadding(LayoutDirection.Ltr) +
                            contentPadding.calculateEndPadding(LayoutDirection.Ltr)).toPx()
                }.toInt()

                layoutInfo.setMaxRowWidth(availableWidth)
                layoutInfo.addItems(scope.items)
                layoutInfo.computeLayout()
                layoutInfo.getRows()
            }
        }

        LazyColumn(
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(itemPadding.calculateTopPadding())
        ) {
            items(
                count = rows.size,
                key = { index -> rows[index].firstOrNull()?.key ?: index }
            ) { rowIndex ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(with(density) { rows[rowIndex].first().height.toDp() }),
                    horizontalArrangement = Arrangement.spacedBy(
                        itemPadding.calculateStartPadding(LayoutDirection.Ltr)
                    )
                ) {
                    rows[rowIndex].forEach { item ->
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
        }
    }
}