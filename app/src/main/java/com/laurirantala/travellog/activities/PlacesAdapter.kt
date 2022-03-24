package com.laurirantala.travellog.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.laurirantala.travellog.R
import com.laurirantala.travellog.database.PlaceEntity
import com.laurirantala.travellog.databinding.PlaceItemRowBinding

class PlacesAdapter(
    private val context: Context,
    private val items: ArrayList<PlaceEntity>,
    private val updateListener: (id: Int) -> Unit,
    private val deleteListener: (id: Int) -> Unit
) : RecyclerView.Adapter<PlacesAdapter.ViewHolder>() {


    class ViewHolder(binding: PlaceItemRowBinding) : RecyclerView.ViewHolder(binding.root) {
        val ivPlaceImage = binding.ivPlaceImage
        val tvTitle = binding.tvTitle
        val tvDescription = binding.tvDescription
        val cvMain = binding.cvMain

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            PlaceItemRowBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.ivPlaceImage.setImageURI(Uri.parse(item.image))
        holder.tvTitle.text = item.title
        holder.tvDescription.text = item.description

        holder.cvMain.setOnClickListener {
            updateListener.invoke(item.id)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    private fun getPlaceId(position: Int): Int {
        return items[position].id
    }

    fun notifyDelete(position: Int){
        deleteListener.invoke(getPlaceId(position))
        notifyItemChanged(position)
    }

    fun notifyEditItem(activity: Activity, position: Int, requestCode: Int) {
        val intent = Intent(context, AddPlaceActivity::class.java)
        intent.putExtra(MainActivity.PLACE_DETAILS, items[position])
        activity.startActivityForResult(intent, requestCode)
        notifyItemChanged(position)
    }

}