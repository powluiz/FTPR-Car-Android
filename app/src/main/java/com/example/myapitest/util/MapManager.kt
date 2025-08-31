package com.example.myapitest.util

import android.annotation.SuppressLint
import android.content.Context
import com.example.myapitest.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class MapManager(
    private val context: Context,
    private val mapFragment: SupportMapFragment,
    private val onMapReadyAction: ((GoogleMap) -> Unit)? = null
) : OnMapReadyCallback {

    lateinit var googleMap: GoogleMap
    var isMapReady: Boolean = false

    private var currentMarker: Marker? = null
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    init {
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        isMapReady = true
        onMapReadyAction?.invoke(googleMap)
    }

    fun showLocationOnMap(position: LatLng, title: String? = null, zoomLevel: Float = 15f) {
        if (!::googleMap.isInitialized) return

        currentMarker?.remove()
        currentMarker = googleMap.addMarker(
            MarkerOptions()
                .position(position)
                .title(title ?: "Localização")
        )
        googleMap.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                position,
                zoomLevel
            )
        )
    }

    @SuppressLint("MissingPermission")
    fun fetchAndShowCurrentUserLocation(
        onSuccess: (LatLng) -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (!PermissionManager.hasLocationPermission(context)) {
            onFailure(context.getString(R.string.error_request_location_permission))
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    val latLng = LatLng(location.latitude, location.longitude)
                    showLocationOnMap(latLng, context.getString(R.string.success_request_location))
                    onSuccess(latLng)
                } else {
                    onFailure(context.getString(R.string.error_request_location))
                }
            }
            .addOnFailureListener { exception ->
                onFailure(context.getString(R.string.error_request_location) + ": ${exception.message}")
            }
    }
}