package com.nibav.dialer.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nibav.dialer.activities.CallActivity
import com.nibav.dialer.helpers.ACCEPT_CALL
import com.nibav.dialer.helpers.CallManager
import com.nibav.dialer.helpers.DECLINE_CALL

class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACCEPT_CALL -> {
                context.startActivity(com.nibav.dialer.activities.CallActivity.getStartIntent(context))
                CallManager.accept()
            }
            DECLINE_CALL -> CallManager.reject()
        }
    }
}
