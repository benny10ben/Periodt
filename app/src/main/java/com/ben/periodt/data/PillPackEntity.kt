package com.ben.periodt.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pill_packs")
data class PillPackEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val startDate: String,
    val pillCount: Int,
    val endDate: String? = null // Null means the pack is currently active
)