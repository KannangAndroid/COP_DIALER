package com.nibav.dialer.services

import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import androidx.annotation.RequiresApi
import com.nibav.commons.extensions.baseConfig
import com.nibav.commons.extensions.getMyContactsCursor
import com.nibav.commons.extensions.isNumberBlocked
import com.nibav.commons.extensions.normalizePhoneNumber
import com.nibav.commons.helpers.SimpleContactsHelper
import com.nibav.dialer.extensions.config

@RequiresApi(Build.VERSION_CODES.N)
class SimpleCallScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        if (config.isPhoneLocked || config.empCode.isNullOrEmpty())
            respondToCall(callDetails, isBlocked = true)
        else {
            val number = callDetails.handle?.schemeSpecificPart
            when {
                number != null && isNumberBlocked(number.normalizePhoneNumber()) -> {
                    respondToCall(callDetails, isBlocked = true)
                }

                number != null && baseConfig.blockUnknownNumbers -> {
                    val simpleContactsHelper = SimpleContactsHelper(this)
                    val privateCursor = getMyContactsCursor(favoritesOnly = false, withPhoneNumbersOnly = true)
                    simpleContactsHelper.exists(number, privateCursor) { exists ->
                        respondToCall(callDetails, isBlocked = !exists)
                    }
                }

                number == null && baseConfig.blockHiddenNumbers -> {
                    respondToCall(callDetails, isBlocked = true)
                }

                else -> {
                    respondToCall(callDetails, isBlocked = false)
                }
            }
        }
    }

    private fun respondToCall(callDetails: Call.Details, isBlocked: Boolean) {
        val response = CallResponse.Builder()
            .setDisallowCall(isBlocked)
            .setRejectCall(isBlocked)
            .setSkipCallLog(isBlocked)
            .setSkipNotification(isBlocked)
            .build()

        respondToCall(callDetails, response)
    }
}
