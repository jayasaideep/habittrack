package com.habittracker.heatmap.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habit_days")
data class HabitDayEntity(
    @PrimaryKey
    val date: String,
    val isCompleted: Boolean = false,
    val habitId: String = "default_habit",
    val updatedAt: Long = System.currentTimeMillis()
)