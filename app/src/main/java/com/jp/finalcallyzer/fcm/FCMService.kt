package com.jp.finalcallyzer.fcm

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.KeyguardManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.jp.finalcallyzer.screendialogReceiver.CallOverlayActivity
import com.jp.finalcallyzer.screendialogReceiver.DismissNotificationReceiver
import androidx.core.content.edit

class FCMService : FirebaseMessagingService() {

    companion object {
        private const val CALL_CHANNEL_ID = "call_channel"
        private const val CALL_CHANNEL_NAME = "Incoming Call Requests"
        private var activeNotifId = 1

    }

    override fun onNewToken(token: String) {
        println("New Token : FCM => $token")
        super.onNewToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val phone = data["customerPhone"]?.takeIf { it.isNotBlank() } ?: return
        val name = data["customerName"]?.takeIf { it.isNotBlank() } ?: "N/A"

        saveFCMData(phone, name)

        println("FCM received: $data")

        createNotificationChannel()
        wakeScreen()

        val notifId = System.currentTimeMillis().toInt()
        FCMService.activeNotifId = notifId

        // Always use notification path — fullScreenIntent handles foreground too
        startCall(phone, name)
    }

    private fun saveFCMData(phone: String, name: String) {

        val prefs = getSharedPreferences("fcm_data", Context.MODE_PRIVATE)

        prefs.edit {
            putString("customer_phone", phone)
                .putString("customer_name", name)
                .putLong("received_time", System.currentTimeMillis())
        }
    }

    // ── Wake screen so full-screen intent shows on lock screen ─────────
    private fun wakeScreen() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        val wl = pm.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or
                    PowerManager.ACQUIRE_CAUSES_WAKEUP or
                    PowerManager.ON_AFTER_RELEASE,
            "callyzer:fcm_wake"
        )
        wl.acquire(5_000) // release after 5s
    }

    // ── Auto-dial via full-screen notification ─────────────────────────
    // Full-screen intent is the only way to break through when app is closed
    // or screen is off — works in foreground, background, and killed state


    private fun startCall(phone: String, name: String) {
        val notifId = System.currentTimeMillis().toInt()

        if (isAppInForeground()) {
            // App is open — direct call works
            startActivity(
                Intent(Intent.ACTION_CALL).apply {
                    data = Uri.parse("tel:$phone")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        } else {
            // App is background/killed — must use notification
            showCallNotification(phone, name, notifId)
        }
    }

    @SuppressLint("ServiceCast")
    private fun isAppInForeground(): Boolean {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val appProcesses = am.runningAppProcesses ?: return false
        val packageName = packageName
        return appProcesses.any { process ->
            process.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                    && process.processName == packageName
        }
    }

    private fun showCallNotification(phone: String, name: String, notifId: Int) {
        val overlayIntent = Intent(this, CallOverlayActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_NO_HISTORY or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("phone", phone)
            putExtra("name", name)
            putExtra("state", "INCOMING")
            putExtra("notifId", notifId)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, notifId,
            overlayIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Tap notification → open dialler + dismiss notification
        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phone")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val dialPendingIntent = PendingIntent.getActivity(
            this, notifId + 1,
            dialIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss intent — fires when notification is tapped or action pressed
        val dismissIntent = Intent(this, DismissNotificationReceiver::class.java).apply {
            putExtra("notifId", notifId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            this, notifId + 2,
            dismissIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CALL_CHANNEL_ID)
            .setContentTitle("Incoming Call Request")
            .setContentText("$name • $phone")
            .setSmallIcon(android.R.drawable.sym_call_outgoing)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(dialPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            .setOngoing(false)                              // ← not sticky anymore
            .setDeleteIntent(dismissPendingIntent)          // ← fires when swiped away
            // Action buttons
            .addAction(
                android.R.drawable.ic_menu_call,
                "Call",
                dialPendingIntent                           // tap Call → open dialler
            )
            .addAction(
                android.R.drawable.ic_delete,
                "Dismiss",
                dismissPendingIntent                        // tap Dismiss → cancel notification
            )
            .build()

        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(notifId, notification)
    }

    private fun showCallNotification0(phone: String, name: String, notifId: Int) {
        val overlayIntent = Intent(this, CallOverlayActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_NO_HISTORY or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("phone", phone)
            putExtra("name", name)
            putExtra("state", "INCOMING")
            putExtra("notifId", notifId)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, notifId,
            overlayIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Tap notification → open dialler directly
        val dialIntent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:$phone")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val dialPendingIntent = PendingIntent.getActivity(
            this, notifId + 1,
            dialIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CALL_CHANNEL_ID)
            .setContentTitle("Incoming Call Request")
            .setContentText("$name • $phone")
            .setSmallIcon(android.R.drawable.sym_call_outgoing)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(dialPendingIntent)           // tap → open dialler
            .setFullScreenIntent(fullScreenPendingIntent, true)  // auto-launch overlay
            .setAutoCancel(false)
            .setOngoing(true)
            .build()

        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(notifId, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CALL_CHANNEL_ID,
                CALL_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Auto-dial requests from web dashboard"
                enableVibration(true)
                setBypassDnd(true)           // bypasses Do Not Disturb
                lockscreenVisibility =
                    NotificationCompat.VISIBILITY_PUBLIC   // show on lock screen
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }
}