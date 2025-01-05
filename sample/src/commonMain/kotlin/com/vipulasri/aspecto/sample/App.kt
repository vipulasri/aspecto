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

            var items by remember { mutableStateOf(getItems().take(20)) }

            LaunchedEffect(Unit) {
                delay(2000)
                items += getItems().takeLast(20)
            }

            AspectoGrid(
                modifier = Modifier.padding(padding),
                maxRowHeight = 250.dp,
                itemPadding = PaddingValues(
                    horizontal = 4.dp,
                    vertical = 4.dp
                ),
                contentPadding = PaddingValues(4.dp)
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

private fun getItems(): List<Artwork> {
    return listOf(
        Artwork(
            id = "1",
            aspectRatio = 2000f / 1446f,
            imageUrl = "https://uploads0.wikiart.org/00475/images/salvador-dali/w1siziisijm4njq3mcjdlfsiccisimnvbnzlcnqilcitcxvhbgl0esa5mcatcmvzaxplidiwmdb4mjawmfx1mdazzsjdxq.jpg",
            title = "The Persistence of Memory"
        ),
        Artwork(
            id = "2",
            aspectRatio = 1020f / 1500f,
            imageUrl = "https://uploads8.wikiart.org/00339/images/leonardo-da-vinci/mona-lisa-c-1503-1519.jpg",
            title = "Mona Lisa"
        ),
        Artwork(
            id = "3",
            aspectRatio = 2000f / 1594f,
            imageUrl = "https://uploads3.wikiart.org/00142/images/vincent-van-gogh/the-starry-night.jpg",
            title = "The Starry Night"
        ),
        Artwork(
            id = "4",
            aspectRatio = 5773f / 4478f,
            imageUrl = "https://uploads8.wikiart.org/00129/images/claude-monet/impression-sunrise.jpg",
            title = "Impression, sunrise"
        ),
        Artwork(
            id = "5",
            aspectRatio = 3369f / 1523f,
            imageUrl = "https://uploads8.wikiart.org/00139/images/pablo-picasso/guernica-by-pablo-picasso.jpg",
            title = "Guernica"
        ),
        Artwork(
            id = "6",
            aspectRatio = 3000f / 2190f,
            imageUrl = "https://uploads1.wikiart.org/images/salvador-dali/the-great-masturbator-1929.jpg",
            title = "The Great Masturbator"
        ),
        Artwork(
            id = "7",
            aspectRatio = 1280f / 812f,
            imageUrl = "https://uploads0.wikiart.org/00242/images/alexandre-cabanel/fallen-angel-alexandre-cabanel.jpg",
            title = "Fallen Angel"
        ),
        Artwork(
            id = "8",
            aspectRatio = 5000f / 5017f,
            imageUrl = "https://uploads6.wikiart.org/00142/images/57726d7eedc2cb3880b47e13/the-kiss-gustav-klimt-google-cultural-institute.jpg",
            title = "The Kiss"
        ),
        Artwork(
            id = "9",
            aspectRatio = 3524f / 1599f,
            imageUrl = "https://uploads8.wikiart.org/00475/images/michelangelo/the-creation-of-adam-michelangelo-c-1512.jpg",
            title = "Sistine Chapel Ceiling: Creation of Adam"
        ),
        Artwork(
            id = "10",
            aspectRatio = 1442f / 1005f,
            imageUrl = "https://uploads7.wikiart.org/images/rene-magritte/the-treachery-of-images-this-is-not-a-pipe-1948(2).jpg",
            title = "The treachery of images (This is not a pipe)"
        ),
        Artwork(
            id = "11",
            aspectRatio = 1616f / 2889f,
            imageUrl = "https://uploads8.wikiart.org/images/francisco-goya/saturn-devouring-one-of-his-children-1823.jpg",
            title = "Saturn Devouring One of His Sons"
        ),
        Artwork(
            id = "12",
            aspectRatio = 8533f / 4325f,
            imageUrl = "https://uploads6.wikiart.org/images/hieronymus-bosch/the-garden-of-earthly-delights-1515-7.jpg",
            title = "The Garden of Earthly Delights"
        ),
        Artwork(
            id = "13",
            aspectRatio = 1600f / 1067f,
            imageUrl = "https://uploads5.wikiart.org/images/sandro-botticelli/the-birth-of-venus-1485(1).jpg",
            title = "The Birth of Venus"
        ),
        Artwork(
            id = "14",
            aspectRatio = 1611f / 2000f,
            imageUrl = "https://uploads1.wikiart.org/images/edvard-munch/the-scream-1893(2).jpg",
            title = "The Scream"
        ),
        Artwork(
            id = "15",
            aspectRatio = 2305f / 1845f,
            imageUrl = "https://uploads6.wikiart.org/images/magdalena-carmen-frieda-kahlo-y-calderón-de-rivera/henry-ford-hospital-the-flying-bed-1932.jpg",
            title = "Henry Ford Hospital (The Flying Bed)"
        ),
        Artwork(
            id = "16",
            aspectRatio = 520f / 600f,
            imageUrl = "https://uploads5.wikiart.org/images/balthus/guitar-lesson-1934.jpg",
            title = "Guitar lesson"
        ),
        Artwork(
            id = "17",
            aspectRatio = 5357f / 4009f,
            imageUrl = "https://uploads3.wikiart.org/00144/images/jean-francois-millet/jean-fran-ois-millet-gleaners-google-art-project.jpg",
            title = "The Gleaners"
        ),
        Artwork(
            id = "18",
            aspectRatio = 1930f / 2400f,
            imageUrl = "https://uploads8.wikiart.org/00142/images/rembrandt/christ-in-the-storm.jpg",
            title = "The Storm on the Sea of Galilee"
        ),
        Artwork(
            id = "19",
            aspectRatio = 640f / 479f,
            imageUrl = "https://uploads3.wikiart.org/images/wassily-kandinsky/color-study-squares-with-concentric-circles-1913(1).jpg",
            title = "Color Study: Squares with Concentric Circles"
        ),
        Artwork(
            id = "20",
            aspectRatio = 1697f / 1280f,
            imageUrl = "https://uploads8.wikiart.org/images/octavio-ocampo/forever-always.jpg",
            title = "Forever Always"
        ),
        Artwork(
            id = "21",
            aspectRatio = 640f / 475f,
            imageUrl = "https://uploads3.wikiart.org/images/salvador-dali/hitler-masturbating.jpg",
            title = "Hitler Masturbating"
        ),
        Artwork(
            id = "22",
            aspectRatio = 3648f / 5472f,
            imageUrl = "https://uploads1.wikiart.org/00198/images/pablo-picasso/old-guitarist-chicago.jpg",
            title = "The old blind guitarist"
        ),
        Artwork(
            id = "23",
            aspectRatio = 759f / 900f,
            imageUrl = "https://uploads5.wikiart.org/00129/images/johannes-vermeer/the-girl-with-a-pearl-earring.jpg",
            title = "The Girl with a Pearl Earring"
        ),
        Artwork(
            id = "24",
            aspectRatio = 592f / 843f,
            imageUrl = "https://uploads2.wikiart.org/00180/images/leonardo-da-vinci/da-vinci-vitruve-luc-viatour.jpg",
            title = "The proportions of the human figure (The Vitruvian Man)"
        ),
        Artwork(
            id = "25",
            aspectRatio = 1252f / 1624f,
            imageUrl = "https://uploads2.wikiart.org/images/rene-magritte/son-of-man-1964(1).jpg",
            title = "The Son of Man"
        ),
        Artwork(
            id = "26",
            aspectRatio = 3768f / 6214f,
            imageUrl = "https://uploads4.wikiart.org/images/marcel-duchamp/nude-descending-a-staircase-no-2-1912.jpg",
            title = "Nude Descending a Staircase, No.2"
        ),
        Artwork(
            id = "27",
            aspectRatio = 6000f / 3274f,
            imageUrl = "https://uploads1.wikiart.org/00129/images/edward-hopper/nighthawks.jpg",
            title = "Nighthawks"
        ),
        Artwork(
            id = "28",
            aspectRatio = 1400f / 997f,
            imageUrl = "https://uploads5.wikiart.org/00475/images/raphael/1-xvkpn0qm3eiqpzivkggfea.jpg",
            title = "The School of Athens"
        ),
        Artwork(
            id = "29",
            aspectRatio = 1197f / 1039f,
            imageUrl = "https://uploads7.wikiart.org/images/magdalena-carmen-frieda-kahlo-y-calderón-de-rivera/my-birth-1932.jpg",
            title = "My Birth"
        ),
        Artwork(
            id = "30",
            aspectRatio = 1980f / 3131f,
            imageUrl = "https://uploads1.wikiart.org/00475/images/georges-seurat/eiffel-tower-c-1889-1.jpeg",
            title = "The Eiffel Tower"
        ),
        Artwork(
            id = "31",
            aspectRatio = 2343f / 3000f,
            imageUrl = "https://uploads8.wikiart.org/images/caspar-david-friedrich/the-wanderer-above-the-sea-of-fog.jpg",
            title = "The Wanderer Above the Sea of Fog"
        ),
        Artwork(
            id = "32",
            aspectRatio = 1139f / 1437f,
            imageUrl = "https://uploads5.wikiart.org/images/salvador-dali/dream-caused-by-the-flight-of-a-bee-around-a-pomegranate-one-second-before-awakening.jpg",
            title = "Dream Caused by the Flight of a Bee around a Pomegranate a Second before Awakening"
        ),
        Artwork(
            id = "33",
            aspectRatio = 681f / 850f,
            imageUrl = "https://uploads2.wikiart.org/images/claude-monet/women-in-the-garden.jpg",
            title = "Women in the garden"
        ),
        Artwork(
            id = "34",
            aspectRatio = 660f / 495f,
            imageUrl = "https://uploads2.wikiart.org/00304/images/andy-warhol/marilyn-diptych.jpg",
            title = "Marilyn Diptych"
        ),
        Artwork(
            id = "35",
            aspectRatio = 804f / 1022f,
            imageUrl = "https://uploads2.wikiart.org/images/pablo-picasso/self-portrait-1907.jpg",
            title = "Self-Portrait"
        ),
        Artwork(
            id = "36",
            aspectRatio = 2023f / 1589f,
            imageUrl = "https://uploads6.wikiart.org/00207/images/57726d84edc2cb3880b48a43/iv-n-el-terrible-y-su-hijo-por-ili-repin-2.jpg",
            title = "Ivan the Terrible and His Son Ivan on November 16, 1581"
        ),
        Artwork(
            id = "37",
            aspectRatio = 1827f / 2160f,
            imageUrl = "https://uploads4.wikiart.org/images/jean-michel-basquiat/head.jpg",
            title = "Skull"
        ),
        Artwork(
            id = "38",
            aspectRatio = 500f / 342f,
            imageUrl = "https://uploads0.wikiart.org/images/man-ray/minotaur-1934.jpg",
            title = "Minotaur"
        ),
        Artwork(
            id = "39",
            aspectRatio = 2206f / 2186f,
            imageUrl = "https://uploads4.wikiart.org/images/magdalena-carmen-frieda-kahlo-y-calderón-de-rivera/the-two-fridas-1939.jpg",
            title = "The Two Fridas"
        ),
        Artwork(
            id = "40",
            aspectRatio = 1500f / 1198f,
            imageUrl = "https://uploads6.wikiart.org/00129/images/eugene-delacroix/the-liberty-leading-the-people.jpg",
            title = "Liberty Leading the People"
        )
    )
}
