package com.vipulasri.aspecto

import androidx.compose.runtime.Composable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.abs

class AspectoRowCalculatorTest {
    
    companion object {
        private const val MAX_ROW_HEIGHT = 600
        private const val HORIZONTAL_PADDING = 8
        private const val AVAILABLE_WIDTH = 1000
    }
    
    private lateinit var calculator: AspectoRowCalculator
    
    @Before
    fun setup() {
        calculator = AspectoRowCalculator(
            maxRowHeight = MAX_ROW_HEIGHT,
            horizontalPadding = HORIZONTAL_PADDING
        ).apply {
            setMaxRowWidth(AVAILABLE_WIDTH)
        }
    }
    
    @Test
    fun `two items with same aspect ratio should split width equally`() {
        // Given
        val items = listOf(
            createTestItem(aspectRatio = 1.0f),
            createTestItem(aspectRatio = 1.0f)
        )
        
        // When
        calculator.addItems(items)
        
        // Then
        val row = calculator.getRows().first()
        val (item1, item2) = row.items
        
        val expectedWidth = (AVAILABLE_WIDTH - HORIZONTAL_PADDING) / 2 // total width minus padding divided by 2
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
        calculator.addItems(items)
        
        // Then
        val item = calculator.getRows().first().items.first()
        assertEquals(MAX_ROW_HEIGHT / 2, item.height) // Should be at minRowHeight (maxRowHeight * 0.5)
        assertEquals(AVAILABLE_WIDTH, item.width)
    }
    
    @Test
    fun `should handle empty list`() {
        // When
        calculator.addItems(emptyList())
        
        // Then
        val rows = calculator.getRows()
        assertEquals(0, rows.size)
    }
    
    @Test
    fun `should maintain previous items when adding new ones`() {
        // Given
        val initialItems = listOf(createTestItem(aspectRatio = 1.0f))
        calculator.addItems(initialItems)


        // Then
        val rows = calculator.getRows()
        assertEquals(1, rows.size)
        assertEquals(1, rows[0].items.size)
        
        // When
        val newItems = listOf(createTestItem(aspectRatio = 2.0f))
        calculator.addItems(initialItems + newItems)
        
        // Then
        val updatedRows = calculator.getRows()
        assertEquals(2, rows.size)
        assertEquals(1, rows[1].items.size)
        assertEquals(1.0f, updatedRows[0].items[0].aspectRatio)
        assertEquals(2.0f, updatedRows[1].items[0].aspectRatio)
    }
    
    @Test
    fun `should use 75 percent of max row height when item has bigger height`() {
        // Given
        val item = createTestItem(aspectRatio = 0.5f) // Aspect ratio that would result in height > maxRowHeight
        calculator.setMaxRowWidth(1000) // This width with 0.5 aspect ratio would result in height = 2000
        
        // When
        calculator.addItems(listOf(item))

        // Then
        val layoutItem = calculator.getRows().first().items.first()
        val expectedHeight = (MAX_ROW_HEIGHT * 0.75f).toInt() // 75% of 600 = 450
        assertEquals(expectedHeight, layoutItem.height)
        assertEquals((expectedHeight * item.aspectRatio).toInt(), layoutItem.width)
    }
    
    @Test
    fun `should handle very large number of items`() {
        // Given
        val items = List(100) { createTestItem(aspectRatio = 1.0f) }
        
        // When
        calculator.addItems(items)
        
        // Then
        val rows = calculator.getRows()
        assertTrue(rows.size > 1)
        assertTrue(rows.all { it.items.size <= 3 }) // No row should exceed max items
    }
    
    @Test
    fun `should distribute items evenly across rows based on available width`() {
        // Given
        val items = List(4) { createTestItem(aspectRatio = 1.0f) }
        
        // When
        calculator.addItems(items)
        
        // Then
        val rows = calculator.getRows()
        assertTrue(rows.isNotEmpty()) // Should have at least one row
        assertTrue(rows.all { row -> 
            // Each row's total width should be close to effective width
            val effectiveWidth = AVAILABLE_WIDTH - (HORIZONTAL_PADDING * (row.items.size - 1))
            val totalWidth = row.items.sumOf { it.width }
            abs(totalWidth - effectiveWidth) < 1 // Allow for rounding
        })
        
        // All items in each row should have the same height
        rows.forEach { row ->
            assertEquals(1, row.items.map { it.height }.distinct().size)
        }
    }
    
    @Test
    fun `should handle incremental updates efficiently`() {
        // Given
        val initialItems = List(3) { createTestItem(aspectRatio = 1.0f, key = "item$it") }
        calculator.addItems(initialItems)
        val initialRows = calculator.getRows()

        // When - add a new item at the end
        val updatedItems = initialItems + createTestItem(aspectRatio = 1.0f, key = "new")
        calculator.addItems(updatedItems)

        // Then
        val updatedRows = calculator.getRows()
        // Initial items should be present
        val allKeys = updatedRows.flatMap { row -> row.items.map { it.key } }
        assertTrue(allKeys.containsAll(listOf("item0", "item1", "item2", "new")))
        assertEquals(updatedItems.size, allKeys.size)
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
        calculator.addItems(items)
        
        // Then
        calculator.getRows().forEach { row ->
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
        calculator.addItems(items)
        
        // Then
        calculator.getRows().forEach { row ->
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