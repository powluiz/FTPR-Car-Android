package com.example.myapitest.service

import com.example.myapitest.model.Vehicle
import com.example.myapitest.model.VehicleById
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {

    @GET("car")
    suspend fun getVehicles(): List<Vehicle>

    @GET("car/{id}")
    suspend fun getVehicleById(@Path("id") id: String): VehicleById

    @POST("car")
    suspend fun createVehicle(@Body item: Vehicle): Vehicle

    @PATCH("car/{id}")
    suspend fun updateVehicle(@Path("id") id: String, @Body item: Vehicle): Vehicle

    @DELETE("car/{id}")
    suspend fun deleteVehicle(@Path("id") id: String)
}


