package com.example.myapitest.model

data class Vehicle (
    val id: String,
    val imageUrl: String,
    val year: String,
    val name: String,
    val licence: String,
    val place: Location,
)

data class VehicleById (
    val id: String,
    val value: Vehicle
)