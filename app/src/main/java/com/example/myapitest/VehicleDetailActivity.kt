package com.example.myapitest

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.myapitest.databinding.ActivityVehicleDetailBinding
import com.example.myapitest.model.Vehicle
import com.example.myapitest.service.Result
import com.example.myapitest.service.RetrofitClient
import com.example.myapitest.service.safeApiCall
import com.example.myapitest.ui.loadUrl
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VehicleDetailActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityVehicleDetailBinding

    private lateinit var vehicle: Vehicle
    private lateinit var map: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVehicleDetailBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        setupView()
        loadData()
        setupGoogleMap()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        if (::vehicle.isInitialized) {
            loadItemLocationInGoogleMap()
        }
    }

    private fun setupView() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        binding.deleteCTA.setOnClickListener {
            deleteVehicle()
        }
        binding.editCTA.setOnClickListener {
            editVehicle()
        }
    }

    private fun loadItemLocationInGoogleMap() {
        vehicle.place.apply {
            binding.googleMapContent.visibility = View.VISIBLE
            val latLong = LatLng(lat, long)
            map.addMarker(
                MarkerOptions()
                    .position(latLong)
                    .title(vehicle.name)
            )
            map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    latLong,
                    15f
                )
            )
        }
    }

    private fun setupGoogleMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun loadData() {
        val vehicleId = intent.getStringExtra(ARG_ID) ?: ""

        CoroutineScope(Dispatchers.IO).launch {
            val result = safeApiCall { RetrofitClient.apiService.getVehicleById(vehicleId) }

            withContext(Dispatchers.Main) {
                when (result) {
                    is Result.Success -> {
                        vehicle = result.data.value
                        binding.name.setText(vehicle.name)
                        binding.year.setText(vehicle.year)
                        binding.license.setText(vehicle.licence)
                        binding.image.loadUrl(vehicle.imageUrl)
                        loadItemLocationInGoogleMap()
                    }
                    is Result.Error -> {
                        Toast.makeText(
                            this@VehicleDetailActivity,
                            R.string.unknown_error, Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                }
            }
        }
    }

    private fun deleteVehicle() {
        CoroutineScope(Dispatchers.IO).launch {
            val result = safeApiCall { RetrofitClient.apiService.deleteVehicle(vehicle.id) }

            withContext(Dispatchers.Main) {
                when (result) {
                    is Result.Error -> {
                        Toast.makeText(
                            this@VehicleDetailActivity,
                            R.string.error_delete,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    is Result.Success -> {
                        Toast.makeText(
                            this@VehicleDetailActivity,
                            R.string.success_delete,
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                }
            }
        }
    }

    private fun editVehicle() {
        CoroutineScope(Dispatchers.IO).launch {
            val result = safeApiCall {
                RetrofitClient.apiService.updateVehicle(
                    vehicle.id,
                    vehicle.copy(
                        name = binding.name.text.toString(),
                        year = binding.year.text.toString(),
                        licence = binding.license.text.toString(),
                    )
                )
            }
            withContext(Dispatchers.Main) {
                when (result) {
                    is Result.Error -> {
                        Toast.makeText(
                            this@VehicleDetailActivity,
                            R.string.error_update,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    is Result.Success -> {
                        Toast.makeText(
                            this@VehicleDetailActivity,
                            R.string.success_update,
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                }
            }
        }
    }


    companion object {
        private const val ARG_ID = "arg_id"

        fun newIntent(
            context: Context,
            vehicleId: String
        ) = Intent(context, VehicleDetailActivity::class.java).apply {
            putExtra(ARG_ID, vehicleId)
        }
    }
}