package com.jp.finalcallyzer

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.google.firebase.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.jp.finalcallyzer.ui.recording.RecordingFolderScreen
import com.jp.finalcallyzer.ui.theme.FinalCallyzerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FinalCallyzerTheme {

                LaunchedEffect(Unit) {
                    FirebaseMessaging.getInstance().token.addOnSuccessListener {
                        println("FCM TOKEN = $it")
                    }




                    requestPermissions(
                        arrayOf(
                            Manifest.permission.READ_PHONE_STATE,
                            Manifest.permission.READ_CALL_LOG,
                            Manifest.permission.POST_NOTIFICATIONS,
                            Manifest.permission.CALL_PHONE
                        ), 0

                    )
                }


                RecordingFolderScreen()
//                SelectDirectoryScreen(this)

            }
        }
    }
}

@Composable
fun SelectDirectoryScreen(context: Context) {

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->

        uri?.let {

            // Persist permission
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )

            // Save in SharedPreferences
            val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

            prefs.edit()
                .putString("recording_directory", it.toString())
                .apply()

            println("Saved directory: $it")
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Button(onClick = {
            launcher.launch(null)
        }) {

            Text("Select recording path")
        }

    }
}