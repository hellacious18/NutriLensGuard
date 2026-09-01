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
            is ScanIntent.SendMessage -> sendMessage(intent.message)
            is ScanIntent.UpdateInputText -> _uiState.update { it.copy(inputText = intent.text) }
            is ScanIntent.OnProductScanned -> sendMessage(intent.productName)
            is ScanIntent.ToggleProfileExpanded -> _uiState.update { 
                it.copy(isProfileExpanded = intent.expanded ?: !it.isProfileExpanded) 
            }
            is ScanIntent.ToggleDiabetic -> _uiState.update { it.copy(isDiabetic = intent.enabled) }
            is ScanIntent.ToggleHypertension -> _uiState.update { it.copy(isHypertension = intent.enabled) }
            is ScanIntent.TogglePeanutAllergy -> _uiState.update { it.copy(isPeanutAllergy = intent.enabled) }
            is ScanIntent.ToggleDairyAllergy -> _uiState.update { it.copy(isDairyAllergy = intent.enabled) }
            is ScanIntent.ToggleGlutenIntolerance -> _uiState.update { it.copy(isGlutenIntolerance = intent.enabled) }
            ScanIntent.ClearChat -> _uiState.update { 
                it.copy(
                    messages = listOf(
                        ChatMessage(
                            text = "👋 Chat history cleared. What food product or ingredients would you like me to analyze next?",
                            isUser = false
                        )
                    ),
                    analysisResult = null,
                    errorMessage = null
                )
            }
            ScanIntent.ResetScan -> _uiState.update { it.copy(analysisResult = null, errorMessage = null) }
        }
    }

    private fun sendMessage(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank() || _uiState.value.isLoading) return

        val userMessage = ChatMessage(text = trimmed, isUser = true)
        _uiState.update { state ->
            state.copy(
                messages = state.messages + userMessage,
                inputText = "",
                isLoading = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                val response = api.analyzeProduct(
                    ScanApiRequest(
                        product_name = trimmed,
                        diabetic = currentState.isDiabetic,
                        hypertension = currentState.isHypertension,
                        peanut_allergy = currentState.isPeanutAllergy,
                        dairy_allergy = currentState.isDairyAllergy,
                        gluten_intolerance = currentState.isGlutenIntolerance
                    )
                )
                val aiMessage = ChatMessage(
                    text = response.analysis,
                    isUser = false
                )
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        messages = it.messages + aiMessage,
                        analysisResult = response.analysis
                    ) 
                }
            } catch (e: Exception) {
                val errorText = e.localizedMessage ?: "Network error"
                val errorMessage = ChatMessage(
                    text = "⚠️ **Error analyzing product:**\n$errorText\n\nPlease make sure your backend is active and try again.",
                    isUser = false,
                    isError = true
                )
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        messages = it.messages + errorMessage,
                        errorMessage = errorText
                    ) 
                }
            }
        }
    }
}