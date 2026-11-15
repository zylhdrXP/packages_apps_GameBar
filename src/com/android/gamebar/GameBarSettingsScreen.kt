/*
 * SPDX-FileCopyrightText: 2026 kenway214
 * SPDX-License-Identifier: Apache-2.0
 */

@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package com.android.gamebar

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.preference.PreferenceManager
import com.android.gamebar.colorpicker.ColorPickerDialog
import com.android.gamebar.colorpicker.ColorPickerDialogListener
import com.android.gamebar.colorpicker.ColorShape
import com.android.gamebar.ui.components.ConnectedHomeMenuGrid
import com.android.gamebar.ui.components.FloatingNavToolbar
import com.android.gamebar.ui.components.GameBarHeroLottieBanner
import com.android.gamebar.ui.components.GameBarPageHeading
import com.android.gamebar.ui.components.GameBarTopAppBar
import androidx.compose.ui.graphics.Color
import com.android.gamebar.ui.components.HomeMenuCard
import com.android.gamebar.ui.components.ModernConfirmDialog
import com.android.gamebar.ui.components.ModernSettingActionRow
import com.android.gamebar.ui.components.ModernSettingSelector
import com.android.gamebar.ui.components.ModernSettingSlider
import com.android.gamebar.ui.components.ModernSettingSwitch
import com.android.gamebar.ui.components.ModernSettingsCard
import com.android.gamebar.ui.components.MorphShape
import com.android.gamebar.ui.components.SelectOption
import com.android.gamebar.ui.components.connectedItemShape
import com.android.gamebar.ui.theme.UiStyleController
import com.android.gamebar.utils.rememberHapticFeedback
import androidx.graphics.shapes.Morph
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun GameBarSettingsScreen(
    onOpenPerAppConfig: () -> Unit,
    onOpenFpsRecord: () -> Unit,
    onOpenFontSelector: () -> Unit,
    onOpenPresetManager: () -> Unit,
    onToggleLauncherIcon: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    UiStyleController.ensureInitialized(context)
    val amoledBlackEnabled by UiStyleController.amoledBlackEnabled.collectAsState()
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    val presetManager = remember { PresetManager.getInstance(context) }
    val haptic = rememberHapticFeedback()
    val coroutineScope = rememberCoroutineScope()

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 5 })
    val density = LocalDensity.current
    var toolbarHeightPx by remember { mutableIntStateOf(0) }
    val toolbarBottomPadding = with(density) { toolbarHeightPx.toDp() } + 32.dp

    val homeListState = rememberLazyListState()
    val featuresListState = rememberLazyListState()
    val customizationListState = rememberLazyListState()
    val recordsListState = rememberLazyListState()
    val presetsListState = rememberLazyListState()

    val showTitle by remember {
        derivedStateOf {
            when (pagerState.currentPage) {
                0 -> homeListState.firstVisibleItemIndex > 0
                1 -> featuresListState.firstVisibleItemIndex > 0
                2 -> customizationListState.firstVisibleItemIndex > 0
                3 -> recordsListState.firstVisibleItemIndex > 0
                4 -> presetsListState.firstVisibleItemIndex > 0
                else -> false
            }
        }
    }

    val currentRoute = when (pagerState.targetPage) {
        0 -> "home"
        1 -> "features"
        2 -> "customization"
        3 -> "records"
        4 -> "presets"
        else -> "home"
    }

    val currentTabTitle = when (pagerState.currentPage) {
        0 -> stringResource(R.string.game_bar_title)
        1 -> stringResource(R.string.tab_features)
        2 -> stringResource(R.string.tab_customization)
        3 -> stringResource(R.string.tab_records)
        4 -> stringResource(R.string.tab_presets)
        else -> stringResource(R.string.game_bar_title)
    }

    fun putBoolean(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()
    fun putInt(key: String, value: Int) = prefs.edit().putInt(key, value).apply()
    fun putString(key: String, value: String) = prefs.edit().putString(key, value).apply()
    fun readGestureAction(key: String, defaultValue: String): String {
        val raw = prefs.getString(key, defaultValue) ?: defaultValue
        return if (raw == "capture_logs") {
            putString(key, "toggle_fps_logging")
            "toggle_fps_logging"
        } else {
            raw
        }
    }

    fun applyPrefs(rebuild: Boolean = false) {
        if (!GameBar.isInstanceCreated()) return
        val gameBar = GameBar.getInstance(context)
        gameBar.applyPreferences()
        if (rebuild && GameBar.isShowing()) {
            gameBar.hide()
            gameBar.show()
        }
    }

    var showSavePresetDialog by remember { mutableStateOf(false) }
    var savePresetName by remember { mutableStateOf("") }
    var showResetDefaultsDialog by remember { mutableStateOf(false) }

    var gameBarEnabled by remember { mutableStateOf(prefs.getBoolean("game_bar_enable", false)) }
    var autoEnable by remember { mutableStateOf(prefs.getBoolean("game_bar_auto_enable", false)) }
    var fpsRecordControlEnabled by remember {
        mutableStateOf(prefs.getBoolean("game_bar_fps_record_control_enabled", false))
    }
    var fontName by remember {
        mutableStateOf(prefs.getString("game_bar_font_name", "System Default") ?: "System Default")
    }

    var showLauncherIcon by remember {
        mutableStateOf(
            context.getSharedPreferences(GameBarSettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(GameBarSettingsActivity.KEY_SHOW_LAUNCHER_ICON, false)
        )
    }

    var fpsEnable by remember { mutableStateOf(prefs.getBoolean("game_bar_fps_enable", true)) }
    var frameTimeEnable by remember { mutableStateOf(prefs.getBoolean("game_bar_frame_time_enable", false)) }
    var tempEnable by remember { mutableStateOf(prefs.getBoolean("game_bar_temp_enable", false)) }
    var cpuUsageEnable by remember { mutableStateOf(prefs.getBoolean("game_bar_cpu_usage_enable", true)) }
    var cpuClockEnable by remember { mutableStateOf(prefs.getBoolean("game_bar_cpu_clock_enable", false)) }
    var cpuTempEnable by remember { mutableStateOf(prefs.getBoolean("game_bar_cpu_temp_enable", false)) }
    var ramEnable by remember { mutableStateOf(prefs.getBoolean("game_bar_ram_enable", false)) }
    var ramSpeedEnable by remember { mutableStateOf(prefs.getBoolean("game_bar_ram_speed_enable", false)) }
    var ramTempEnable by remember { mutableStateOf(prefs.getBoolean("game_bar_ram_temp_enable", false)) }
    var gpuUsageEnable by remember { mutableStateOf(prefs.getBoolean("game_bar_gpu_usage_enable", true)) }
    var gpuClockEnable by remember { mutableStateOf(prefs.getBoolean("game_bar_gpu_clock_enable", false)) }
    var gpuTempEnable by remember { mutableStateOf(prefs.getBoolean("game_bar_gpu_temp_enable", false)) }

    var fpsDisplayMode by remember { mutableStateOf(prefs.getString("game_bar_fps_display_mode", "basic") ?: "basic") }
    var fpsMethod by remember { mutableStateOf(prefs.getString("game_bar_fps_method", "new") ?: "new") }

    var textSize by remember { mutableStateOf(prefs.getInt("game_bar_text_size", 15)) }
    var bgAlpha by remember { mutableStateOf(prefs.getInt("game_bar_background_alpha", 255)) }
    var bgColor by remember { mutableStateOf(prefs.getInt("game_bar_background_color", 0xFF0D1117.toInt())) }
    var cornerRadius by remember { mutableStateOf(prefs.getInt("game_bar_corner_radius", 30)) }
    var paddingValue by remember { mutableStateOf(prefs.getInt("game_bar_padding", 4)) }
    var itemSpacing by remember { mutableStateOf(prefs.getInt("game_bar_item_spacing", 8)) }
    var overlayWidthDp by remember { mutableStateOf(prefs.getInt("game_bar_overlay_width_dp", 280)) }
    var updateInterval by remember { mutableStateOf(prefs.getString("game_bar_update_interval", "1000") ?: "1000") }
    var titleColor by remember { mutableStateOf(prefs.getInt("game_bar_title_color", 0xFF9FCEDE.toInt())) }
    var valueColor by remember { mutableStateOf(prefs.getInt("game_bar_value_color", 0xFFFFB347.toInt())) }
    var overlayFormat by remember { mutableStateOf(prefs.getString("game_bar_format", "full") ?: "full") }
    var splitMode by remember { mutableStateOf(prefs.getString("game_bar_split_mode", "side_by_side") ?: "side_by_side") }

    var singleTapEnable by remember { mutableStateOf(prefs.getBoolean("game_bar_single_tap_enable", true)) }
    var singleTapFunction by remember { mutableStateOf(readGestureAction("game_bar_single_tap_function", "toggle_format")) }
    var doubleTapEnable by remember { mutableStateOf(prefs.getBoolean("game_bar_doubletap_enable", true)) }
    var doubleTapFunction by remember { mutableStateOf(readGestureAction("game_bar_doubletap_function", "toggle_fps_logging")) }
    var longPressEnable by remember { mutableStateOf(prefs.getBoolean("game_bar_longpress_enable", true)) }
    var longPressFunction by remember { mutableStateOf(readGestureAction("game_bar_longpress_function", "load_preset")) }
    var longPressTimeout by remember { mutableStateOf(prefs.getString("game_bar_longpress_timeout", "500") ?: "500") }

    fun reloadAllStates() {
        gameBarEnabled = prefs.getBoolean("game_bar_enable", false)
        autoEnable = prefs.getBoolean("game_bar_auto_enable", false)
        fpsRecordControlEnabled = prefs.getBoolean("game_bar_fps_record_control_enabled", false)
        fontName = prefs.getString("game_bar_font_name", "System Default") ?: "System Default"

        fpsEnable = prefs.getBoolean("game_bar_fps_enable", true)
        frameTimeEnable = prefs.getBoolean("game_bar_frame_time_enable", false)
        tempEnable = prefs.getBoolean("game_bar_temp_enable", false)
        cpuUsageEnable = prefs.getBoolean("game_bar_cpu_usage_enable", true)
        cpuClockEnable = prefs.getBoolean("game_bar_cpu_clock_enable", false)
        cpuTempEnable = prefs.getBoolean("game_bar_cpu_temp_enable", false)
        ramEnable = prefs.getBoolean("game_bar_ram_enable", false)
        ramSpeedEnable = prefs.getBoolean("game_bar_ram_speed_enable", false)
        ramTempEnable = prefs.getBoolean("game_bar_ram_temp_enable", false)
        gpuUsageEnable = prefs.getBoolean("game_bar_gpu_usage_enable", true)
        gpuClockEnable = prefs.getBoolean("game_bar_gpu_clock_enable", false)
        gpuTempEnable = prefs.getBoolean("game_bar_gpu_temp_enable", false)

        fpsDisplayMode = prefs.getString("game_bar_fps_display_mode", "basic") ?: "basic"
        fpsMethod = prefs.getString("game_bar_fps_method", "new") ?: "new"

        textSize = prefs.getInt("game_bar_text_size", 15)
        bgAlpha = prefs.getInt("game_bar_background_alpha", 255)
        bgColor = prefs.getInt("game_bar_background_color", 0xFF0D1117.toInt())
        cornerRadius = prefs.getInt("game_bar_corner_radius", 30)
        paddingValue = prefs.getInt("game_bar_padding", 4)
        itemSpacing = prefs.getInt("game_bar_item_spacing", 8)
        overlayWidthDp = prefs.getInt("game_bar_overlay_width_dp", 280)
        updateInterval = prefs.getString("game_bar_update_interval", "1000") ?: "1000"
        titleColor = prefs.getInt("game_bar_title_color", 0xFF9FCEDE.toInt())
        valueColor = prefs.getInt("game_bar_value_color", 0xFFFFB347.toInt())
        overlayFormat = prefs.getString("game_bar_format", "full") ?: "full"
        splitMode = prefs.getString("game_bar_split_mode", "side_by_side") ?: "side_by_side"

        singleTapEnable = prefs.getBoolean("game_bar_single_tap_enable", true)
        singleTapFunction = readGestureAction("game_bar_single_tap_function", "toggle_format")
        doubleTapEnable = prefs.getBoolean("game_bar_doubletap_enable", true)
        doubleTapFunction = readGestureAction("game_bar_doubletap_function", "toggle_fps_logging")
        longPressEnable = prefs.getBoolean("game_bar_longpress_enable", true)
        longPressFunction = readGestureAction("game_bar_longpress_function", "load_preset")
        longPressTimeout = prefs.getString("game_bar_longpress_timeout", "500") ?: "500"
        showLauncherIcon = context.getSharedPreferences(GameBarSettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(GameBarSettingsActivity.KEY_SHOW_LAUNCHER_ICON, false)
    }

    DisposableEffect(prefs) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            reloadAllStates()
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        reloadAllStates()
    }

    val importPresetLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val imported = runCatching {
            val tmp = File(context.cacheDir, "gamebar_import_${System.currentTimeMillis()}.json")
            context.contentResolver.openInputStream(uri)?.use { input ->
                tmp.outputStream().use { output -> input.copyTo(output) }
            } ?: return@runCatching false
            val result = presetManager.importPreset(tmp)
            tmp.delete()
            result
        }.getOrDefault(false)
        if (imported) {
            reloadAllStates()
            applyPrefs(rebuild = true)
        }
        Toast.makeText(
            context,
            if (imported) context.getString(R.string.toast_preset_imported) else context.getString(R.string.toast_preset_import_failed),
            Toast.LENGTH_SHORT
        ).show()
    }

    val fpsDisplayOptions = listOf(
        SelectOption("basic", "Basic (Current)"),
        SelectOption("advanced", "Advanced (1% Low)"),
    )
    val fpsMethodOptions = listOf(
        SelectOption("new", "New API"),
        SelectOption("legacy", "Legacy Sysfs"),
    )
    val intervalOptions = listOf(
        SelectOption("500", "500ms"),
        SelectOption("1000", "1s"),
        SelectOption("2000", "2s"),
        SelectOption("5000", "5s"),
    )
    val overlayFormatOptions = listOf(
        SelectOption("full", "Full"),
        SelectOption("minimal", "Minimal"),
    )
    val splitModeOptions = listOf(
        SelectOption("side_by_side", "Side-by-Side"),
        SelectOption("stacked", "Stacked"),
    )
    val gestureOptions = listOf(
        SelectOption("no_action", "No Action"),
        SelectOption("toggle_fps_logging", "Start/Stop FPS Logging"),
        SelectOption("adjust_length", "Length Adjustment Mode"),
        SelectOption("toggle_format", "Toggle Full/Minimal Format"),
        SelectOption("open_settings", "Open GameBar Settings"),
        SelectOption("take_screenshot", "Take Screenshot"),
        SelectOption("screen_record", "Start/Stop Screen Record"),
        SelectOption("load_preset", "Load Preset (Quick)"),
    )
    val longPressOptions = listOf(
        SelectOption("500", "500ms"),
        SelectOption("1000", "1.0s"),
        SelectOption("2000", "2.0s"),
        SelectOption("3000", "3.0s"),
    )

    fun colorLabel(color: Int): String = String.format("#%08X", color)
    fun openColorPicker(dialogId: Int, currentColor: Int, showAlpha: Boolean, onColor: (Int) -> Unit) {
        val activity = context as? FragmentActivity ?: return
        val picker = ColorPickerDialog.newBuilder()
            .setDialogId(dialogId)
            .setDialogType(ColorPickerDialog.TYPE_CUSTOM)
            .setDialogTitle(R.string.gamebar_default_title)
            .setColorShape(ColorShape.CIRCLE)
            .setAllowPresets(true)
            .setAllowCustom(true)
            .setShowAlphaSlider(showAlpha)
            .setShowColorShades(true)
            .setColor(currentColor)
            .create()
        picker.setColorPickerDialogListener(object : ColorPickerDialogListener {
            override fun onColorSelected(dialogId: Int, color: Int) {
                onColor(color)
            }
            override fun onDialogDismissed(dialogId: Int) {}
        })
        picker.show(activity.supportFragmentManager, "gamebar-color-$dialogId")
    }

    Scaffold(
        topBar = {
            GameBarTopAppBar(
                title = currentTabTitle,
                showTitle = showTitle,
                actions = {
                    IconButton(onClick = {
                        val url = context.getString(R.string.game_bar_user_guide_url)
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = stringResource(R.string.game_bar_user_guide),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(onClick = { showResetDefaultsDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = stringResource(R.string.dialog_reset_all_title),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) { scaffoldPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    when (page) {
                        0 -> GameBarHomeScreen(
                            listState = homeListState,
                            bottomPadding = toolbarBottomPadding,
                            gameBarEnabled = gameBarEnabled,
                            onEnabledChange = { checked ->
                                if (checked && !Settings.canDrawOverlays(context)) {
                                    context.startActivity(
                                        Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                    gameBarEnabled = false
                                    putBoolean("game_bar_enable", false)
                                    return@GameBarHomeScreen
                                }
                                gameBarEnabled = checked
                                putBoolean("game_bar_enable", checked)
                                if (checked) {
                                    val gameBar = GameBar.getInstance(context)
                                    gameBar.hide()
                                    gameBar.applyPreferences()
                                    gameBar.show()
                                    context.startService(Intent(context, GameBarMonitorService::class.java))
                                } else {
                                    if (GameBar.isInstanceCreated()) {
                                        GameBar.getInstance(context).hide()
                                        GameBar.destroyInstance()
                                    }
                                    if (!autoEnable) context.stopService(Intent(context, GameBarMonitorService::class.java))
                                }
                            },
                            onNavigateToPage = { targetPage ->
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(targetPage)
                                }
                            },
                        )

                        1 -> GameBarFeaturesScreen(
                            listState = featuresListState,
                            bottomPadding = toolbarBottomPadding,
                            fpsEnable = fpsEnable,
                            onFpsEnableChange = { fpsEnable = it; putBoolean("game_bar_fps_enable", it); applyPrefs() },
                            fpsDisplayMode = fpsDisplayMode,
                            fpsDisplayOptions = fpsDisplayOptions,
                            onFpsDisplayModeChange = { fpsDisplayMode = it; putString("game_bar_fps_display_mode", it); applyPrefs(rebuild = true) },
                            fpsMethod = fpsMethod,
                            fpsMethodOptions = fpsMethodOptions,
                            onFpsMethodChange = { fpsMethod = it; putString("game_bar_fps_method", it); applyPrefs(rebuild = true) },
                            frameTimeEnable = frameTimeEnable,
                            onFrameTimeEnableChange = { frameTimeEnable = it; putBoolean("game_bar_frame_time_enable", it); applyPrefs() },
                            tempEnable = tempEnable,
                            onTempEnableChange = { tempEnable = it; putBoolean("game_bar_temp_enable", it); applyPrefs() },
                            cpuUsageEnable = cpuUsageEnable,
                            onCpuUsageEnableChange = { cpuUsageEnable = it; putBoolean("game_bar_cpu_usage_enable", it); applyPrefs() },
                            cpuClockEnable = cpuClockEnable,
                            onCpuClockEnableChange = { cpuClockEnable = it; putBoolean("game_bar_cpu_clock_enable", it); applyPrefs() },
                            cpuTempEnable = cpuTempEnable,
                            onCpuTempEnableChange = { cpuTempEnable = it; putBoolean("game_bar_cpu_temp_enable", it); applyPrefs() },
                            ramEnable = ramEnable,
                            onRamEnableChange = { ramEnable = it; putBoolean("game_bar_ram_enable", it); applyPrefs() },
                            ramSpeedEnable = ramSpeedEnable,
                            onRamSpeedEnableChange = { ramSpeedEnable = it; putBoolean("game_bar_ram_speed_enable", it); applyPrefs() },
                            ramTempEnable = ramTempEnable,
                            onRamTempEnableChange = { ramTempEnable = it; putBoolean("game_bar_ram_temp_enable", it); applyPrefs() },
                            gpuUsageEnable = gpuUsageEnable,
                            onGpuUsageEnableChange = { gpuUsageEnable = it; putBoolean("game_bar_gpu_usage_enable", it); applyPrefs() },
                            gpuClockEnable = gpuClockEnable,
                            onGpuClockEnableChange = { gpuClockEnable = it; putBoolean("game_bar_gpu_clock_enable", it); applyPrefs() },
                            gpuTempEnable = gpuTempEnable,
                            onGpuTempEnableChange = { gpuTempEnable = it; putBoolean("game_bar_gpu_temp_enable", it); applyPrefs() },
                        )

                        2 -> GameBarCustomizationScreen(
                            listState = customizationListState,
                            bottomPadding = toolbarBottomPadding,
                            textSize = textSize,
                            onTextSizeChange = { textSize = it.toInt(); putInt("game_bar_text_size", it.toInt()); applyPrefs() },
                            bgAlpha = bgAlpha,
                            onBgAlphaChange = { bgAlpha = it.toInt(); putInt("game_bar_background_alpha", it.toInt()); applyPrefs() },
                            bgColor = bgColor,
                            onBgColorClick = {
                                openColorPicker(1001, bgColor, true) { picked ->
                                    bgColor = picked
                                    putInt("game_bar_background_color", picked)
                                    applyPrefs()
                                }
                            },
                            cornerRadius = cornerRadius,
                            onCornerRadiusChange = { cornerRadius = it.toInt(); putInt("game_bar_corner_radius", it.toInt()); applyPrefs() },
                            paddingValue = paddingValue,
                            onPaddingChange = { paddingValue = it.toInt(); putInt("game_bar_padding", it.toInt()); applyPrefs() },
                            itemSpacing = itemSpacing,
                            onItemSpacingChange = { itemSpacing = it.toInt(); putInt("game_bar_item_spacing", it.toInt()); applyPrefs() },
                            overlayWidthDp = overlayWidthDp,
                            onOverlayWidthChange = { overlayWidthDp = it.toInt(); putInt("game_bar_overlay_width_dp", it.toInt()); applyPrefs() },
                            updateInterval = updateInterval,
                            intervalOptions = intervalOptions,
                            onUpdateIntervalChange = { updateInterval = it; putString("game_bar_update_interval", it); applyPrefs() },
                            titleColor = titleColor,
                            onTitleColorClick = {
                                openColorPicker(1002, titleColor, false) { picked ->
                                    titleColor = picked
                                    putInt("game_bar_title_color", picked)
                                    applyPrefs()
                                }
                            },
                            valueColor = valueColor,
                            onValueColorClick = {
                                openColorPicker(1003, valueColor, false) { picked ->
                                    valueColor = picked
                                    putInt("game_bar_value_color", picked)
                                    applyPrefs()
                                }
                            },
                            fontName = fontName,
                            onOpenFontSelector = onOpenFontSelector,
                            overlayFormat = overlayFormat,
                            overlayFormatOptions = overlayFormatOptions,
                            onOverlayFormatChange = { overlayFormat = it; putString("game_bar_format", it); applyPrefs() },
                            splitMode = splitMode,
                            splitModeOptions = splitModeOptions,
                            onSplitModeChange = { splitMode = it; putString("game_bar_split_mode", it); applyPrefs() },
                            singleTapEnable = singleTapEnable,
                            onSingleTapEnableChange = { singleTapEnable = it; putBoolean("game_bar_single_tap_enable", it); applyPrefs() },
                            singleTapFunction = singleTapFunction,
                            gestureOptions = gestureOptions,
                            onSingleTapFunctionChange = { singleTapFunction = it; putString("game_bar_single_tap_function", it); applyPrefs() },
                            doubleTapEnable = doubleTapEnable,
                            onDoubleTapEnableChange = { doubleTapEnable = it; putBoolean("game_bar_doubletap_enable", it); applyPrefs() },
                            doubleTapFunction = doubleTapFunction,
                            onDoubleTapFunctionChange = { doubleTapFunction = it; putString("game_bar_doubletap_function", it); applyPrefs() },
                            longPressEnable = longPressEnable,
                            onLongPressEnableChange = { longPressEnable = it; putBoolean("game_bar_longpress_enable", it); applyPrefs() },
                            longPressFunction = longPressFunction,
                            onLongPressFunctionChange = { longPressFunction = it; putString("game_bar_longpress_function", it); applyPrefs() },
                            longPressTimeout = longPressTimeout,
                            longPressOptions = longPressOptions,
                            onLongPressTimeoutChange = { longPressTimeout = it; putString("game_bar_longpress_timeout", it); applyPrefs() },
                        )

                        3 -> GameBarRecordsScreen(
                            listState = recordsListState,
                            bottomPadding = toolbarBottomPadding,
                            fpsRecordControlEnabled = fpsRecordControlEnabled,
                            onFpsRecordControlEnabledChange = { enabled ->
                                fpsRecordControlEnabled = enabled
                                putBoolean("game_bar_fps_record_control_enabled", enabled)
                                val svcIntent = Intent(context, FpsRecordControlOverlayService::class.java).apply {
                                    action = FpsRecordControlOverlayService.ACTION_SET_ENABLED
                                    putExtra(FpsRecordControlOverlayService.EXTRA_ENABLED, enabled)
                                }
                                context.startService(svcIntent)
                            },
                            onOpenFpsRecord = onOpenFpsRecord,
                        )

                        4 -> GameBarPresetsScreen(
                            listState = presetsListState,
                            bottomPadding = toolbarBottomPadding,
                            amoledBlackEnabled = amoledBlackEnabled,
                            onAmoledBlackChange = { UiStyleController.setAmoledBlackEnabled(context, it) },
                            autoEnable = autoEnable,
                            onAutoEnableChange = {
                                autoEnable = it
                                putBoolean("game_bar_auto_enable", it)
                                if (it || gameBarEnabled) {
                                    context.startService(Intent(context, GameBarMonitorService::class.java))
                                } else {
                                    context.stopService(Intent(context, GameBarMonitorService::class.java))
                                }
                            },
                            onOpenPerAppConfig = onOpenPerAppConfig,
                            showLauncherIcon = showLauncherIcon,
                            onShowLauncherIconChange = {
                                showLauncherIcon = it
                                context.getSharedPreferences(GameBarSettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)
                                    .edit().putBoolean(GameBarSettingsActivity.KEY_SHOW_LAUNCHER_ICON, it).apply()
                                onToggleLauncherIcon(it)
                            },
                            onSavePresetClick = { showSavePresetDialog = true },
                            onImportPresetClick = { importPresetLauncher.launch("application/json") },
                            onResetDefaultsClick = { showResetDefaultsDialog = true },
                            onOpenPresetManager = onOpenPresetManager,
                        )
                    }
                }

                FloatingNavToolbar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        val page = when (route) {
                            "home" -> 0
                            "features" -> 1
                            "customization" -> 2
                            "records" -> 3
                            "presets" -> 4
                            else -> 0
                        }
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(page)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .onSizeChanged { toolbarHeightPx = it.height }
                        .padding(bottom = 16.dp)
                        .navigationBarsPadding(),
                )
            }
        }
    }

    if (showResetDefaultsDialog) {
        ModernConfirmDialog(
            title = stringResource(R.string.dialog_reset_all_title),
            message = stringResource(R.string.dialog_reset_all_message),
            icon = Icons.Default.RestartAlt,
            onConfirm = {
                showResetDefaultsDialog = false
                val success = presetManager.resetToDefaults()
                if (success) {
                    reloadAllStates()
                    applyPrefs(rebuild = true)
                    Toast.makeText(context, R.string.toast_preset_reset_success, Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showResetDefaultsDialog = false },
        )
    }

    if (showSavePresetDialog) {
        AlertDialog(
            onDismissRequest = { showSavePresetDialog = false },
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = {
                Text(
                    stringResource(R.string.dialog_title_save_preset),
                    style = MaterialTheme.typography.titleLargeEmphasized,
                )
            },
            text = {
                OutlinedTextField(
                    value = savePresetName,
                    onValueChange = { savePresetName = it },
                    label = { Text(stringResource(R.string.hint_preset_name)) },
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (savePresetName.isNotBlank()) {
                            val saved = presetManager.savePreset(savePresetName.trim())
                            if (saved) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.toast_preset_saved, savePresetName),
                                    Toast.LENGTH_SHORT
                                ).show()
                                savePresetName = ""
                                showSavePresetDialog = false
                            } else {
                                Toast.makeText(context, R.string.toast_preset_save_failed, Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, R.string.toast_preset_name_empty, Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = MaterialTheme.shapes.large,
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSavePresetDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun GameBarHomeScreen(
    listState: LazyListState,
    bottomPadding: Dp,
    gameBarEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onNavigateToPage: (Int) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(
            start = 20.dp, top = 12.dp, end = 20.dp, bottom = 12.dp + bottomPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            GameBarPageHeading(
                title = stringResource(R.string.game_bar_title),
                subtitle = stringResource(R.string.game_bar_summary),
            )
        }
        item {
            GameBarHeroLottieBanner(
                enabled = gameBarEnabled,
                onEnabledChange = onEnabledChange,
            )
        }
        item {
            ConnectedHomeMenuGrid(onNavigate = onNavigateToPage)
        }
    }
}

@Composable
private fun GameBarFeaturesScreen(
    listState: LazyListState,
    bottomPadding: Dp,
    fpsEnable: Boolean,
    onFpsEnableChange: (Boolean) -> Unit,
    fpsDisplayMode: String,
    fpsDisplayOptions: List<SelectOption>,
    onFpsDisplayModeChange: (String) -> Unit,
    fpsMethod: String,
    fpsMethodOptions: List<SelectOption>,
    onFpsMethodChange: (String) -> Unit,
    frameTimeEnable: Boolean,
    onFrameTimeEnableChange: (Boolean) -> Unit,
    tempEnable: Boolean,
    onTempEnableChange: (Boolean) -> Unit,
    cpuUsageEnable: Boolean,
    onCpuUsageEnableChange: (Boolean) -> Unit,
    cpuClockEnable: Boolean,
    onCpuClockEnableChange: (Boolean) -> Unit,
    cpuTempEnable: Boolean,
    onCpuTempEnableChange: (Boolean) -> Unit,
    ramEnable: Boolean,
    onRamEnableChange: (Boolean) -> Unit,
    ramSpeedEnable: Boolean,
    onRamSpeedEnableChange: (Boolean) -> Unit,
    ramTempEnable: Boolean,
    onRamTempEnableChange: (Boolean) -> Unit,
    gpuUsageEnable: Boolean,
    onGpuUsageEnableChange: (Boolean) -> Unit,
    gpuClockEnable: Boolean,
    onGpuClockEnableChange: (Boolean) -> Unit,
    gpuTempEnable: Boolean,
    onGpuTempEnableChange: (Boolean) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(
            start = 20.dp, top = 12.dp, end = 20.dp, bottom = 12.dp + bottomPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            GameBarPageHeading(
                title = stringResource(R.string.tab_features),
                subtitle = stringResource(R.string.features_summary),
            )
        }
        item {
            ModernSettingsCard(
                title = "FPS & Frame Timing",
                iconPainter = painterResource(R.drawable.ic_nav_features),
                badgePolygon = MaterialShapes.Sunny,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        ModernSettingSwitch(
                            title = "FPS Overlay",
                            subtitle = "Show real-time frames per second",
                            checked = fpsEnable,
                            onCheckedChange = onFpsEnableChange,
                            shape = connectedItemShape(0, 2),
                        )
                        ModernSettingSwitch(
                            title = "Frame Time",
                            subtitle = "Show millisecond frame pacing metrics",
                            checked = frameTimeEnable,
                            onCheckedChange = onFrameTimeEnableChange,
                            shape = connectedItemShape(1, 2),
                        )
                    }
                    ModernSettingSelector(
                        title = "FPS Display Mode",
                        selectedValue = fpsDisplayMode,
                        options = fpsDisplayOptions,
                        onValueSelected = onFpsDisplayModeChange,
                    )
                    ModernSettingSelector(
                        title = "Calculation Method",
                        selectedValue = fpsMethod,
                        options = fpsMethodOptions,
                        onValueSelected = onFpsMethodChange,
                    )
                }
            }
        }
        item {
            ModernSettingsCard(
                title = "Hardware Telemetry",
                icon = Icons.Default.Info,
                badgePolygon = MaterialShapes.Cookie9Sided,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    ModernSettingSwitch(
                        title = "Device Temperature",
                        subtitle = "Display thermal sensor readings",
                        checked = tempEnable,
                        onCheckedChange = onTempEnableChange,
                        shape = connectedItemShape(0, 10),
                    )
                    ModernSettingSwitch(
                        title = "CPU Usage",
                        subtitle = "Overall processor utilization percentage",
                        checked = cpuUsageEnable,
                        onCheckedChange = onCpuUsageEnableChange,
                        shape = connectedItemShape(1, 10),
                    )
                    ModernSettingSwitch(
                        title = "CPU Clock Speeds",
                        subtitle = "Real-time frequencies across active cores",
                        checked = cpuClockEnable,
                        onCheckedChange = onCpuClockEnableChange,
                        shape = connectedItemShape(2, 10),
                    )
                    ModernSettingSwitch(
                        title = "CPU Temperature",
                        subtitle = "Thermal sensor on CPU cluster",
                        checked = cpuTempEnable,
                        onCheckedChange = onCpuTempEnableChange,
                        shape = connectedItemShape(3, 10),
                    )
                    ModernSettingSwitch(
                        title = "GPU Usage",
                        subtitle = "Graphics processor load percentage",
                        checked = gpuUsageEnable,
                        onCheckedChange = onGpuUsageEnableChange,
                        shape = connectedItemShape(4, 10),
                    )
                    ModernSettingSwitch(
                        title = "GPU Clock Speed",
                        subtitle = "Current GPU operating frequency",
                        checked = gpuClockEnable,
                        onCheckedChange = onGpuClockEnableChange,
                        shape = connectedItemShape(5, 10),
                    )
                    ModernSettingSwitch(
                        title = "GPU Temperature",
                        subtitle = "Thermal sensor on GPU engine",
                        checked = gpuTempEnable,
                        onCheckedChange = onGpuTempEnableChange,
                        shape = connectedItemShape(6, 10),
                    )
                    ModernSettingSwitch(
                        title = "RAM Usage",
                        subtitle = "Occupied physical memory in MB",
                        checked = ramEnable,
                        onCheckedChange = onRamEnableChange,
                        shape = connectedItemShape(7, 10),
                    )
                    ModernSettingSwitch(
                        title = "RAM Frequency",
                        subtitle = "Memory bus frequency in GHz",
                        checked = ramSpeedEnable,
                        onCheckedChange = onRamSpeedEnableChange,
                        shape = connectedItemShape(8, 10),
                    )
                    ModernSettingSwitch(
                        title = "RAM Temperature",
                        subtitle = "Thermal sensor on RAM module",
                        checked = ramTempEnable,
                        onCheckedChange = onRamTempEnableChange,
                        shape = connectedItemShape(9, 10),
                    )
                }
            }
        }
    }
}

@Composable
private fun GameBarCustomizationScreen(
    listState: LazyListState,
    bottomPadding: Dp,
    textSize: Int,
    onTextSizeChange: (Float) -> Unit,
    bgAlpha: Int,
    onBgAlphaChange: (Float) -> Unit,
    bgColor: Int,
    onBgColorClick: () -> Unit,
    cornerRadius: Int,
    onCornerRadiusChange: (Float) -> Unit,
    paddingValue: Int,
    onPaddingChange: (Float) -> Unit,
    itemSpacing: Int,
    onItemSpacingChange: (Float) -> Unit,
    overlayWidthDp: Int,
    onOverlayWidthChange: (Float) -> Unit,
    updateInterval: String,
    intervalOptions: List<SelectOption>,
    onUpdateIntervalChange: (String) -> Unit,
    titleColor: Int,
    onTitleColorClick: () -> Unit,
    valueColor: Int,
    onValueColorClick: () -> Unit,
    fontName: String,
    onOpenFontSelector: () -> Unit,
    overlayFormat: String,
    overlayFormatOptions: List<SelectOption>,
    onOverlayFormatChange: (String) -> Unit,
    splitMode: String,
    splitModeOptions: List<SelectOption>,
    onSplitModeChange: (String) -> Unit,
    singleTapEnable: Boolean,
    onSingleTapEnableChange: (Boolean) -> Unit,
    singleTapFunction: String,
    gestureOptions: List<SelectOption>,
    onSingleTapFunctionChange: (String) -> Unit,
    doubleTapEnable: Boolean,
    onDoubleTapEnableChange: (Boolean) -> Unit,
    doubleTapFunction: String,
    onDoubleTapFunctionChange: (String) -> Unit,
    longPressEnable: Boolean,
    onLongPressEnableChange: (Boolean) -> Unit,
    longPressFunction: String,
    onLongPressFunctionChange: (String) -> Unit,
    longPressTimeout: String,
    longPressOptions: List<SelectOption>,
    onLongPressTimeoutChange: (String) -> Unit,
) {
    fun colorLabel(color: Int): String = String.format("#%08X", color)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(
            start = 20.dp, top = 12.dp, end = 20.dp, bottom = 12.dp + bottomPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            GameBarPageHeading(
                title = stringResource(R.string.tab_customization),
                subtitle = stringResource(R.string.customization_summary),
            )
        }
        item {
            ModernSettingsCard(
                title = "Appearance & Sizing",
                iconPainter = painterResource(R.drawable.ic_nav_customization),
                badgePolygon = MaterialShapes.Sunny,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    ModernSettingSlider(
                        title = "Text Size",
                        value = textSize,
                        onValueChange = onTextSizeChange,
                        valueRange = 5f..50f,
                        steps = 44,
                        defaultValue = 15,
                        onReset = { onTextSizeChange(15f) },
                    )
                    ModernSettingSlider(
                        title = "Background Opacity",
                        value = bgAlpha,
                        onValueChange = onBgAlphaChange,
                        valueRange = 0f..255f,
                        steps = 254,
                        defaultValue = 255,
                        onReset = { onBgAlphaChange(255f) },
                    )
                    ModernSettingActionRow(
                        title = "Overlay Background Color",
                        summary = "Adjust tint and background alpha",
                        valueBadge = colorLabel(bgColor),
                        onClick = onBgColorClick,
                    )
                    ModernSettingSlider(
                        title = "Corner Radius",
                        value = cornerRadius,
                        onValueChange = onCornerRadiusChange,
                        valueRange = 0f..100f,
                        steps = 99,
                        defaultValue = 30,
                        onReset = { onCornerRadiusChange(30f) },
                    )
                    ModernSettingSlider(
                        title = "Internal Padding",
                        value = paddingValue,
                        onValueChange = onPaddingChange,
                        valueRange = 0f..64f,
                        steps = 63,
                        defaultValue = 4,
                        onReset = { onPaddingChange(4f) },
                    )
                    ModernSettingSlider(
                        title = "Item Spacing",
                        value = itemSpacing,
                        onValueChange = onItemSpacingChange,
                        valueRange = 0f..50f,
                        steps = 49,
                        defaultValue = 8,
                        onReset = { onItemSpacingChange(8f) },
                    )
                    ModernSettingSlider(
                        title = "Overlay Length",
                        value = overlayWidthDp,
                        onValueChange = onOverlayWidthChange,
                        valueRange = 160f..720f,
                        steps = 55,
                        units = "dp",
                        defaultValue = 280,
                        onReset = { onOverlayWidthChange(280f) },
                    )
                    ModernSettingSelector(
                        title = "Refresh Interval",
                        selectedValue = updateInterval,
                        options = intervalOptions,
                        onValueSelected = onUpdateIntervalChange,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        ModernSettingActionRow(
                            title = "Stat Title Color",
                            summary = "Color palette for metric labels",
                            valueBadge = colorLabel(titleColor),
                            colorPreview = Color(titleColor),
                            shape = connectedItemShape(0, 3),
                            onClick = onTitleColorClick,
                        )
                        ModernSettingActionRow(
                            title = "Stat Value Color",
                            summary = "Color palette for real-time numbers",
                            valueBadge = colorLabel(valueColor),
                            colorPreview = Color(valueColor),
                            shape = connectedItemShape(1, 3),
                            onClick = onValueColorClick,
                        )
                        ModernSettingActionRow(
                            title = "Overlay Typography",
                            summary = "Select custom font for the overlay",
                            valueBadge = fontName,
                            shape = connectedItemShape(2, 3),
                            onClick = onOpenFontSelector,
                        )
                    }
                    ModernSettingSelector(
                        title = "Overlay Format",
                        selectedValue = overlayFormat,
                        options = overlayFormatOptions,
                        onValueSelected = onOverlayFormatChange,
                    )
                }
            }
        }
        item {
            ModernSettingsCard(
                title = "Split Layout Mode",
                icon = Icons.Default.Info,
                badgePolygon = MaterialShapes.Clover4Leaf,
            ) {
                ModernSettingSelector(
                    title = "Arrangement",
                    selectedValue = splitMode,
                    options = splitModeOptions,
                    onValueSelected = onSplitModeChange,
                )
            }
        }
        item {
            ModernSettingsCard(
                title = "Overlay Gesture Controls",
                icon = Icons.Default.Info,
                badgePolygon = MaterialShapes.Boom,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    ModernSettingSwitch(
                        title = "Single Tap Gesture",
                        subtitle = "Tap overlay once to trigger action",
                        checked = singleTapEnable,
                        onCheckedChange = onSingleTapEnableChange,
                    )
                    ModernSettingSelector(
                        title = "Single Tap Action",
                        selectedValue = singleTapFunction,
                        options = gestureOptions,
                        onValueSelected = onSingleTapFunctionChange,
                        enabled = singleTapEnable,
                    )
                    ModernSettingSwitch(
                        title = "Double Tap Gesture",
                        subtitle = "Double tap overlay to trigger action",
                        checked = doubleTapEnable,
                        onCheckedChange = onDoubleTapEnableChange,
                    )
                    ModernSettingSelector(
                        title = "Double Tap Action",
                        selectedValue = doubleTapFunction,
                        options = gestureOptions,
                        onValueSelected = onDoubleTapFunctionChange,
                        enabled = doubleTapEnable,
                    )
                    ModernSettingSwitch(
                        title = "Long Press Gesture",
                        subtitle = "Press and hold overlay to trigger action",
                        checked = longPressEnable,
                        onCheckedChange = onLongPressEnableChange,
                    )
                    ModernSettingSelector(
                        title = "Long Press Action",
                        selectedValue = longPressFunction,
                        options = gestureOptions,
                        onValueSelected = onLongPressFunctionChange,
                        enabled = longPressEnable,
                    )
                    ModernSettingSelector(
                        title = "Hold Duration",
                        selectedValue = longPressTimeout,
                        options = longPressOptions,
                        onValueSelected = onLongPressTimeoutChange,
                        enabled = longPressEnable,
                    )
                }
            }
        }
    }
}

@Composable
private fun GameBarRecordsScreen(
    listState: LazyListState,
    bottomPadding: Dp,
    fpsRecordControlEnabled: Boolean,
    onFpsRecordControlEnabledChange: (Boolean) -> Unit,
    onOpenFpsRecord: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(
            start = 20.dp, top = 12.dp, end = 20.dp, bottom = 12.dp + bottomPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            GameBarPageHeading(
                title = stringResource(R.string.tab_records),
                subtitle = stringResource(R.string.fps_record_summary),
            )
        }
        item {
            ModernSettingsCard(
                title = "Floating Control",
                iconPainter = painterResource(R.drawable.ic_nav_fps_record),
                badgePolygon = MaterialShapes.Sunny,
            ) {
                ModernSettingSwitch(
                    title = "Record Controller Bubble",
                    subtitle = "Show movable bubble to start/stop recording sessions without overlay",
                    checked = fpsRecordControlEnabled,
                    onCheckedChange = onFpsRecordControlEnabledChange,
                )
            }
        }
        item {
            ModernSettingsCard(
                title = "Sessions & Analytics",
                icon = Icons.Default.Info,
                badgePolygon = MaterialShapes.Cookie9Sided,
            ) {
                ModernSettingActionRow(
                    title = "View FPS Records",
                    summary = "Analyze historical frame rate records, averages and graphs",
                    onClick = onOpenFpsRecord,
                )
            }
        }
    }
}

@Composable
private fun GameBarPresetsScreen(
    listState: LazyListState,
    bottomPadding: Dp,
    amoledBlackEnabled: Boolean,
    onAmoledBlackChange: (Boolean) -> Unit,
    autoEnable: Boolean,
    onAutoEnableChange: (Boolean) -> Unit,
    onOpenPerAppConfig: () -> Unit,
    showLauncherIcon: Boolean,
    onShowLauncherIconChange: (Boolean) -> Unit,
    onSavePresetClick: () -> Unit,
    onImportPresetClick: () -> Unit,
    onResetDefaultsClick: () -> Unit,
    onOpenPresetManager: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(
            start = 20.dp, top = 12.dp, end = 20.dp, bottom = 12.dp + bottomPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            GameBarPageHeading(
                title = stringResource(R.string.tab_presets),
                subtitle = stringResource(R.string.presets_tab_summary),
            )
        }
        item {
            ModernSettingsCard(
                title = "Theme & Appearance",
                iconPainter = painterResource(R.drawable.ic_nav_presets),
                badgePolygon = MaterialShapes.Sunny,
            ) {
                ModernSettingSwitch(
                    title = "AMOLED Black Theme",
                    subtitle = "Render pure black backgrounds and deep surfaces",
                    checked = amoledBlackEnabled,
                    onCheckedChange = onAmoledBlackChange,
                )
            }
        }
        item {
            ModernSettingsCard(
                title = "Per-App Automation",
                icon = Icons.Default.Info,
                badgePolygon = MaterialShapes.Clover4Leaf,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    ModernSettingSwitch(
                        title = "Auto-Enable for Apps",
                        subtitle = "Automatically toggle GameBar when designated games launch",
                        checked = autoEnable,
                        onCheckedChange = onAutoEnableChange,
                        shape = connectedItemShape(0, 2),
                    )
                    ModernSettingActionRow(
                        title = "Configure Games & Apps",
                        summary = "Choose specific packages to trigger overlay automatically",
                        onClick = onOpenPerAppConfig,
                        shape = connectedItemShape(1, 2),
                    )
                }
            }
        }
        item {
            ModernSettingsCard(
                title = "Launcher Integration",
                icon = Icons.Default.Info,
                badgePolygon = MaterialShapes.Cookie9Sided,
            ) {
                ModernSettingSwitch(
                    title = "Show Launcher Icon",
                    subtitle = "Show or hide GameBar icon in the app drawer",
                    checked = showLauncherIcon,
                    onCheckedChange = onShowLauncherIconChange,
                )
            }
        }
        item {
            ModernSettingsCard(
                title = "Preset Management",
                icon = Icons.Default.Info,
                badgePolygon = MaterialShapes.SoftBurst,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    ModernSettingActionRow(
                        title = stringResource(R.string.preset_save_current),
                        summary = stringResource(R.string.preset_save_current_summary),
                        onClick = onSavePresetClick,
                        shape = connectedItemShape(0, 4),
                    )
                    ModernSettingActionRow(
                        title = stringResource(R.string.preset_import),
                        summary = stringResource(R.string.preset_import_summary),
                        onClick = onImportPresetClick,
                        shape = connectedItemShape(1, 4),
                    )
                    ModernSettingActionRow(
                        title = stringResource(R.string.preset_reset_default),
                        summary = stringResource(R.string.preset_reset_default_summary),
                        onClick = onResetDefaultsClick,
                        shape = connectedItemShape(2, 4),
                    )
                    ModernSettingActionRow(
                        title = stringResource(R.string.preset_saved_presets),
                        summary = "View, apply, export or delete saved configurations",
                        onClick = onOpenPresetManager,
                        shape = connectedItemShape(3, 4),
                    )
                }
            }
        }
        item {
            AboutGameBarCard()
        }
    }
}

