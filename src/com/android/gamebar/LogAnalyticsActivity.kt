/*
 * SPDX-FileCopyrightText: 2025 kenway214
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.gamebar

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Toast
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity
import com.android.gamebar.R

class LogAnalyticsActivity : CollapsingToolbarBaseActivity() {

    companion object {
        const val EXTRA_LOG_FILE_PATH = "log_file_path"
        const val EXTRA_LOG_FILE_NAME = "log_file_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gamebar_log)
        
        val logFilePath = intent.getStringExtra(EXTRA_LOG_FILE_PATH) ?: ""
        val logFileName = intent.getStringExtra(EXTRA_LOG_FILE_NAME) ?: "Log Analytics"
        
        title = logFileName
        
        if (savedInstanceState == null && logFilePath.isNotEmpty()) {
            showAnalytics(logFilePath, logFileName)
        }
    }
    
    private fun showAnalytics(filePath: String, fileName: String) {
        // Show loading message
        val loadingDialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.gb_analyzing_log))
            .setMessage("Please wait while we analyze the session data.")
            .setCancelable(false)
            .create()
        loadingDialog.show()
        
        // Analyze log file in background thread
        Thread {
            val logReader = PerAppLogReader()
            val analytics = logReader.analyzeLogFile(filePath)
            
            // Update UI on main thread
            runOnUiThread {
                loadingDialog.dismiss()
                
                if (analytics != null) {
                    // Create and show fragment with analytics
                    val fragment = LogAnalyticsFragment().apply {
                        arguments = Bundle().apply {
                            putString("LOG_FILE_PATH", filePath)
                            putString("LOG_FILE_NAME", fileName)
                            putSerializable("ANALYTICS_DATA", analytics)
                        }
                    }
                    
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.content_frame, fragment)
                        .commit()
                } else {
                    Toast.makeText(this, "Failed to analyze log file", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }.start()
    }
}
