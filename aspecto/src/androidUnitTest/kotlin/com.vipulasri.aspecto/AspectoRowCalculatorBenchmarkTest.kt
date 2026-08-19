package com.vipulasri.aspecto

import androidx.compose.runtime.Composable
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Micro-benchmark (JVM/Robolectric) for [calculateRows] — not a correctness test.
 *
 * Measures the two properties that matter for the pure-function design:
 *  1. Time scaling vs item count (linearity check), including the cost of a single
 *     item insert/remove at the front, middle, or end.
 *  2. Retained layout memory per item (every row holds an [AspectoLayoutInfo] copy with its
 *     content lambda for the whole dataset, even off-screen rows).
 *  3. Row-key stability across mutations (append/remove at the end must not disturb existing
 *     rows; keys must stay deterministic for any mutation).
 *
 * Numbers are best-of-N (min) to be robust to GC noise. Assertions are deliberately loose —
 * they exist to catch catastrophic super-linear blowups, not to gate on absolute speed.
 */
class AspectoRowCalculatorBenchmarkTest {

    companion object {
        private const val MAX_ROW_HEIGHT = 600
        private const val HORIZONTAL_PADDING = 8
        private const val AVAILABLE_WIDTH = 1000
        private const val WARMUP_ITERATIONS = 5
        private const val MEASURE_ITERATIONS = 7
        private val ITEM_COUNTS = intArrayOf(1_000, 10_000, 100_000)
    }

    private fun item(ratio: Float, index: Int) = AspectoLayoutInfo(
        aspectRatio = ratio,
        key = index,
        contentType = null,
        content = @Composable {}
    )

    private fun buildItems(count: Int, ratioAt: (Int) -> Float): List<AspectoLayoutInfo> =
        List(count) { item(ratioAt(it), it) }

    /** Deterministic pseudo-random mix of realistic artwork ratios (0.5 .. 2.0). */
    private fun realisticRatio(): (Int) -> Float {
        val random = Random(42)
        return { 0.5f + random.nextFloat() * 1.5f }
    }

    private fun timeMillis(items: List<AspectoLayoutInfo>): Double {
        repeat(WARMUP_ITERATIONS) { calculateRows(items, AVAILABLE_WIDTH, MAX_ROW_HEIGHT, HORIZONTAL_PADDING) }
        var best = Double.MAX_VALUE
        repeat(MEASURE_ITERATIONS) {
            val start = System.nanoTime()
            calculateRows(items, AVAILABLE_WIDTH, MAX_ROW_HEIGHT, HORIZONTAL_PADDING)
            val elapsed = (System.nanoTime() - start) / 1_000_000.0
            if (elapsed < best) best = elapsed
        }
        return best
    }

    private fun timeMillis(count: Int, ratioAt: (Int) -> Float): Double =
        timeMillis(buildItems(count, ratioAt))

    /** Retained bytes of the returned layout for the whole dataset, divided by item count. */
    private fun retainedBytesPerItem(count: Int, ratioAt: (Int) -> Float): Long {
        val items = buildItems(count, ratioAt)
        var sink: Any? = null
        System.gc()
        Thread.sleep(200)
        val before = usedBytes()
        sink = calculateRows(items, AVAILABLE_WIDTH, MAX_ROW_HEIGHT, HORIZONTAL_PADDING)
        val after = usedBytes()
        sink = null
        return ((after - before).toDouble() / count).toLong().coerceAtLeast(0)
    }

    private fun usedBytes(): Long =
        Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()

    @Test
    fun `calculateRows time scales linearly with item count`() {
        val scenarios = listOf(
            "realistic (0.5..2.0)" to realisticRatio(),
            "narrow 0.05" to { _: Int -> 0.05f },
            "wide 2.0" to { _: Int -> 2.0f }
        )

        for ((label, ratioAt) in scenarios) {
            val times = ITEM_COUNTS.associateWith { timeMillis(it, ratioAt) }
            println("\n=== calculateRows time scaling: $label ===")
            times.forEach { (count, ms) ->
                println("  items=%,8d  best=%8.3f ms  (%,10d items/ms)".format(count, ms, (count / ms).toInt()))
            }

            val (t1k, t100k) = times[1_000]!! to times[100_000]!!
            val ratio = t100k / t1k
            println("  100k/1k time ratio = %.1fx (linear ≈ 100x)".format(ratio))

            assertTrue(
                t100k <= t1k * 250 + 10,
                "$label: 100k took ${"%.2f".format(t100k)}ms vs ${"%.2f".format(t1k)}ms for 1k " +
                    "(ratio ${"%.1f".format(ratio)}x) — looks super-linear!"
            )
        }
    }

    @Test
    fun `retained layout memory is linear in item count`() {
        println("\n=== retained layout memory (whole dataset) ===")
        for (ratioAt in listOf(realisticRatio(), { _: Int -> 0.05f })) {
            val label = if (ratioAt(0) < 0.1f) "narrow 0.05" else "realistic"
            val bytes = retainedBytesPerItem(100_000, ratioAt)
            println("  $label: ~%,d bytes/item retained".format(bytes))
            assertTrue(
                bytes in 1..2_000,
                "$label retained bytes/item ($bytes) out of expected range"
            )
        }
    }

    private enum class Mutation(val label: String) {
        INSERT_FRONT("insert-front"),
        INSERT_MIDDLE("insert-middle"),
        INSERT_END("insert-end"),
        REMOVE_FRONT("remove-front"),
        REMOVE_MIDDLE("remove-middle"),
        REMOVE_END("remove-end");
    }

