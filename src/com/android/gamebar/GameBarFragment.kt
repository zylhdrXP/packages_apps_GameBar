/*
 * SPDX-FileCopyrightText: 2025 kenway214
 * SPDX-License-Identifier: Apache-2.0
 */


package com.android.gamebar

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.preference.ListPreference
import androidx.preference.Preference
import com.android.gamebar.utils.PartsCustomSeekBarPreference
import androidx.preference.SwitchPreferenceCompat
import com.android.settingslib.widget.MainSwitchPreference
import com.android.gamebar.R
import com.android.gamebar.colorpicker.ColorPreferenceCompat

import java.util.Locale

import com.android.settingslib.widget.SettingsBasePreferenceFragment

class GameBarFragment : SettingsBasePreferenceFragment() {

    private var gameBar: GameBar? = null
    private var masterSwitch: MainSwitchPreference? = null
    private var autoEnableSwitch: SwitchPreferenceCompat? = null
    private var fpsSwitch: SwitchPreferenceCompat? = null
    private var frameTimeSwitch: SwitchPreferenceCompat? = null
    private var batteryTempSwitch: SwitchPreferenceCompat? = null
    private var cpuUsageSwitch: SwitchPreferenceCompat? = null
    private var cpuClockSwitch: SwitchPreferenceCompat? = null
    private var cpuTempSwitch: SwitchPreferenceCompat? = null
    private var ramSwitch: SwitchPreferenceCompat? = null
    private var gpuUsageSwitch: SwitchPreferenceCompat? = null
    private var gpuClockSwitch: SwitchPreferenceCompat? = null
    private var gpuTempSwitch: SwitchPreferenceCompat? = null
    private var doubleTapCapturePref: SwitchPreferenceCompat? = null
    private var singleTapTogglePref: SwitchPreferenceCompat? = null
    private var longPressEnablePref: SwitchPreferenceCompat? = null
    private var longPressTimeoutPref: ListPreference? = null
    private var textSizePref: PartsCustomSeekBarPreference? = null
    private var bgAlphaPref: PartsCustomSeekBarPreference? = null
    private var cornerRadiusPref: PartsCustomSeekBarPreference? = null
    private var paddingPref: PartsCustomSeekBarPreference? = null
    private var itemSpacingPref: PartsCustomSeekBarPreference? = null
    private var updateIntervalPref: ListPreference? = null
    private var textColorPref: ListPreference? = null
    private var titleColorPref: ColorPreferenceCompat? = null
    private var valueColorPref: ColorPreferenceCompat? = null
    private var backgroundColorPref: ColorPreferenceCompat? = null
    private var positionPref: ListPreference? = null
    private var splitModePref: ListPreference? = null
    private var overlayFormatPref: ListPreference? = null
    private var ramSpeedSwitch: SwitchPreferenceCompat? = null
    private var ramTempSwitch: SwitchPreferenceCompat? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.game_bar_preferences, rootKey)

        gameBar = GameBar.getInstance(requireContext())

        // Initialize all preferences
        masterSwitch = findPreference("game_bar_enable")
        autoEnableSwitch = findPreference("game_bar_auto_enable")
        fpsSwitch = findPreference("game_bar_fps_enable")
        frameTimeSwitch = findPreference("game_bar_frame_time_enable")
        batteryTempSwitch = findPreference("game_bar_temp_enable")
        cpuUsageSwitch = findPreference("game_bar_cpu_usage_enable")
        cpuClockSwitch = findPreference("game_bar_cpu_clock_enable")
        cpuTempSwitch = findPreference("game_bar_cpu_temp_enable")
        ramSwitch = findPreference("game_bar_ram_enable")
        gpuUsageSwitch = findPreference("game_bar_gpu_usage_enable")
        gpuClockSwitch = findPreference("game_bar_gpu_clock_enable")
        gpuTempSwitch = findPreference("game_bar_gpu_temp_enable")
        ramSpeedSwitch = findPreference("game_bar_ram_speed_enable")
        ramTempSwitch = findPreference("game_bar_ram_temp_enable")

        doubleTapCapturePref = findPreference("game_bar_doubletap_capture")
        singleTapTogglePref = findPreference("game_bar_single_tap_toggle")
        longPressEnablePref = findPreference("game_bar_longpress_enable")
        longPressTimeoutPref = findPreference("game_bar_longpress_timeout")

        textSizePref = findPreference("game_bar_text_size")
        bgAlphaPref = findPreference("game_bar_background_alpha")
        cornerRadiusPref = findPreference("game_bar_corner_radius")
        paddingPref = findPreference("game_bar_padding")
        itemSpacingPref = findPreference("game_bar_item_spacing")

        updateIntervalPref = findPreference("game_bar_update_interval")
        textColorPref = findPreference("game_bar_text_color")
        titleColorPref = findPreference("game_bar_title_color")
        valueColorPref = findPreference("game_bar_value_color")
        backgroundColorPref = findPreference("game_bar_background_color")
        positionPref = findPreference("game_bar_position")
        splitModePref = findPreference("game_bar_split_mode")
        overlayFormatPref = findPreference("game_bar_format")

        setupPerAppConfig()
        setupMasterSwitchListener()
        setupAutoEnableSwitchListener()
        setupFeatureSwitchListeners()
        setupGesturePrefListeners()
        setupStylePrefListeners()
        setupExpandableCategories()
    }
    
    private fun setupExpandableCategories() {
        // Get all expandable categories by their keys
        val overlayFeaturesCategory: ExpandablePreferenceCategory? = findPreference("category_overlay_features")
        val fpsMethodCategory: ExpandablePreferenceCategory? = findPreference("category_fps_method")
        val customizationCategory: ExpandablePreferenceCategory? = findPreference("category_customization")
        val splitConfigCategory: ExpandablePreferenceCategory? = findPreference("category_split_config")
        val gestureControlsCategory: ExpandablePreferenceCategory? = findPreference("category_gesture_controls")
        val perAppCategory: ExpandablePreferenceCategory? = findPreference("category_per_app")
        
        // Categories are collapsed by default
        // You can expand some by default if needed, e.g.:
        // overlayFeaturesCategory?.setExpanded(true)
    }

    private fun setupPerAppConfig() {
        val perAppConfigPref: Preference? = findPreference("game_bar_per_app_config")
        perAppConfigPref?.setOnPreferenceClickListener {
            startActivity(Intent(requireContext(), GameBarPerAppConfigActivity::class.java))
            true
        }
        
        val fontSelectorPref: Preference? = findPreference("game_bar_font_selector")
        fontSelectorPref?.setOnPreferenceClickListener {
            startActivity(Intent(requireContext(), GameBarFontSelectorActivity::class.java))
            true
        }
    }

    private fun setupMasterSwitchListener() {
        masterSwitch?.setOnPreferenceChangeListener { _, newValue ->
            val enabled = newValue as Boolean
            if (enabled) {
                if (Settings.canDrawOverlays(requireContext())) {
                    android.util.Log.d("GameBarFragment", "Enabling GameBar from settings")
                    
                    // Ensure we have a fresh GameBar instance
                    if (!GameBar.isInstanceCreated()) {
                        gameBar = GameBar.getInstance(requireContext())
                    }
                    
                    // Clean up any existing instance before creating new one
                    gameBar?.hide()
                    gameBar?.applyPreferences()
                    gameBar?.show()
                    requireContext().startService(Intent(requireContext(), GameBarMonitorService::class.java))
                    true
                } else {
                    Toast.makeText(requireContext(), R.string.overlay_permission_required, Toast.LENGTH_SHORT).show()
                    false
                }
            } else {
                android.util.Log.d("GameBarFragment", "Disabling GameBar from settings")
                
                gameBar?.hide()
                
                // Destroy singleton to ensure clean state
                GameBar.destroyInstance()
                
                // Only stop monitor service if auto-enable is also disabled
                if (autoEnableSwitch?.isChecked != true) {
                    requireContext().stopService(Intent(requireContext(), GameBarMonitorService::class.java))
                }
                true
            }
        }
    }

    private fun setupAutoEnableSwitchListener() {
        autoEnableSwitch?.setOnPreferenceChangeListener { _, newValue ->
            val autoEnabled = newValue as Boolean
            if (autoEnabled) {
                requireContext().startService(Intent(requireContext(), GameBarMonitorService::class.java))
            } else {
                if (masterSwitch?.isChecked != true) {
                    requireContext().stopService(Intent(requireContext(), GameBarMonitorService::class.java))
                }
            }
            true
        }
    }

    private fun setupFeatureSwitchListeners() {
        fpsSwitch?.setOnPreferenceChangeListener { _, newValue ->
            gameBar?.setShowFps(newValue as Boolean)
            true
        }
        frameTimeSwitch?.setOnPreferenceChangeListener { _, newValue ->
            gameBar?.setShowFrameTime(newValue as Boolean)
            true
        }
        batteryTempSwitch?.setOnPreferenceChangeListener { _, newValue ->
            gameBar?.setShowBatteryTemp(newValue as Boolean)
            true
        }
        cpuUsageSwitch?.setOnPreferenceChangeListener { _, newValue ->
            gameBar?.setShowCpuUsage(newValue as Boolean)
            true
        }
        cpuClockSwitch?.setOnPreferenceChangeListener { _, newValue ->
            gameBar?.setShowCpuClock(newValue as Boolean)
            true
        }
        cpuTempSwitch?.setOnPreferenceChangeListener { _, newValue ->
            gameBar?.setShowCpuTemp(newValue as Boolean)
            true
        }
        ramSwitch?.setOnPreferenceChangeListener { _, newValue ->
            gameBar?.setShowRam(newValue as Boolean)
            true
        }
        gpuUsageSwitch?.setOnPreferenceChangeListener { _, newValue ->
            gameBar?.setShowGpuUsage(newValue as Boolean)
            true
        }
        gpuClockSwitch?.setOnPreferenceChangeListener { _, newValue ->
            gameBar?.setShowGpuClock(newValue as Boolean)
            true
        }
        gpuTempSwitch?.setOnPreferenceChangeListener { _, newValue ->
            gameBar?.setShowGpuTemp(newValue as Boolean)
            true
        }
        ramSpeedSwitch?.setOnPreferenceChangeListener { _, newValue ->
            gameBar?.setShowRamSpeed(newValue as Boolean)
            true
        }
        ramTempSwitch?.setOnPreferenceChangeListener { _, newValue ->
            gameBar?.setShowRamTemp(newValue as Boolean)
            true
        }
    }

    private fun setupGesturePrefListeners() {
        doubleTapCapturePref?.setOnPreferenceChangeListener { _, newValue ->
            gameBar?.setDoubleTapCaptureEnabled(newValue as Boolean)
            true
        }
        singleTapTogglePref?.setOnPreferenceChangeListener { _, newValue ->
            gameBar?.setSingleTapToggleEnabled(newValue as Boolean)
            true
        }
        longPressEnablePref?.setOnPreferenceChangeListener { _, newValue ->
            gameBar?.setLongPressEnabled(newValue as Boolean)
            true
        }
        longPressTimeoutPref?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue is String) {
                val ms = newValue.toLong()
                gameBar?.setLongPressThresholdMs(ms)
            }
            true
        }
    }

    private fun setupStylePrefListeners() {
        textSizePref?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue is Int) {
                gameBar?.updateTextSize(newValue)
            }
            true
        }
        bgAlphaPref?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue is Int) {
                gameBar?.updateBackgroundAlpha(newValue)
            }
            true
        }
        cornerRadiusPref?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue is Int) {
                gameBar?.updateCornerRadius(newValue)
            }
            true
        }
        paddingPref?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue is Int) {
                gameBar?.updatePadding(newValue)
            }
            true
        }
        itemSpacingPref?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue is Int) {
                gameBar?.updateItemSpacing(newValue)
            }
            true
        }
        updateIntervalPref?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue is String) {
                gameBar?.updateUpdateInterval(newValue)
            }
            true
        }
        textColorPref?.setOnPreferenceChangeListener { _, _ ->
            true
        }
        titleColorPref?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue is Int) {
                val hexColor = String.format("#%06X", 0xFFFFFF and newValue)
                gameBar?.updateTitleColor(hexColor)
            }
            true
        }
        valueColorPref?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue is Int) {
                val hexColor = String.format("#%06X", 0xFFFFFF and newValue)
                gameBar?.updateValueColor(hexColor)
            }
            true
        }
        backgroundColorPref?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue is Int) {
                gameBar?.updateBackgroundColor(newValue)
            }
            true
        }
        positionPref?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue is String) {
                gameBar?.updatePosition(newValue)
            }
            true
        }
        splitModePref?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue is String) {
                gameBar?.updateSplitMode(newValue)
            }
            true
        }
        overlayFormatPref?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue is String) {
                gameBar?.updateOverlayFormat(newValue)
            }
            true
        }
    }

    override fun onResume() {
        super.onResume()
        if (!hasUsageStatsPermission(requireContext())) {
            requestUsageStatsPermission()
        }
        requireContext().let { context ->
            if ((masterSwitch?.isChecked == true) || (autoEnableSwitch?.isChecked == true)) {
                context.startService(Intent(context, GameBarMonitorService::class.java))
            } else {
                context.stopService(Intent(context, GameBarMonitorService::class.java))
            }
        }
    }

    private fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? android.app.AppOpsManager
            ?: return false
        val mode = appOps.checkOpNoThrow(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }

    private fun requestUsageStatsPermission() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        startActivity(intent)
    }
}