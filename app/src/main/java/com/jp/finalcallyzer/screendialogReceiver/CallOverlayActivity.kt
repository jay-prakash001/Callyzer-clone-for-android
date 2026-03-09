package com.jp.finalcallyzer.screendialogReceiver

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class CallOverlayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent.getBooleanExtra("dismiss", false)) {
            finish()
            return
        }

        // Show over lock screen + wake device
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            (getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager)
                .requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD  or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON    or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        val phone = intent.getStringExtra("phone") ?: "Unknown"
        val name  = intent.getStringExtra("name")  ?: phone
        val state = intent.getStringExtra("state") ?: "INCOMING"

        setContent {
            CallOverlayScreen(
                phone     = phone,
                name      = name,
                state     = state,
                onAccept  = { dialNumber(phone) },
                onDismiss = { finish() }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra("dismiss", false)) finish()
    }

    private fun dialNumber(phone: String) {
        startActivity(
            Intent(Intent.ACTION_CALL).apply {
                data  = Uri.parse("tel:$phone")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )
        finish()
    }
}

@Composable
fun CallOverlayScreen(
    phone: String,
    name: String,
    state: String,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape  = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))
        ) {
            Column(
                modifier            = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // Avatar
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0077FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = name.first().uppercaseChar().toString(),
                        color      = Color.White,
                        fontSize   = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(name,  color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(phone, color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)

                Text(
                    text     = when (state) {
                        "INCOMING" -> "Incoming call request"
                        "ENDED"    -> "Call Ended"
                        else       -> state
                    },
                    color    = Color(0xFF00E5A0),
                    fontSize = 13.sp
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                when (state) {
                    "INCOMING" -> {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Decline
                            FilledTonalButton(
                                onClick = onDismiss,
                                colors  = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFFFF4060)
                                )
                            ) {
                                Text("Decline", color = Color.White)
                            }

                            // Call now
                            FilledTonalButton(
                                onClick = onAccept,
                                colors  = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFF00E5A0)
                                )
                            ) {
                                Text("Call Now", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    else -> {
                        TextButton(onClick = onDismiss) {
                            Text("Dismiss", color = Color.White.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }
    }
}