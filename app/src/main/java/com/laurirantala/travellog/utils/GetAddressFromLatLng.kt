package com.laurirantala.travellog.utils

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Looper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.lang.Exception
import java.util.*
import java.util.concurrent.Executors
import java.util.logging.Handler

class GetAddressFromLatLng(
    context: Context,
    private val latitude: Double,
    private val longitude: Double
) {

    private val geocoder: Geocoder = Geocoder(context, Locale.getDefault())
    private lateinit var addressListener: AddressListener
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = android.os.Handler(Looper.getMainLooper())



    private fun doInBackground() = runBlocking {
        launch {
            var addressString: String? = null
            try {
                val addressList: List<Address>? = geocoder.getFromLocation(latitude,longitude, 1)

                if(addressList != null && addressList.isNotEmpty()){
                    val address: Address = addressList[0]
                    val sb = StringBuilder()
                    for(i in 0..address.maxAddressLineIndex) {
                        sb.append(address.getAddressLine(i)).append(" ")
                    }
                    sb.deleteCharAt(sb.length -1)
                    addressString = sb.toString()
                }
            }
            catch (e: Exception){
                e.printStackTrace()
            }

            if(addressString == null){
                addressListener.onError()
            } else {
                addressListener.onAddressFound(addressString)
            }
        }
    }

    fun setAddressListener(newAddressListener: AddressListener){
        addressListener = newAddressListener
    }

    fun getAddress(){
        doInBackground()
    }

    interface AddressListener{
        fun onAddressFound(address:String?)
        fun onError()
    }
}