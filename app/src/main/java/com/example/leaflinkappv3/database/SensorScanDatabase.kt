package com.example.leaflinkappv3.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.leaflinkappv3.dao.sensorScanDao
import com.example.leaflinkappv3.model.sensorScan

@Database(entities = [sensorScan::class], version = 1)
abstract class SensorScanDatabase : RoomDatabase() {
    abstract fun sensorScanDao(): sensorScanDao

    companion object {
        @Volatile
        private var INSTANCE: SensorScanDatabase? = null

        fun getDatabase(context: Context): SensorScanDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SensorScanDatabase::class.java,
                    "sensor_scan_database"
                ).build()
                INSTANCE = instance
                return instance
            }
        }
    }
}