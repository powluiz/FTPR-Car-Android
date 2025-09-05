package com.example.myapitest

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.content.FileProvider
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
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
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID

class NewVehicleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNewVehicleBinding
    private lateinit var mapManager: MapManager
    private lateinit var locationPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var imageUri: Uri
    private var selectedMarker: Marker? = null
    private var imageFile: File? = null
    private var uploadedImageUrl: String? = null

    private val cameraLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            imageFile?.let {
                binding.image.setImageBitmap(BitmapFactory.decodeFile(it.path))
                uploadedImageUrl = it.path
            }
//            uploadImageToFirebase()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewVehicleBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        setupManagers()
        setupView()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE_CAMERA -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PERMISSION_GRANTED) {
                    openCamera()
                } else {
                    Toast.makeText(this, R.string.error_request_camera, Toast.LENGTH_SHORT).show()
                }
            }
        }
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

        binding.image.setOnClickListener {
            onTakePicture()
        }

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
        if (uploadedImageUrl.isNullOrBlank()) {
            Toast.makeText(this, "Por favor, tire uma foto do veículo", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun onTakePicture() {
        if (checkSelfPermission(this, android.Manifest.permission.CAMERA) == PERMISSION_GRANTED) {
            openCamera()
        } else {
            requestCameraPermission()
        }
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.CAMERA),
            REQUEST_CODE_CAMERA
        )
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        imageUri = createImageUri()
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        cameraLauncher.launch(intent)
    }

    private fun createImageUri(): Uri {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd-HHmmss").format(Date())
        val imageFileName = "JPEG_${timeStamp}_"

        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        imageFile = File.createTempFile(
            imageFileName,
            ".jpg",
            storageDir
        )

        return FileProvider.getUriForFile(
            this,
            "com.example.myapitest.fileprovider",
            imageFile!!
        )
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
            imageUrl = uploadedImageUrl!!,
            place = Location(lat = lat, long = long)
        )

        binding.createCTA.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            val result = safeApiCall { RetrofitClient.apiService.createVehicle(newVehicle) }

            withContext(Dispatchers.Main) {
                binding.createCTA.isEnabled = true
                when (result) {
                    is Result.Success -> {
                        Toast.makeText(
                            this@NewVehicleActivity,
                            "Veículo ${newVehicle.name} criado com sucesso!",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                    is Result.Error -> {
                        Toast.makeText(
                            this@NewVehicleActivity,
                            "Erro ao criar veículo",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    companion object {
        const val REQUEST_CODE_CAMERA = 101
        fun newIntent(context: Context) = Intent(context, NewVehicleActivity::class.java)
    }
}