package com.example.myapitest

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.example.myapitest.databinding.ActivityNewVehicleBinding
import com.example.myapitest.model.Location
import com.example.myapitest.model.Vehicle
import com.example.myapitest.service.Result
import com.example.myapitest.service.RetrofitClient
import com.example.myapitest.service.safeApiCall
import com.example.myapitest.util.MapManager
import com.example.myapitest.util.PermissionManager
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class NewVehicleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNewVehicleBinding
    private lateinit var mapManager: MapManager
    private lateinit var locationPermissionLauncher: ActivityResultLauncher<String>

    private var selectedMarker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewVehicleBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        setupManagers()
        setupView()
    }

    private fun setupManagers() {
        binding.googleMapContent.visibility = View.VISIBLE
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapManager = MapManager(this, mapFragment) { map ->
            PermissionManager.requestLocationPermission(
                this,
                locationPermissionLauncher
            ) { fetchCurrentLocationWithManager(centerOnly = true) }

            map.setOnMapClickListener { latLng ->
                selectedMarker?.remove()
                selectedMarker = mapManager.googleMap.addMarker(
                    com.google.android.gms.maps.model.MarkerOptions().position(latLng)
                )
                binding.latitude.setText(latLng.latitude.toString())
                binding.longitude.setText(latLng.longitude.toString())
            }
        }

        locationPermissionLauncher = PermissionManager.registerForLocationPermission(
            activity = this,
            onGranted = { fetchCurrentLocationWithManager(centerOnly = true) },
            onDenied = { Toast.makeText(this, R.string.error_request_location_permission, Toast.LENGTH_SHORT).show() }
        )
    }

    private fun setupView() {
        binding.toolbar.title = "Novo Veículo"
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.latitude.addTextChangedListener { updateMapFromTextFields() }
        binding.longitude.addTextChangedListener { updateMapFromTextFields() }

        binding.createCTA.setOnClickListener { createVehicle() }

        binding.currentLocationCTA.setOnClickListener {
            PermissionManager.requestLocationPermission(
                context = this,
                locationPermissionLauncher = locationPermissionLauncher,
                onPermissionAlreadyGranted = { fetchCurrentLocationWithManager() }
            )
        }
    }

    private fun fetchCurrentLocationWithManager(centerOnly: Boolean = false) {
        mapManager.fetchAndShowCurrentUserLocation(
            onSuccess = { latLng ->
                if (!centerOnly) {
                    binding.latitude.setText(latLng.latitude.toString())
                    binding.longitude.setText(latLng.longitude.toString())
                    Toast.makeText(this, R.string.success_request_location, Toast.LENGTH_SHORT).show()
                }
            },
            onFailure = { errorMessage ->
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun updateMapFromTextFields() {
        val lat = binding.latitude.text.toString().toDoubleOrNull()
        val long = binding.longitude.text.toString().toDoubleOrNull()

        if (lat != null && long != null) {
            val newPosition = LatLng(lat, long)
            selectedMarker?.remove()
            selectedMarker = mapManager.googleMap.addMarker(
                com.google.android.gms.maps.model.MarkerOptions().position(newPosition)
            )
        }
    }

    private fun validateForm(): Boolean {
        if (binding.name.text.isNullOrBlank() ||
            binding.year.text.isNullOrBlank() ||
            binding.license.text.isNullOrBlank()) {
            Toast.makeText(this, R.string.error_validate_all_form_fields, Toast.LENGTH_SHORT).show()
            return false
        }
        if (binding.latitude.text.isNullOrBlank() || binding.longitude.text.isNullOrBlank()) {
            Toast.makeText(this, R.string.error_validate_form_location, Toast.LENGTH_SHORT).show()
            return false
        }
        // TODO: Adicionar validação de imagem (se o usuário tirou uma foto).
        return true
    }

    private fun createVehicle() {
        if (!validateForm()) return

        val lat = binding.latitude.text.toString().toDoubleOrNull()
        val long = binding.longitude.text.toString().toDoubleOrNull()

        if (lat == null || long == null) {
            Toast.makeText(this, R.string.error_invalid_location_values, Toast.LENGTH_SHORT).show()
            return
        }

        val newVehicle = Vehicle(
            id = UUID.randomUUID().toString(),
            name = binding.name.text.toString(),
            year = binding.year.text.toString(),
            licence = binding.license.text.toString(),
            imageUrl = "https://picsum.photos/200", // TODO: Substituir pela URL da imagem capturada
            place = Location(lat = lat, long = long)
        )

        CoroutineScope(Dispatchers.IO).launch {
            val result = safeApiCall { RetrofitClient.apiService.createVehicle(newVehicle) }

            withContext(Dispatchers.Main) {
                when (result) {
                    is Result.Success -> {
                        Toast.makeText(this@NewVehicleActivity, R.string.success_create, Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    is Result.Error -> {
                        Toast.makeText(this@NewVehicleActivity, R.string.error_create, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    companion object {
        fun newIntent(context: Context) = Intent(context, NewVehicleActivity::class.java)
    }
}