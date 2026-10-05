package com.habittracker.heatmap.data

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.habittracker.heatmap.widget.HabitHeatmapWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HabitRepository(
    private val context: Context,
    private val dao: HabitDayDao
) {
    private val prefs = context.getSharedPreferences("habit_prefs", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO)

    val habitName: String
        get() = prefs.getString("habit_name", "Daily Workout") ?: "Daily Workout"

    fun setHabitName(name: String) {
        prefs.edit().putString("habit_name", name).apply()
        triggerWidgetUpdate()
    }

    val widgetRangePreference: String
        get() = prefs.getString("widget_range", "1_MONTH") ?: "1_MONTH"

    fun setWidgetRangePreference(range: String) {
        prefs.edit().putString("widget_range", range).apply()
        triggerWidgetUpdate()
    }

    fun getAllDaysFlow(): Flow<Map<String, Boolean>> {
        return dao.getAllDaysFlow().map { list ->
            list.associate { it.date to it.isCompleted }
        }
    }

    suspend fun getAllDaysMap(): Map<String, Boolean> {
        return dao.getAllDays().associate { it.date to it.isCompleted }
    }

    suspend fun toggleDate(date: String): Boolean {
        val existing = dao.getDay(date)
        val newStatus = if (existing == null) true else !existing.isCompleted
        dao.upsert(
            HabitDayEntity(
                date = date,
                isCompleted = newStatus,
                updatedAt = System.currentTimeMillis()
            )
        )
        triggerWidgetUpdate()
        return newStatus
    }

    fun triggerWidgetUpdate() {
        scope.launch {
            try {
                HabitHeatmapWidget().updateAll(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun calculateStreaks(daysMap: Map<String, Boolean>): Pair<Int, Int> {
        var currentStreak = 0
        var maxStreak = 0
        var tempStreak = 0
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE

        var checkDate = today
        val todayStr = checkDate.format(formatter)
        val todayDone = daysMap[todayStr] == true

        if (todayDone) {
            currentStreak++
            checkDate = checkDate.minusDays(1)
            while (daysMap[checkDate.format(formatter)] == true) {
                currentStreak++
                checkDate = checkDate.minusDays(1)
            }
        } else {
            val yesterday = today.minusDays(1)
            if (daysMap[yesterday.format(formatter)] == true) {
                checkDate = yesterday
                while (daysMap[checkDate.format(formatter)] == true) {
                    currentStreak++
                    checkDate = checkDate.minusDays(1)
                }
            }
        }

        val sortedDates = daysMap.filter { it.value }.keys.sorted()
        var previousDate: LocalDate? = null

        for (dateStr in sortedDates) {
            try {
                val d = LocalDate.parse(dateStr)
                if (previousDate != null && d == previousDate.plusDays(1)) {
                    tempStreak++
                } else {
                    tempStreak = 1
                }
                if (tempStreak > maxStreak) {
                    maxStreak = tempStreak
                }
                previousDate = d
            } catch (e: Exception) {}
        }

        return Pair(currentStreak, maxOf(maxStreak, currentStreak))
    }
}