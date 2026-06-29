package com.gps.dashboard.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.gps.dashboard.data.model.Track
import com.gps.dashboard.data.model.TrackPoint

@Database(entities = [Track::class, TrackPoint::class], version = 1, exportSchema = false)
abstract class TrackingDatabase : RoomDatabase() {

    abstract fun trackDao(): TrackDao
    abstract fun trackPointDao(): TrackPointDao

    companion object {
        @Volatile
        private var INSTANCE: TrackingDatabase? = null

        fun getInstance(context: Context): TrackingDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TrackingDatabase::class.java,
                    "gps_tracking.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
