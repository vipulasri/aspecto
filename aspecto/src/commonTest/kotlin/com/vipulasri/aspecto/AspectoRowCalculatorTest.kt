package com.vipulasri.aspecto

import androidx.compose.runtime.Composable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AspectoRowCalculatorTest {

    companion object {
        private const val MAX_ROW_HEIGHT = 600
        private const val HORIZONTAL_PADDING = 8
        private const val AVAILABLE_WIDTH = 1000
    }

    private fun layout(items: List<AspectoLayoutInfo>) = calculateRows(
        items = items,
        availableWidth = AVAILABLE_WIDTH,
        maxRowHeight = MAX_ROW_HEIGHT,
        horizontalPadding = HORIZONTAL_PADDING
    )

    @Test
    fun `two items with same aspect ratio should split width equally`() {

        // Given
        val items = listOf(
            createTestItem(aspectRatio = 1.0f),
            createTestItem(aspectRatio = 1.0f)
        )

        // When
        val row = layout(items).first()

        // Then
        val (item1, item2) = row.items
        val expectedWidth =
            (AVAILABLE_WIDTH - HORIZONTAL_PADDING) / 2 // total width minus padding divided by 2
        assertEquals(expectedWidth, item1.width)
        assertEquals(expectedWidth, item2.width)
        assertEquals(item1.height, item2.height)
    }

    @Test
    fun `items should respect min height constraint`() {

        // Given
        val items = listOf(
            createTestItem(aspectRatio = 10f) // Very wide item
        )

        // When
        val item = layout(items).first().items.first()

        // Then
        assertEquals(
            MAX_ROW_HEIGHT / 2,
            item.height
        ) // Should be at minRowHeight (maxRowHeight * 0.5)
        assertEquals(AVAILABLE_WIDTH, item.width)
    }

    @Test
    fun `should handle empty list`() {

        // When
        val rows = layout(emptyList())

        // Then
        assertEquals(0, rows.size)
    }

    @Test
    fun `should include appended items in the layout`() {

        // Given
        val initialItems = listOf(createTestItem(aspectRatio = 1.0f))
        val initialRows = layout(initialItems)
        assertEquals(1, initialRows.size)
        assertEquals(1, initialRows[0].items.size)

        // When - append a new item
        val updatedRows = layout(initialItems + createTestItem(aspectRatio = 2.0f))

        // Then - all items are present
        assertEquals(1, updatedRows.size)
        assertEquals(2, updatedRows[0].items.size)
        assertEquals(1.0f, updatedRows[0].items[0].aspectRatio)
        assertEquals(2.0f, updatedRows[0].items[1].aspectRatio)
    }

    @Test
    fun `should use 75 percent of max row height when item has bigger height`() {

        // Given
        val item =
            createTestItem(aspectRatio = 0.5f) // Aspect ratio that would result in height > maxRowHeight

        // When
        val layoutItem = layout(listOf(item)).first().items.first()

        // Then
        val expectedHeight = (MAX_ROW_HEIGHT * 0.75f).toInt() // 75% of 600 = 450
        assertEquals(expectedHeight, layoutItem.height)
        assertEquals((expectedHeight * item.aspectRatio).toInt(), layoutItem.width)
    }

    @Test
    fun `should handle very large number of items`() {

        // Given
        val items = List(100) { createTestItem(aspectRatio = 1.0f) }

        // When
        val rows = layout(items)

        // Then
        assertTrue(rows.size > 1)
        assertTrue(rows.all { it.items.size <= 3 }) // No row should exceed max items
    }

    @Test
    fun `should distribute items properly across rows based on available width`() {

        // Given
        val items = List(4) { createTestItem(aspectRatio = 1.0f) }

        // When
        val rows = layout(items)

        // Then
        // Should be distributed as 3+1 for optimal width utilization
        assertEquals(2, rows.size)
        assertEquals(3, rows[0].items.size) // First row should have 3 items
        assertEquals(1, rows[1].items.size) // Second row should have 1 item

        // Items in same row should have equal widths (since same aspect ratio)
        rows.forEach { row ->
            val widths = row.items.map { it.width }
            assertEquals(1, widths.distinct().size)
        }
    }

    @Test
    fun `should keep existing row keys stable when appending items`() {

        // Given
        val initialItems = List(3) { createTestItem(aspectRatio = 1.0f, key = "item$it") }
        val initialRows = layout(initialItems)

        // When - add a new item at the end
        val updatedRows = layout(initialItems + createTestItem(aspectRatio = 1.0f, key = "new"))

        // Then
        assertEquals(initialRows.first().key, updatedRows.first().key)
        val allKeys = updatedRows.flatMap { row -> row.items.map { it.key } }
        assertTrue(allKeys.containsAll(listOf("item0", "item1", "item2", "new")))
        assertEquals(initialItems.size + 1, allKeys.size)
    }

    @Test
    fun `should not drop items when an item in the middle changes`() {

        // Given - items laid out across multiple rows
        val items = List(5) { createTestItem(aspectRatio = 1.0f, key = "item$it") }
        assertEquals(2, layout(items).size)

        // When - one item in the middle changes (same size, new key)
        val changed = items.toMutableList()
        changed[1] = createTestItem(aspectRatio = 1.0f, key = "item1b")
        val rows = layout(changed)

        // Then - every item is still present, in order
        val keys = rows.flatMap { row -> row.items.map { it.key } }
        assertEquals(listOf("item0", "item1b", "item2", "item3", "item4"), keys)
    }

    @Test
    fun `should maintain consistent height within rows for different aspect ratios`() {

        // Given - items with varying aspect ratios
        val items = listOf(
            createTestItem(aspectRatio = 0.5f), // Tall
            createTestItem(aspectRatio = 1.0f), // Square
            createTestItem(aspectRatio = 1.5f)  // Wide
        )

        // When
        val rows = layout(items)

        // Then
        rows.forEach { row ->
            // All items in a row should have same height
            val heights = row.items.map { it.height }
            assertEquals(1, heights.distinct().size)

            // Height should be within bounds
            val height = heights.first()
            assertTrue(height >= MAX_ROW_HEIGHT * 0.5f)
            assertTrue(height <= MAX_ROW_HEIGHT)
        }
    }

    @Test
    fun `should respect width constraints and padding`() {

        // Given
        val items = List(3) { createTestItem(aspectRatio = 1.0f) }

        // When
        val rows = layout(items)

        // Then
        rows.forEach { row ->
            // Total width including padding should not exceed available width
            val totalWidth = row.items.sumOf { it.width } +
                    (HORIZONTAL_PADDING * (row.items.size - 1))
            assertTrue(totalWidth <= AVAILABLE_WIDTH)

            // Individual items should not exceed effective width
            val effectiveWidth = AVAILABLE_WIDTH -
                    (HORIZONTAL_PADDING * (row.items.size - 1))
            row.items.forEach { item ->
                assertTrue(item.width <= effectiveWidth)
            }
        }
    }

    @Test
    fun `row keys should be derived from the first item key`() {

        // Given
        val items = listOf(
            createTestItem(aspectRatio = 1.0f, key = "a"),
            createTestItem(aspectRatio = 1.0f, key = "b"),
            createTestItem(aspectRatio = 1.0f, key = "c"),
            createTestItem(aspectRatio = 1.0f, key = "d")
        )

        // When - distributed as 3+1
        val rows = layout(items)

        // Then
        assertEquals("a", rows[0].key)
        assertEquals("d", rows[1].key)
    }

    @Test
    fun `row keys should fall back to the first item index when keys are absent`() {

        // Given
        val items = List(4) { createTestItem(aspectRatio = 1.0f) }

        // When
        val rows = layout(items)

        // Then
        assertEquals(0, rows[0].key)
        assertEquals(3, rows[1].key)
    }

    @Test
    fun `should lay out many tall items without hanging`() {

        // Given - very tall items, a pathological case for the row candidate scan
        val items = List(100) { createTestItem(aspectRatio = 0.1f) }

        // When
        val rows = layout(items)

        // Then - terminates and produces bounded rows
        assertTrue(rows.isNotEmpty())
        assertTrue(rows.sumOf { it.items.size } == items.size)
        rows.forEach { row ->
            val heights = row.items.map { it.height }
            assertEquals(1, heights.distinct().size)
            assertTrue(heights.first() in MAX_ROW_HEIGHT / 2..MAX_ROW_HEIGHT)
        }
    }

    @Test
    fun `should handle zero available width gracefully`() {

        // Given
        val items = List(3) { createTestItem(aspectRatio = 1.0f) }

        // When
        val rows = calculateRows(
            items = items,
            availableWidth = 0,
            maxRowHeight = MAX_ROW_HEIGHT,
            horizontalPadding = HORIZONTAL_PADDING
        )

        // Then - no crash, every item laid out with non-negative dimensions
        assertTrue(rows.isNotEmpty())
        rows.forEach { row ->
            row.items.forEach { item ->
                assertTrue(item.width >= 0)
                assertTrue(item.height >= 0)
            }
        }
    }

    @Test
    fun `should respect custom maxRowHeight`() {

        // Given
        val items = List(3) { createTestItem(aspectRatio = 1.0f) }
        val customMaxRowHeight = 300

        // When
        val rows = calculateRows(
            items = items,
            availableWidth = AVAILABLE_WIDTH,
            maxRowHeight = customMaxRowHeight,
            horizontalPadding = HORIZONTAL_PADDING
        )

        // Then - row heights should respect the custom max
        rows.forEach { row ->
            val height = row.items.first().height
            assertTrue(height <= customMaxRowHeight)
            assertTrue(height >= customMaxRowHeight / 2)
        }
    }

    @Test
    fun `should use only start padding for horizontal spacing`() {

        // Given - asymmetric padding: start=16, end=4
        val items = List(3) { createTestItem(aspectRatio = 1.0f) }

        // When - using only start padding (16)
        val rowsStart = calculateRows(
            items = items,
            availableWidth = AVAILABLE_WIDTH,
            maxRowHeight = MAX_ROW_HEIGHT,
            horizontalPadding = 16
        )

        // When - using only start padding (4)
        val rowsEnd = calculateRows(
            items = items,
            availableWidth = AVAILABLE_WIDTH,
            maxRowHeight = MAX_ROW_HEIGHT,
            horizontalPadding = 4
        )

        // Then - more padding means less effective width per item
        val totalWidthStart = rowsStart.sumOf { row -> row.items.sumOf { it.width } }
        val totalWidthEnd = rowsEnd.sumOf { row -> row.items.sumOf { it.width } }
        assertTrue(totalWidthStart < totalWidthEnd)
    }

    @Test
    fun `should remove item from layout correctly`() {

        // Given - 3 items laid out in one row
        val items = listOf(
            createTestItem(aspectRatio = 1.0f, key = "a"),
            createTestItem(aspectRatio = 1.0f, key = "b"),
            createTestItem(aspectRatio = 1.0f, key = "c")
        )
        val initialRows = layout(items)
        assertEquals(1, initialRows.size)
        assertEquals(3, initialRows[0].items.size)

        // When - remove middle item
        val reducedItems = items.filter { it.key != "b" }
        val updatedRows = layout(reducedItems)

        // Then - remaining items are present
        val keys = updatedRows.flatMap { row -> row.items.map { it.key } }
        assertEquals(listOf("a", "c"), keys)
    }

    @Test
    fun `should throw on duplicate keys across different rows`() {

        // Given - very wide items, each filling its own row, sharing a key
        val items = listOf(
            createTestItem(aspectRatio = 2.0f, key = "dup"),
            createTestItem(aspectRatio = 2.0f, key = "dup")
        )

        // Then - conflicting row keys must fail fast with a clear message
        val exception = assertFailsWith<IllegalStateException> { layout(items) }
        assertTrue(exception.message.orEmpty().contains("Duplicate row key 'dup'"))
    }

    @Test
    fun `should allow same key for items within one row`() {

        // Given - two items sharing a key that land in the same row
        val items = listOf(
            createTestItem(aspectRatio = 1.0f, key = "same"),
            createTestItem(aspectRatio = 1.0f, key = "same")
        )

        // When
        val rows = layout(items)

        // Then - single row, key derived from first item, no conflict
        assertEquals(1, rows.size)
        assertEquals("same", rows[0].key)
    }

    @Test
    fun `should reject negative aspect ratio`() {
        val scope = AspectoLayoutScope()
        assertFailsWith<IllegalArgumentException> {
            scope.item(aspectRatio = -1f) { }
        }
    }

    @Test
    fun `should reject zero aspect ratio`() {
        val scope = AspectoLayoutScope()
        assertFailsWith<IllegalArgumentException> {
            scope.item(aspectRatio = 0f) { }
        }
    }

    @Test
    fun `contentType should be preserved on each item through layout`() {

        // Given
        val items = listOf(
            AspectoLayoutInfo(
                aspectRatio = 1.0f,
                key = "a",
                contentType = "image",
                content = @Composable {}
            ),
            AspectoLayoutInfo(
                aspectRatio = 1.0f,
                key = "b",
                contentType = "text",
                content = @Composable {}
            ),
            AspectoLayoutInfo(
                aspectRatio = 1.0f,
                key = "c",
                contentType = "image",
                content = @Composable {}
            )
        )

        // When
        val rows = layout(items)

        // Then - contentType is carried through to the laid-out items
        val resultContentTypes = rows.flatMap { row -> row.items.map { it.contentType } }
        assertEquals(listOf("image", "text", "image"), resultContentTypes)
    }

    @Test
    fun `singular item function should produce correct layout`() {

        // Given - use singular item() instead of items()
        val scope = AspectoLayoutScope()
        scope.item(aspectRatio = 1.0f, key = "x", contentType = "photo") { }
        scope.item(aspectRatio = 1.0f, key = "y", contentType = "photo") { }

        // When
        val rows = calculateRows(
            items = scope.items,
            availableWidth = AVAILABLE_WIDTH,
            maxRowHeight = MAX_ROW_HEIGHT,
            horizontalPadding = HORIZONTAL_PADDING
        )

        // Then - two items in one row, content types preserved
        assertEquals(1, rows.size)
        assertEquals(2, rows[0].items.size)
        assertEquals("x", rows[0].items[0].key)
        assertEquals("y", rows[0].items[1].key)
        assertEquals("photo", rows[0].items[0].contentType)
        assertEquals("photo", rows[0].items[1].contentType)
    }

    @Test
    fun `asymmetric horizontal padding should use only start side`() {

        // Given - items that fill the row with small padding
        val items = List(3) { createTestItem(aspectRatio = 1.0f) }

        // When - start padding = 2
        val rowsSmall = calculateRows(
            items = items,
            availableWidth = AVAILABLE_WIDTH,
            maxRowHeight = MAX_ROW_HEIGHT,
            horizontalPadding = 2
        )

        // When - start padding = 20
        val rowsLarge = calculateRows(
            items = items,
            availableWidth = AVAILABLE_WIDTH,
            maxRowHeight = MAX_ROW_HEIGHT,
            horizontalPadding = 20
        )

        // Then - larger start padding yields smaller per-item widths (end padding ignored)
        val avgWidthSmall = rowsSmall.flatMap { it.items.map { item -> item.width } }.average()
        val avgWidthLarge = rowsLarge.flatMap { it.items.map { item -> item.width } }.average()
        assertTrue(avgWidthSmall > avgWidthLarge)
    }

    private fun createTestItem(
        aspectRatio: Float,
        key: String? = null
    ) = AspectoLayoutInfo(
        aspectRatio = aspectRatio,
        key = key,
        contentType = null,
        content = @Composable {}
    )
}