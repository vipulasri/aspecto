package com.vipulasri.aspecto

import androidx.compose.runtime.Composable
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

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
        private const val AD_INTERVAL_ROWS = 3
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

    // region Decoration benchmarks

    private fun buildDecorations(
        rowCount: Int,
        adInterval: Int = AD_INTERVAL_ROWS
    ): List<RowDecoration> {
        val decorations = mutableListOf<RowDecoration>()
        decorations.add(RowDecoration(index = 0, key = "header") {})
        var row = adInterval
        while (row < rowCount) {
            decorations.add(RowDecoration(index = row, key = "ad-$row", contentType = "ad") {})
            row += adInterval
        }
        return decorations
    }

    private fun timeMillisWithDecorations(count: Int, ratioAt: (Int) -> Float): Double {
        val items = buildItems(count, ratioAt)
        val estimatedRows = count / 3
        val decorations = buildDecorations(estimatedRows)
        repeat(WARMUP_ITERATIONS) {
            calculateRows(items, AVAILABLE_WIDTH, MAX_ROW_HEIGHT, HORIZONTAL_PADDING, decorations)
        }
        var best = Double.MAX_VALUE
        repeat(MEASURE_ITERATIONS) {
            val start = System.nanoTime()
            calculateRows(items, AVAILABLE_WIDTH, MAX_ROW_HEIGHT, HORIZONTAL_PADDING, decorations)
            val elapsed = (System.nanoTime() - start) / 1_000_000.0
            if (elapsed < best) best = elapsed
        }
        return best
    }

    private fun timeMillisWithoutDecorations(count: Int, ratioAt: (Int) -> Float): Double {
        val items = buildItems(count, ratioAt)
        repeat(WARMUP_ITERATIONS) {
            calculateRows(items, AVAILABLE_WIDTH, MAX_ROW_HEIGHT, HORIZONTAL_PADDING)
        }
        var best = Double.MAX_VALUE
        repeat(MEASURE_ITERATIONS) {
            val start = System.nanoTime()
            calculateRows(items, AVAILABLE_WIDTH, MAX_ROW_HEIGHT, HORIZONTAL_PADDING)
            val elapsed = (System.nanoTime() - start) / 1_000_000.0
            if (elapsed < best) best = elapsed
        }
        return best
    }

    @Test
    fun `decorated calculateRows time scales linearly with item count`() {
        val times = ITEM_COUNTS.associateWith { timeMillisWithDecorations(it, realisticRatio()) }
        println("\n=== decorated calculateRows time scaling ===")
        times.forEach { (count, ms) ->
            println("  items=%,8d  best=%8.3f ms  (%,10d items/ms)".format(count, ms, (count / ms).toInt()))
        }

        val (t1k, t100k) = times[1_000]!! to times[100_000]!!
        val ratio = t100k / t1k
        println("  100k/1k time ratio = %.1fx (linear ≈ 100x)".format(ratio))

        assertTrue(
            t100k <= t1k * 250 + 10,
            "decorated 100k took ${"%.2f".format(t100k)}ms vs ${"%.2f".format(t1k)}ms for 1k " +
                "(ratio ${"%.1f".format(ratio)}x) — looks super-linear!"
        )
    }

    @Test
    fun `decoration overhead is minimal compared to plain layout`() {
        val ratioAt = realisticRatio()
        println("\n=== decoration overhead ===")
        println("  ${"items".padStart(10)}  ${"plain (ms)".padStart(12)}  ${"decorated (ms)".padEnd(14)}  ${"overhead".padStart(10)}")

        for (count in ITEM_COUNTS) {
            val plain = timeMillisWithoutDecorations(count, ratioAt)
            val decorated = timeMillisWithDecorations(count, ratioAt)
            val overhead = (decorated - plain) / plain * 100
            println(
                "  ${"$count".padStart(10)}  ${"%.3f".format(plain).padStart(12)}  " +
                    "${"%.3f".format(decorated).padEnd(14)}  ${"%.1f%%".format(overhead).padStart(10)}"
            )
            // SpliceDecorations is a single O(rows) pass; it should not double the time.
            assertTrue(
                decorated <= plain * 2.5 + 5,
                "decorated layout on ${count} items took ${"%.2f".format(decorated)}ms vs " +
                    "${"%.2f".format(plain)}ms plain — decoration overhead too high!"
            )
        }
    }

    @Test
    fun `decorated row keys stay stable and deterministic across add and remove`() {
        val sizes = listOf(10 to "few", 1_000 to "medium", 100_000 to "large")

        for ((size, label) in sizes) {
            val items = buildKeyedItems(size, realisticRatio())
            val estimatedRows = size / 3
            val decorations = buildDecorations(estimatedRows)
            val original = calculateRows(items, AVAILABLE_WIDTH, MAX_ROW_HEIGHT, HORIZONTAL_PADDING, decorations)

            for (mutation in Mutation.entries) {
                val mutated = applyMutation(items, mutation)
                val mutatedEstimatedRows = mutated.size / 3
                val mutatedDecorations = buildDecorations(mutatedEstimatedRows)
                val rows = calculateRows(mutated, AVAILABLE_WIDTH, MAX_ROW_HEIGHT, HORIZONTAL_PADDING, mutatedDecorations)

                // Determinism
                val recomputed = calculateRows(mutated, AVAILABLE_WIDTH, MAX_ROW_HEIGHT, HORIZONTAL_PADDING, mutatedDecorations)
                assertEquals(rows, recomputed, "$label ${mutation.label} decorated must be deterministic")

                // All keys are unique
                val keys = rows.map { it.key }
                assertEquals(keys.size, keys.toSet().size, "$label ${mutation.label} decorated has duplicate keys")

                // Regular rows cover all items
                var itemCursor = 0
                for (row in rows) {
                    if (!row.isFullWidth) {
                        assertEquals(mutated[itemCursor].key, row.key, "$label ${mutation.label} regular row key")
                        itemCursor += row.items.size
                    }
                }
                assertEquals(mutated.size, itemCursor, "$label ${mutation.label} covers all items")
            }

            // Append-at-end preserves existing row keys
            val appendedDecorations = buildDecorations((size + 1) / 3)
            val appended = calculateRows(
                items + keyedItem(2.0f, -1), AVAILABLE_WIDTH, MAX_ROW_HEIGHT, HORIZONTAL_PADDING, appendedDecorations
            )
            val originalRegularKeys = original.filter { !it.isFullWidth }.map { it.key }
            val appendedRegularKeys = appended.filter { !it.isFullWidth }.map { it.key }
            assertEquals(
                originalRegularKeys,
                appendedRegularKeys.take(originalRegularKeys.size),
                "$label decorated append-at-end preserved existing regular row keys"
            )
        }
    }

    @Test
    fun `high decoration density scales linearly`() {
        // Scenario: a decoration between every row (header + ad every row)
        val ratioAt = realisticRatio()
        println("\n=== high decoration density (decoration every row) ===")

        val times = mutableMapOf<Int, Double>()
        for (count in ITEM_COUNTS) {
            val items = buildItems(count, ratioAt)
            val estimatedRows = count / 3
            // Decorations at every row index (very dense)
            val decorations = List(estimatedRows) { i ->
                RowDecoration(index = i, key = "dense-$i") {}
            }
            repeat(WARMUP_ITERATIONS) {
                calculateRows(items, AVAILABLE_WIDTH, MAX_ROW_HEIGHT, HORIZONTAL_PADDING, decorations)
            }
            var best = Double.MAX_VALUE
            repeat(MEASURE_ITERATIONS) {
                val start = System.nanoTime()
                calculateRows(items, AVAILABLE_WIDTH, MAX_ROW_HEIGHT, HORIZONTAL_PADDING, decorations)
                val elapsed = (System.nanoTime() - start) / 1_000_000.0
                if (elapsed < best) best = elapsed
            }
            times[count] = best
            println("  items=%,8d  rows=%,6d  decorations=%,6d  best=%8.3f ms".format(
                count, estimatedRows, estimatedRows, best
            ))
        }

        val (t1k, t100k) = times[1_000]!! to times[100_000]!!
        val ratio = t100k / t1k
        println("  100k/1k time ratio = %.1fx (linear ≈ 100x)".format(ratio))

        assertTrue(
            t100k <= t1k * 250 + 10,
            "high-density decorated 100k took ${"%.2f".format(t100k)}ms vs " +
                "${"%.2f".format(t1k)}ms for 1k (ratio ${"%.1f".format(ratio)}x) — looks super-linear!"
        )
    }

    // endregion

    // region Percentile helpers

    private data class PercentileResult(
        val min: Double,
        val p50: Double,
        val p75: Double,
        val p90: Double,
        val p95: Double,
        val p99: Double,
        val max: Double,
        val iterations: Int
    ) {
        fun format(): String =
            "min=${"%.3f".format(min)}  p50=${"%.3f".format(p50)}  p75=${"%.3f".format(p75)}  " +
                "p90=${"%.3f".format(p90)}  p95=${"%.3f".format(p95)}  p99=${"%.3f".format(p99)}  " +
                "max=${"%.3f".format(max)} ms  (n=$iterations)"
    }

    private fun computePercentiles(samples: List<Double>): PercentileResult {
        val sorted = samples.sorted()
        return PercentileResult(
            min = sorted.first(),
            p50 = percentile(sorted, 50),
            p75 = percentile(sorted, 75),
            p90 = percentile(sorted, 90),
            p95 = percentile(sorted, 95),
            p99 = percentile(sorted, 99),
            max = sorted.last(),
            iterations = sorted.size
        )
    }

    private fun percentile(sorted: List<Double>, p: Int): Double {
        val index = (p / 100.0) * (sorted.size - 1)
        val lower = index.toInt()
        val upper = lower + 1
        if (upper >= sorted.size) return sorted.last()
        val fraction = index - lower
        return sorted[lower] + (sorted[upper] - sorted[lower]) * fraction
    }

    private fun collectTimings(
        iterations: Int,
        block: () -> Unit
    ): List<Double> {
        // Warmup
        repeat(WARMUP_ITERATIONS) { block() }
        // Collect
        return List(iterations) {
            val start = System.nanoTime()
            block()
            TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - start) / 1000.0
        }
    }

    private fun collectTimings(
        count: Int,
        ratioAt: (Int) -> Float,
        iterations: Int,
        decorations: List<RowDecoration>? = null
    ): List<Double> {
        val items = buildItems(count, ratioAt)
        return collectTimings(iterations) {
            if (decorations != null) {
                calculateRows(items, AVAILABLE_WIDTH, MAX_ROW_HEIGHT, HORIZONTAL_PADDING, decorations)
            } else {
                calculateRows(items, AVAILABLE_WIDTH, MAX_ROW_HEIGHT, HORIZONTAL_PADDING)
            }
        }
    }

    private fun collectHighDensityTimings(
        count: Int,
        ratioAt: (Int) -> Float,
        iterations: Int
    ): List<Double> {
        val items = buildItems(count, ratioAt)
        val estimatedRows = count / 3
        val decorations = List(estimatedRows) { i ->
            RowDecoration(index = i, key = "dense-$i") {}
        }
        return collectTimings(iterations) {
            calculateRows(items, AVAILABLE_WIDTH, MAX_ROW_HEIGHT, HORIZONTAL_PADDING, decorations)
        }
    }

    // endregion

    // region Report generation

    private data class BenchmarkScenario(
        val name: String,
        val itemCounts: List<Int>,
        val collect: (count: Int) -> List<Double>
    )

    @Test
    fun `generate benchmark report with percentiles`() {
        val iterations = 30
        val ratioAt = realisticRatio()
        val estimatedRows = { count: Int -> count / 3 }

        val scenarios = listOf(
            BenchmarkScenario("plain", ITEM_COUNTS.toList()) { count ->
                collectTimings(count, ratioAt, iterations, decorations = null)
            },
            BenchmarkScenario("decorated", ITEM_COUNTS.toList()) { count ->
                val decs = buildDecorations(estimatedRows(count))
                collectTimings(count, ratioAt, iterations, decorations = decs)
            },
            BenchmarkScenario("high_density", ITEM_COUNTS.toList()) { count ->
                collectHighDensityTimings(count, ratioAt, iterations)
            }
        )

        val results = mutableMapOf<String, Map<Int, PercentileResult>>()

        println("\n=== Benchmark Report ($iterations iterations) ===\n")

        for (scenario in scenarios) {
            println("--- ${scenario.name} ---")
            val byCount = mutableMapOf<Int, PercentileResult>()
            for (count in scenario.itemCounts) {
                val timings = scenario.collect(count)
                val pct = computePercentiles(timings)
                byCount[count] = pct
                println("  items=%,8d  %s".format(count, pct.format()))
            }
            results[scenario.name] = byCount
            println()
        }

        // Overhead comparison
        println("--- overhead (decorated vs plain) ---")
        for (count in ITEM_COUNTS) {
            val plain = results["plain"]!![count]!!
            val decorated = results["decorated"]!![count]!!
            val overheadP50 = (decorated.p50 - plain.p50) / plain.p50 * 100
            val overheadP99 = (decorated.p99 - plain.p99) / plain.p99 * 100
            println(
                "  items=%,8d  p50 overhead=%6.1f%%  p99 overhead=%6.1f%%".format(
                    count, overheadP50, overheadP99
                )
            )
        }
        println()

        // Linearity check
        println("--- linearity (100k / 1k ratio, lower = better, linear ≈ 100x) ---")
        for (scenario in scenarios) {
            val t1k = results[scenario.name]!![1_000]!!
            val t100k = results[scenario.name]!![100_000]!!
            val ratioP50 = t100k.p50 / t1k.p50
            val ratioP99 = t100k.p99 / t1k.p99
            println("  %-14s  p50 ratio=%6.1fx  p99 ratio=%6.1fx".format(scenario.name, ratioP50, ratioP99))
        }
        println()

        // Write JSON report
        writeReport(results, iterations)
    }

    private fun writeReport(
        results: Map<String, Map<Int, PercentileResult>>,
        iterations: Int
    ) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date())
        val gitCommit = runCatching {
            Runtime.getRuntime().exec(arrayOf("git", "rev-parse", "--short", "HEAD"))
                .inputStream.bufferedReader().readText().trim()
        }.getOrDefault("unknown")
        val gitBranch = runCatching {
            Runtime.getRuntime().exec(arrayOf("git", "rev-parse", "--abbrev-ref", "HEAD"))
                .inputStream.bufferedReader().readText().trim()
        }.getOrDefault("unknown")

        val sb = StringBuilder()
        sb.appendLine("{")
        sb.appendLine("  \"timestamp\": \"$timestamp\",")
        sb.appendLine("  \"gitCommit\": \"$gitCommit\",")
        sb.appendLine("  \"gitBranch\": \"$gitBranch\",")
        sb.appendLine("  \"iterations\": $iterations,")
        sb.appendLine("  \"results\": {")

        val scenarioEntries = results.entries.toList()
        for (sIdx in scenarioEntries.indices) {
            val scenarioName = scenarioEntries[sIdx].key
            val scenarioCounts = scenarioEntries[sIdx].value
            sb.appendLine("    \"$scenarioName\": {")
            val countEntries = scenarioCounts.entries.toList()
            for (cIdx in countEntries.indices) {
                val countKey = countEntries[cIdx].key
                val pct = countEntries[cIdx].value
                val comma = if (cIdx < countEntries.size - 1) "," else ""
                sb.appendLine("      \"$countKey\": {")
                sb.appendLine("        \"min\": ${pct.min},")
                sb.appendLine("        \"p50\": ${pct.p50},")
                sb.appendLine("        \"p75\": ${pct.p75},")
                sb.appendLine("        \"p90\": ${pct.p90},")
                sb.appendLine("        \"p95\": ${pct.p95},")
                sb.appendLine("        \"p99\": ${pct.p99},")
                sb.appendLine("        \"max\": ${pct.max}")
                sb.appendLine("      }$comma")
            }
            val comma = if (sIdx < scenarioEntries.size - 1) "," else ""
            sb.appendLine("    }$comma")
        }

        sb.appendLine("  }")
        sb.appendLine("}")

        val projectRoot = File(System.getProperty("user.dir")).let { dir ->
            generateSequence(dir) { it.parentFile }
                .firstOrNull { File(it, "settings.gradle.kts").exists() } ?: dir
        }
        val reportFile = File(projectRoot, "benchmark/benchmark-results.json")
        reportFile.writeText(sb.toString())
        println("Report written to: ${reportFile.absolutePath}")
    }

    // endregion
}
