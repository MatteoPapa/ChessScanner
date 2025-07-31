package com.tutorials.chessscanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.tutorials.chessscanner.nav.AppNavigator
import com.tutorials.chessscanner.ui.theme.ChessScannerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChessScannerTheme {
                AppNavigator()
            }
        }
    }
}
