package com.laurirantala.travellog.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase


@Database(entities = [PlaceEntity::class], version = 1)
abstract class PlacesDatabase : RoomDatabase() {

    abstract fun placesDao(): PlacesDao

    companion object {

        @Volatile
        private var INSTANCE: PlacesDatabase? = null

        fun getInstance(context: Context): PlacesDatabase {

            synchronized(this) {
                var instance = INSTANCE

                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        PlacesDatabase::class.java,
                        "places_database"
                    ).fallbackToDestructiveMigration().build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }

}