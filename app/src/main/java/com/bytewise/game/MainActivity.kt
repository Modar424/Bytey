package com.bytewise.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.bytewise.game.ui.BytewiseApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { BytewiseApp(onQuit = { finish() }) }
    }

    /** Nothing may keep sounding while the app is in the background. */
    override fun onStop() {
        super.onStop()
        (application as BytewiseApplication).container.soundManager.stopAll()
    }
}
