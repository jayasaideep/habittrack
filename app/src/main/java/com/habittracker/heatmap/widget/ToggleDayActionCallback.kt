package com.habittracker.heatmap.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import com.habittracker.heatmap.data.HabitDatabase
import com.habittracker.heatmap.data.HabitRepository

class ToggleDayActionCallback : ActionCallback {

    companion object {
        val DATE_KEY = ActionParameters.Key<String>("date_key")
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val dateString = parameters[DATE_KEY] ?: return

        val database = HabitDatabase.getInstance(context)
        val repository = HabitRepository(context, database.habitDayDao())

        // Toggle: Red (missed) -> Green (completed), Green (completed) -> Red (missed)
        repository.toggleDate(dateString)

        // Immediately re-render the Glance widget on the user's home screen
        HabitHeatmapWidget().update(context, glanceId)
        HabitHeatmapWidget().updateAll(context)
    }
}