package com.laurirantala.travellog.database

import android.app.Application

class PlacesApp : Application() {
    val db by lazy {
        PlacesDatabase.getInstance(this)
    }
}