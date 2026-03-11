package com.ultimate.engine.input

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.MotionEvent
import kotlin.math.atan2
import kotlin.math.hypot

interface GestureListener {
    fun onTap(x: Float, y: Float) {}
    fun onDrag(dx: Float, dy: Float) {}
    fun onSwipe(angle: Float, velocity: Float) {}
}

class TouchInputProcessor(private val listener: GestureListener) {
    private var lastX = 0f
    private var lastY = 0f

    fun onTouch(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y
                listener.onTap(event.x, event.y)
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastX
                val dy = event.y - lastY
                listener.onDrag(dx, dy)
                val velocity = hypot(dx.toDouble(), dy.toDouble()).toFloat()
                listener.onSwipe(atan2(dy, dx), velocity)
                lastX = event.x
                lastY = event.y
            }
        }
        return true
    }
}

class VirtualJoystick {
    var axisX: Float = 0f
    var axisY: Float = 0f
    fun update(touchX: Float, touchY: Float, centerX: Float, centerY: Float, radius: Float) {
        axisX = ((touchX - centerX) / radius).coerceIn(-1f, 1f)
        axisY = ((touchY - centerY) / radius).coerceIn(-1f, 1f)
    }
}

class SensorInput(context: Context, private val onUpdate: (FloatArray, FloatArray) -> Unit) : SensorEventListener {
    private val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var accel = FloatArray(3)
    private var gyro = FloatArray(3)

    fun start() {
        manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also {
            manager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)?.also {
            manager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() = manager.unregisterListener(this)

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> accel = event.values.clone()
            Sensor.TYPE_GYROSCOPE -> gyro = event.values.clone()
        }
        onUpdate(accel, gyro)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
