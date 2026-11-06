package de.uhrwerksarchiv.watchaccurancy.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watches")
data class Watch(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val model: String,
    val serialNumber: String = "",
    val caliber: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