    private fun keyedItem(ratio: Float, key: Int) = AspectoLayoutInfo(
        aspectRatio = ratio,
        key = key,
        contentType = null,
        content = @Composable {}
    )

    private fun buildKeyedItems(count: Int, ratioAt: (Int) -> Float): List<AspectoLayoutInfo> =
        List(count) { keyedItem(ratioAt(it), it) }

    /** Returns the list after a single insert/remove; the inserted item uses a unique key (-1). */
    private fun applyMutation(items: List<AspectoLayoutInfo>, mutation: Mutation): List<AspectoLayoutInfo> {
        val mutable = items.toMutableList()
        when (mutation) {
            Mutation.INSERT_FRONT -> mutable.add(0, keyedItem(2.0f, -1))
            Mutation.INSERT_MIDDLE -> mutable.add(items.size / 2, keyedItem(2.0f, -1))
            Mutation.INSERT_END -> mutable.add(keyedItem(2.0f, -1))
            Mutation.REMOVE_FRONT -> mutable.removeAt(0)
            Mutation.REMOVE_MIDDLE -> mutable.removeAt(items.size / 2)
            Mutation.REMOVE_END -> mutable.removeAt(items.lastIndex)
        }
        return mutable
    }

    @Test
    fun `add and remove recalc time scales linearly with dataset size`() {
        val sizes = listOf(10 to "few", 1_000 to "medium", 100_000 to "large")
        val operations = listOf(null to "baseline") +
            Mutation.entries.map { it to it.label }

        println("\n=== calculateRows recalc time after one add/remove (best of $MEASURE_ITERATIONS) ===")
        println("  ${"operation".padEnd(16)}" + sizes.joinToString("") { "  ${it.second.padStart(10)}" })

        // operation -> best time per size
        val timesPerOp = mutableMapOf<String, Map<Int, Double>>()
        for ((mutation, label) in operations) {
            val bySize = sizes.associate { (size, _) ->
                val items = buildKeyedItems(size, realisticRatio())
                val target = mutation?.let { applyMutation(items, it) } ?: items
                size to timeMillis(target)
            }
            timesPerOp[label] = bySize
            println(
                "  ${label.padEnd(16)}" +
                    sizes.joinToString("") { (size, _) -> "  ${"%.3f ms".format(bySize[size]!!).padStart(10)}" }
            )
        }

        // Every mutation is the same O(n) pass as a plain recalculation, so time must scale
        // linearly (large/medium ≈ 100x); guard against catastrophic super-linear blowups.
        for (mutation in Mutation.entries) {
            val times = timesPerOp.getValue(mutation.label)
            val medium = times.getValue(1_000)
            val large = times.getValue(100_000)
            assertTrue(
                large <= medium * 250 + 10,
                "${mutation.label} on 100k took ${"%.2f".format(large)}ms vs " +
                    "${"%.2f".format(medium)}ms on 1k — looks super-linear!"
            )
        }

        // A single mutation should cost no more than a full recalculation of the same dataset.
        val baselineLarge = timesPerOp.getValue("baseline").getValue(100_000)
        for (mutation in Mutation.entries) {
            val large = timesPerOp.getValue(mutation.label).getValue(100_000)
            assertTrue(
                large <= baselineLarge * 5 + 5,
                "${mutation.label} on 100k (${"%.2f".format(large)}ms) should not exceed a plain " +
                    "recalc (${"%.2f".format(baselineLarge)}ms) by much — the layout is fully recomputed anyway."
            )
        }
    }

    @Test
    fun `row keys stay stable and deterministic across add and remove`() {
        val sizes = listOf(10 to "few", 1_000 to "medium", 100_000 to "large")

        for ((size, label) in sizes) {
            val items = buildKeyedItems(size, realisticRatio())
            val original = calculateRows(items, AVAILABLE_WIDTH, MAX_ROW_HEIGHT, HORIZONTAL_PADDING)

            for (mutation in Mutation.entries) {
                val mutated = applyMutation(items, mutation)
                val rows = calculateRows(mutated, AVAILABLE_WIDTH, MAX_ROW_HEIGHT, HORIZONTAL_PADDING)

                // Determinism: the same input always yields the same rows.
                val recomputed = calculateRows(mutated, AVAILABLE_WIDTH, MAX_ROW_HEIGHT, HORIZONTAL_PADDING)
                assertEquals(rows, recomputed, "$label ${mutation.label} must be deterministic")

                // Every row key equals the key of its first item, and rows cover the dataset exactly.
                var itemCursor = 0
                for (row in rows) {
                    assertEquals(mutated[itemCursor].key, row.key, "$label ${mutation.label} row key")
                    itemCursor += row.items.size
                }
                assertEquals(mutated.size, itemCursor, "$label ${mutation.label} covers all items")
            }

            // Appending and truncating at the end must never disturb existing rows.
            val appended = calculateRows(
                items + keyedItem(2.0f, -1), AVAILABLE_WIDTH, MAX_ROW_HEIGHT, HORIZONTAL_PADDING
            )
            assertEquals(
                original.map { it.key },
                appended.map { it.key }.take(original.size),
                "$label append-at-end preserved existing row keys"
            )

            val truncated = calculateRows(
                items.dropLast(1), AVAILABLE_WIDTH, MAX_ROW_HEIGHT, HORIZONTAL_PADDING
            )
            assertEquals(
                original.map { it.key }.take(truncated.size),
                truncated.map { it.key },
                "$label remove-at-end preserved existing row keys"
            )
        }
    }
}
