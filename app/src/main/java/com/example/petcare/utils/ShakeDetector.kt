package com.example.petcare.utils

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.util.Log
import kotlin.math.sqrt

object ShakeDetector {
    private const val GRAVITY_EARTH = 9.80665f

    const val DEFAULT_SHAKE_THRESHOLD_GRAVITY = 2.2f
    const val DEFAULT_SHAKE_SLOP_TIME_MS = 1000L

    fun gForce(x: Float, y: Float, z: Float): Float {
        val gX = x / GRAVITY_EARTH
        val gY = y / GRAVITY_EARTH
        val gZ = z / GRAVITY_EARTH
        return sqrt(gX * gX + gY * gY + gZ * gZ)
    }

    fun isShake(
        gForce: Float,
        now: Long,
        lastShakeTimeMs: Long,
        thresholdGravity: Float = DEFAULT_SHAKE_THRESHOLD_GRAVITY,
        slopTimeMs: Long = DEFAULT_SHAKE_SLOP_TIME_MS
    ): Boolean {
        if (gForce <= thresholdGravity) return false
        return now - lastShakeTimeMs >= slopTimeMs
    }
}

class ShakeEventListener(
    private val thresholdGravity: Float = ShakeDetector.DEFAULT_SHAKE_THRESHOLD_GRAVITY,
    private val slopTimeMs: Long = ShakeDetector.DEFAULT_SHAKE_SLOP_TIME_MS,
    private val onShake: () -> Unit
) : SensorEventListener {
    private var lastShakeTimeMs = 0L

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val gForce = ShakeDetector.gForce(event.values[0], event.values[1], event.values[2])
        if (gForce > 1.05f) {
            Log.d("PetCareShake", "Accelerometer event received: gForce=$gForce")
        }
        val now = System.currentTimeMillis()
        if (ShakeDetector.isShake(gForce, now, lastShakeTimeMs, thresholdGravity, slopTimeMs)) {
            lastShakeTimeMs = now
            Log.d("PetCareShake", "Shake detected: gForce=$gForce threshold=$thresholdGravity")
            onShake()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
