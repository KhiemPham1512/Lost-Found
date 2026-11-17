package com.lostfound.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.lostfound.app.ui.navigation.AppNavHost
import com.lostfound.app.ui.theme.LostFoundTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LostFoundTheme {
                // Toàn bộ điều hướng nằm trong ui/navigation/AppNavHost.kt (B quản lý)
                AppNavHost()
            }
        }
    }
}
