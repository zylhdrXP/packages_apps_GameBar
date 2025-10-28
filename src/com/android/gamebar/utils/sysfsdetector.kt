/*
 * SPDX-FileCopyrightText: 2025 kenway214
 * SPDX-FileCopyrightText: 2025 carlos 'klozz' jesus
 * SPDX-License-Identifier: Apache-2.0
 */

 package com.android.gamebar.utils
 import android.util.Log
import java.io.File

/**
 * Utility class to automatically detect sysfs nodes for hardware monitoring.
 * Eliminates the need for device-specific overlays.
 */
object SysfsDetector {
    private const val TAG = "SysfsDetector"
    
    // Cache for detected paths to avoid repeated filesystem scans
    private val detectedPaths = mutableMapOf<String, String?>()

    // Cache for detected dividers
    private val detectedDividers = mutableMapOf<String, Int>()

    /**
     * Automatically detects the temperature divider by reading the node value
     * and analyzing the raw temperature format (milli, deci, centi, or celsius)
     *
     * @param path The sysfs path to analyze
     * @return The appropriate divider value (1000, 100, 10, or 1)
     */
    private fun detectTemperatureDivider(path: String): Int {
        return try {
            val file = File(path)
            if (!file.exists() || !file.canRead()) return 1000 //by default

            val content = file.readText().trim()
            if (content.isEmpty()) return 1000

            val rawValue = content.toInt()

            // Detect format based on typical temperature values
            when {
                rawValue in 20000..50000 -> 1000 // Mili-celsius (example: 35000 = 35°C)
                rawValue in 2000..5000 -> 100    // Centi-celsius (example: 3500 = 35°c)
                rawValue in 200..500 -> 10       // Deci-celsius (example: 350 = 35°C)
                rawValue in 20..100 -> 1         //celcius
                else -> 1000 // by default use 1000
            }.also { divider ->
                Log.d("GameBarConfig:","Detected temperature divider for $path: $divider (raw value: $rawValue)")
            }

        } catch (e: Exception){
            Log.w(TAG, "Failed to detect temperature divider for $path, using default 1000")
            1000
        }
    }

    /**
     * Detects path and divider for battery temperature
     */
    fun getBatteryTempInfo(): Pair<String?, Int> {
        val path = getBatteryTempPath()
        if (path == null) return Pair(null, 1000)
        
        if (detectedDividers.containsKey("battery_temp")) {
            return Pair(path, detectedDividers["battery_temp"]!!)
        }
        
        // Detectar divisor automáticamente
        val divider = detectTemperatureDivider(path)
        detectedDividers["battery_temp"] = divider
        return Pair(path, divider)
    }

    // Possible paths for each hardware component
    private val BATTERY_TEMP_PATHS = arrayOf(
        "/sys/class/power_supply/battery/temp",
        "/sys/class/power_supply/battery/batt_temp",
        "/sys/class/power_supply/bms/temp",
        "/sys/class/oplus_chg/battery/temp",
        "/sys/class/oplus_chg/battery/batt_temp",
        "/sys/class/oplus_chg/battery/temperature",
        "/sys/class/oplus_chg/bq27541/temp",
        "/sys/class/thermal/thermal_zone0/temp"
    )
    
    private val FPS_PATHS = arrayOf(
        "/sys/class/drm/card0/sde_crtc_fps",
        "/sys/class/graphics/fb0/fps",
        "/sys/class/graphics/fb0/measured_fps",
        "/sys/class/drm/sde-crtc-0/measured_fps"
    )

    /**
     * Detect a valid sysfs path from a list of possible paths
     */
    private fun detectPath(pathType: String, possiblePaths: Array<String>): String? {
        // Check cache first
        detectedPaths[pathType]?.let { return it }
        
        for (path in possiblePaths) {
            val file = File(path)
            if (file.exists() && file.canRead()) {
                Log.d(TAG, "Detected $pathType: $path")
                detectedPaths[pathType] = path
                return path
            }
        }
        
        Log.w(TAG, "No valid path found for $pathType")
        detectedPaths[pathType] = null
        return null
    }

    // Public API for each hardware component
    
    fun getFpsPath(): String? = detectPath("fps", FPS_PATHS)
    
    fun getBatteryTempPath(): String? = detectPath("battery_temp", BATTERY_TEMP_PATHS)

    /**
     * Clear cache and re-detect all paths (useful for debugging)
     */
    fun resetDetection() {
        detectedPaths.clear()
        Log.d(TAG, "Sysfs detection cache cleared")
    }
    
    /**
     * Check if a specific hardware component is supported
     */
    fun isComponentSupported(component: String): Boolean {
        return when (component) {
            "fps" -> getFpsPath() != null
            "battery_temp" -> getBatteryTempPath() != null
            else -> false
        }
    }
}