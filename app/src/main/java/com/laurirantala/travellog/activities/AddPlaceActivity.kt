package com.laurirantala.travellog.activities

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.laurirantala.travellog.R
import com.laurirantala.travellog.database.PlaceEntity
import com.laurirantala.travellog.database.PlacesApp
import com.laurirantala.travellog.database.PlacesDao
import com.laurirantala.travellog.databinding.ActivityAddPlaceBinding
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class AddPlaceActivity : AppCompatActivity(), View.OnClickListener {

    private var cal = Calendar.getInstance()
    private lateinit var dateSetListener: DatePickerDialog.OnDateSetListener
    private var binding: ActivityAddPlaceBinding? = null
    private var currentImgLocation: Uri? = null
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private lateinit var placesDao: PlacesDao

    private var placeDetails: PlaceEntity? = null

    private val getResultForGallery =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.data != null) {
                try {
                    result.data!!.data?.let { getImageBitmap(it) }
                } catch (e: IOException) {
                    e.printStackTrace()
                    Toast.makeText(
                        this@AddPlaceActivity,
                        "Failed to load an image",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

    private val getResultForCamera =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                try {
                    handleCameraImage(result.data)
                } catch (e: IOException) {
                    e.printStackTrace()
                    Toast.makeText(
                        this@AddPlaceActivity,
                        "Failed to load an image",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

    private val getResultFromGoogleMaps =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val place: Place = Autocomplete.getPlaceFromIntent(result.data)
                binding?.etLocation?.setText(place.address)
                latitude = place.latLng!!.latitude
                longitude = place.latLng!!.longitude
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddPlaceBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        setSupportActionBar(binding?.tbAddPlace)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding?.tbAddPlace?.setNavigationOnClickListener {
            onBackPressed()
        }

        if (!Places.isInitialized()) {
            Places.initialize(
                this@AddPlaceActivity,
                resources.getString(R.string.google_maps_api_key)
            )
        }

        if (intent.hasExtra(MainActivity.PLACE_DETAILS)) {
            placeDetails = intent.getSerializableExtra(MainActivity.PLACE_DETAILS) as PlaceEntity
        }

        dateSetListener = DatePickerDialog.OnDateSetListener { datePicker, i, i2, i3 ->
            cal.set(Calendar.YEAR, i)
            cal.set(Calendar.MONTH, i2)
            cal.set(Calendar.DAY_OF_MONTH, i3)
            updateDate()
        }
        updateDate()

        if (placeDetails != null) {
            supportActionBar?.title = "Edit Place"
            binding?.etTitle?.setText(placeDetails!!.title)
            binding?.etDescription?.setText(placeDetails!!.description)
            binding?.etDate?.setText(placeDetails!!.date)
            binding?.etLocation?.setText(placeDetails!!.location)
            latitude = placeDetails!!.latitude
            longitude = placeDetails!!.longitude
            currentImgLocation = Uri.parse(placeDetails!!.image)
            binding?.ivPlaceImage?.setImageURI(currentImgLocation)
        }

        placesDao = (application as PlacesApp).db.placesDao()
        binding?.etDate?.setOnClickListener(this)
        binding?.tvAddImage?.setOnClickListener(this)
        binding?.btnSave?.setOnClickListener(this)
        binding?.etLocation?.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            binding?.etDate?.id -> {
                DatePickerDialog(
                    this@AddPlaceActivity,
                    dateSetListener,
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
            binding?.tvAddImage?.id -> {
                val pictureDialog = AlertDialog.Builder(this)
                pictureDialog.setTitle("Select Action")
                val pictureDialogItems =
                    arrayOf("Select photo from Gallery", "Capture photo from camera")
                pictureDialog.setItems(pictureDialogItems) { dialog, which ->
                    when (which) {
                        0 -> choosePhotoFromGallery()
                        1 -> choosePhotoFromCamera()
                    }
                }
                pictureDialog.show()
            }
            binding?.btnSave?.id -> {
                savePlaceToDatabase(placesDao)
            }
            binding?.etLocation?.id -> {
                startGoogleMaps()
            }
        }
    }

    private fun startGoogleMaps() {
        try {
            val fields = listOf(
                Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG,
                Place.Field.ADDRESS
            )
            val intent =
                Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
                    .build(this@AddPlaceActivity)
            getResultFromGoogleMaps.launch(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getImageBitmap(selectedPhotoUri: Uri) {
        val bitmap = when {
            Build.VERSION.SDK_INT < 28 -> MediaStore.Images.Media.getBitmap(
                this.contentResolver,
                selectedPhotoUri
            )
            else -> {
                val source = ImageDecoder.createSource(this.contentResolver, selectedPhotoUri)
                ImageDecoder.decodeBitmap(source)
            }
        }
        currentImgLocation = saveImagesToInternalStorage(bitmap)
        Log.e("Saved Image:", currentImgLocation.toString())
        binding?.ivPlaceImage?.setImageBitmap(bitmap)
    }

    private fun handleCameraImage(intent: Intent?) {
        val bitmap = intent?.extras?.get("data") as Bitmap
        currentImgLocation = saveImagesToInternalStorage(bitmap)
        Log.e("Saved Image:", currentImgLocation.toString())
        binding?.ivPlaceImage?.setImageBitmap(bitmap)

    }

    private fun choosePhotoFromGallery() {
        Dexter.withContext(this@AddPlaceActivity).withPermissions(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ).withListener(object : MultiplePermissionsListener {
            override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                if (report.areAllPermissionsGranted()) {
                    val galleryIntent =
                        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    getResultForGallery.launch(galleryIntent)
                }

            }

            override fun onPermissionRationaleShouldBeShown(
                p0: MutableList<PermissionRequest>,
                p1: PermissionToken
            ) {
                showRationalDialogForPermissions()
            }
        }).onSameThread().check()
    }

    private fun choosePhotoFromCamera() {
        Dexter.withContext(this@AddPlaceActivity).withPermissions(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
        ).withListener(object : MultiplePermissionsListener {
            override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                if (report.areAllPermissionsGranted()) {
                    val galleryIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    getResultForCamera.launch(galleryIntent)
                }
            }

            override fun onPermissionRationaleShouldBeShown(
                p0: MutableList<PermissionRequest>,
                p1: PermissionToken
            ) {
                showRationalDialogForPermissions()
            }
        }).onSameThread().check()
    }

    private fun showRationalDialogForPermissions() {
        AlertDialog.Builder(this@AddPlaceActivity)
            .setMessage("It looks like you have disabled permissions for this function.")
            .setPositiveButton("GO TO SETTINGS") { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    private fun updateDate() {
        val myFormat = "dd.MM.yyyy"
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
        binding?.etDate?.setText(sdf.format(cal.time).toString())
    }

    private fun saveImagesToInternalStorage(bitmap: Bitmap): Uri {
        val wrapper = ContextWrapper(applicationContext)
        var file = wrapper.getDir(IMAGE_DIRECTORY, Context.MODE_PRIVATE)
        file = File(file, "${UUID.randomUUID()}.jpg")

        try {
            val stream: OutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            stream.flush()
            stream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return Uri.parse(file.absolutePath)
    }

    private fun savePlaceToDatabase(placesDao: PlacesDao) {
        val title = binding?.etTitle?.text.toString()
        val image = currentImgLocation.toString()
        val description = binding?.etDescription?.text.toString()
        val date = binding?.etDate?.text.toString()
        val location = binding?.etLocation?.text.toString()

        if (title.isNotEmpty() && currentImgLocation != null && description.isNotEmpty()
            && date.isNotEmpty() && location.isNotEmpty()
        ) {
            if (placeDetails == null) {
                lifecycleScope.launch {
                    placesDao.insert(
                        PlaceEntity(
                            title = title,
                            image = image,
                            description = description,
                            date = date,
                            location = location,
                            latitude = latitude,
                            longitude = longitude
                        )
                    )
                    binding?.etTitle?.text?.clear()
                    binding?.etDescription?.text?.clear()
                    binding?.etDate?.text?.clear()
                    binding?.etLocation?.text?.clear()
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            } else {
                lifecycleScope.launch {
                    placesDao.update(
                        PlaceEntity(
                            placeDetails!!.id,
                            title = title,
                            image = image,
                            description = description,
                            date = date,
                            location = location,
                            latitude = latitude,
                            longitude = longitude
                        )
                    )
                    binding?.etTitle?.text?.clear()
                    binding?.etDescription?.text?.clear()
                    binding?.etDate?.text?.clear()
                    binding?.etLocation?.text?.clear()
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            }

        } else {
            Toast.makeText(
                applicationContext,
                "Please fill all the fields and add a photo",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    companion object {
        private const val IMAGE_DIRECTORY = "TravelLogImages"
    }

}