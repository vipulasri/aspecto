package com.vipulasri.aspecto

import androidx.compose.runtime.Composable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.measureTime
import kotlin.time.Duration

/**
 * Performance and optimization validation tests for AspectoRowCalculator.
 * These tests validate that the optimizations improve performance while
 * preserving business requirements.
 */
class AspectoPerformanceTest {

    companion object {
        private const val MAX_ROW_HEIGHT = 600
        private const val HORIZONTAL_PADDING = 8
        private const val AVAILABLE_WIDTH = 1000
    }

    private fun createCalculator() = AspectoRowCalculator(
        maxRowHeight = MAX_ROW_HEIGHT,
        horizontalPadding = HORIZONTAL_PADDING
    ).apply {
        setMaxRowWidth(AVAILABLE_WIDTH)
    }

    @Test
    fun `performance test - large item count should complete in reasonable time`() {
        val calculator = createCalculator()
        
        // Given: A large number of items (simulating a real photo gallery)
        val items = List(200) { index ->
            createTestItem(
                aspectRatio = (0.5f + (index % 10) * 0.2f), // Varying aspect ratios
                key = "item$index"
            )
        }

        // When: Calculate rows
        val duration = measureTime {
            calculator.addItems(items)
        }

        // Then: Should complete reasonably fast
        val rows = calculator.getRows()
        assertTrue(rows.isNotEmpty(), "Should create rows")
        assertTrue(duration.inWholeMilliseconds < 1000, 
            "Calculation for 200 items should take less than 1 second, took ${duration.inWholeMilliseconds}ms")
        
        // Validate all items are included
        val totalItems = rows.sumOf { it.items.size }
        assertEquals(items.size, totalItems, "All items should be included in rows")
    }

    @Test
    fun `incremental update performance - appending items should be fast`() {
        val calculator = createCalculator()
        
        // Given: Initial set of items
        val initialItems = List(50) { index ->
            createTestItem(aspectRatio = 1.0f, key = "item$index")
        }
        calculator.addItems(initialItems)
        
        // When: Append more items (simulating loading more content)
        val appendedItems = initialItems + List(50) { index ->
            createTestItem(aspectRatio = 1.0f, key = "new$index")
        }
        
        val duration = measureTime {
            calculator.addItems(appendedItems)
        }

        // Then: Incremental update should be fast
        assertTrue(duration.inWholeMilliseconds < 500, 
            "Incremental update should be faster than full recalculation, took ${duration.inWholeMilliseconds}ms")
        
        val rows = calculator.getRows()
        val totalItems = rows.sumOf { it.items.size }
        assertEquals(100, totalItems, "All items should be included")
    }

    @Test
    fun `aspect ratio preservation - all items maintain exact aspect ratio`() {
        val calculator = createCalculator()
        
        // Given: Items with precise aspect ratios
        val aspectRatios = listOf(0.75f, 1.33f, 1.5f, 0.56f, 1.78f, 2.0f)
        val items = aspectRatios.mapIndexed { index, ratio ->
            createTestItem(aspectRatio = ratio, key = "item$index")
        }

        // When: Calculate rows
        calculator.addItems(items)

        // Then: Each item should maintain its aspect ratio (within rounding tolerance)
        calculator.getRows().forEach { row ->
            row.items.forEachIndexed { index, layoutItem ->
                val expectedRatio = layoutItem.aspectRatio
                val actualRatio = layoutItem.width.toFloat() / layoutItem.height.toFloat()
                val tolerance = 0.01f // 1% tolerance for integer rounding
                
                assertTrue(
                    kotlin.math.abs(actualRatio - expectedRatio) / expectedRatio < tolerance,
                    "Item $index should maintain aspect ratio $expectedRatio, got $actualRatio"
                )
            }
        }
    }

    @Test
    fun `row distribution optimization - items arranged for minimal waste`() {
        val calculator = createCalculator()
        
        // Given: Multiple items with same aspect ratio
        val items = List(10) { createTestItem(aspectRatio = 1.0f, key = "item$it") }

        // When: Calculate rows
        calculator.addItems(items)

        // Then: Rows should efficiently use available width
        calculator.getRows().forEach { row ->
            val totalWidth = row.items.sumOf { it.width }
            val totalPadding = HORIZONTAL_PADDING * (row.items.size - 1)
            val usedWidth = totalWidth + totalPadding
            
            // Width utilization should be good (within 20% of available width)
            val utilization = usedWidth.toFloat() / AVAILABLE_WIDTH
            assertTrue(
                utilization > 0.8f,
                "Row should use at least 80% of available width, used ${(utilization * 100).toInt()}%"
            )
        }
    }

