package com.vipulasri.aspecto.sample

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.vipulasri.aspecto.AspectoGrid
import com.vipulasri.aspecto.sample.ui.theme.AspectoTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {

            var items by remember { mutableStateOf(getItems()) }

            LaunchedEffect(Unit) {
                delay(6000)
                items += getItems()
            }

            AspectoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    key(items) {
                        AspectoGrid(
                            modifier = Modifier.padding(padding),
                            maxRowHeight = 200.dp,
                            itemPadding = PaddingValues(
                                horizontal = 4.dp,
                                vertical = 4.dp
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            items(
                                items = items,
                                key = { it.id },
                                aspectRatio = { it.aspectRatio }
                            ) { item ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(item.imageUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = item.title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getItems(): List<Artwork> {
        return listOf(
            Artwork(
                aspectRatio = 498f / 395f,
                imageUrl = "https://uploads3.wikiart.org/images/maynard-dixon/inyo-mountains-1944.jpg",
                title = "Inyo Mountains"
            ),
            Artwork(
                aspectRatio = 417f / 500f,
                imageUrl = "https://uploads1.wikiart.org/images/enrico-baj/regard-multiple-1989.jpg",
                title = "Regard multiple"
            ),
            Artwork(
                aspectRatio = 1534f / 1004f,
                imageUrl = "https://uploads8.wikiart.org/images/peter-phillips/gravy-for-the-navy-1975.jpg",
                title = "Gravy for the Navy"
            ),
            Artwork(
                aspectRatio = 575f / 455f,
                imageUrl = "https://uploads0.wikiart.org/images/j-e-h-macdonald/mountain-snowfall-lake-oesa-1932.jpg",
                title = "Mountain Snowfall"
            ),
            Artwork(
                aspectRatio = 585f / 480f,
                imageUrl = "https://uploads5.wikiart.org/images/ding-yi/shi-shi-91-12-1991.jpg",
                title = "Shi-Shi"
            ),
            Artwork(
                aspectRatio = 448f / 400f,
                imageUrl = "https://uploads4.wikiart.org/images/karl-schrag/full-moon-silence-1980.jpg",
                title = "Full Moon & Silence"
            ),
            Artwork(
                aspectRatio = 493f / 432f,
                imageUrl = "https://uploads8.wikiart.org/images/vasan-sitthiket/war-against-capitalism.jpg",
                title = "War Against Capitalism"
            ),
            Artwork(
                aspectRatio = 474f / 480f,
                imageUrl = "https://uploads5.wikiart.org/images/robert-barry/untitled-2010.jpg",
                title = "Untitled"
            ),
            Artwork(
                aspectRatio = 806f / 485f,
                imageUrl = "https://uploads7.wikiart.org/images/maxim-vorobiev/view-of-jerusalem-1836.jpg",
                title = "View of Jerusalem"
            ),
            Artwork(
                aspectRatio = 357f / 480f,
                imageUrl = "https://uploads8.wikiart.org/images/samuel-bak/mauve-passage-1980.jpg",
                title = "Mauve Passage"
            ),
            Artwork(
                aspectRatio = 700f / 593f,
                imageUrl = "https://uploads4.wikiart.org/images/vladimir-dimitrov/unknown-title-20.jpg",
                title = "Untitled"
            ),
            Artwork(
                aspectRatio = 350f / 490f,
                imageUrl = "https://uploads3.wikiart.org/images/david-ligare/landscape-with-an-archer-1991.jpg",
                title = "Landscape with an Archer"
            ),
            Artwork(
                aspectRatio = 673f / 973f,
                imageUrl = "https://uploads6.wikiart.org/images/helene-schjerfbeck/self-portrait-with-black-mouth-1939.jpg",
                title = "Self-Portrait With Black Mouth"
            ),
            Artwork(
                aspectRatio = 570f / 425f,
                imageUrl = "https://uploads5.wikiart.org/images/malcolm-morley/natural-history-1997.jpg",
                title = "Natural History"
            ),
            Artwork(
                aspectRatio = 679f / 533f,
                imageUrl = "https://uploads6.wikiart.org/images/antonio-sanfilippo/nero-e-rosso-schermo-astratto-28-55.jpg",
                title = "Nero e Rosso"
            ),
            Artwork(
                aspectRatio = 624f / 410f,
                imageUrl = "https://uploads8.wikiart.org/images/jacob-lawrence/to-preserve-their-freedom-from-the-toussaint-l-ouverture-series-1988.jpg",
                title = "To Preserve Their Freedom"
            ),
            Artwork(
                aspectRatio = 1363f / 1900f,
                imageUrl = "https://uploads5.wikiart.org/images/fyodor-solntsev/people-clothes-the-province-of-kiev-2.jpg",
                title = "People clothes the province of Kyiv",
            ),
            Artwork(
                aspectRatio = 361f / 553f,
                imageUrl = "https://uploads4.wikiart.org/images/luigi-russolo/solidity-of-fog-1912.jpg",
                title = "Solidity of Fog"
            ),
            Artwork(
                aspectRatio = 492f / 480f,
                imageUrl = "https://uploads4.wikiart.org/images/burhan-dogancay/mystery-woman-1989.jpg",
                title = "Mystery Woman"
            ),
            Artwork(
                aspectRatio = 482f / 374f,
                imageUrl = "https://uploads5.wikiart.org/images/carlos-orozco-romero/destiny-makers.jpg",
                title = "Destiny Makers"
            )
        )
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