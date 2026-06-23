package com.gps.dashboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gps.dashboard.ui.screen.DashboardScreen
import com.gps.dashboard.ui.theme.GpsDashboardTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GpsDashboardTheme {
                DashboardScreen()
            }
        }
    }
}
