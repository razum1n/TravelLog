package com.laurirantala.travellog.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.laurirantala.travellog.R
import com.laurirantala.travellog.database.PlaceEntity
import com.laurirantala.travellog.databinding.ActivityMapBinding

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private var binding: ActivityMapBinding? = null
    private var placeDetails: PlaceEntity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        if (intent.hasExtra(MainActivity.PLACE_DETAILS)) {
            placeDetails = intent.getSerializableExtra(MainActivity.PLACE_DETAILS) as PlaceEntity
        }

        if (placeDetails != null) {
            setSupportActionBar(binding?.tbMap)
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.title = placeDetails!!.title

            binding?.tbMap?.setNavigationOnClickListener {
                onBackPressed()
            }

            val supportMapFragment: SupportMapFragment =
                supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
            supportMapFragment.getMapAsync(this)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        val position = LatLng(placeDetails!!.latitude, placeDetails!!.longitude)
        googleMap.addMarker(MarkerOptions().position(position).title(placeDetails!!.location))
        val newLatLngZoom = CameraUpdateFactory.newLatLngZoom(position, 15f)
        googleMap.animateCamera(newLatLngZoom)
    }
}