package com.jp.finalcallyzer.screendialogReceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.jp.finalcallyzer.screendialogReceiver.CallOverlayActivity
import com.jp.finalcallyzer.worker.CallUploadWorker
import com.jp.finalcallyzer.worker.startUploadWorker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class IncomingCallReceiver : BroadcastReceiver() {

    companion object {
        private var lastState = TelephonyManager.EXTRA_STATE_IDLE
        private var callStartTime: Long = 0
        private var isIncoming = false
        private var savedNumber: String? = null
    }

    // ringing -> only for incoming call
    // offhook -> call picked (answered )
    // idle -> no call condition (default call state)


    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        // Update saved number if we got one
        if (!number.isNullOrBlank()) savedNumber = number

        println("Call state: $state | number: $number | saved: $savedNumber")

        when {

            // ── Incoming call ringing ──────────────────────────────────
            state == TelephonyManager.EXTRA_STATE_RINGING
                    && lastState == TelephonyManager.EXTRA_STATE_IDLE -> {
                isIncoming = true
                callStartTime = System.currentTimeMillis()
                showOverlay(context, savedNumber, "RINGING")
            }

            // ── incoming Call answered ──────────────────────────────────────────
            state == TelephonyManager.EXTRA_STATE_OFFHOOK
                    && lastState == TelephonyManager.EXTRA_STATE_RINGING -> {
                callStartTime = System.currentTimeMillis()
                showOverlay(context, savedNumber, "ACTIVE")
            }

            // ── Outgoing call started ──────────────────────────────────
            state == TelephonyManager.EXTRA_STATE_OFFHOOK
                    && lastState == TelephonyManager.EXTRA_STATE_IDLE -> {
                isIncoming = false
                callStartTime = System.currentTimeMillis()
            }

            // ── Call ended (from ringing => missed) ─────────────────────
            state == TelephonyManager.EXTRA_STATE_IDLE
                    && lastState == TelephonyManager.EXTRA_STATE_RINGING -> {
                val duration = System.currentTimeMillis() - callStartTime
                showOverlay(context, savedNumber, "MISSED")
                savedNumber = null
            }

            // ── Call ended (was active) ──────────────────────────────── upload the recording and data to the server
            state == TelephonyManager.EXTRA_STATE_IDLE
                    && lastState == TelephonyManager.EXTRA_STATE_OFFHOOK -> {

                val duration = ((System.currentTimeMillis() - callStartTime) / 1000).toInt()

                showOverlay(context, savedNumber, "ENDED", duration)
                val prefs = context.getSharedPreferences("callyzer_prefs", Context.MODE_PRIVATE)


                val dateTime = SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss",
                    Locale.getDefault()
                ).format(Date())

                val contactNo = savedNumber ?: ""

                val folderPath = prefs.getString("recording_folder_uri", "") ?: ""

                startUploadWorker(
                    context = context,
                    duration = duration,
                    dateTime = dateTime,
                    contactNo = contactNo,
                    filePath = folderPath
                )

                println("Call ended with duration: $duration")

                savedNumber = null
            }
        }

        lastState = state
    }

    private fun showOverlay(
        context: Context,
        phone: String?,
        state: String,
        duration: Int = 0,
    ) {
        val intent = Intent(context, CallOverlayActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_NO_HISTORY or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("phone", phone ?: "Unknown")
            putExtra("name", phone ?: "Unknown")  // replace with contact lookup if needed
            putExtra("state", state)
            putExtra("duration", duration)
        }
        context.startActivity(intent)
    }
}