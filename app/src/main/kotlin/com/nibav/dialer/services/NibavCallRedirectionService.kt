package com.nibav.dialer.services

import android.net.Uri
import android.os.Build
import android.telecom.CallRedirectionService
import android.telecom.PhoneAccountHandle
import android.util.Log
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.Q)
class NibavCallRedirectionService: CallRedirectionService() {

    override fun onPlaceCall(handle: Uri, initialPhoneAccount: PhoneAccountHandle, allowInteractiveResponse: Boolean) {
        Log.d("AppLog", "handle:$handle , initialPhoneAccount:$initialPhoneAccount , allowInteractiveResponse:$allowInteractiveResponse")
        //cancelCall()
    }
}
