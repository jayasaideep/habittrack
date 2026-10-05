package com.habittracker.heatmap.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDayDao {
    @Query("SELECT * FROM habit_days WHERE date = :date LIMIT 1")
    suspend fun getDay(date: String): HabitDayEntity?

    @Query("SELECT * FROM habit_days WHERE habitId = :habitId ORDER BY date ASC")
    fun getAllDaysFlow(habitId: String = "default_habit"): Flow<List<HabitDayEntity>>

    @Query("SELECT * FROM habit_days WHERE habitId = :habitId ORDER BY date ASC")
    suspend fun getAllDays(habitId: String = "default_habit"): List<HabitDayEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(habitDay: HabitDayEntity)

    @Query("DELETE FROM habit_days WHERE habitId = :habitId")
    suspend fun clearAll(habitId: String = "default_habit")
}