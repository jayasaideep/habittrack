package com.habittracker.heatmap.widget

import android.content.Context
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.habittracker.heatmap.data.HabitDatabase
import com.habittracker.heatmap.data.HabitRepository
import com.habittracker.heatmap.ui.MainActivity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

class HabitHeatmapWidget : GlanceAppWidget() {

    companion object {
        private val SMALL_ONE_WEEK = DpSize(180.dp, 80.dp)
        private val MEDIUM_TWO_WEEKS = DpSize(240.dp, 120.dp)
        private val LARGE_ONE_MONTH = DpSize(280.dp, 180.dp)

        // Colors
        val CompletedGreen = androidx.compose.ui.graphics.Color(0xFF22C55E)
        val MissedRed = androidx.compose.ui.graphics.Color(0xFFEF4444)
        val WidgetBackground = androidx.compose.ui.graphics.Color(0xFF13151A)
        val WidgetHeaderBg = androidx.compose.ui.graphics.Color(0xFF1E2129)
        val TodayBorderColor = androidx.compose.ui.graphics.Color(0xFF38BDF8)
        val CellTextColor = androidx.compose.ui.graphics.Color(0xFFFFFFFF)
        val SubtitleColor = androidx.compose.ui.graphics.Color(0xFF94A3B8)
    }

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(SMALL_ONE_WEEK, MEDIUM_TWO_WEEKS, LARGE_ONE_MONTH)
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = HabitDatabase.getInstance(context)
        val repository = HabitRepository(context, database.habitDayDao())
        val daysMap = repository.getAllDaysMap()
        val habitName = repository.habitName
        val preferredRange = repository.widgetRangePreference

