package com.example.myapitest

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import com.example.myapitest.databinding.ActivityVehicleDetailBinding
import com.example.myapitest.model.Vehicle
import com.example.myapitest.service.Result
import com.example.myapitest.service.RetrofitClient
import com.example.myapitest.service.safeApiCall
import com.example.myapitest.ui.loadUrl
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VehicleDetailActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityVehicleDetailBinding

    private lateinit var vehicle: Vehicle
    private lateinit var map: GoogleMap

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            fetchCurrentLocation()
        } else {
            Toast.makeText(this, R.string.error_request_location_permission, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVehicleDetailBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        setupView()
        loadData()
        setupGoogleMap()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
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

        binding.latitude.addTextChangedListener {
            validateGeoPosition(
                editText = binding.latitude,
                min = -90.0,
                max = 90.0,
            )
            updateMapFromTextFields()
        }

        binding.longitude.addTextChangedListener {
            validateGeoPosition(
                editText = binding.longitude,
                min = -180.0,
                max = 180.0,
            )
            updateMapFromTextFields()
        }

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        binding.deleteCTA.setOnClickListener {
            deleteVehicle()
        }
        binding.editCTA.setOnClickListener {
            editVehicle()
        }

        binding.currentLocationCTA.setOnClickListener {
            requestLocationPermission()
        }
    }

    private fun requestLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                fetchCurrentLocation()
            }
            else -> {
                locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchCurrentLocation() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    binding.latitude.setText(location.latitude.toString())
                    binding.longitude.setText(location.longitude.toString())
                    Toast.makeText(this, R.string.success_request_location, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, R.string.error_request_location, Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun loadItemLocationInGoogleMap() {
        vehicle.place.apply {
            binding.googleMapContent.visibility = View.VISIBLE
            val latLong = LatLng(lat, long)
            showLocationOnMap(latLong)
        }
    }

    private fun updateMapFromTextFields() {
        if (!::map.isInitialized) return

        val lat = binding.latitude.text.toString().toDoubleOrNull()
        val long = binding.longitude.text.toString().toDoubleOrNull()

        if (lat != null && long != null) {
            val newPosition = LatLng(lat, long)
            showLocationOnMap(newPosition)
        }
    }

    private fun showLocationOnMap(position: LatLng) {
        map.clear()
        map.addMarker(
            MarkerOptions()
                .position(position)
                .title(vehicle.name)
        )
        map.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                position,
                15f
            )
        )
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
                        binding.latitude.setText(vehicle.place.lat.toString())
                        binding.longitude.setText(vehicle.place.long.toString())
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
        val lat = binding.latitude.text.toString().toDoubleOrNull()
        val long = binding.longitude.text.toString().toDoubleOrNull()

        if (lat == null || long == null) {
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val result = safeApiCall {
                RetrofitClient.apiService.updateVehicle(
                    vehicle.id,
                    vehicle.copy(
                        name = binding.name.text.toString(),
                        year = binding.year.text.toString(),
                        licence = binding.license.text.toString(),
                        place = vehicle.place.copy(
                            lat = lat,
                            long = long,
                        )
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

    private fun validateGeoPosition(
        editText: EditText,
        min: Double,
        max: Double,
        errorMessage: String = "Valor inválido"
    ) {
        val text = editText.text.toString()
        val position = text.toDoubleOrNull()

        if (text.isEmpty() || position == null) {
            editText.error = null
            return
        }

        if (position < min || position > max) {
            editText.error = errorMessage
        } else {
            editText.error = null
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