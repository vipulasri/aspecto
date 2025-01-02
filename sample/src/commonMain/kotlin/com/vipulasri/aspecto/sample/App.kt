package com.vipulasri.aspecto.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.compose.setSingletonImageLoaderFactory
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.util.DebugLogger
import com.vipulasri.aspecto.AspectoGrid
import com.vipulasri.aspecto.sample.ui.theme.AspectoTheme
import kotlinx.coroutines.delay

/**
 * Created by Vipul Asri on 30/12/24.
 */

@Composable
fun App() {

    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .crossfade(true)
            .logger(DebugLogger())
            .build()
    }

    AspectoTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { padding ->

            var items by remember { mutableStateOf(getItems()) }

            LaunchedEffect(Unit) {
                delay(2000)
                items += getItems(idMultiplier = 2)
            }

            AspectoGrid(
                modifier = Modifier.padding(padding),
                maxRowHeight = 250.dp,
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
                            model = ImageRequest.Builder(LocalPlatformContext.current)
                                .data(item.imageUrl)
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

private fun getItems(idMultiplier: Int = 1): List<Artwork> {
    return listOf(
        Artwork(
            id = (1 * idMultiplier).toString(),
            aspectRatio = 498f / 395f,
            imageUrl = "https://uploads3.wikiart.org/images/maynard-dixon/inyo-mountains-1944.jpg",
            title = "Inyo Mountains"
        ),
        Artwork(
            id = (2 * idMultiplier).toString(),
            aspectRatio = 417f / 500f,
            imageUrl = "https://uploads1.wikiart.org/images/enrico-baj/regard-multiple-1989.jpg",
            title = "Regard multiple"
        ),
        Artwork(
            id = (3 * idMultiplier).toString(),
            aspectRatio = 1534f / 1004f,
            imageUrl = "https://uploads8.wikiart.org/images/peter-phillips/gravy-for-the-navy-1975.jpg",
            title = "Gravy for the Navy"
        ),
        Artwork(
            id = (4 * idMultiplier).toString(),
            aspectRatio = 575f / 455f,
            imageUrl = "https://uploads0.wikiart.org/images/j-e-h-macdonald/mountain-snowfall-lake-oesa-1932.jpg",
            title = "Mountain Snowfall"
        ),
        Artwork(
            id = (5 * idMultiplier).toString(),
            aspectRatio = 585f / 480f,
            imageUrl = "https://uploads5.wikiart.org/images/ding-yi/shi-shi-91-12-1991.jpg",
            title = "Shi-Shi"
        ),
        Artwork(
            id = (6 * idMultiplier).toString(),
            aspectRatio = 448f / 400f,
            imageUrl = "https://uploads4.wikiart.org/images/karl-schrag/full-moon-silence-1980.jpg",
            title = "Full Moon & Silence"
        ),
        Artwork(
            id = (7 * idMultiplier).toString(),
            aspectRatio = 493f / 432f,
            imageUrl = "https://uploads8.wikiart.org/images/vasan-sitthiket/war-against-capitalism.jpg",
            title = "War Against Capitalism"
        ),
        Artwork(
            id = (8 * idMultiplier).toString(),
            aspectRatio = 474f / 480f,
            imageUrl = "https://uploads5.wikiart.org/images/robert-barry/untitled-2010.jpg",
            title = "Untitled"
        ),
        Artwork(
            id = (9 * idMultiplier).toString(),
            aspectRatio = 806f / 485f,
            imageUrl = "https://uploads7.wikiart.org/images/maxim-vorobiev/view-of-jerusalem-1836.jpg",
            title = "View of Jerusalem"
        ),
        Artwork(
            id = (10 * idMultiplier).toString(),
            aspectRatio = 357f / 480f,
            imageUrl = "https://uploads8.wikiart.org/images/samuel-bak/mauve-passage-1980.jpg",
            title = "Mauve Passage"
        ),
        Artwork(
            id = (11 * idMultiplier).toString(),
            aspectRatio = 700f / 593f,
            imageUrl = "https://uploads4.wikiart.org/images/vladimir-dimitrov/unknown-title-20.jpg",
            title = "Untitled"
        ),
        Artwork(
            id = (12 * idMultiplier).toString(),
            aspectRatio = 350f / 490f,
            imageUrl = "https://uploads3.wikiart.org/images/david-ligare/landscape-with-an-archer-1991.jpg",
            title = "Landscape with an Archer"
        ),
        Artwork(
            id = (13 * idMultiplier).toString(),
            aspectRatio = 673f / 973f,
            imageUrl = "https://uploads6.wikiart.org/images/helene-schjerfbeck/self-portrait-with-black-mouth-1939.jpg",
            title = "Self-Portrait With Black Mouth"
        ),
        Artwork(
            id = (14 * idMultiplier).toString(),
            aspectRatio = 570f / 425f,
            imageUrl = "https://uploads5.wikiart.org/images/malcolm-morley/natural-history-1997.jpg",
            title = "Natural History"
        ),
        Artwork(
            id = (15 * idMultiplier).toString(),
            aspectRatio = 679f / 533f,
            imageUrl = "https://uploads6.wikiart.org/images/antonio-sanfilippo/nero-e-rosso-schermo-astratto-28-55.jpg",
            title = "Nero e Rosso"
        ),
        Artwork(
            id = (16 * idMultiplier).toString(),
            aspectRatio = 624f / 410f,
            imageUrl = "https://uploads8.wikiart.org/images/jacob-lawrence/to-preserve-their-freedom-from-the-toussaint-l-ouverture-series-1988.jpg",
            title = "To Preserve Their Freedom"
        ),
        Artwork(
            id = (17 * idMultiplier).toString(),
            aspectRatio = 1363f / 1900f,
            imageUrl = "https://uploads5.wikiart.org/images/fyodor-solntsev/people-clothes-the-province-of-kiev-2.jpg",
            title = "People clothes the province of Kyiv",
        ),
        Artwork(
            id = (18 * idMultiplier).toString(),
            aspectRatio = 361f / 553f,
            imageUrl = "https://uploads4.wikiart.org/images/luigi-russolo/solidity-of-fog-1912.jpg",
            title = "Solidity of Fog"
        ),
        Artwork(
            id = (19 * idMultiplier).toString(),
            aspectRatio = 492f / 480f,
            imageUrl = "https://uploads4.wikiart.org/images/burhan-dogancay/mystery-woman-1989.jpg",
            title = "Mystery Woman"
        ),
        Artwork(
            id = (20 * idMultiplier).toString(),
            aspectRatio = 482f / 374f,
            imageUrl = "https://uploads5.wikiart.org/images/carlos-orozco-romero/destiny-makers.jpg",
            title = "Destiny Makers"
        )
    )
}
