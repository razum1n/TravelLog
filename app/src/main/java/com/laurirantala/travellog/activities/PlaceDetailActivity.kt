package com.laurirantala.travellog.activities

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.laurirantala.travellog.database.PlaceEntity
import com.laurirantala.travellog.database.PlacesApp
import com.laurirantala.travellog.databinding.ActivityPlaceDetailBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class PlaceDetailActivity : AppCompatActivity() {

    private var binding: ActivityPlaceDetailBinding? = null
    private var placeModel: PlaceEntity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlaceDetailBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        if (intent.hasExtra(MainActivity.PLACE_DETAILS)) {
            placeModel = intent.getSerializableExtra(MainActivity.PLACE_DETAILS) as PlaceEntity
            binding?.tvDescriptionDetails?.text = placeModel?.description
            binding?.tvLocationDetails?.text = placeModel?.location
            binding?.ivPlaceImageDetails?.setImageURI(Uri.parse(placeModel?.image))
        }

        setSupportActionBar(binding?.tbPlaceDetail)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        if (placeModel != null) {
            supportActionBar!!.title = placeModel?.title
        }
        binding?.tbPlaceDetail?.setNavigationOnClickListener {
            onBackPressed()
        }

        binding?.btnViewOnMap?.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            intent.putExtra(MainActivity.PLACE_DETAILS, placeModel)
            startActivity(intent)
        }

    }
}