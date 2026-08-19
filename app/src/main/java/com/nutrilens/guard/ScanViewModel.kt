package com.nutrilens.guard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ScanViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    // Base URL for Cloud Shell Web Preview or local network
    // ScanViewModel.kt
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val api = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL) // Loaded securely from local.properties
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(NutriLensApi::class.java)

    fun processIntent(intent: ScanIntent) {
        when (intent) {
            is ScanIntent.OnProductScanned -> triggerAnalysis(intent.productName)
            is ScanIntent.ToggleDiabetic -> _uiState.update { it.copy(isDiabetic = intent.enabled) }
            is ScanIntent.ToggleHypertension -> _uiState.update { it.copy(isHypertension = intent.enabled) }
            ScanIntent.ResetScan -> _uiState.update { it.copy(analysisResult = null, errorMessage = null) }
        }
    }

    private fun triggerAnalysis(productName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(scannedText = productName, isLoading = true, errorMessage = null) }
            try {
                val response = api.analyzeProduct(
                    ScanApiRequest(
                        product_name = productName,
                        diabetic = _uiState.value.isDiabetic,
                        hypertension = _uiState.value.isHypertension
                    )
                )
                _uiState.update { it.copy(isLoading = false, analysisResult = response.analysis) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage ?: "Network error") }
            }
        }
    }
}