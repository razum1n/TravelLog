package com.laurirantala.travellog.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.laurirantala.travellog.database.PlaceEntity
import com.laurirantala.travellog.database.PlacesApp
import com.laurirantala.travellog.database.PlacesDao
import com.laurirantala.travellog.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.collect
import com.laurirantala.travellog.R
import kotlinx.coroutines.launch
import pl.kitek.rvswipetodelete.SwipeToDeleteCallback
import pl.kitek.rvswipetodelete.SwipeToEditCallback
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private var binding: ActivityMainBinding? = null

    lateinit var addPlaceResultIntent: ActivityResultLauncher<Intent>
    lateinit var placesDao: PlacesDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        binding?.fbAddPlace?.setOnClickListener {
            val intent = Intent(this, AddPlaceActivity::class.java)
            addPlaceResultIntent.launch(intent)
        }

        placesDao = (application as PlacesApp).db.placesDao()

        addPlaceResultIntent =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                if (result.resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this@MainActivity, "Log added successfully", Toast.LENGTH_LONG)
                        .show()
                }
            }

        lifecycleScope.launch {
            placesDao.fetchAllPlaces().collect {
                val list = ArrayList(it)
                setupListOfDataIntoRecyclerView(list, placesDao)
            }
        }
    }

    private fun setupListOfDataIntoRecyclerView(
        placesList: ArrayList<PlaceEntity>,
        placesDao: PlacesDao
    ) {
        if (placesList.isNotEmpty()) {
            val placesAdapter = PlacesAdapter(
                this@MainActivity,
                placesList,
                { updateListener -> updatePlace(updateListener) },
                { deleteListener -> deletePlace(deleteListener) })
            binding?.rvPlacesList?.layoutManager = LinearLayoutManager(this)
            binding?.rvPlacesList?.adapter = placesAdapter
            binding?.rvPlacesList?.visibility = View.VISIBLE
            binding?.tvNoRecords?.visibility = View.GONE

            val editSwipeHandler = object : SwipeToEditCallback(this@MainActivity) {
                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val adapter = binding?.rvPlacesList?.adapter as PlacesAdapter
                    adapter.notifyEditItem(
                        this@MainActivity,
                        viewHolder.adapterPosition,
                        EDIT_PLACE_CODE
                    )
                }
            }

            val editItemTouchHelper = ItemTouchHelper(editSwipeHandler)
            editItemTouchHelper.attachToRecyclerView(binding?.rvPlacesList)

            val deleteSwipeHandler = object : SwipeToDeleteCallback(this@MainActivity) {
                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val adapter = binding?.rvPlacesList?.adapter as PlacesAdapter
                    adapter.notifyDelete(viewHolder.adapterPosition)
                }
            }
            val deleteItemTouchHelper = ItemTouchHelper(deleteSwipeHandler)
            deleteItemTouchHelper.attachToRecyclerView(binding?.rvPlacesList)

        } else {
            binding?.tvNoRecords?.visibility = View.VISIBLE
            binding?.rvPlacesList?.visibility = View.GONE
        }
    }

    private fun updatePlace(id: Int) {

        lifecycleScope.launch {
            placesDao.fetchPlaceById(id).collect {
                val intent = Intent(this@MainActivity, PlaceDetailActivity::class.java)
                intent.putExtra(PLACE_DETAILS, it)
                startActivity(intent)
            }
        }
    }

    private fun deletePlace(id: Int) {
        lifecycleScope.launch {
            placesDao.delete(PlaceEntity(id))
        }
    }


    companion object {
        const val EDIT_PLACE_CODE = 1
        const val PLACE_DETAILS = "Place"
    }
}