package com.example.myapitest.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapitest.R
import com.example.myapitest.model.Vehicle
import com.example.myapitest.ui.loadUrl

class VehicleAdapter(
    private val vehicles: MutableList<Vehicle> = mutableListOf(),
    private val itemClickListener: (Vehicle) -> Unit
): RecyclerView.Adapter<VehicleAdapter.VehicleViewHolder>() {

    fun updateVehicles(newVehicles: List<Vehicle>) {
        vehicles.clear()
        vehicles.addAll(newVehicles)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VehicleViewHolder {
        val view = LayoutInflater
            .from(parent.context)
            .inflate(R.layout.item_car_layout, parent, false)
        return VehicleViewHolder(view)
    }

    override fun onBindViewHolder(holder: VehicleViewHolder, position: Int) {
        val vehicle = vehicles[position]
        holder.model.text = vehicle.name
        holder.license.text = vehicle.licence
        holder.year.text = vehicle.year
        holder.image.loadUrl(vehicle.imageUrl)
        holder.itemView.setOnClickListener {
            itemClickListener(vehicle)
        }
    }

    override fun getItemCount(): Int = vehicles.size

    class VehicleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.image)
        val model: TextView = view.findViewById(R.id.model)
        val year: TextView = view.findViewById(R.id.year)
        val license: TextView = view.findViewById(R.id.license)
    }
}