    @Test
    fun `height constraints - all rows respect min and max height`() {
        val calculator = createCalculator()
        
        // Given: Items with extreme aspect ratios
        val items = listOf(
            createTestItem(aspectRatio = 0.3f, key = "very_tall"),  // Very tall
            createTestItem(aspectRatio = 5.0f, key = "very_wide"),  // Very wide
            createTestItem(aspectRatio = 1.0f, key = "square")
        )

        // When: Calculate rows
        calculator.addItems(items)

        // Then: All rows should respect height constraints
        val minHeight = MAX_ROW_HEIGHT / 2
        calculator.getRows().forEach { row ->
            val rowHeight = row.items.first().height
            assertTrue(
                rowHeight >= minHeight,
                "Row height $rowHeight should be at least $minHeight"
            )
            assertTrue(
                rowHeight <= MAX_ROW_HEIGHT,
                "Row height $rowHeight should not exceed $MAX_ROW_HEIGHT"
            )
        }
    }

    @Test
    fun `early termination optimization - stops when score deteriorates`() {
        val calculator = createCalculator()
        
        // Given: Many items where optimal configuration is early in the search
        val items = List(20) { index ->
            // First 3 items have aspect ratios that fit well together
            val ratio = if (index < 3) 1.0f else 0.5f
            createTestItem(aspectRatio = ratio, key = "item$index")
        }

        // When: Calculate rows
        val duration = measureTime {
            calculator.addItems(items)
        }

        // Then: Should complete quickly due to early termination
        assertTrue(duration.inWholeMilliseconds < 200, 
            "Should benefit from early termination, took ${duration.inWholeMilliseconds}ms")
        
        // And: First row should contain the optimally-fitting items
        val firstRow = calculator.getRows().first()
        assertTrue(firstRow.items.size >= 2, 
            "First row should contain multiple items that fit well together")
    }

    @Test
    fun `memory efficiency - no unnecessary allocations for identical input`() {
        val calculator = createCalculator()
        
        // Given: Items already processed
        val items = List(10) { createTestItem(aspectRatio = 1.0f, key = "item$it") }
        calculator.addItems(items)
        val firstRowCount = calculator.getRows().size

        // When: Add the same items again
        calculator.addItems(items)

        // Then: Should skip processing (referential equality check)
        assertEquals(firstRowCount, calculator.getRows().size, 
            "Should not reprocess identical items")
    }

    @Test
    fun `width fitting - items distributed to maximize width usage per row`() {
        val calculator = createCalculator()
        
        // Given: Items that could be arranged in different ways
        val items = List(6) { createTestItem(aspectRatio = 1.5f, key = "item$it") }

        // When: Calculate rows
        calculator.addItems(items)

        // Then: Should create rows that maximize width usage
        calculator.getRows().forEach { row ->
            val rowWidthWithPadding = row.items.sumOf { it.width } + 
                (HORIZONTAL_PADDING * (row.items.size - 1))
            
            // Each row should use most of the available width
            val wastedSpace = AVAILABLE_WIDTH - rowWidthWithPadding
            val wastedPercentage = wastedSpace.toFloat() / AVAILABLE_WIDTH
            
            assertTrue(
                wastedPercentage < 0.25f, // Less than 25% waste
                "Row should waste less than 25% of width, wasted ${(wastedPercentage * 100).toInt()}%"
            )
        }
    }

    @Test
    fun `consistent behavior - same input produces same output`() {
        val calculator1 = createCalculator()
        val calculator2 = createCalculator()
        
        // Given: Same items
        val items = List(20) { index ->
            createTestItem(
                aspectRatio = 0.8f + (index % 5) * 0.3f,
                key = "item$index"
            )
        }

        // When: Calculate rows with both calculators
        calculator1.addItems(items)
        calculator2.addItems(items)

        // Then: Results should be identical
        val rows1 = calculator1.getRows()
        val rows2 = calculator2.getRows()
        
        assertEquals(rows1.size, rows2.size, "Should produce same number of rows")
        
        rows1.indices.forEach { i ->
            val row1 = rows1[i]
            val row2 = rows2[i]
            
            assertEquals(row1.items.size, row2.items.size, 
                "Row $i should have same number of items")
            assertEquals(row1.items.first().height, row2.items.first().height,
                "Row $i should have same height")
        }
    }

    private fun createTestItem(
        aspectRatio: Float,
        key: String = "test"
    ) = AspectoLayoutInfo(
        aspectRatio = aspectRatio,
        key = key,
        contentType = null,
        content = @Composable {}
    )
}
