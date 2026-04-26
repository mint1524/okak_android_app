package com.example.okakapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.okakapp.ui.OkakNavHost
import com.example.okakapp.ui.theme.OKAKAPPTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OKAKAPPTheme {
                OkakNavHost()
            }
        }
    }
}
