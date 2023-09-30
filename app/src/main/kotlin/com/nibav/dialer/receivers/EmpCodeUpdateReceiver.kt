package com.nibav.dialer.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nibav.dialer.extensions.config

class EmpCodeUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(mContext: Context, intent: Intent) {
        if (intent.action == "com.nibav.dialer.data") {
            val helpLine = intent.getStringExtra("helpLine") ?: ""
            mContext.config.helpLine = helpLine
        }
    }
}
