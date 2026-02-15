/*
 * SPDX-FileCopyrightText: 2026 kenway214
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.gamebar

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max

private data class FpsSessionItem(
    val packageName: String,
    val appName: String,
    val file: File,
    val dateText: String,
    val avgFps: Float,
    val avgPower: Float,
    val duration: String
)

@Composable
fun FpsRecordScreen(
    onBack: () -> Unit
) {
    var sessions by remember { mutableStateOf<List<FpsSessionItem>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var selectedSession by remember { mutableStateOf<FpsSessionItem?>(null) }
    var selectedAnalytics by remember { mutableStateOf<LogAnalytics?>(null) }
    var loadingAnalytics by remember { mutableStateOf(false) }

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        sessions = withContext(Dispatchers.IO) { loadSessions(context.packageManager) }
    }

    if (selectedSession != null) {
        if (loadingAnalytics) {
            LaunchedEffect(selectedSession!!.file.absolutePath) {
                selectedAnalytics = withContext(Dispatchers.IO) {
                    PerAppLogReader().analyzeLogFile(selectedSession!!.file.absolutePath)
                }
                loadingAnalytics = false
            }
        }

        FpsRecordDetailScreen(
            session = selectedSession!!,
            analytics = selectedAnalytics,
            loading = loadingAnalytics,
            onBack = {
                selectedSession = null
                selectedAnalytics = null
                loadingAnalytics = false
            }
        )
        return
    }

    val filtered = remember(sessions, query) {
        if (query.isBlank()) {
            sessions
        } else {
            val q = query.trim().lowercase(Locale.getDefault())
            sessions.filter { item ->
                item.appName.lowercase(Locale.getDefault()).contains(q) ||
                    item.packageName.lowercase(Locale.getDefault()).contains(q) ||
                    item.file.name.lowercase(Locale.getDefault()).contains(q)
            }
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "FPS Record",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Text(
            text = "Per-session game performance logs with session summary and graphs.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
        )

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search sessions") }
        )

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No FPS record sessions found",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(filtered, key = { it.file.absolutePath }) { session ->
                FpsRecordSessionRow(
                    session = session,
                    onOpen = {
                        selectedSession = session
                        selectedAnalytics = null
                        loadingAnalytics = true
                    },
                    onDelete = {
                        if (session.file.delete()) {
                            sessions = sessions.filter { it.file.absolutePath != session.file.absolutePath }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun FpsRecordSessionRow(
    session: FpsSessionItem,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val pm = context.packageManager
    val iconBitmap =
        remember(session.packageName) {
            runCatching { pm.getApplicationIcon(session.packageName).toBitmap() }.getOrNull()
        }

    val avgFps = if (session.avgFps > 0f) formatValue(session.avgFps) else "--"
    val power = if (session.avgPower > 0f) "${formatValue(session.avgPower)}W" else "--"
    val subTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onOpen() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (iconBitmap != null) {
                    Image(
                        bitmap = iconBitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(44.dp).background(Color.Transparent, CircleShape),
                    )
                } else {
                    Box(
                        modifier = Modifier.size(44.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = session.appName,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(session.dateText, color = subTextColor, fontSize = 12.sp)
                        Text(power, color = subTextColor, fontSize = 12.sp)
                        Text(session.duration, color = subTextColor, fontSize = 12.sp)
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                val chartColors = rememberChartColors()
                Text(
                    text = avgFps,
                    color = chartColors.blue,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(end = 12.dp),
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun FpsRecordDetailScreen(
    session: FpsSessionItem,
    analytics: LogAnalytics?,
    loading: Boolean,
    onBack: () -> Unit,
) {
    if (loading || analytics == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val dateText = analytics.sessionDate

    val fpsValues = analytics.fpsTimeData.map { it.second.toFloat() }
    val frameTimeValues = analytics.frameTimeData.map { it.second.toFloat() }
    val cpuUsageValues = analytics.cpuUsageTimeData.map { it.second.toFloat() }
    val cpuTempValues = analytics.cpuTempTimeData.map { it.second.toFloat() }
    val gpuUsageValues = analytics.gpuUsageTimeData.map { it.second.toFloat() }
    val gpuClockValues = analytics.gpuClockTimeData.map { it.second.toFloat() }
    val gpuTempValues = analytics.gpuTempTimeData.map { it.second.toFloat() }
    val powerValues = analytics.powerTimeData.map { it.second.toFloat() }
    val capacityValues = analytics.batteryLevelTimeData.map { it.second.toFloat() }
    val batteryTempValues = analytics.batteryTempTimeData.map { it.second.toFloat() }
    val ramUsageValues = analytics.ramUsageTimeData.map { it.second.toFloat() }
    val ramSpeedValues = analytics.ramSpeedTimeData.map { it.second.toFloat() }
    val ramTempValues = analytics.ramTempTimeData.map { it.second.toFloat() }
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val cpuClockSeries = remember(analytics.cpuClockTimeData, isDark) {
        buildCpuClusterSeries(analytics.cpuClockTimeData, isDark)
    }

    val subTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val context = LocalContext.current
    val iconBitmap = remember(session.packageName) {
        runCatching { context.packageManager.getApplicationIcon(session.packageName).toBitmap() }.getOrNull()
    }
    var showMenu by remember { mutableStateOf(false) }

    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (iconBitmap != null) {
                        Image(
                            bitmap = iconBitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.size(36.dp).background(Color.Transparent, CircleShape),
                        )
                    } else {
                        Box(
                            modifier =
                                Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = session.appName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Session options",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Export Session Log (CSV)") },
                            onClick = {
                                showMenu = false
                                val ok = exportFileToDownloads(
                                    context = context,
                                    source = session.file,
                                    mimeType = "text/csv",
                                    displayName = session.file.name,
                                )
                                Toast.makeText(
                                    context,
                                    if (ok) "Session log exported to Downloads" else "Failed to export session log",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Share Session Log (CSV)") },
                            onClick = {
                                showMenu = false
                                shareSessionFile(context, session.file)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Save Graphics (PNG)") },
                            onClick = {
                                showMenu = false
                                val ok = FpsRecordImageGenerator.saveGraphics(context, session.appName, analytics)
                                Toast.makeText(
                                    context,
                                    if (ok) "Graphics saved to Downloads" else "Failed to save graphics",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Share Graphics (PNG)") },
                            onClick = {
                                showMenu = false
                                FpsRecordImageGenerator.generateAndShareImage(context, session.appName, analytics)
                            }
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(dateText, color = subTextColor, fontSize = 12.sp)
                    Text(
                        text = "Session Summary",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                    StatsGrid(analytics = analytics)
                }
            }
        }

        item {
            val chartColors = rememberChartColors()
            val fpsMax = fpsValues.maxOrNull() ?: 0f
            val fpsStep = 30f
            val yMax = ((fpsMax / fpsStep).toInt() + 1).coerceAtLeast(2) * fpsStep

            LineChartCard(
                title = "FPS",
                series = listOf(ChartSeries("FPS", chartColors.blue, fpsValues)),
                chartContent = {
                    FixedAxisLineChart(
                        series = listOf(ChartSeries("FPS", chartColors.blue, fpsValues)),
                        yMin = 0f,
                        yMax = yMax,
                        yStep = fpsStep,
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                    )
                },
                footerStats = ChartFooter(
                    max = fpsMax,
                    min = fpsValues.minOrNull(),
                    avg = avgOrNull(fpsValues),
                    unit = "",
                )
            )
        }

        if (batteryTempValues.isNotEmpty()) {
            item {
                val chartColors = rememberChartColors()
                val maxTemp = batteryTempValues.maxOrNull() ?: 0f
                val yMax = (((maxTemp / 10f).toInt() + 1).coerceAtLeast(4) * 10f)
                LineChartCard(
                    title = "Battery Temp (°C)",
                    series = listOf(ChartSeries("Battery Temp", chartColors.orange, batteryTempValues)),
                    chartContent = {
                        FixedAxisLineChart(
                            series = listOf(ChartSeries("Battery Temp", chartColors.orange, batteryTempValues)),
                            yMin = 0f,
                            yMax = yMax,
                            yStep = 10f,
                            modifier = Modifier.fillMaxWidth().height(180.dp),
                        )
                    },
                    footerStats = ChartFooter(
                        max = maxTemp,
                        min = batteryTempValues.minOrNull(),
                        avg = avgOrNull(batteryTempValues),
                        unit = "°C",
                    )
                )
            }
        }

        item {
            val chartColors = rememberChartColors()
            LineChartCard(
                title = "Frame Time (ms)",
                series = listOf(ChartSeries("Frame Time", chartColors.blue, frameTimeValues)),
                chartContent = {
                    AreaChart(
                        values = frameTimeValues,
                        color = chartColors.blue,
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                    )
                },
                footerStats = ChartFooter(
                    max = frameTimeValues.maxOrNull(),
                    min = frameTimeValues.minOrNull(),
                    avg = avgOrNull(frameTimeValues),
                    unit = "ms",
                )
            )
        }

        item {
            val chartColors = rememberChartColors()
            LineChartCard(
                title = "CPU Usage (%)",
                series = listOf(ChartSeries("Total", chartColors.blue, cpuUsageValues)),
                chartContent = {
                    FixedAxisLineChart(
                        series = listOf(ChartSeries("Total", chartColors.blue, cpuUsageValues)),
                        yMin = 0f,
                        yMax = 100f,
                        yStep = 10f,
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                    )
                },
                footerStats = ChartFooter(
                    max = cpuUsageValues.maxOrNull(),
                    min = cpuUsageValues.minOrNull(),
                    avg = avgOrNull(cpuUsageValues),
                    unit = "%",
                )
            )
        }

        if (cpuClockSeries.isNotEmpty()) {
            item {
                val cpuClockFlatValues = cpuClockSeries.flatMap { it.values }.mapNotNull { it }
                val maxFreq = cpuClockFlatValues.maxOrNull() ?: 0f
                val freqUpper = (((maxFreq / 300f).toInt() + 1).coerceAtLeast(4)) * 300f
                LineChartCard(
                    title = "CPU Freq (MHz)",
                    series = cpuClockSeries,
                    chartContent = {
                        FixedAxisLineChart(
                            series = cpuClockSeries,
                            yMin = 0f,
                            yMax = freqUpper,
                            yStep = 300f,
                            modifier = Modifier.fillMaxWidth().height(180.dp),
                        )
                    },
                    footerStats = ChartFooter(
                        max = maxFreq,
                        min = cpuClockFlatValues.filter { it > 0f }.minOrNull(),
                        avg = avgOrNull(cpuClockFlatValues.filter { it > 0f }),
                        unit = "MHz",
                    )
                )
            }
        }

        if (cpuTempValues.isNotEmpty()) {
            item {
                val chartColors = rememberChartColors()
                val maxTemp = cpuTempValues.maxOrNull() ?: 0f
                val yMax = (((maxTemp / 10f).toInt() + 1).coerceAtLeast(4) * 10f)

                LineChartCard(
                    title = "CPU Temp (°C)",
                    series = listOf(ChartSeries("Temp", chartColors.orange, cpuTempValues)),
                    chartContent = {
                        FixedAxisLineChart(
                            series = listOf(ChartSeries("Temp", chartColors.orange, cpuTempValues)),
                            yMin = 0f,
                            yMax = yMax,
                            yStep = 10f,
                            modifier = Modifier.fillMaxWidth().height(180.dp),
                        )
                    },
                    footerStats = ChartFooter(
                        max = maxTemp,
                        min = cpuTempValues.minOrNull(),
                        avg = avgOrNull(cpuTempValues),
                        unit = "°C",
                    )
                )
            }
        }

        if (gpuClockValues.isNotEmpty() || gpuUsageValues.isNotEmpty()) {
            item {
                val chartColors = rememberChartColors()
                LineChartCard(
                    title = "GPU Frequency (MHz)",
                    rightTitle = "Usage(%)",
                    series =
                        listOf(
                            ChartSeries("Frequency(MHz)", chartColors.blue, gpuClockValues),
                            ChartSeries("Usage(%)", chartColors.orange, gpuUsageValues),
                        ),
                    chartContent = {
                        GpuDualAxisChart(
                            freq = gpuClockValues,
                            usage = gpuUsageValues,
                            modifier = Modifier.fillMaxWidth().height(180.dp),
                            colors = chartColors
                        )
                    },
                    footerStats = ChartFooter(
                        max = gpuClockValues.maxOrNull(),
                        min = gpuClockValues.filter { it > 0f }.minOrNull(),
                        avg = avgOrNull(gpuClockValues.filter { it > 0f }),
                        unit = "MHz",
                    )
                )
            }
        }

        if (gpuTempValues.isNotEmpty()) {
            item {
                val chartColors = rememberChartColors()
                val maxTemp = gpuTempValues.maxOrNull() ?: 0f
                val yMax = (((maxTemp / 10f).toInt() + 1).coerceAtLeast(4) * 10f)

                LineChartCard(
                    title = "GPU Temp (°C)",
                    series = listOf(ChartSeries("Temp", chartColors.orange, gpuTempValues)),
                    chartContent = {
                        FixedAxisLineChart(
                            series = listOf(ChartSeries("Temp", chartColors.orange, gpuTempValues)),
                            yMin = 0f,
                            yMax = yMax,
                            yStep = 10f,
                            modifier = Modifier.fillMaxWidth().height(180.dp),
                        )
                    },
                    footerStats = ChartFooter(
                        max = maxTemp,
                        min = gpuTempValues.minOrNull(),
                        avg = avgOrNull(gpuTempValues),
                        unit = "°C",
                    )
                )
            }
        }

        if (ramUsageValues.isNotEmpty()) {
            item {
                val chartColors = rememberChartColors()
                val maxUsage = ramUsageValues.maxOrNull() ?: 0f
                val yMax = (((maxUsage / 512f).toInt() + 1).coerceAtLeast(4) * 512f)
                LineChartCard(
                    title = "RAM Usage (MB)",
                    series = listOf(ChartSeries("RAM Usage", chartColors.cyan, ramUsageValues)),
                    chartContent = {
                        FixedAxisLineChart(
                            series = listOf(ChartSeries("RAM Usage", chartColors.cyan, ramUsageValues)),
                            yMin = 0f,
                            yMax = yMax,
                            yStep = 512f,
                            modifier = Modifier.fillMaxWidth().height(180.dp),
                        )
                    },
                    footerStats = ChartFooter(
                        max = maxUsage,
                        min = ramUsageValues.minOrNull(),
                        avg = avgOrNull(ramUsageValues),
                        unit = "MB",
                    )
                )
            }
        }

        if (ramSpeedValues.isNotEmpty()) {
            item {
                val chartColors = rememberChartColors()
                val maxFreq = ramSpeedValues.maxOrNull() ?: 0f
                val yMax = (((maxFreq / 200f).toInt() + 1).coerceAtLeast(4) * 200f)
                LineChartCard(
                    title = "RAM Frequency (MHz)",
                    series = listOf(ChartSeries("RAM Freq", chartColors.blue, ramSpeedValues)),
                    chartContent = {
                        FixedAxisLineChart(
                            series = listOf(ChartSeries("RAM Freq", chartColors.blue, ramSpeedValues)),
                            yMin = 0f,
                            yMax = yMax,
                            yStep = 200f,
                            modifier = Modifier.fillMaxWidth().height(180.dp),
                        )
                    },
                    footerStats = ChartFooter(
                        max = maxFreq,
                        min = ramSpeedValues.filter { it > 0f }.minOrNull(),
                        avg = avgOrNull(ramSpeedValues.filter { it > 0f }),
                        unit = "MHz",
                    )
                )
            }
        }

        if (ramTempValues.isNotEmpty()) {
            item {
                val chartColors = rememberChartColors()
                val maxTemp = ramTempValues.maxOrNull() ?: 0f
                val yMax = (((maxTemp / 10f).toInt() + 1).coerceAtLeast(4) * 10f)
                LineChartCard(
                    title = "RAM Temp (°C)",
                    series = listOf(ChartSeries("RAM Temp", chartColors.orange, ramTempValues)),
                    chartContent = {
                        FixedAxisLineChart(
                            series = listOf(ChartSeries("RAM Temp", chartColors.orange, ramTempValues)),
                            yMin = 0f,
                            yMax = yMax,
                            yStep = 10f,
                            modifier = Modifier.fillMaxWidth().height(180.dp),
                        )
                    },
                    footerStats = ChartFooter(
                        max = maxTemp,
                        min = ramTempValues.minOrNull(),
                        avg = avgOrNull(ramTempValues),
                        unit = "°C",
                    )
                )
            }
        }

        if (powerValues.isNotEmpty()) {
            item {
                val chartColors = rememberChartColors()
                LineChartCard(
                    title = "Power(W)",
                    rightTitle = "Capacity %",
                    series =
                        listOf(
                            ChartSeries("Power(W)", chartColors.blue, powerValues),
                            ChartSeries("Capacity(%)", chartColors.orange, capacityValues),
                        ),
                    chartContent = {
                        PowerCapacityDualAxisChart(
                            power = powerValues,
                            capacity = capacityValues,
                            modifier = Modifier.fillMaxWidth().height(180.dp),
                            colors = chartColors
                        )
                    },
                    footerStats = ChartFooter(
                        max = powerValues.maxOrNull(),
                        min = powerValues.minOrNull(),
                        avg = avgOrNull(powerValues),
                        unit = "W",
                    )
                )
            }
        }

        if (capacityValues.isNotEmpty() && powerValues.isEmpty()) {
            item {
                val chartColors = rememberChartColors()
                LineChartCard(
                    title = "Battery Level (%)",
                    series = listOf(ChartSeries("Capacity(%)", chartColors.orange, capacityValues)),
                    chartContent = {
                        FixedAxisLineChart(
                            series = listOf(ChartSeries("Capacity(%)", chartColors.orange, capacityValues)),
                            yMin = 0f,
                            yMax = 100f,
                            yStep = 10f,
                            modifier = Modifier.fillMaxWidth().height(180.dp),
                        )
                    },
                    footerStats = ChartFooter(
                        max = capacityValues.maxOrNull(),
                        min = capacityValues.minOrNull(),
                        avg = avgOrNull(capacityValues),
                        unit = "%",
                    )
                )
            }
        }
    }
}

@Composable
private fun StatsGrid(analytics: LogAnalytics) {
    val maxTemp = max(analytics.cpuStats.maxTemp.toFloat(), analytics.gpuStats.maxTemp.toFloat())
    val fpsValues = analytics.fpsTimeData.map { it.second.toFloat() }.sorted()
    val low5 = averageSlowestPercent(fpsValues, 0.05f)
    val chartColors = rememberChartColors()

    val items =
        listOf(
            "MAX" to formatValue(analytics.fpsStats.maxFps.toFloat()),
            "MIN" to formatValue(analytics.fpsStats.minFps.toFloat()),
            "AVERAGE" to formatValue(analytics.fpsStats.avgFps.toFloat()),
            "1% LOW" to formatValue(analytics.fpsStats.fps1PercentLow.toFloat()),
            "0.1% LOW" to formatValue(analytics.fpsStats.fps0_1PercentLow.toFloat()),
            "5% LOW" to formatValue(low5),
            "MAX TEMP" to formatValue(maxTemp),
            "AVG POWER" to formatValue(analytics.powerStats.avgPower.toFloat()),
        )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in items.chunked(4)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                row.forEach { (label, value) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        Text(value, color = chartColors.blue, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private data class ChartSeries(
    val label: String,
    val color: Color,
    val values: List<Float?>,
)

@Composable
private fun LineChartCard(
    title: String,
    series: List<ChartSeries>,
    rightTitle: String? = null,
    footerStats: ChartFooter? = null,
    chartHeight: Int = 180,
    chartContent: (@Composable () -> Unit)? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                if (!rightTitle.isNullOrBlank()) {
                    Text(rightTitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
            if (chartContent != null) {
                chartContent()
            } else {
                LineChart(series = series, modifier = Modifier.fillMaxWidth().height(chartHeight.dp))
            }
            ChartLegend(series = series)
            if (footerStats != null) {
                ChartFooterRow(footerStats)
            }
        }
    }
}

@Composable
private fun LineChart(
    series: List<ChartSeries>,
    modifier: Modifier = Modifier,
) {
    val samplesCount = series.maxOfOrNull { it.values.size } ?: 0
    if (samplesCount == 0) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No data", color = Color(0xFF8B949E))
        }
        return
    }
    val step = max(1, samplesCount / 220)
    val filteredSeries =
        series.map { s ->
            s.copy(values = s.values.filterIndexed { index, _ -> index % step == 0 })
        }

    val allValues = filteredSeries.flatMap { s -> s.values.filterNotNull() }
    val minY = allValues.minOrNull() ?: 0f
    val maxY = allValues.maxOrNull() ?: 1f
    val range = if (maxY - minY <= 0f) 1f else maxY - minY
    val pad = range * 0.1f
    val yMin = minY - pad
    val yMax = maxY + pad

    val baseOnSurface = MaterialTheme.colorScheme.onSurface
    val gridColor = baseOnSurface.copy(alpha = 0.12f)
    val labelColor = baseOnSurface.copy(alpha = 0.6f)
    val steps = 5
    val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val leftPad = 70f
        val rightPad = 16f
        val topPad = 8f
        val bottomPad = 26f
        val plotW = (w - leftPad - rightPad).coerceAtLeast(1f)
        val plotH = (h - topPad - bottomPad).coerceAtLeast(1f)
        for (i in 0..steps) {
            val y = topPad + plotH * i / steps
            drawLine(
                gridColor,
                start = Offset(leftPad, y),
                end = Offset(leftPad + plotW, y),
                strokeWidth = 1f,
                pathEffect = dash
            )
            val value = yMax - (range + pad * 2f) * (i / steps.toFloat())
            drawContext.canvas.nativeCanvas.drawText(
                formatAxis(value),
                leftPad - 10f,
                y - 6f,
                android.graphics.Paint().apply {
                    setColor(labelColor.toArgb())
                    textSize = 22f
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.RIGHT
                }
            )
        }

        val xSteps = 4
        for (i in 0..xSteps) {
            val x = leftPad + plotW * i / xSteps
            drawLine(
                gridColor,
                start = Offset(x, topPad),
                end = Offset(x, topPad + plotH),
                strokeWidth = 1f,
                pathEffect = dash
            )
            val label = formatTimeLabel(i, xSteps, samplesCount)
            drawContext.canvas.nativeCanvas.drawText(
                label,
                x,
                topPad + plotH + 24f,
                android.graphics.Paint().apply {
                    setColor(labelColor.toArgb())
                    textSize = 22f
                    isAntiAlias = true
                    textAlign =
                        when (i) {
                            0 -> android.graphics.Paint.Align.LEFT
                            xSteps -> android.graphics.Paint.Align.RIGHT
                            else -> android.graphics.Paint.Align.CENTER
                        }
                }
            )
        }

        filteredSeries.forEach { s ->
            val values = s.values
            if (values.size <= 1) return@forEach
            val path = Path()
            var hasStarted = false
            values.forEachIndexed { index, value ->
                val v = value ?: return@forEachIndexed
                val x = leftPad + plotW * index / (values.size - 1).coerceAtLeast(1)
                val y = topPad + plotH - ((v - yMin) / (yMax - yMin)) * plotH
                if (!hasStarted) {
                    path.moveTo(leftPad, y)
                    path.lineTo(x, y)
                    hasStarted = true
                } else {
                    path.lineTo(x, y)
                }
            }
            if (hasStarted) {
                drawPath(
                    path,
                    color = s.color,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.2f)
                )
            }
        }
    }
}

@Composable
private fun ChartLegend(series: List<ChartSeries>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        series.forEach { s ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(8.dp).background(s.color, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(s.label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
        }
    }
}

private data class ChartFooter(
    val max: Float?,
    val min: Float?,
    val avg: Float?,
    val unit: String,
)

@Composable
private fun ChartFooterRow(stats: ChartFooter) {
    val maxText = stats.max?.let { formatValue(it) + stats.unit } ?: "--"
    val minText = stats.min?.let { formatValue(it) + stats.unit } ?: "--"
    val avgText = stats.avg?.let { formatValue(it) + stats.unit } ?: "--"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        val textColor = MaterialTheme.colorScheme.onSurfaceVariant
        Text("MAX: $maxText", color = textColor, fontSize = 12.sp)
        Text("MIN: $minText", color = textColor, fontSize = 12.sp)
        Text("AVG: $avgText", color = textColor, fontSize = 12.sp)
    }
}

private fun formatValue(value: Float): String {
    return if (value <= 0f) "0.0" else String.format(Locale.getDefault(), "%.1f", value)
}

private fun buildCpuClusterSeries(
    cpuClockTimeData: Map<Int, List<Pair<Long, Double>>>,
    isDark: Boolean,
): List<ChartSeries> {
    if (cpuClockTimeData.isEmpty()) return emptyList()

    val sortedCoreIds = cpuClockTimeData.keys.sorted()
    if (sortedCoreIds.isEmpty()) return emptyList()

    val coreAverages = sortedCoreIds.associateWith { coreId ->
        val values = cpuClockTimeData[coreId].orEmpty().map { it.second.toFloat() }.filter { it > 0f }
        if (values.isEmpty()) 0f else values.average().toFloat()
    }

    val groups = mutableListOf<MutableList<Int>>()
    for (coreId in sortedCoreIds) {
        if (groups.isEmpty()) {
            groups.add(mutableListOf(coreId))
            continue
        }
        val currentGroup = groups.last()
        val prevCoreId = currentGroup.last()
        val prevAvg = coreAverages[prevCoreId] ?: 0f
        val currentAvg = coreAverages[coreId] ?: 0f
        val maxBase = max(prevAvg, currentAvg).coerceAtLeast(1f)
        val ratioDelta = kotlin.math.abs(currentAvg - prevAvg) / maxBase

        if (coreId == prevCoreId + 1 && ratioDelta <= 0.18f) {
            currentGroup.add(coreId)
        } else {
            groups.add(mutableListOf(coreId))
        }
    }

    return groups.mapIndexed { index, cores ->
        val values = mergeCoreSeries(cores, cpuClockTimeData)
        ChartSeries(
            label = "Cluster ${index + 1} ${formatCoreRange(cores)}",
            color = cpuClusterColor(index, isDark),
            values = values,
        )
    }
}

private fun mergeCoreSeries(
    coreIds: List<Int>,
    cpuClockTimeData: Map<Int, List<Pair<Long, Double>>>,
): List<Float?> {
    val coreSeries = coreIds.map { coreId ->
        cpuClockTimeData[coreId].orEmpty().map { it.second.toFloat() }
    }
    val maxSamples = coreSeries.maxOfOrNull { it.size } ?: 0
    if (maxSamples == 0) return emptyList()

    return List(maxSamples) { sampleIndex ->
        val sampleValues = coreSeries.mapNotNull { series -> series.getOrNull(sampleIndex) }.filter { it > 0f }
        if (sampleValues.isEmpty()) null else sampleValues.average().toFloat()
    }
}

private fun formatCoreRange(coreIds: List<Int>): String {
    if (coreIds.isEmpty()) return ""
    return "(${coreIds.joinToString(" ")})"
}

private fun cpuClusterColor(index: Int, isDark: Boolean): Color {
    return if (isDark) {
        when (index) {
            0 -> Color(0xFF8774E7)
            1 -> Color(0xFF08D2D4)
            else -> Color(0xFFFF8626)
        }
    } else {
        when (index) {
            0 -> Color(0xFF5B21B6)
            1 -> Color(0xFF007A99)
            else -> Color(0xFFB54D00)
        }
    }
}

private fun formatAxis(value: Float): String {
    return if (value >= 1000f) {
        String.format(Locale.getDefault(), "%.0f", value)
    } else {
        String.format(Locale.getDefault(), "%.1f", value)
    }
}

private fun formatTimeLabel(index: Int, steps: Int, samples: Int): String {
    if (samples <= 1) return "0s"
    val seconds = ((samples - 1) * index / steps)
    val mins = seconds / 60
    val secs = seconds % 60
    return if (mins > 0) "${mins}m${secs}s" else "${secs}s"
}

private fun avgOrNull(values: List<Float>): Float? {
    if (values.isEmpty()) return null
    return values.average().toFloat()
}

private fun shareSessionFile(context: Context, file: File) {
    if (!file.exists() || !file.canRead()) {
        Toast.makeText(context, "Session log is not readable", Toast.LENGTH_SHORT).show()
        return
    }
    runCatching {
        val shareSource =
            File(context.externalCacheDir ?: context.cacheDir, "share_${System.currentTimeMillis()}_${file.name}").apply {
                file.copyTo(this, overwrite = true)
            }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            shareSource
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "FPS Session Log")
            putExtra(Intent.EXTRA_TEXT, "FPS session log: ${file.name}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share session log"))
    }.onFailure {
        Toast.makeText(context, "Failed to share session log", Toast.LENGTH_SHORT).show()
    }
}

private fun exportFileToDownloads(
    context: Context,
    source: File,
    mimeType: String,
    displayName: String,
): Boolean {
    if (!source.exists() || !source.canRead()) return false

    return runCatching {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, displayName)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
        }

        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val uri = resolver.insert(collection, values) ?: return false

        resolver.openOutputStream(uri)?.use { out ->
            FileInputStream(source).use { input -> input.copyTo(out) }
        } ?: return false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val done = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
            resolver.update(uri, done, null, null)
        }
        true
    }.getOrElse { false }
}

@Composable
private fun GpuDualAxisChart(
    freq: List<Float>,
    usage: List<Float?>,
    modifier: Modifier = Modifier,
    colors: ChartColors,
) {
    val freqMax = freq.maxOrNull() ?: 0f
    val freqStep = 100f
    val freqUpper = ((freqMax / freqStep).toInt() + 1).coerceAtLeast(1) * freqStep
    val usageSteps = listOf(0f, 50f, 75f, 90f, 100f)
    val steps = 5
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val leftPad = 70f
        val rightPad = 58f
        val topPad = 8f
        val bottomPad = 26f
        val plotW = (w - leftPad - rightPad).coerceAtLeast(1f)
        val plotH = (h - topPad - bottomPad).coerceAtLeast(1f)

        for (i in 0..steps) {
            val y = topPad + plotH * i / steps
            drawLine(
                gridColor,
                start = Offset(leftPad, y),
                end = Offset(leftPad + plotW, y),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f),
            )
            val value = freqUpper - (freqUpper * i / steps)
            drawContext.canvas.nativeCanvas.drawText(
                formatAxis(value),
                leftPad - 10f,
                y - 6f,
                android.graphics.Paint().apply {
                    setColor(Color(0xFF86D3FF).toArgb())
                    textSize = 22f
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.RIGHT
                }
            )
        }

        usageSteps.forEach { u ->
            val y = topPad + plotH - (u / 100f) * plotH
            drawContext.canvas.nativeCanvas.drawText(
                formatAxis(u),
                leftPad + plotW + 8f,
                y - 6f,
                android.graphics.Paint().apply {
                    setColor(Color(0xFFFF8626).toArgb())
                    textSize = 22f
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.LEFT
                }
            )
        }

        val xSteps = 4
        for (i in 0..xSteps) {
            val x = leftPad + plotW * i / xSteps
            drawLine(
                gridColor,
                start = Offset(x, topPad),
                end = Offset(x, topPad + plotH),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f),
            )
            val label = formatTimeLabel(i, xSteps, freq.size)
            drawContext.canvas.nativeCanvas.drawText(
                label,
                x,
                topPad + plotH + 24f,
                android.graphics.Paint().apply {
                    setColor(labelColor.toArgb())
                    textSize = 22f
                    isAntiAlias = true
                    textAlign =
                        when (i) {
                            0 -> android.graphics.Paint.Align.LEFT
                            xSteps -> android.graphics.Paint.Align.RIGHT
                            else -> android.graphics.Paint.Align.CENTER
                        }
                }
            )
        }

        if (freq.size > 1) {
            val path = Path()
            freq.forEachIndexed { index, value ->
                val x = leftPad + plotW * index / (freq.size - 1).coerceAtLeast(1)
                val y = topPad + plotH - (value / freqUpper).coerceIn(0f, 1f) * plotH
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color = colors.blue, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.2f))
        }

        if (usage.isNotEmpty()) {
            val path = Path()
            var hasMove = false
            usage.forEachIndexed { index, value ->
                val v = value ?: run {
                    hasMove = false
                    return@forEachIndexed
                }
                val x = leftPad + plotW * index / (usage.size - 1).coerceAtLeast(1)
                val y = topPad + plotH - (v.coerceIn(0f, 100f) / 100f) * plotH
                if (!hasMove) {
                    path.moveTo(x, y)
                    hasMove = true
                } else {
                    path.lineTo(x, y)
                }
            }
            if (hasMove) {
                drawPath(path, color = colors.orange, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.2f))
            }
        }
    }
}

@Composable
private fun PowerCapacityDualAxisChart(
    power: List<Float>,
    capacity: List<Float?>,
    modifier: Modifier = Modifier,
    colors: ChartColors,
) {
    val powerMax = power.maxOrNull() ?: 0f
    val powerUpper = ceil(powerMax.toDouble()).toFloat().coerceAtLeast(1f)
    val steps = 5
    val capacitySteps = listOf(0f, 20f, 40f, 60f, 80f, 100f)
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val leftPad = 70f
        val rightPad = 58f
        val topPad = 8f
        val bottomPad = 26f
        val plotW = (w - leftPad - rightPad).coerceAtLeast(1f)
        val plotH = (h - topPad - bottomPad).coerceAtLeast(1f)

        for (i in 0..steps) {
            val y = topPad + plotH * i / steps
            drawLine(
                gridColor,
                start = Offset(leftPad, y),
                end = Offset(leftPad + plotW, y),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f),
            )
            val value = powerUpper - (powerUpper * i / steps)
            drawContext.canvas.nativeCanvas.drawText(
                formatAxis(value),
                leftPad - 10f,
                y - 6f,
                android.graphics.Paint().apply {
                    setColor(colors.blue.toArgb())
                    textSize = 22f
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.RIGHT
                }
            )
        }

        capacitySteps.forEach { u ->
            val y = topPad + plotH - (u / 100f) * plotH
            drawContext.canvas.nativeCanvas.drawText(
                formatAxis(u),
                leftPad + plotW + 8f,
                y - 6f,
                android.graphics.Paint().apply {
                    setColor(colors.orange.toArgb())
                    textSize = 22f
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.LEFT
                }
            )
        }

        val xSteps = 4
        for (i in 0..xSteps) {
            val x = leftPad + plotW * i / xSteps
            drawLine(
                gridColor,
                start = Offset(x, topPad),
                end = Offset(x, topPad + plotH),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f),
            )
            val label = formatTimeLabel(i, xSteps, power.size)
            drawContext.canvas.nativeCanvas.drawText(
                label,
                x,
                topPad + plotH + 24f,
                android.graphics.Paint().apply {
                    setColor(labelColor.toArgb())
                    textSize = 22f
                    isAntiAlias = true
                    textAlign =
                        when (i) {
                            0 -> android.graphics.Paint.Align.LEFT
                            xSteps -> android.graphics.Paint.Align.RIGHT
                            else -> android.graphics.Paint.Align.CENTER
                        }
                }
            )
        }

        if (power.size > 1) {
            val path = Path()
            power.forEachIndexed { index, value ->
                val x = leftPad + plotW * index / (power.size - 1).coerceAtLeast(1)
                val y = topPad + plotH - (value / powerUpper) * plotH
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color = colors.blue, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.2f))
        }

        if (capacity.isNotEmpty()) {
            val path = Path()
            var hasMove = false
            capacity.forEachIndexed { index, value ->
                val v = value ?: run {
                    hasMove = false
                    return@forEachIndexed
                }
                val x = leftPad + plotW * index / (capacity.size - 1).coerceAtLeast(1)
                val y = topPad + plotH - (v.coerceIn(0f, 100f) / 100f) * plotH
                if (!hasMove) {
                    path.moveTo(x, y)
                    hasMove = true
                } else {
                    path.lineTo(x, y)
                }
            }
            if (hasMove) {
                drawPath(path, color = colors.orange, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.2f))
            }
        }
    }
}

data class ChartColors(
    val blue: Color,
    val orange: Color,
    val cyan: Color
)

@Composable
private fun rememberChartColors(): ChartColors {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return remember(isDark) {
        if (isDark) {
            ChartColors(
                blue = Color(0xFF58A6FF),
                orange = Color(0xFFFF8626),
                cyan = Color(0xFF3FB950)
            )
        } else {
            ChartColors(
                blue = Color(0xFF0969DA),
                orange = Color(0xFF9A6700),
                cyan = Color(0xFF1A7F37)
            )
        }
    }
}

@Composable
private fun AreaChart(
    values: List<Float?>,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val points = values.filterNotNull()
    if (points.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No data", color = Color(0xFF8B949E))
        }
        return
    }
    val maxY = points.maxOrNull() ?: 1f
    val steps = 6
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val leftPad = 44f
        val rightPad = 16f
        val topPad = 8f
        val bottomPad = 26f
        val plotW = (w - leftPad - rightPad).coerceAtLeast(1f)
        val plotH = (h - topPad - bottomPad).coerceAtLeast(1f)
        for (i in 0..steps) {
            val y = topPad + plotH * i / steps
            drawLine(gridColor, start = Offset(leftPad, y), end = Offset(leftPad + plotW, y), strokeWidth = 1f, pathEffect = dash)
            val value = maxY - (maxY * i / steps)
            drawContext.canvas.nativeCanvas.drawText(
                formatAxis(value),
                leftPad - 6f,
                y - 6f,
                android.graphics.Paint().apply {
                    setColor(labelColor.toArgb())
                    textSize = 22f
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.RIGHT
                }
            )
        }
        val xSteps = 4
        for (i in 0..xSteps) {
            val x = leftPad + plotW * i / xSteps
            drawLine(gridColor, start = Offset(x, topPad), end = Offset(x, topPad + plotH), strokeWidth = 1f, pathEffect = dash)
            val label = formatTimeLabel(i, xSteps, values.size)
            drawContext.canvas.nativeCanvas.drawText(
                label,
                x,
                topPad + plotH + 24f,
                android.graphics.Paint().apply {
                    setColor(labelColor.toArgb())
                    textSize = 22f
                    isAntiAlias = true
                    textAlign =
                        when (i) {
                            0 -> android.graphics.Paint.Align.LEFT
                            xSteps -> android.graphics.Paint.Align.RIGHT
                            else -> android.graphics.Paint.Align.CENTER
                        }
                }
            )
        }

        val path = Path()
        val fillPath = Path()
        values.filterNotNull().forEachIndexed { index, value ->
            val x = leftPad + plotW * index / (values.size - 1).coerceAtLeast(1)
            val y = topPad + plotH - (value / maxY) * plotH
            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, topPad + plotH)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo(leftPad + plotW, topPad + plotH)
        fillPath.close()
        drawPath(fillPath, color = color.copy(alpha = 0.35f))
        drawPath(path, color = color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.2f))
    }
}

@Composable
private fun FixedAxisLineChart(
    series: List<ChartSeries>,
    yMin: Float,
    yMax: Float,
    yStep: Float,
    modifier: Modifier = Modifier,
) {
    if (series.isEmpty()) return
    val samplesCount = series.maxOfOrNull { it.values.size } ?: 0
    if (samplesCount == 0) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No data", color = Color(0xFF8B949E))
        }
        return
    }
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val leftPad = 44f
        val rightPad = 16f
        val topPad = 8f
        val bottomPad = 26f
        val plotW = (w - leftPad - rightPad).coerceAtLeast(1f)
        val plotH = (h - topPad - bottomPad).coerceAtLeast(1f)
        val steps = ((yMax - yMin) / yStep).toInt().coerceAtLeast(1)
        for (i in 0..steps) {
            val y = topPad + plotH * i / steps
            drawLine(gridColor, start = Offset(leftPad, y), end = Offset(leftPad + plotW, y), strokeWidth = 1f, pathEffect = dash)
            val value = yMax - yStep * i
            drawContext.canvas.nativeCanvas.drawText(
                formatAxis(value),
                leftPad - 6f,
                y - 6f,
                android.graphics.Paint().apply {
                    setColor(labelColor.toArgb())
                    textSize = 22f
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.RIGHT
                }
            )
        }
        val xSteps = 4
        for (i in 0..xSteps) {
            val x = leftPad + plotW * i / xSteps
            drawLine(gridColor, start = Offset(x, topPad), end = Offset(x, topPad + plotH), strokeWidth = 1f, pathEffect = dash)
            val label = formatTimeLabel(i, xSteps, samplesCount)
            drawContext.canvas.nativeCanvas.drawText(
                label,
                x,
                topPad + plotH + 24f,
                android.graphics.Paint().apply {
                    setColor(labelColor.toArgb())
                    textSize = 22f
                    isAntiAlias = true
                    textAlign =
                        when (i) {
                            0 -> android.graphics.Paint.Align.LEFT
                            xSteps -> android.graphics.Paint.Align.RIGHT
                            else -> android.graphics.Paint.Align.CENTER
                        }
                }
            )
        }

        series.forEach { s ->
            val values = s.values
            if (values.size <= 1) return@forEach
            val path = Path()
            var hasStarted = false
            values.forEachIndexed { index, value ->
                val v = value ?: return@forEachIndexed
                val x = leftPad + plotW * index / (values.size - 1).coerceAtLeast(1)
                val clamped = v.coerceIn(yMin, yMax)
                val y = topPad + plotH - ((clamped - yMin) / (yMax - yMin)) * plotH
                if (!hasStarted) {
                    path.moveTo(x, y)
                    hasStarted = true
                } else {
                    path.lineTo(x, y)
                }
            }
            if (hasStarted) {
                drawPath(path, color = s.color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.2f))
            }
        }
    }
}

private fun averageSlowestPercent(sortedAscendingFps: List<Float>, percent: Float): Float {
    if (sortedAscendingFps.isEmpty()) return 0f
    val sampleCount = ceil(sortedAscendingFps.size * percent).toInt().coerceAtLeast(1)
    return sortedAscendingFps.take(sampleCount).average().toFloat()
}

private fun loadSessions(pm: android.content.pm.PackageManager): List<FpsSessionItem> {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val reader = PerAppLogReader()

    return PerAppLogManager.getInstance().getAllPerAppLogFiles()
        .flatMap { (packageName, files) ->
            files.map { file ->
                val appName = try {
                    val appInfo = pm.getApplicationInfo(packageName, 0)
                    pm.getApplicationLabel(appInfo).toString()
                } catch (_: Exception) {
                    packageName
                }

                val analytics = runCatching { reader.analyzeLogFile(file.absolutePath) }.getOrNull()
                val avgFps = analytics?.fpsStats?.avgFps?.toFloat() ?: 0f
                val avgPower = analytics?.powerStats?.avgPower?.toFloat() ?: 0f
                val duration = analytics?.sessionDuration ?: "--"

                FpsSessionItem(
                    packageName = packageName,
                    appName = appName,
                    file = file,
                    dateText = dateFormat.format(Date(file.lastModified())),
                    avgFps = avgFps,
                    avgPower = avgPower,
                    duration = duration
                )
            }
        }
        .sortedByDescending { it.file.lastModified() }
}
