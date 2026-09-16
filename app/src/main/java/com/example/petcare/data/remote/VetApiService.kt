package com.example.petcare.data.remote

import com.example.petcare.data.remote.model.ExternalPetRecord
import retrofit2.http.GET

interface VetApiService {
    @GET("pet")
    suspend fun getPets(): List<ExternalPetRecord>
}
