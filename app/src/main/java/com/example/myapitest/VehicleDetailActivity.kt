package com.example.myapitest

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.example.myapitest.databinding.ActivityVehicleDetailBinding
import com.example.myapitest.model.Vehicle
import com.example.myapitest.service.Result
import com.example.myapitest.service.RetrofitClient
import com.example.myapitest.service.safeApiCall
import com.example.myapitest.ui.loadUrl
import com.example.myapitest.util.MapManager
import com.example.myapitest.util.PermissionManager
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VehicleDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVehicleDetailBinding
    private lateinit var vehicle: Vehicle
    private lateinit var mapManager: MapManager
    private lateinit var locationPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVehicleDetailBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        setupManagers()
        setupView()
        loadData()
    }

    private fun setupManagers() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapManager = MapManager(this, mapFragment) {
            if (::vehicle.isInitialized) {
                loadItemLocationInGoogleMap()
            }
        }

        locationPermissionLauncher = PermissionManager.registerForLocationPermission(
            activity = this,
            onGranted = { fetchCurrentLocationWithManager() },
            onDenied = { Toast.makeText(this, R.string.error_request_location_permission, Toast.LENGTH_SHORT).show() }
        )
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
            PermissionManager.requestLocationPermission(
                context = this,
                locationPermissionLauncher = locationPermissionLauncher,
                onPermissionAlreadyGranted = { fetchCurrentLocationWithManager() }
            )
        }
    }

    private fun fetchCurrentLocationWithManager() {
        mapManager.fetchAndShowCurrentUserLocation(
            onSuccess = { latLng ->
                binding.latitude.setText(latLng.latitude.toString())
                binding.longitude.setText(latLng.longitude.toString())
                Toast.makeText(this, R.string.success_request_location, Toast.LENGTH_SHORT).show()
            },
            onFailure = { errorMessage ->
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun loadItemLocationInGoogleMap() {
        binding.googleMapContent.visibility = View.VISIBLE
        val latLong = LatLng(vehicle.place.lat, vehicle.place.long)
        mapManager.showLocationOnMap(latLong, vehicle.name)
    }

    private fun updateMapFromTextFields() {
        val lat = binding.latitude.text.toString().toDoubleOrNull()
        val long = binding.longitude.text.toString().toDoubleOrNull()

        if (lat != null && long != null) {
            val newPosition = LatLng(lat, long)
            mapManager.showLocationOnMap(newPosition, vehicle.name)
        }
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
                        if (mapManager.isMapReady) {
                            loadItemLocationInGoogleMap()
                        }
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