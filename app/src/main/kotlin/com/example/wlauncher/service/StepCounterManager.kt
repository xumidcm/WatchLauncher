package com.example.wlauncher.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object StepCounterManager : SensorEventListener {

    private const val DEFAULT_STEP_GOAL = 10000

    private var sensorManager: SensorManager? = null
    private var stepSensor: Sensor? = null

    private val _steps = MutableStateFlow(0)
    val steps: StateFlow<Int> = _steps.asStateFlow()

    private val _goal = MutableStateFlow(DEFAULT_STEP_GOAL)
    val goal: StateFlow<Int> = _goal.asStateFlow()

    private val _available = MutableStateFlow(false)
    val available: StateFlow<Boolean> = _available.asStateFlow()

    fun initialize(context: Context) {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        _available.value = stepSensor != null

        if (stepSensor != null) {
            sensorManager?.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun release() {
        sensorManager?.unregisterListener(this)
    }

    fun setGoal(newGoal: Int) {
        _goal.value = newGoal.coerceIn(1000, 50000)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            // 步数传感器返回的是自启动以来的累计步数
            // 这里我们直接使用这个值作为当日步数（简化处理）
            _steps.value = event.values[0].toInt()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun hasStepSensor(context: Context): Boolean {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        return sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null
    }
}
