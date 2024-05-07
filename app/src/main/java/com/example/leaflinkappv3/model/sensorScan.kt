package com.example.leaflinkappv3.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class sensorScan(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val timestamp: Long,
    val latitude: Double?,
    val longitude: Double?,
    val sensorId: String?
)