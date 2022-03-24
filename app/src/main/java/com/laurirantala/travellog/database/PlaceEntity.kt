package com.laurirantala.travellog.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable


@Entity(tableName = "places-table")
data class PlaceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String = "",
    val image: String = "",
    val description: String = "",
    val date: String = "",
    val location: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
) : Serializable