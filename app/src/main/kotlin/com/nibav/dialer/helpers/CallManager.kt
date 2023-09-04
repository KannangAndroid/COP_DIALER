package com.nibav.dialer.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.telecom.VideoProfile
import com.nibav.commons.extensions.getFormattedDuration
import com.nibav.dialer.dialogs.WindowOverLayout
import com.nibav.dialer.extensions.*
import com.nibav.dialer.interfaces.OverlayCallListener
import com.nibav.dialer.models.AudioRoute
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.concurrent.CopyOnWriteArraySet

// inspired by https://github.com/Chooloo/call_manage
class CallManager {
    companion object {
        @SuppressLint("StaticFieldLeak")
        var inCallService: InCallService? = null
        private var call: Call? = null
        private val calls = mutableListOf<Call>()
        private val listeners = CopyOnWriteArraySet<CallManagerListener>()
        private var recorder: MediaRecorder? = null
        private var audiofile: File? = null
        private var isCallRecorded = false
        private var mContext: Context? = null
        private var callState = ""
        private var callInProgress = false
        private var totalCallDetails: HashMap<String, JSONObject> = HashMap()
        private var windowOverLayout: WindowOverLayout? = null
        private val callDurationHandler = Handler(Looper.getMainLooper())

        fun onCallAdded(context: Context, call: Call) {
            callInProgress = true
            this.mContext = context
            windowOverLayout = windowOverLayout ?: WindowOverLayout(context, overlayCallListener)
            this.call = call
            calls.add(call)
            for (listener in listeners) {
                listener.onPrimaryCallChanged(call)
            }
            call.registerCallback(object : Call.Callback() {
                override fun onStateChanged(call: Call, state: Int) {
                    updateState()
                }

                override fun onDetailsChanged(call: Call, details: Call.Details) {
                    updateState()
                }

                override fun onConferenceableCallsChanged(call: Call, conferenceableCalls: MutableList<Call>) {
                    updateState()
                }
            })
        }

        fun onCallRemoved(call: Call) {
            calls.remove(call)
            updateState()
        }

        fun onAudioStateChanged(audioState: CallAudioState) {
            val route = AudioRoute.fromRoute(audioState.route) ?: return
            for (listener in listeners) {
                listener.onAudioStateChanged(route)
            }
        }

        fun getPhoneState(): PhoneState {
            return when (calls.size) {
                0 -> NoCall
                1 -> SingleCall(calls.first())
                2 -> {
                    val active = calls.find { it.getStateCompat() == Call.STATE_ACTIVE }
                    val newCall = calls.find { it.getStateCompat() == Call.STATE_CONNECTING || it.getStateCompat() == Call.STATE_DIALING }
                    val onHold = calls.find { it.getStateCompat() == Call.STATE_HOLDING }
                    if (active != null && newCall != null) {
                        TwoCalls(newCall, active)
                    } else if (newCall != null && onHold != null) {
                        TwoCalls(newCall, onHold)
                    } else if (active != null && onHold != null) {
                        TwoCalls(active, onHold)
                    } else {
                        TwoCalls(calls[0], calls[1])
                    }
                }

                else -> {
                    val conference = calls.find { it.isConference() } ?: return NoCall
                    val secondCall = if (conference.children.size + 1 != calls.size) {
                        calls.filter { !it.isConference() }
                            .subtract(conference.children.toSet())
                            .firstOrNull()
                    } else {
                        null
                    }
                    if (secondCall == null) {
                        SingleCall(conference)
                    } else {
                        val newCallState = secondCall.getStateCompat()
                        if (newCallState == Call.STATE_ACTIVE || newCallState == Call.STATE_CONNECTING || newCallState == Call.STATE_DIALING) {
                            TwoCalls(secondCall, conference)
                        } else {
                            TwoCalls(conference, secondCall)
                        }
                    }
                }
            }
        }

        private fun getCallAudioState() = inCallService?.callAudioState

        fun getSupportedAudioRoutes(): Array<AudioRoute> {
            return AudioRoute.values().filter {
                val supportedRouteMask = getCallAudioState()?.supportedRouteMask
                if (supportedRouteMask != null) {
                    supportedRouteMask and it.route == it.route
                } else {
                    false
                }
            }.toTypedArray()
        }

        fun getCallAudioRoute() = AudioRoute.fromRoute(getCallAudioState()?.route)

        fun setAudioRoute(newRoute: Int) {
            inCallService?.setAudioRoute(newRoute)
        }

        private fun updateState() {
            val phoneState = getPhoneState()
            val primaryCall = when (phoneState) {
                is NoCall -> null
                is SingleCall -> phoneState.call
                is TwoCalls -> phoneState.active
            }
            var notify = true
            if (primaryCall == null) {
                call = null
            } else if (primaryCall != call) {
                call = primaryCall
                for (listener in listeners) {
                    listener.onPrimaryCallChanged(primaryCall)
                }
                notify = false
            }
            if (notify) {
                for (listener in listeners) {
                    listener.onStateChanged()
                }
            }
            try {
                if (phoneState is SingleCall) {
                    if (call?.children?.size == 0) {
                        if (call.getStateCompat() == Call.STATE_ACTIVE) {
                            startRecording()
                            updateCallDetails(call)
                        } else if (call.getStateCompat() == Call.STATE_RINGING) {
                            callState = "inbound"
                        } else if (call.getStateCompat() == Call.STATE_CONNECTING || call.getStateCompat() == Call.STATE_DIALING) {
                            callState = "outbound"
                        } else if (call.getStateCompat() == Call.STATE_DISCONNECTED) {
                            callInProgress = false
                            checkCallStatus(true)
                            updateCallDetails(call)
                            stopRecording()
                        }
                    } else if ((call?.children?.size ?: 0) > 0) {
                        call?.children?.let {
                            it.forEach { individualCall ->
                                //if (individualCall.getStateCompat() == Call.STATE_DISCONNECTED || individualCall.getStateCompat() == Call.STATE_ACTIVE)
                                updateCallDetails(individualCall)
                            }
                        }
                    }
                } else if (phoneState is TwoCalls) {
                    if ((call?.children?.size ?: 0) > 0) {
                        call?.children?.let {
                            it.forEach { individualCall ->
                                //if (individualCall.getStateCompat() == Call.STATE_DISCONNECTED || individualCall.getStateCompat() == Call.STATE_ACTIVE)
                                updateCallDetails(individualCall)
                            }
                        }
                    }
                }/*else if (phoneState is TwoCalls) {
                    if(phoneState.onHold!=null) {
                        secondNumber = phoneState.onHold?.details?.handle?.schemeSpecificPart ?: ""
                        callState ="conference_hold"
                    }
                }*/
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // remove all disconnected calls manually in case they are still here
            calls.removeAll { it.getStateCompat() == Call.STATE_DISCONNECTED }
        }

        private fun updateCallDetails(call: Call?) {
            val obj = JSONObject()
            val callerDisplayName = call?.details?.callerDisplayName ?: ""
            val number = call?.details?.handle?.schemeSpecificPart ?: null
            if (callerDisplayName.isNullOrEmpty() && number.isNullOrEmpty())
                return
            obj.put("number", number ?: callerDisplayName)
            obj.put("isOutgoing", call?.isOutgoing())
            obj.put("callDuration", call?.getCallDuration())
            totalCallDetails[number ?: callerDisplayName] = obj
        }

        fun getPrimaryCall(): Call? {
            return call
        }

        fun getConferenceCalls(): List<Call> {
            return calls.find { it.isConference() }?.children ?: emptyList()
        }

        fun accept() {
            call?.answer(VideoProfile.STATE_AUDIO_ONLY)
        }

        fun reject() {
            if (call != null) {
                val state = getState()
                if (state == Call.STATE_RINGING) {
                    call!!.reject(false, null)
                } else if (state != Call.STATE_DISCONNECTED && state != Call.STATE_DISCONNECTING) {
                    call!!.disconnect()
                }
            }
        }

        fun toggleHold(): Boolean {
            val isOnHold = getState() == Call.STATE_HOLDING
            if (isOnHold) {
                call?.unhold()
            } else {
                call?.hold()
            }
            return !isOnHold
        }

        fun swap() {
            if (calls.size > 1) {
                calls.find { it.getStateCompat() == Call.STATE_HOLDING }?.unhold()
            }
        }

        fun merge() {
            val conferenceableCalls = call!!.conferenceableCalls
            if (conferenceableCalls.isNotEmpty()) {
                call!!.conference(conferenceableCalls.first())
            } else {
                if (call!!.hasCapability(Call.Details.CAPABILITY_MERGE_CONFERENCE)) {
                    call!!.mergeConference()
                }
            }
        }

        fun addListener(listener: CallManagerListener) {
            listeners.add(listener)
            checkCallStatus(true)
        }

        fun removeListener(listener: CallManagerListener) {
            listeners.remove(listener)
            checkCallStatus(false)
        }

        fun getState() = getPrimaryCall()?.getStateCompat()

        fun keypad(char: Char) {
            call?.playDtmfTone(char)
            Handler().postDelayed({
                call?.stopDtmfTone()
            }, DIALPAD_TONE_LENGTH_MS)
        }

        @Throws(IOException::class)
        private fun startRecording() {
            if (isCallRecorded)
                return
            isCallRecorded = true

            //Creating file
            try {
                val nibavdir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        .toString() + "/Nibav"
                )
                if (!nibavdir.exists())
                    Files.createDirectory(nibavdir.toPath())

                val recordingDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        .toString() + "/Nibav/recording"
                )

                if (!recordingDir.exists())
                    Files.createDirectory(recordingDir.toPath())

                audiofile = File.createTempFile("sound", ".m4a", recordingDir)
            } catch (e: IOException) {
                return
            }
            recorder = MediaRecorder()
            recorder?.reset()
            recorder?.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL)
            recorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            recorder?.setAudioEncodingBitRate(16)
            recorder?.setAudioSamplingRate(44100)
            recorder?.setOutputFile(audiofile)
            try {
                recorder?.prepare()
                recorder?.start()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


        private fun stopRecording() {
            recorder = try {
                recorder?.release()
                recorder?.stop()
                recorder?.reset()
                null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
            if (isCallRecorded)
                updateFile()
            isCallRecorded = false
        }

        private fun updateFile() {
            /* Log.d("NibavDialer_details", totalCallDetails.toString())
             Log.d("NibavDialer_callState", callState)*/
            val callArray = JSONArray()
            totalCallDetails.forEach { (k, v) ->
                callArray.put(v)
            }
            totalCallDetails.clear()
            val intent = Intent()
            intent.action = "com.nibav.employee.incall"
            intent.putExtra("fileName", audiofile?.name)
            intent.putExtra("callState", callState)
            intent.putExtra("callDetails", callArray.toString())
            intent.setPackage("com.nibav.employee")
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            mContext?.sendBroadcast(intent)
        }

        private fun checkCallStatus(isScreenOpen: Boolean) {
            try {
                if (isScreenOpen) {
                    windowOverLayout?.hide()
                } else if (callInProgress) {
                    initCallLogo()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


        private fun initCallLogo() {
            try {
                windowOverLayout?.show()
                when (call.getStateCompat()) {
                    Call.STATE_RINGING -> windowOverLayout?.updateStatus("Ringing")
                    Call.STATE_ACTIVE -> callStarted()
                    Call.STATE_DISCONNECTED -> windowOverLayout?.hide()
                    Call.STATE_CONNECTING, Call.STATE_DIALING -> windowOverLayout?.updateStatus("Connecting")
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun callStarted(){
            callDurationHandler.removeCallbacks(updateCallDurationTask)
            callDurationHandler.post(updateCallDurationTask)
        }

        private val updateCallDurationTask = object : Runnable {
            override fun run() {
                val callDuration = getPrimaryCall().getCallDuration()
                if (callInProgress) {
                    windowOverLayout?.updateStatus(callDuration.getFormattedDuration())
                    callDurationHandler.postDelayed(this, 1000)
                }
            }
        }

        private val overlayCallListener = object : OverlayCallListener {
            override fun onClickedCall() {
                mContext?.startActivity(com.nibav.dialer.activities.CallActivity.getStartIntent(mContext!!))
            }

        }

    }


}

interface CallManagerListener {
    fun onStateChanged()
    fun onAudioStateChanged(audioState: AudioRoute)
    fun onPrimaryCallChanged(call: Call)
}

sealed class PhoneState
object NoCall : PhoneState()
class SingleCall(val call: Call) : PhoneState()
class TwoCalls(val active: Call, val onHold: Call) : PhoneState()
