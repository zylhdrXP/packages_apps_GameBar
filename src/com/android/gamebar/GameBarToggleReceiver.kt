/*
 * SPDX-FileCopyrightText: 2025 kenway214
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.gamebar

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.UserHandle
import android.provider.Settings
import androidx.preference.PreferenceManager

class GameBarToggleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TOGGLE) return

        val enabled = intent.getBooleanExtra(EXTRA_ENABLED, false)
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().putBoolean("game_bar_enable", enabled).apply()
        Settings.System.putIntForUser(
            context.contentResolver,
            "game_bar_enabled",
            if (enabled) 1 else 0,
            UserHandle.USER_CURRENT
        )
        
        val gameBar = GameBar.getInstance(context)
        if (enabled) {
            if (android.provider.Settings.canDrawOverlays(context)) {
                gameBar.applyPreferences()
                gameBar.show()
            }
        } else {
            gameBar.hide()
            GameBar.destroyInstance()
        }
    }

    companion object {
        const val ACTION_TOGGLE = "com.android.gamebar.ACTION_TOGGLE"
        const val EXTRA_ENABLED = "enabled"
    }
}
