package com.laurirantala.travellog.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlacesDao {

    @Insert
    suspend fun insert(placeEntity: PlaceEntity)

    @Update
    suspend fun update(placeEntity: PlaceEntity)

    @Delete
    suspend fun delete(placeEntity: PlaceEntity)

    @Query("SELECT * FROM `places-table`")
    fun fetchAllPlaces(): Flow<List<PlaceEntity>>

    @Query("SELECT * FROM `places-table` where id=:id")
    fun fetchPlaceById(id: Int): Flow<PlaceEntity>
}