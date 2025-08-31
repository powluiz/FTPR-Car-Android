package com.example.myapitest.service


import retrofit2.HttpException

sealed class Result<out T> {
    data class Success<out T>(val data:T):Result<T>()
    data class Error(val code: Int, val message: String):Result<Nothing>()
}


suspend fun <T> safeApiCall(apiCall: suspend() -> T): Result<T> {
    return try {
        Result.Success(apiCall())
    } catch (error: Exception) {
        when (error) {
            is HttpException -> {
                Result.Error(error.code(), error.message())
            }
            else -> {
                Result.Error(-1, "Erro desconhecido")
            }
        }
    }
}