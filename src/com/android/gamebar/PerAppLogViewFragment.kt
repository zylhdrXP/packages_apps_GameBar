/*
 * SPDX-FileCopyrightText: 2025 kenway214
 * SPDX-License-Identifier: Apache-2.0
 */


package com.android.gamebar

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.gamebar.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class PerAppLogViewFragment : Fragment() {

    private lateinit var searchBar: EditText
    private lateinit var logHistoryRecyclerView: RecyclerView
    private lateinit var logHistoryAdapter: LogHistoryAdapter
    private lateinit var emptyStateView: View
    private lateinit var emptyMessageView: TextView
    private val logFiles = mutableListOf<GameBarLogFragment.LogFile>()
    private val allLogFiles = mutableListOf<GameBarLogFragment.LogFile>()
    private val perAppLogManager = PerAppLogManager.getInstance()
    
    private var packageName: String = ""
    private var appName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        packageName = arguments?.getString(PerAppLogViewActivity.EXTRA_PACKAGE_NAME) ?: ""
        appName = arguments?.getString(PerAppLogViewActivity.EXTRA_APP_NAME) ?: "Unknown App"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_per_app_log_view, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        try {
            initViews(view)
            setupRecyclerView()
            setupSearchBar()
            loadPerAppLogHistory()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), getString(R.string.toast_error_loading_logs, e.message), Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun initViews(view: View) {
        searchBar = view.findViewById(R.id.search_bar)
        logHistoryRecyclerView = view.findViewById(R.id.rv_log_history)
        emptyStateView = view.findViewById(R.id.empty_state)
        emptyMessageView = view.findViewById(R.id.tv_empty_message)
    }

    private fun setupRecyclerView() {
        logHistoryAdapter = LogHistoryAdapter(logFiles) { logFile, view ->
            showLogFilePopupMenu(logFile, view)
        }
        logHistoryRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = logHistoryAdapter
        }
    }

    private fun setupSearchBar() {
        searchBar.hint = "Search $appName logs..."
        searchBar.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterLogs(s.toString().lowercase())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun filterLogs(query: String) {
        logFiles.clear()
        if (query.isEmpty()) {
            logFiles.addAll(allLogFiles)
        } else {
            allLogFiles.forEach { logFile ->
                if (logFile.name.lowercase().contains(query) || 
                    logFile.lastModified.lowercase().contains(query)) {
                    logFiles.add(logFile)
                }
            }
        }
        logHistoryAdapter.notifyDataSetChanged()
        updateEmptyState()
    }

    private fun updateEmptyState() {
        if (logFiles.isEmpty()) {
            logHistoryRecyclerView.visibility = View.GONE
            emptyStateView.visibility = View.VISIBLE
            emptyMessageView.text = "No logs available for $appName"
        } else {
            logHistoryRecyclerView.visibility = View.VISIBLE
            emptyStateView.visibility = View.GONE
        }
    }

    private fun loadPerAppLogHistory() {
        logFiles.clear()
        allLogFiles.clear()
        
        val files = perAppLogManager.getPerAppLogFiles(packageName)
        
        for (file in files) {
            val logFile = GameBarLogFragment.LogFile(
                name = file.name,
                path = file.absolutePath,
                size = formatFileSize(file.length()),
                lastModified = formatDate(file.lastModified())
            )
            allLogFiles.add(logFile)
            logFiles.add(logFile)
        }
        
        logHistoryAdapter.notifyDataSetChanged()
        updateEmptyState()
    }

    private fun showLogFilePopupMenu(logFile: GameBarLogFragment.LogFile, anchorView: View) {
        val popupMenu = PopupMenu(requireContext(), anchorView)
        popupMenu.menuInflater.inflate(R.menu.log_file_popup_menu, popupMenu.menu)
        
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_open -> {
                    openLogFile(logFile)
                    true
                }
                R.id.menu_share -> {
                    shareLogFile(logFile)
                    true
                }
                R.id.menu_export -> {
                    exportLogFile(logFile)
                    true
                }
                R.id.menu_delete -> {
                    deleteLogFile(logFile)
                    true
                }
                else -> false
            }
        }
        
        popupMenu.show()
    }

    private fun openLogFile(logFile: GameBarLogFragment.LogFile) {
        // Show analytics popup
        showLogAnalyticsDialog(logFile)
    }
    
    private fun showLogAnalyticsDialog(logFile: GameBarLogFragment.LogFile) {
        // Show loading message
        val loadingDialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_title_analyzing_log)
            .setMessage(R.string.dialog_message_analyzing)
            .setCancelable(false)
            .create()
        loadingDialog.show()
        
        // Analyze log file in background thread
        Thread {
            val logReader = PerAppLogReader()
            val analytics = logReader.analyzeLogFile(logFile.path)
            
            // Update UI on main thread
            requireActivity().runOnUiThread {
                loadingDialog.dismiss()
                
                if (analytics != null) {
                    showAnalyticsResult(logFile, analytics)
                } else {
                    Toast.makeText(requireContext(), R.string.toast_failed_analyze_log, Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
    
    private fun showAnalyticsResult(logFile: GameBarLogFragment.LogFile, analytics: LogAnalytics) {
        // Inflate custom dialog layout
        val dialogView = layoutInflater.inflate(R.layout.dialog_log_analytics, null)
        
        // Get views
        val sessionInfoText = dialogView.findViewById<TextView>(R.id.tv_session_info)
        val fpsGraphView = dialogView.findViewById<FpsGraphView>(R.id.fps_graph_view)
        val frameTimeGraphView = dialogView.findViewById<FrameTimeGraphView>(R.id.frame_time_graph_view)
        val maxFpsText = dialogView.findViewById<TextView>(R.id.tv_max_fps)
        val minFpsText = dialogView.findViewById<TextView>(R.id.tv_min_fps)
        val avgFpsText = dialogView.findViewById<TextView>(R.id.tv_avg_fps)
        val varianceText = dialogView.findViewById<TextView>(R.id.tv_variance)
        val stdDevText = dialogView.findViewById<TextView>(R.id.tv_std_dev)
        val smoothnessText = dialogView.findViewById<TextView>(R.id.tv_smoothness)
        val fps1PercentText = dialogView.findViewById<TextView>(R.id.tv_1percent_low)
        val fps01PercentText = dialogView.findViewById<TextView>(R.id.tv_01percent_low)
        
        // Set session info
        sessionInfoText.text = buildString {
            appendLine("📅 ${analytics.sessionDate}")
            appendLine("⏱️  ${analytics.sessionDuration}")
            appendLine("📊 ${analytics.totalSamples} samples")
            append("📁 ${logFile.name}")
        }
        
        // Set FPS statistics
        maxFpsText.text = String.format("Max FPS:      %.1f", analytics.fpsStats.maxFps)
        minFpsText.text = String.format("Min FPS:      %.1f", analytics.fpsStats.minFps)
        avgFpsText.text = String.format("Avg FPS:      %.1f", analytics.fpsStats.avgFps)
        varianceText.text = String.format("Variance:     %.2f", analytics.fpsStats.variance)
        stdDevText.text = String.format("Std Dev:      %.2f", analytics.fpsStats.standardDeviation)
        smoothnessText.text = String.format("Smoothness:   %.1f%%", analytics.fpsStats.smoothnessPercentage)
        fps1PercentText.text = String.format("1%% Low:       %.1f FPS", analytics.fpsStats.fps1PercentLow)
        fps01PercentText.text = String.format("0.1%% Low:     %.1f FPS", analytics.fpsStats.fps0_1PercentLow)
        
        // Set FPS graph data
        fpsGraphView.setData(
            analytics.fpsTimeData,
            analytics.fpsStats.avgFps,
            analytics.fpsStats.fps1PercentLow
        )
        
        // Set Frame Time graph data
        val avgFrameTime = if (analytics.fpsStats.avgFps > 0) {
            1000.0 / analytics.fpsStats.avgFps
        } else {
            0.0
        }
        frameTimeGraphView.setData(analytics.frameTimeData, avgFrameTime)
        
        // Get CPU graph views
        val cpuUsageGraphView = dialogView.findViewById<CpuGraphView>(R.id.cpu_usage_graph_view)
        val cpuTempGraphView = dialogView.findViewById<CpuTempGraphView>(R.id.cpu_temp_graph_view)
        val cpuClockGraphView = dialogView.findViewById<CpuClockGraphView>(R.id.cpu_clock_graph_view)
        
        // Get CPU statistics views
        val maxCpuUsageText = dialogView.findViewById<TextView>(R.id.tv_max_cpu_usage)
        val minCpuUsageText = dialogView.findViewById<TextView>(R.id.tv_min_cpu_usage)
        val avgCpuUsageText = dialogView.findViewById<TextView>(R.id.tv_avg_cpu_usage)
        val maxCpuTempText = dialogView.findViewById<TextView>(R.id.tv_max_cpu_temp)
        val minCpuTempText = dialogView.findViewById<TextView>(R.id.tv_min_cpu_temp)
        val avgCpuTempText = dialogView.findViewById<TextView>(R.id.tv_avg_cpu_temp)
        
        // Set CPU graph data
        cpuUsageGraphView.setData(analytics.cpuUsageTimeData, analytics.cpuStats.avgUsage)
        cpuTempGraphView.setData(analytics.cpuTempTimeData, analytics.cpuStats.avgTemp)
        cpuClockGraphView.setData(analytics.cpuClockTimeData)
        
        // Set CPU statistics
        maxCpuUsageText.text = String.format("Max Usage:    %.0f%%", analytics.cpuStats.maxUsage)
        minCpuUsageText.text = String.format("Min Usage:    %.0f%%", analytics.cpuStats.minUsage)
        avgCpuUsageText.text = String.format("Avg Usage:    %.1f%%", analytics.cpuStats.avgUsage)
        maxCpuTempText.text = String.format("Max Temp:     %.1f°C", analytics.cpuStats.maxTemp)
        minCpuTempText.text = String.format("Min Temp:     %.1f°C", analytics.cpuStats.minTemp)
        avgCpuTempText.text = String.format("Avg Temp:     %.1f°C", analytics.cpuStats.avgTemp)
        
        // Get GPU graph views
        val gpuUsageGraphView = dialogView.findViewById<GpuUsageGraphView>(R.id.gpu_usage_graph_view)
        val gpuTempGraphView = dialogView.findViewById<GpuTempGraphView>(R.id.gpu_temp_graph_view)
        val gpuClockGraphView = dialogView.findViewById<GpuClockGraphView>(R.id.gpu_clock_graph_view)
        
        // Get GPU statistics views
        val maxGpuUsageText = dialogView.findViewById<TextView>(R.id.tv_max_gpu_usage)
        val minGpuUsageText = dialogView.findViewById<TextView>(R.id.tv_min_gpu_usage)
        val avgGpuUsageText = dialogView.findViewById<TextView>(R.id.tv_avg_gpu_usage)
        val maxGpuClockText = dialogView.findViewById<TextView>(R.id.tv_max_gpu_clock)
        val minGpuClockText = dialogView.findViewById<TextView>(R.id.tv_min_gpu_clock)
        val avgGpuClockText = dialogView.findViewById<TextView>(R.id.tv_avg_gpu_clock)
        val maxGpuTempText = dialogView.findViewById<TextView>(R.id.tv_max_gpu_temp)
        val minGpuTempText = dialogView.findViewById<TextView>(R.id.tv_min_gpu_temp)
        val avgGpuTempText = dialogView.findViewById<TextView>(R.id.tv_avg_gpu_temp)
        
        // Set GPU graph data
        gpuUsageGraphView.setData(analytics.gpuUsageTimeData, analytics.gpuStats.avgUsage)
        gpuTempGraphView.setData(analytics.gpuTempTimeData, analytics.gpuStats.avgTemp)
        gpuClockGraphView.setData(analytics.gpuClockTimeData, analytics.gpuStats.avgClock)
        
        // Set GPU statistics
        maxGpuUsageText.text = String.format("Max Usage:    %.0f%%", analytics.gpuStats.maxUsage)
        minGpuUsageText.text = String.format("Min Usage:    %.0f%%", analytics.gpuStats.minUsage)
        avgGpuUsageText.text = String.format("Avg Usage:    %.1f%%", analytics.gpuStats.avgUsage)
        maxGpuClockText.text = String.format("Max Clock:    %.0f MHz", analytics.gpuStats.maxClock)
        minGpuClockText.text = String.format("Min Clock:    %.0f MHz", analytics.gpuStats.minClock)
        avgGpuClockText.text = String.format("Avg Clock:    %.0f MHz", analytics.gpuStats.avgClock)
        maxGpuTempText.text = String.format("Max Temp:     %.1f°C", analytics.gpuStats.maxTemp)
        minGpuTempText.text = String.format("Min Temp:     %.1f°C", analytics.gpuStats.minTemp)
        avgGpuTempText.text = String.format("Avg Temp:     %.1f°C", analytics.gpuStats.avgTemp)
        
        // Create and show dialog with menu
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_title_session_analytics)
            .setView(dialogView)
            .setPositiveButton(R.string.button_actions) { _, _ ->
                // Will be overridden
            }
            .setNegativeButton(R.string.button_close, null)
            .create()
        
        dialog.show()
        
        // Override the Actions button to show menu
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            showActionsMenu(it, dialogView, analytics, logFile)
        }
        
        // Make sure dialog is wide enough
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.95).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
    
    private fun showActionsMenu(
        anchorView: View,
        dialogView: View,
        analytics: LogAnalytics,
        logFile: GameBarLogFragment.LogFile
    ) {
        val popup = android.widget.PopupMenu(requireContext(), anchorView)
        popup.menu.apply {
            add(0, 1, 1, "📊 Export Data (CSV)")
            add(0, 2, 2, "📸 Save Graphics (PNG)")
            add(0, 3, 3, "📤 Share Data (CSV)")
            add(0, 4, 4, "🖼️ Share Graphics (PNG)")
        }
        
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    exportLogFile(logFile)
                    true
                }
                2 -> {
                    saveGraphicsAsImage(dialogView, analytics, logFile)
                    true
                }
                3 -> {
                    shareLogFile(logFile)
                    true
                }
                4 -> {
                    shareGraphicsAsImage(dialogView, analytics, logFile)
                    true
                }
                else -> false
            }
        }
        
        popup.show()
    }

    private fun saveGraphicsAsImage(view: View, analytics: LogAnalytics, logFile: GameBarLogFragment.LogFile) {
        try {
            // Ensure view is properly measured and laid out
            view.post {
                try {
                    // Create bitmap from view
                    val bitmap = captureViewAsBitmap(view)
                    
                    // Save to external storage
                    val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                    val fileName = "GameBar_Stats_${appName}_$timeStamp.png"
                    val file = File(android.os.Environment.getExternalStorageDirectory(), fileName)
                    
                    val fos = java.io.FileOutputStream(file)
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, fos)
                    fos.flush()
                    fos.close()
                    
                    Toast.makeText(requireContext(), getString(R.string.toast_saved_file, file.absolutePath), Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), getString(R.string.toast_failed_save, e.message), Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), getString(R.string.toast_error_generic, e.message), Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
    
    private fun shareGraphicsAsImage(view: View, analytics: LogAnalytics, logFile: GameBarLogFragment.LogFile) {
        try {
            // Ensure view is properly measured and laid out
            view.post {
                try {
                    // Create bitmap from view
                    val bitmap = captureViewAsBitmap(view)
                    
                    // Save to cache directory
                    val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                    val fileName = "GameBar_Stats_${appName}_$timeStamp.png"
                    val file = File(requireContext().cacheDir, fileName)
                    
                    val fos = java.io.FileOutputStream(file)
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, fos)
                    fos.flush()
                    fos.close()
                    
                    view.isDrawingCacheEnabled = false
                    
                    // Share the image
                    val uri = FileProvider.getUriForFile(
                        requireContext(),
                        "${requireContext().packageName}.fileprovider",
                        file
                    )
                    
                    val intent = Intent(Intent.ACTION_SEND)
                    intent.type = "image/png"
                    intent.putExtra(Intent.EXTRA_STREAM, uri)
                    intent.putExtra(Intent.EXTRA_SUBJECT, "GameBar Performance Stats - $appName")
                    intent.putExtra(Intent.EXTRA_TEXT, buildString {
                        appendLine("🎮 GameBar Performance Stats")
                        appendLine("")
                        appendLine("📱 App: $appName")
                        appendLine("📅 Session: ${analytics.sessionDate}")
                        appendLine("⏱️ Duration: ${analytics.sessionDuration}")
                        appendLine("")
                        appendLine("🎯 FPS: Avg ${String.format("%.1f", analytics.fpsStats.avgFps)} | Max ${String.format("%.1f", analytics.fpsStats.maxFps)}")
                        appendLine("✨ Smoothness: ${String.format("%.1f%%", analytics.fpsStats.smoothnessPercentage)}")
                        appendLine("🔥 1% Low: ${String.format("%.1f", analytics.fpsStats.fps1PercentLow)} FPS")
                    })
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    
                    val chooser = Intent.createChooser(intent, getString(R.string.chooser_share_graphics))
                    startActivity(chooser)
                    
                    Toast.makeText(requireContext(), R.string.toast_graphics_ready, Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), getString(R.string.toast_failed_share, e.message), Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), getString(R.string.toast_error_generic, e.message), Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
    
    private fun captureViewAsBitmap(view: View): android.graphics.Bitmap {
        // The root view of the dialog is a ScrollView, get its content
        val contentView = if (view is android.widget.ScrollView) {
            // Get the LinearLayout inside the ScrollView
            view.getChildAt(0)
        } else {
            view
        }
        
        // Measure the full content size (not just what's visible)
        contentView.measure(
            View.MeasureSpec.makeMeasureSpec(contentView.width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        
        // Create bitmap with full content dimensions
        val bitmap = android.graphics.Bitmap.createBitmap(
            contentView.width,
            contentView.measuredHeight,
            android.graphics.Bitmap.Config.ARGB_8888
        )
        
        val canvas = android.graphics.Canvas(bitmap)
        
        // Draw the entire content including off-screen parts
        contentView.layout(
            contentView.left,
            contentView.top,
            contentView.right,
            contentView.bottom + contentView.measuredHeight
        )
        contentView.draw(canvas)
        
        return bitmap
    }
    
    private fun shareLogFile(logFile: GameBarLogFragment.LogFile) {
        try {
            val intent = Intent(Intent.ACTION_SEND)
            val file = File(logFile.path)
            
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )
            
            intent.type = "text/csv"
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            intent.putExtra(Intent.EXTRA_SUBJECT, "GameBar Performance Log for $appName")
            intent.putExtra(Intent.EXTRA_TEXT, "GameBar performance log file for $appName: ${logFile.name}")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            
            val chooser = Intent.createChooser(intent, getString(R.string.chooser_share_log))
            startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), getString(R.string.toast_file_location, logFile.path), Toast.LENGTH_LONG).show()
        }
    }

    private fun exportLogFile(logFile: GameBarLogFragment.LogFile) {
        Toast.makeText(requireContext(), getString(R.string.toast_file_saved_at, logFile.path), Toast.LENGTH_LONG).show()
    }

    private fun deleteLogFile(logFile: GameBarLogFragment.LogFile) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_title_delete_log)
            .setMessage(getString(R.string.dialog_message_delete_log, logFile.name))
            .setPositiveButton(R.string.button_delete) { _, _ ->
                try {
                    val file = File(logFile.path)
                    if (file.delete()) {
                        Toast.makeText(requireContext(), R.string.toast_log_deleted, Toast.LENGTH_SHORT).show()
                        loadPerAppLogHistory() // Refresh the list
                    } else {
                        Toast.makeText(requireContext(), R.string.toast_log_delete_failed, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), getString(R.string.toast_error_deleting_file, e.message), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }

    private fun formatFileSize(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        
        return when {
            mb >= 1 -> String.format("%.1f MB", mb)
            kb >= 1 -> String.format("%.1f KB", kb)
            else -> "$bytes bytes"
        }
    }

    private fun formatDate(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }

    override fun onResume() {
        super.onResume()
        loadPerAppLogHistory()
    }
}
