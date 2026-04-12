package com.ben.periodt.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val profileUuid: String = UUID.randomUUID().toString(), // permanent identity, used for import matching
    val name: String,
    val avatarColor: String = "avatar_1",
    val createdAt: Long = System.currentTimeMillis()
)