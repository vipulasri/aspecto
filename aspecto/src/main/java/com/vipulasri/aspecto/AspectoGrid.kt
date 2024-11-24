package com.vipulasri.aspecto

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
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
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth()
    ) {

    }
}