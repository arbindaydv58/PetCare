package com.example.petcare.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class ShakeDetectorTest {

    @Test
    fun gForce_atRest_isApproximatelyOneG() {
        val g = ShakeDetector.gForce(0f, 0f, 9.80665f)
        assertEquals(1f, g, 0.001f)
    }

    @Test
    fun gForce_computesEuclideanMagnitudeAcrossAxes() {
        val g = ShakeDetector.gForce(9.80665f, 9.80665f, 9.80665f)
        assertEquals(sqrt(3f), g, 0.001f)
    }

    @Test
    fun isShake_belowThreshold_isNotAShake() {
        val result = ShakeDetector.isShake(
            gForce = 1.0f,
            now = 10_000L,
            lastShakeTimeMs = 0L
        )
        assertFalse(result)
    }

    @Test
    fun isShake_aboveThreshold_afterCooldown_isAShake() {
        val result = ShakeDetector.isShake(
            gForce = 3.5f,
            now = 10_000L,
            lastShakeTimeMs = 0L
        )
        assertTrue(result)
    }

    @Test
    fun isShake_aboveThreshold_withinCooldown_isDebounced() {
        val firstShakeTime = 10_000L
        val secondSampleTime = firstShakeTime + 200

        val result = ShakeDetector.isShake(
            gForce = 4.0f,
            now = secondSampleTime,
            lastShakeTimeMs = firstShakeTime
        )
        assertFalse(result)
    }

    @Test
    fun isShake_aboveThreshold_afterCooldownWindowElapses_triggersAgain() {
        val firstShakeTime = 10_000L
        val laterSampleTime = firstShakeTime + ShakeDetector.DEFAULT_SHAKE_SLOP_TIME_MS

        val result = ShakeDetector.isShake(
            gForce = 4.0f,
            now = laterSampleTime,
            lastShakeTimeMs = firstShakeTime
        )
        assertTrue(result)
    }

    @Test
    fun isShake_respectsCustomThresholdAndSlopTime() {
        val result = ShakeDetector.isShake(
            gForce = 1.5f,
            now = 1000L,
            lastShakeTimeMs = 0L,
            thresholdGravity = 1.0f,
            slopTimeMs = 500L
        )
        assertTrue(result)

        val debounced = ShakeDetector.isShake(
            gForce = 1.5f,
            now = 400L,
            lastShakeTimeMs = 0L,
            thresholdGravity = 1.0f,
            slopTimeMs = 500L
        )
        assertFalse(debounced)
    }
}
