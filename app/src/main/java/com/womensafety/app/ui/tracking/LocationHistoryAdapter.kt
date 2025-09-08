package com.womensafety.app.ui.tracking

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.womensafety.app.R
import com.womensafety.app.data.model.LocationData
import java.text.SimpleDateFormat
import java.util.*

class LocationHistoryAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var locations = listOf<LocationData>()
    private var isLoading = false
    private val VIEW_TYPE_ITEM = 0
    private val VIEW_TYPE_LOADING = 1

    fun updateLocations(newLocations: List<LocationData>) {
        locations = newLocations
        isLoading = false
        notifyDataSetChanged()
    }

    fun showLoading() {
        isLoading = true
        notifyDataSetChanged()
    }

    fun hideLoading() {
        isLoading = false
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (isLoading && position == locations.size) {
            VIEW_TYPE_LOADING
        } else {
            VIEW_TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_ITEM) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_location, parent, false)
            LocationViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_location_skeleton, parent, false)
            LoadingViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is LocationViewHolder && position < locations.size) {
            holder.bind(locations[position])
        }
    }

    override fun getItemCount(): Int {
        return if (isLoading) {
            locations.size + 1
        } else {
            locations.size
        }
    }

    inner class LocationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvLocationAddress: TextView = itemView.findViewById(R.id.tv_location_address)
        private val tvLocationTime: TextView = itemView.findViewById(R.id.tv_location_time)
        private val tvLocationCoordinates: TextView = itemView.findViewById(R.id.tv_location_coordinates)
        private val tvLocationAccuracy: TextView = itemView.findViewById(R.id.tv_location_accuracy)

        fun bind(location: LocationData) {
            // Set address or "Unknown" if null
            tvLocationAddress.text = location.address ?: "Unknown address"
            
            // Format timestamp
            val dateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            tvLocationTime.text = dateFormat.format(Date(location.timestamp))
            
            // Set coordinates
            tvLocationCoordinates.text = "Lat: ${String.format("%.6f", location.latitude)}, Lng: ${String.format("%.6f", location.longitude)}"
            
            // Set accuracy
            tvLocationAccuracy.text = "Accuracy: ±${location.accuracy.toInt()}m"
        }
    }

    inner class LoadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
