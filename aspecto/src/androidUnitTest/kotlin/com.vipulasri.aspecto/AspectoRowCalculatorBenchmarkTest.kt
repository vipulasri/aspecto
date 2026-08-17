package com.vipulasri.aspecto

import androidx.compose.runtime.Composable
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Micro-benchmark (JVM/Robolectric) for [calculateRows] — not a correctness test.
 *
 * Measures the two properties that matter for the pure-function design:
 *  1. Time scaling vs item count (linearity check).
 *  2. Retained layout memory per item (every row holds an [AspectoLayoutInfo] copy with its
 *     content lambda for the whole dataset, even off-screen rows).
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

    private fun timeMillis(count: Int, ratioAt: (Int) -> Float): Double {
        val items = buildItems(count, ratioAt)
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
}
