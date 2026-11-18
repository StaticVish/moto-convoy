package com.staticvish.motoconvoy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.staticvish.motoconvoy.ui.ConvoyHudScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ConvoyHudScreen()
        }
    }
}