        provideContent {
            GlanceTheme {
                val size = androidx.glance.LocalSize.current
                val activeMode = when {
                    preferredRange == "1_WEEK" -> WidgetDisplayMode.ONE_WEEK
                    preferredRange == "2_WEEKS" -> WidgetDisplayMode.TWO_WEEKS
                    preferredRange == "1_MONTH" -> WidgetDisplayMode.ONE_MONTH
                    size.height >= 160.dp -> WidgetDisplayMode.ONE_MONTH
                    size.height >= 110.dp -> WidgetDisplayMode.TWO_WEEKS
                    else -> WidgetDisplayMode.ONE_WEEK
                }

                WidgetRootContent(
                    context = context,
                    habitName = habitName,
                    daysMap = daysMap,
                    displayMode = activeMode,
                    repository = repository
                )
            }
        }
    }

    enum class WidgetDisplayMode {
        ONE_WEEK,
        TWO_WEEKS,
        ONE_MONTH
    }

    @androidx.compose.runtime.Composable
    private fun WidgetRootContent(
        context: Context,
        habitName: String,
        daysMap: Map<String, Boolean>,
        displayMode: WidgetDisplayMode,
        repository: HabitRepository
    ) {
        val streaks = repository.calculateStreaks(daysMap)
        val currentStreak = streaks.first

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(WidgetBackground))
                .cornerRadius(20.dp)
                .padding(10.dp)
        ) {
            // Header with Habit Name & Streak
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .clickable(actionStartActivity<MainActivity>()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = habitName,
                        style = TextStyle(
                            color = ColorProvider(CellTextColor),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1
                    )
                    Text(
                        text = "$currentStreak day streak",
                        style = TextStyle(
                            color = ColorProvider(SubtitleColor),
                            fontSize = 11.sp
                        )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    ModeButton(
                        label = "7d",
                        isSelected = displayMode == WidgetDisplayMode.ONE_WEEK,
                        onClickRange = "1_WEEK"
                    )
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    ModeButton(
                        label = "14d",
                        isSelected = displayMode == WidgetDisplayMode.TWO_WEEKS,
                        onClickRange = "2_WEEKS"
                    )
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    ModeButton(
                        label = "Mo",
                        isSelected = displayMode == WidgetDisplayMode.ONE_MONTH,
                        onClickRange = "1_MONTH"
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Body Heatmap Grid
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight(),
                contentAlignment = Alignment.Center
            ) {
                when (displayMode) {
                    WidgetDisplayMode.ONE_WEEK -> OneWeekHeatmap(daysMap)
                    WidgetDisplayMode.TWO_WEEKS -> TwoWeeksHeatmap(daysMap)
                    WidgetDisplayMode.ONE_MONTH -> OneMonthHeatmap(daysMap)
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun ModeButton(
        label: String,
        isSelected: Boolean,
        onClickRange: String
    ) {
        Box(
            modifier = GlanceModifier
                .cornerRadius(6.dp)
                .background(
                    if (isSelected) ColorProvider(CompletedGreen)
                    else ColorProvider(WidgetHeaderBg)
                )
                .clickable(
                    actionRunCallback<SwitchWidgetViewActionCallback>(
                        actionParametersOf(SwitchWidgetViewActionCallback.RANGE_KEY to onClickRange)
                    )
                )
                .padding(horizontal = 6.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = TextStyle(
                    color = ColorProvider(if (isSelected) androidx.compose.ui.graphics.Color.Black else CellTextColor),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }

    @androidx.compose.runtime.Composable
    private fun OneWeekHeatmap(daysMap: Map<String, Boolean>) {
        val today = LocalDate.now()
        val days = (6 downTo 0).map { offset -> today.minusDays(offset.toLong()) }
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            days.forEachIndexed { index, date ->
                val dateStr = date.format(formatter)
                val isCompleted = daysMap[dateStr] == true
                val isToday = date == today
                val dayOfWeek = date.dayOfWeek.getDisplayName(JavaTextStyle.NARROW, Locale.getDefault())

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = GlanceModifier.padding(horizontal = 2.dp)
                ) {
                    Text(
                        text = dayOfWeek,
                        style = TextStyle(
                            color = ColorProvider(SubtitleColor),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(3.dp))
                    HeatmapCell(
                        dayNumber = date.dayOfMonth,
                        dateStr = dateStr,
                        isCompleted = isCompleted,
                        isToday = isToday,
                        size = 38.dp,
                        fontSize = 13.sp
                    )
                }

                if (index < days.size - 1) {
                    Spacer(modifier = GlanceModifier.width(2.dp))
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun TwoWeeksHeatmap(daysMap: Map<String, Boolean>) {
        val today = LocalDate.now()
        val days = (13 downTo 0).map { offset -> today.minusDays(offset.toLong()) }
        val week1 = days.take(7)
        val week2 = days.drop(7)
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE

        Column(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                week1.forEach { date ->
                    val dateStr = date.format(formatter)
                    val isCompleted = daysMap[dateStr] == true
                    val isToday = date == today
                    HeatmapCell(
                        dayNumber = date.dayOfMonth,
                        dateStr = dateStr,
                        isCompleted = isCompleted,
                        isToday = isToday,
                        size = 32.dp,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = GlanceModifier.width(4.dp))
                }
            }
            Spacer(modifier = GlanceModifier.height(4.dp))
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                week2.forEach { date ->
                    val dateStr = date.format(formatter)
                    val isCompleted = daysMap[dateStr] == true
                    val isToday = date == today
                    HeatmapCell(
                        dayNumber = date.dayOfMonth,
                        dateStr = dateStr,
                        isCompleted = isCompleted,
                        isToday = isToday,
                        size = 32.dp,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = GlanceModifier.width(4.dp))
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun OneMonthHeatmap(daysMap: Map<String, Boolean>) {
        val today = LocalDate.now()
        val firstDayOfMonth = today.withDayOfMonth(1)
        val daysInMonth = today.lengthOfMonth()
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE

        val monthDays = (1..daysInMonth).map { day -> firstDayOfMonth.withDayOfMonth(day) }
        val dayRows = monthDays.chunked(7)

        Column(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            dayRows.forEach { rowDays ->
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    rowDays.forEach { date ->
                        val dateStr = date.format(formatter)
                        val isCompleted = daysMap[dateStr] == true
                        val isToday = date == today
                        HeatmapCell(
                            dayNumber = date.dayOfMonth,
                            dateStr = dateStr,
                            isCompleted = isCompleted,
                            isToday = isToday,
                            size = 28.dp,
                            fontSize = 10.sp
                        )
                        Spacer(modifier = GlanceModifier.width(3.dp))
                    }
                }
                Spacer(modifier = GlanceModifier.height(3.dp))
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun HeatmapCell(
        dayNumber: Int,
        dateStr: String,
        isCompleted: Boolean,
        isToday: Boolean,
        size: androidx.compose.ui.unit.Dp,
        fontSize: androidx.compose.ui.unit.TextUnit
    ) {
        val cellColor = if (isCompleted) CompletedGreen else MissedRed

        Box(
            modifier = GlanceModifier
                .size(size)
                .cornerRadius(if (size > 30.dp) 8.dp else 6.dp)
                .background(
                    if (isToday) ColorProvider(TodayBorderColor)
                    else ColorProvider(cellColor)
                )
                .clickable(
                    actionRunCallback<ToggleDayActionCallback>(
                        actionParametersOf(ToggleDayActionCallback.DATE_KEY to dateStr)
                    )
                )
                .padding(if (isToday) 2.dp else 0.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(if (size > 30.dp) 6.dp else 4.dp)
                    .background(ColorProvider(cellColor)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = dayNumber.toString(),
                    style = TextStyle(
                        color = ColorProvider(CellTextColor),
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}