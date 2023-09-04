package com.nibav.dialer.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nibav.dialer.extensions.config

class EmpCodeUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(mContext: Context, intent: Intent) {
        if (intent.action == "com.nibav.dialer.empCode") {
            val empCode = intent.getStringExtra("empCode") ?: ""
            val empName = intent.getStringExtra("empName") ?: ""
            val empPhoneNumber = intent.getStringExtra("empPhoneNumber") ?: ""
            val isPhoneLocked = intent.getBooleanExtra("isPhoneLocked", false)
            mContext.config.empCode = empCode
            mContext.config.empName = empName
            mContext.config.empPhoneNumber = empPhoneNumber
            mContext.config.isPhoneLocked = isPhoneLocked
        }
    }
}
