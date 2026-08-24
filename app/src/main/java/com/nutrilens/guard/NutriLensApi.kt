package com.nutrilens.guard

import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

data class ScanApiRequest(
    val product_name: String,
    val diabetic: Boolean,
    val hypertension: Boolean,
    val peanut_allergy: Boolean,
    val dairy_allergy: Boolean,
    val gluten_intolerance: Boolean
)

data class ScanApiResponse(
    val query: String,
    val analysis: String
)


interface NutriLensApi {
    @Headers("bypass-tunnel-reminder: true")
    @POST("api/v1/scan")
    suspend fun analyzeProduct(@Body request: ScanApiRequest): ScanApiResponse
}