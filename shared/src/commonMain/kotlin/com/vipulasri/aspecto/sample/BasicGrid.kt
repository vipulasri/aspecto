package com.vipulasri.aspecto.sample

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.vipulasri.aspecto.AspectoGrid
import com.vipulasri.aspecto.sample.ui.theme.AspectoTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged

private const val PAGE_SIZE = 20
private const val MAX_PAGES = 6
private const val APPEND_THRESHOLD_ROWS = 3
private const val LOAD_DELAY_MS = 600L

@Composable
fun BasicGrid(modifier: Modifier = Modifier) {
    val state = rememberLazyListState()
    var items by remember { mutableStateOf(getItems().take(PAGE_SIZE)) }
    var currentPage by remember { mutableIntStateOf(1) }
    var isAppending by remember { mutableStateOf(false) }

    LaunchedEffect(state) {
        snapshotFlow {
            val layoutInfo = state.layoutInfo
            val total = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            total > 0 && lastVisible >= 0 && lastVisible >= total - APPEND_THRESHOLD_ROWS
        }
            .distinctUntilChanged()
            .collect { shouldLoad ->
                if (shouldLoad && !isAppending && currentPage < MAX_PAGES) {
                    isAppending = true
                    delay(LOAD_DELAY_MS)
                    currentPage += 1
                    val newItems = getItems().take(PAGE_SIZE)
                        .map { it.copy(id = "append-${currentPage}-${it.id}") }
                    items += newItems
                    isAppending = false
                }
            }
    }

    Box(modifier = modifier) {
        AspectoGrid(
            modifier = Modifier.fillMaxSize(),
            state = state,
            maxRowHeight = 250.dp,
            itemPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
            contentPadding = PaddingValues(4.dp)
        ) {
            items(
                items = items,
                key = { it.id },
                aspectRatio = { it.aspectRatio }
            ) { item ->
                ArtworkItem(item = item)
            }
        }

        LoadingIndicator(visible = isAppending, currentPage = currentPage)
    }
}

@Composable
private fun BoxScope.LoadingIndicator(visible: Boolean, currentPage: Int) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = Modifier.align(Alignment.BottomCenter)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Loading page ${currentPage + 1}...",
                modifier = Modifier.padding(start = 12.dp),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun BasicGridPreview() {
    AspectoTheme {
        BasicGrid()
    }
}
