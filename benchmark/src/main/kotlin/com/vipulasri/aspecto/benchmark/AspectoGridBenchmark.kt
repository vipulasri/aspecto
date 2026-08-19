package com.vipulasri.aspecto.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMetricApi::class)
class AspectoGridBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun startup() = benchmarkRule.measureRepeated(
        packageName = "com.vipulasri.aspecto.benchmark",
        metrics = listOf(StartupTimingMetric(), FrameTimingMetric()),
        iterations = 3,
        startupMode = StartupMode.COLD,
        compilationMode = CompilationMode.Full()
    ) {
        pressHome()
        startActivityAndWait()
        device.waitForIdle()
    }

    @Test
    fun scroll() = benchmarkRule.measureRepeated(
        packageName = "com.vipulasri.aspecto.benchmark",
        metrics = listOf(
            FrameTimingMetric(),
            TraceSectionMetric(sectionName = "Compose:recompose")
        ),
        iterations = 5,
        compilationMode = CompilationMode.Full()
    ) {
        startActivityAndWait()

        val grid = device.wait(Until.findObject(By.desc("aspecto_grid")), 5_000L)
        val bounds = grid.visibleBounds

        repeat(8) {
            device.swipe(
                (bounds.left + bounds.right) / 2,
                (bounds.top + bounds.bottom * 0.8f).toInt(),
                (bounds.left + bounds.right) / 2,
                (bounds.top + bounds.bottom * 0.2f).toInt(),
                50
            )
            device.waitForIdle()
        }
    }
}
