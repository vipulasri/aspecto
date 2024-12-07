package com.vipulasri.aspecto

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [33],
    qualifiers = "w1080dp-h1920dp-mdpi"
)
class AspectoGridTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `verify Aspecto grid`() {
        composeTestRule.setContent {
            AspectoGrid(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                items(
                    items = listOf(
                        TestItem("square1", 1.0f),
                        TestItem("wide1", 2.0f),
                        TestItem("tall1", 0.5f),
                        TestItem("square2", 1.0f),
                        TestItem("wide2", 1.5f)
                    ),
                    key = { it.id },
                    aspectRatio = { it.aspectRatio }
                ) { item ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Gray)
                            .testTag(item.id),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = item.id, color = Color.White)
                    }
                }
            }
        }

        // Verify dimensions using test tags
        composeTestRule.onNodeWithTag("square1").assertWidthIsEqualTo(304.dp)
        composeTestRule.onNodeWithTag("wide1").assertWidthIsEqualTo(608.dp)
        composeTestRule.onNodeWithTag("tall1").assertWidthIsEqualTo(152.dp)
        composeTestRule.onNodeWithTag("square2").assertWidthIsEqualTo(425.dp)
        composeTestRule.onNodeWithTag("wide2").assertWidthIsEqualTo(638.dp)
    }

    @Test
    fun `verify single item layout`() {
        composeTestRule.setContent {
            AspectoGrid(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                items(
                    items = listOf(TestItem("wide", 2.0f)),
                    key = { it.id },
                    aspectRatio = { it.aspectRatio }
                ) { item ->
                    Box(
                        modifier = Modifier
                            .testTag(item.id)
                            .fillMaxSize()
                            .background(Color.Gray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = item.id, color = Color.White)
                    }
                }
            }
        }

        // Single item should use full width
        composeTestRule.onNodeWithTag("wide")
            .assertExists()
            .assertIsDisplayed()
            .assertWidthIsEqualTo(1064.dp)  // 1080 - (2 * 8)dp padding
    }

    @Test
    fun `verify empty grid`() {
        composeTestRule.setContent {
            AspectoGrid(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                items(
                    items = emptyList<TestItem>(),
                    key = { it.id },
                    aspectRatio = { it.aspectRatio }
                ) { item ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("grid_item_${item.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = item.id)
                    }
                }
            }
        }

        // Verify no grid items exist
        composeTestRule.onNodeWithTag("grid_item")
            .assertDoesNotExist()
    }

    @Test
    fun `verify grid updates`() {
        var items by mutableStateOf(
            listOf(
                TestItem("item1", 1.0f),
                TestItem("item2", 1.0f)
            )
        )

        composeTestRule.setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                AspectoGrid(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    items(
                        items = items,
                        key = { it.id },
                        aspectRatio = { it.aspectRatio }
                    ) { item ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Gray),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = item.id, color = Color.White)
                        }
                    }
                }
            }
        }

        // Initial state
        composeTestRule.onNodeWithText("item1").assertExists()
        composeTestRule.onNodeWithText("item2").assertExists()

        // Update items
        items = listOf(
            TestItem("item3", 1.5f),
            TestItem("item4", 0.5f)
        )

        // Verify updates
        composeTestRule.onNodeWithText("item1").assertDoesNotExist()
        composeTestRule.onNodeWithText("item3").assertExists()
    }

    @Test
    fun `verify single tall item is restricted to 75 percent of max height`() {
        composeTestRule.setContent {
            AspectoGrid(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                items(
                    items = listOf(TestItem("tall", 0.5f)),  // This will result in a tall item
                    key = { it.id },
                    aspectRatio = { it.aspectRatio }
                ) { item ->
                    Box(
                        modifier = Modifier
                            .testTag(item.id)
                            .fillMaxSize()
                            .background(Color.Gray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = item.id, color = Color.White)
                    }
                }
            }
        }

        // For a single item:
        // Available width = 1064dp (1080 - 16dp padding)
        // Height would be 1064/0.5 = 2128dp, which is > maxHeight (600)
        // So height is restricted to maxHeight * 0.75 = 450dp
        // Final width = 450 * 0.5 = 225dp
        
        composeTestRule.onNodeWithTag("tall")
            .assertExists()
            .assertIsDisplayed()
            .assertWidthIsEqualTo(225.dp)  // height(450) * aspectRatio(0.5)
    }

    private data class TestItem(
        val id: String,
        val aspectRatio: Float
    )
} 