package com.example.myapitest.service

import com.example.myapitest.model.Vehicle
import retrofit2.http.GET
import retrofit2.http.Path

interface ApiService {

    @GET("car")
    suspend fun getVehicles(): List<Vehicle>

    @GET("car/{id}")
    suspend fun getVehicleById(@Path("id") id: String): Vehicle
}