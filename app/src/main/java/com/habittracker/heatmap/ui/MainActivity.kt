package com.habittracker.heatmap.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.habittracker.heatmap.HabitApplication
import com.habittracker.heatmap.ui.theme.HabitHeatmapTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as HabitApplication
        val repository = app.repository

        setContent {
            HabitHeatmapTheme {
                HabitScreen(repository = repository)
            }
        }
    }
}