package com.vipulasri.aspecto.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vipulasri.aspecto.AspectoGrid

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            App()
        }
    }

    @Preview
    @Composable
    private fun AspectoGridPreview() {
        val items = listOf(
            1f to "1",
            1.77f to "2",
            1.2f to "3",
            1f to "4",
            0.8f to "5",
            1.5f to "6",
            0.9f to "7",
            1f to "8"
        )

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AspectoGrid(
                maxRowHeight = 200.dp,
                itemPadding = PaddingValues(
                    horizontal = 4.dp,
                    vertical = 4.dp
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                items(
                    items = items,
                    key = { it.hashCode() },
                    aspectRatio = { it.first }
                ) { item ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.second,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }
        }
    }

}