@Composable
fun AboutGameBarCard() {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val packageInfo = runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }.getOrNull()
            val versionName = packageInfo?.versionName ?: "1.0"

            val aboutMorph = remember {
                Morph(MaterialShapes.Circle, MaterialShapes.Cookie9Sided)
            }
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(MorphShape(aboutMorph, 1f))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_gamebar),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(36.dp),
                )
            }

            Text(
                text = stringResource(R.string.about_version, versionName),
                style = MaterialTheme.typography.headlineSmallEmphasized,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.about_description),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = stringResource(R.string.about_developer),
                style = MaterialTheme.typography.titleSmallEmphasized,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                FilledTonalButton(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/kenway214/packages_apps_GameBar")))
                    },
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    shape = CircleShape,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.brand_github),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.about_github))
                }

                FilledTonalButton(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/kenway214/packages_apps_GameBar/issues")))
                    },
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    shape = CircleShape,
                ) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.about_issues))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                FilledTonalButton(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/pandemonium_haydn")))
                    },
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    shape = CircleShape,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.brand_telegram),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.about_support))
                }

                FilledTonalButton(
                    onClick = {
                        val url = context.getString(R.string.game_bar_user_guide_url)
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    },
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    shape = CircleShape,
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.about_guide))
                }
            }
        }
    }
}
