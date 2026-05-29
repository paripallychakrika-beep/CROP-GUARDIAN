package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_history")
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val imageUriPath: String, // local uri or file path of the plant image
    val timestamp: Long = System.currentTimeMillis(),
    val plantName: String,
    val diseaseName: String,
    val status: String,
    val reportJson: String // serialized PlantScanReport object for full detail restoration
)
