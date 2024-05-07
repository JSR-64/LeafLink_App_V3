package com.example.leaflinkappv3.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.leaflinkappv3.model.sensorScan

@Dao
interface sensorScanDao {
    @Insert
    suspend fun insert(scan: sensorScan)

    @Query("SELECT * FROM sensorScan")
    suspend fun getAll(): List<sensorScan>

    @Query("DELETE FROM sensorScan")
    suspend fun deleteAll()

    @Update
    suspend fun update(scan: sensorScan)

    @Delete
    suspend fun delete(scan: sensorScan)

    @Query("SELECT * FROM sensorScan WHERE id = :id")
    suspend fun getById(id: Int): sensorScan?
}