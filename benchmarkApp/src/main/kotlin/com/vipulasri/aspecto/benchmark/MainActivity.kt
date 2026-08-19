package com.vipulasri.aspecto.benchmark

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vipulasri.aspecto.AspectoGrid
import kotlin.random.Random

private const val ITEM_COUNT = 2_000

internal data class BenchmarkItem(
    val id: Int,
    val aspectRatio: Float,
    val color: Long
)

private val benchmarkItems: List<BenchmarkItem> = run {
    val random = Random(7)
    List(ITEM_COUNT) { index ->
        BenchmarkItem(
            id = index,
            aspectRatio = 0.5f + random.nextFloat() * 1.5f,
            color = 0xFF000000 or (random.nextInt().toLong() and 0x00FFFFFF)
        )
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                BenchmarkGrid()
            }
        }
    }
}

@Composable
private fun BenchmarkGrid() {
    AspectoGrid(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "aspecto_grid" },
        contentPadding = PaddingValues(4.dp),
        maxRowHeight = 300.dp
    ) {
        items(
            items = benchmarkItems,
            key = { it.id },
            aspectRatio = { it.aspectRatio }
        ) { item ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(item.color))
            )
        }
    }
}
