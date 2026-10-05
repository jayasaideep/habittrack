package com.habittracker.heatmap.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [HabitDayEntity::class], version = 1, exportSchema = false)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun habitDayDao(): HabitDayDao

    companion object {
        @Volatile
        private var INSTANCE: HabitDatabase? = null

        fun getInstance(context: Context): HabitDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HabitDatabase::class.java,
                    "habit_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}