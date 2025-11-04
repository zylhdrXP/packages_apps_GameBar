/*
 * SPDX-FileCopyrightText: 2025 kenway214
 * SPDX-License-Identifier: Apache-2.0
 */


package com.android.gamebar

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.gamebar.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class GameBarLogFragment : Fragment(), GameDataExport.CaptureStateListener, PerAppLogManager.PerAppStateListener {

    private lateinit var searchBar: EditText
    private lateinit var startCaptureButton: Button
    private lateinit var stopCaptureButton: Button
    private lateinit var manualLogsButton: Button
    private lateinit var logTypeRadioGroup: RadioGroup
    private lateinit var globalLoggingRadio: RadioButton
    private lateinit var perAppLoggingRadio: RadioButton
    private lateinit var logHistoryRecyclerView: RecyclerView
    private lateinit var logHistoryAdapter: LogHistoryAdapter
    private lateinit var perAppLogAdapter: PerAppLogAdapter
    
    private val gameDataExport = GameDataExport.getInstance()
    private val perAppLogManager = PerAppLogManager.getInstance()
    private val logFiles = mutableListOf<LogFile>()
    private val allLogFiles = mutableListOf<LogFile>()
    private val installedApps = mutableListOf<ApplicationInfo>()
    private val filteredApps = mutableListOf<ApplicationInfo>()
    
    private var currentLoggingMode = GameDataExport.LoggingMode.GLOBAL
    
    companion object {
        private const val PREF_LOGGING_MODE = "gamebar_logging_mode"
        
        // Logging parameter preference keys
        const val PREF_LOG_FPS = "gamebar_log_fps"
        const val PREF_LOG_FRAME_TIME = "gamebar_log_frame_time"
        const val PREF_LOG_BATTERY_TEMP = "gamebar_log_battery_temp"
        const val PREF_LOG_CPU_USAGE = "gamebar_log_cpu_usage"
        const val PREF_LOG_CPU_CLOCK = "gamebar_log_cpu_clock"
        const val PREF_LOG_CPU_TEMP = "gamebar_log_cpu_temp"
        const val PREF_LOG_RAM = "gamebar_log_ram"
        const val PREF_LOG_RAM_SPEED = "gamebar_log_ram_speed"
        const val PREF_LOG_RAM_TEMP = "gamebar_log_ram_temp"
        const val PREF_LOG_GPU_USAGE = "gamebar_log_gpu_usage"
        const val PREF_LOG_GPU_CLOCK = "gamebar_log_gpu_clock"
        const val PREF_LOG_GPU_TEMP = "gamebar_log_gpu_temp"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_gamebar_log, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        try {
            initViews(view)
            loadSavedLoggingMode() // Load saved mode first
            setupRecyclerView()
            setupButtonListeners()
            loadInstalledApps()
            initializeUIState() // Initialize UI based on saved mode
            updateButtonStates()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), getString(R.string.toast_error_initializing, e.message), Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun initViews(view: View) {
        searchBar = view.findViewById(R.id.search_bar)
        startCaptureButton = view.findViewById(R.id.btn_start_capture)
        stopCaptureButton = view.findViewById(R.id.btn_stop_capture)
        manualLogsButton = view.findViewById(R.id.btn_manual_logs)
        logTypeRadioGroup = view.findViewById(R.id.rg_log_type)
        globalLoggingRadio = view.findViewById(R.id.rb_global_logging)
        perAppLoggingRadio = view.findViewById(R.id.rb_per_app_logging)
        logHistoryRecyclerView = view.findViewById(R.id.rv_log_history)
    }

    private fun setupRecyclerView() {
        // Setup global log history adapter
        logHistoryAdapter = LogHistoryAdapter(logFiles) { logFile, view ->
            showLogFilePopupMenu(logFile, view)
        }
        
        // Setup per-app log adapter
        perAppLogAdapter = PerAppLogAdapter(
            requireContext(),
            filteredApps,
            onSwitchChanged = { packageName, enabled ->
                perAppLogManager.setAppLoggingEnabled(requireContext(), packageName, enabled)
                updatePerAppAdapterStates()
            },
            onViewLogsClicked = { packageName, appName ->
                val intent = Intent(requireContext(), PerAppLogViewActivity::class.java).apply {
                    putExtra(PerAppLogViewActivity.EXTRA_PACKAGE_NAME, packageName)
                    putExtra(PerAppLogViewActivity.EXTRA_APP_NAME, appName)
                }
                startActivity(intent)
            }
        )
        
        logHistoryRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = logHistoryAdapter // Start with global adapter
        }
    }

    private fun setupButtonListeners() {
        startCaptureButton.setOnClickListener {
            gameDataExport.startCapture()
            
            if (currentLoggingMode == GameDataExport.LoggingMode.PER_APP) {
                // Start dedicated PerAppLogService for logging-specific foreground monitoring
                requireContext().startService(Intent(requireContext(), PerAppLogService::class.java))
            }
            
            val message = if (currentLoggingMode == GameDataExport.LoggingMode.GLOBAL) {
                "Started global logging"
            } else {
                "Per-app logging enabled - logs will start automatically when enabled apps become active"
            }
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            updateButtonStates()
        }

        stopCaptureButton.setOnClickListener {
            if (gameDataExport.isCapturing()) {
                gameDataExport.stopCapture()
                if (currentLoggingMode == GameDataExport.LoggingMode.GLOBAL) {
                    gameDataExport.exportDataToCsv()
                    Toast.makeText(requireContext(), "Stopped logging and exported data", Toast.LENGTH_SHORT).show()
                    loadLogHistory()
                } else {
                    Toast.makeText(requireContext(), "Stopped per-app logging", Toast.LENGTH_SHORT).show()
                    updatePerAppAdapterStates()
                }
            }
            updateButtonStates()
        }
        
        manualLogsButton.setOnClickListener {
            showManualLogsDialog()
        }
        
        logTypeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rb_global_logging -> switchToGlobalMode()
                R.id.rb_per_app_logging -> switchToPerAppMode()
            }
        }
        
        searchBar.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                when (currentLoggingMode) {
                    GameDataExport.LoggingMode.GLOBAL -> filterLogs(s.toString().lowercase())
                    GameDataExport.LoggingMode.PER_APP -> filterApps(s.toString().lowercase())
                }
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
    }

    private fun updateButtonStates() {
        val isCapturing = gameDataExport.isCapturing()
        
        when (currentLoggingMode) {
            GameDataExport.LoggingMode.GLOBAL -> {
                startCaptureButton.isEnabled = !isCapturing
                stopCaptureButton.isEnabled = isCapturing
                manualLogsButton.visibility = View.GONE
                
                if (isCapturing) {
                    startCaptureButton.text = getString(R.string.button_capturing)
                } else {
                    startCaptureButton.text = getString(R.string.button_start_capture)
                }
            }
            GameDataExport.LoggingMode.PER_APP -> {
                val hasEnabledApps = perAppLogManager.getEnabledApps(requireContext()).isNotEmpty()
                val hasManualLogs = getManualLogPackages().isNotEmpty()
                
                startCaptureButton.isEnabled = !isCapturing && hasEnabledApps
                stopCaptureButton.isEnabled = isCapturing
                manualLogsButton.visibility = if (hasManualLogs) View.VISIBLE else View.GONE
                
                if (isCapturing) {
                    startCaptureButton.text = getString(R.string.button_per_app_active)
                } else {
                    startCaptureButton.text = if (hasEnabledApps) getString(R.string.button_enable_per_app) else getString(R.string.button_no_apps_enabled)
                }
            }
        }
    }
    
    private fun getManualLogPackages(): List<String> {
        val allLogFiles = perAppLogManager.getAllPerAppLogFiles()
        val enabledApps = perAppLogManager.getEnabledApps(requireContext())
        
        // Return packages that have log files but aren't in the enabled apps list
        return allLogFiles.keys.filter { packageName ->
            !enabledApps.contains(packageName) && packageName != "unknown"
        }
    }
    
    private fun showManualLogsDialog() {
        val manualLogPackages = getManualLogPackages()
        
        if (manualLogPackages.isEmpty()) {
            Toast.makeText(requireContext(), R.string.toast_no_manual_logs, Toast.LENGTH_SHORT).show()
            return
        }
        
        val pm = requireContext().packageManager
        val items = manualLogPackages.map { packageName ->
            try {
                val appInfo = pm.getApplicationInfo(packageName, 0)
                pm.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                packageName
            }
        }.toTypedArray()
        
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_title_manual_logs)
            .setItems(items) { _, which ->
                val packageName = manualLogPackages[which]
                val appName = items[which]
                
                // Open log view for this package
                val intent = Intent(requireContext(), PerAppLogViewActivity::class.java).apply {
                    putExtra(PerAppLogViewActivity.EXTRA_PACKAGE_NAME, packageName)
                    putExtra(PerAppLogViewActivity.EXTRA_APP_NAME, appName)
                }
                startActivity(intent)
            }
            .setNegativeButton(R.string.button_close, null)
            .show()
    }

    private fun loadLogHistory() {
        logFiles.clear()
        allLogFiles.clear()
        
        val externalStorageDir = Environment.getExternalStorageDirectory()
        val files = externalStorageDir.listFiles { file ->
            file.name.startsWith("GameBar_log_") && file.name.endsWith(".csv")
        }

        files?.let { fileArray ->
            fileArray.sortByDescending { it.lastModified() }
            for (file in fileArray) {
                val logFile = LogFile(
                    name = file.name,
                    path = file.absolutePath,
                    size = formatFileSize(file.length()),
                    lastModified = formatDate(file.lastModified())
                )
                allLogFiles.add(logFile)
                logFiles.add(logFile)
            }
        }
        
        logHistoryAdapter.notifyDataSetChanged()
    }

    private fun showLogFilePopupMenu(logFile: LogFile, anchorView: View) {
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

    private fun openLogFile(logFile: LogFile) {
        // Show analytics dialog for log file
        showLogAnalyticsDialog(logFile)
    }
    
    private fun showLogAnalyticsDialog(logFile: LogFile) {
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
            activity?.runOnUiThread {
                loadingDialog.dismiss()
                
                if (analytics != null) {
                    showAnalyticsInDialog(logFile, analytics)
                } else {
                    Toast.makeText(requireContext(), R.string.toast_failed_analyze_log, Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
    
    private fun showAnalyticsInDialog(logFile: LogFile, analytics: LogAnalytics) {
        // Open LogAnalyticsActivity with the file
        val intent = Intent(requireContext(), LogAnalyticsActivity::class.java).apply {
            putExtra(LogAnalyticsActivity.EXTRA_LOG_FILE_PATH, logFile.path)
            putExtra(LogAnalyticsActivity.EXTRA_LOG_FILE_NAME, logFile.name)
        }
        startActivity(intent)
    }

    private fun shareLogFile(logFile: LogFile) {
        try {
            val intent = Intent(Intent.ACTION_SEND)
            val file = File(logFile.path)
            
            // Use FileProvider to create content:// URI
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )
            
            intent.type = "text/csv"
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.intent_extra_subject))
            intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.intent_extra_text, logFile.name))
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            
            val chooser = Intent.createChooser(intent, getString(R.string.chooser_share_log))
            startActivity(chooser)
        } catch (e: Exception) {
            // Fallback with generic type if CSV doesn't work
            try {
                val intent = Intent(Intent.ACTION_SEND)
                val file = File(logFile.path)
                
                val uri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    file
                )
                
                intent.type = "text/plain"
                intent.putExtra(Intent.EXTRA_STREAM, uri)
                intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.intent_extra_subject))
                intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.intent_extra_text, logFile.name))
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                
                val chooser = Intent.createChooser(intent, getString(R.string.chooser_share_log))
                startActivity(chooser)
            } catch (e2: Exception) {
                Toast.makeText(requireContext(), getString(R.string.toast_file_location, logFile.path), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun exportLogFile(logFile: LogFile) {
        Toast.makeText(requireContext(), getString(R.string.toast_file_saved_at, logFile.path), Toast.LENGTH_LONG).show()
    }

    private fun deleteLogFile(logFile: LogFile) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_title_delete_log)
            .setMessage(getString(R.string.dialog_message_delete_log, logFile.name))
            .setPositiveButton(R.string.button_delete) { _, _ ->
                try {
                    val file = File(logFile.path)
                    if (file.delete()) {
                        Toast.makeText(requireContext(), R.string.toast_log_deleted, Toast.LENGTH_SHORT).show()
                        loadLogHistory() // Refresh the list
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
        gameDataExport.setCaptureStateListener(this)
        perAppLogManager.setPerAppStateListener(this)
        loadLogHistory()
        if (currentLoggingMode == GameDataExport.LoggingMode.PER_APP) {
            updatePerAppAdapterStates()
        }
        // Update button states last to ensure manual logs button visibility is correct
        updateButtonStates()
    }
    
    override fun onPause() {
        super.onPause()
        gameDataExport.setCaptureStateListener(null)
        perAppLogManager.setPerAppStateListener(null)
    }
    
    // CaptureStateListener implementation
    override fun onCaptureStarted() {
        activity?.runOnUiThread {
            updateButtonStates()
        }
    }
    
    override fun onCaptureStopped() {
        activity?.runOnUiThread {
            updateButtonStates()
            // Small delay to ensure file is written before refreshing
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (currentLoggingMode == GameDataExport.LoggingMode.GLOBAL) {
                    loadLogHistory()
                }
            }, 500)
        }
    }
    
    // PerAppStateListener implementation
    override fun onAppLoggingStarted(packageName: String) {
        activity?.runOnUiThread {
            updatePerAppAdapterStates()
        }
    }
    
    override fun onAppLoggingStopped(packageName: String) {
        activity?.runOnUiThread {
            updatePerAppAdapterStates()
        }
    }
    
    private fun loadInstalledApps() {
        try {
            val pm = requireContext().packageManager
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            
            installedApps.clear()
            installedApps.addAll(apps.filter { app ->
                // Filter out system apps and our own app
                (app.flags and ApplicationInfo.FLAG_SYSTEM) == 0 && 
                app.packageName != requireContext().packageName
            }.sortedBy { app ->
                app.loadLabel(pm).toString().lowercase()
            })
            
            filteredApps.clear()
            filteredApps.addAll(installedApps)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), getString(R.string.toast_error_loading_apps, e.message), Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun loadSavedLoggingMode() {
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext())
        val savedMode = prefs.getString(PREF_LOGGING_MODE, "GLOBAL")
        
        currentLoggingMode = if (savedMode == "PER_APP") {
            GameDataExport.LoggingMode.PER_APP
        } else {
            GameDataExport.LoggingMode.GLOBAL
        }
        
        // Set the mode in GameDataExport
        gameDataExport.setLoggingMode(currentLoggingMode)
    }
    
    private fun saveLoggingMode() {
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext())
        prefs.edit().putString(PREF_LOGGING_MODE, currentLoggingMode.name).apply()
    }
    
    private fun initializeUIState() {
        // Set radio button state based on loaded mode
        when (currentLoggingMode) {
            GameDataExport.LoggingMode.GLOBAL -> {
                globalLoggingRadio.isChecked = true
                logHistoryRecyclerView.adapter = logHistoryAdapter
                searchBar.hint = getString(R.string.hint_search_logs)
                loadLogHistory()
            }
            GameDataExport.LoggingMode.PER_APP -> {
                perAppLoggingRadio.isChecked = true
                logHistoryRecyclerView.adapter = perAppLogAdapter
                searchBar.hint = getString(R.string.hint_search_apps)
                updatePerAppAdapterStates()
            }
        }
    }
    
    private fun switchToGlobalMode() {
        if (currentLoggingMode == GameDataExport.LoggingMode.GLOBAL) return
        
        currentLoggingMode = GameDataExport.LoggingMode.GLOBAL
        gameDataExport.setLoggingMode(GameDataExport.LoggingMode.GLOBAL)
        saveLoggingMode() // Save the mode change
        
        // Switch to global log history adapter
        logHistoryRecyclerView.adapter = logHistoryAdapter
        searchBar.hint = getString(R.string.hint_search_logs)
        searchBar.setText("") // Clear search when switching modes
        
        loadLogHistory()
        updateButtonStates()
    }
    
    private fun switchToPerAppMode() {
        if (currentLoggingMode == GameDataExport.LoggingMode.PER_APP) return
        
        currentLoggingMode = GameDataExport.LoggingMode.PER_APP
        gameDataExport.setLoggingMode(GameDataExport.LoggingMode.PER_APP)
        saveLoggingMode() // Save the mode change
        
        // Switch to per-app adapter
        logHistoryRecyclerView.adapter = perAppLogAdapter
        searchBar.hint = getString(R.string.hint_search_apps)
        searchBar.setText("") // Clear search when switching modes
        
        updatePerAppAdapterStates()
        updateButtonStates()
    }
    
    private fun filterApps(query: String) {
        filteredApps.clear()
        if (query.isEmpty()) {
            filteredApps.addAll(installedApps)
        } else {
            installedApps.forEach { app ->
                val pm = requireContext().packageManager
                val label = app.loadLabel(pm).toString().lowercase()
                val pkg = app.packageName.lowercase()
                if (label.contains(query) || pkg.contains(query)) {
                    filteredApps.add(app)
                }
            }
        }
        perAppLogAdapter.updateApps(filteredApps)
    }
    
    private fun updatePerAppAdapterStates() {
        val enabledApps = perAppLogManager.getEnabledApps(requireContext())
        val currentlyLoggingApps = perAppLogManager.getCurrentlyLoggingApps()
        
        perAppLogAdapter.updateEnabledApps(enabledApps)
        perAppLogAdapter.updateCurrentlyLoggingApps(currentlyLoggingApps)
        perAppLogAdapter.refreshLogFileStates() // Refresh to update document icons
        updateButtonStates() // Update button states when per-app states change
    }

    data class LogFile(
        val name: String,
        val path: String,
        val size: String,
        val lastModified: String
    )
}
