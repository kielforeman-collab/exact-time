package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ExactAlarmEntity::class, CountdownEventEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ExactTimeDatabase : RoomDatabase() {
    abstract fun exactAlarmDao(): ExactAlarmDao
    abstract fun countdownEventDao(): CountdownEventDao

    companion object {
        @Volatile
        private var INSTANCE: ExactTimeDatabase? = null

        fun getInstance(context: Context): ExactTimeDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ExactTimeDatabase::class.java,
                    "exacttime_precision.db"
                ).fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
