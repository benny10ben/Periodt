package com.ben.periodt.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pill_packs",
    foreignKeys = [
        ForeignKey(
            entity        = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns  = ["profileId"],
            onDelete      = ForeignKey.CASCADE
        )
    ],
    indices = [Index("profileId")]
)
data class PillPackEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val profileId: Int,
    val startDate: String,
    val pillCount: Int,
    val endDate: String? = null
)