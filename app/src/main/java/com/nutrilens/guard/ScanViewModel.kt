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
            is ScanIntent.AnalyzeExtractedText -> analyzeScannedOcr(intent.text, intent.title, intent.imageUri)
            is ScanIntent.AnalyzeLink -> analyzeLink(intent.url)
            is ScanIntent.ToggleCameraScanner -> _uiState.update { it.copy(showCameraScanner = intent.show) }
            is ScanIntent.ToggleLinkDialog -> _uiState.update { it.copy(showLinkDialog = intent.show) }
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

    private fun analyzeScannedOcr(rawText: String, title: String?, imageUri: String?) {
        val cleanTitle = title?.takeIf { it.isNotBlank() } ?: "Scanned Food Item"
        val displayText = if (rawText.isNotBlank()) {
            val preview = if (rawText.length > 250) "${rawText.take(250)}..." else rawText
            "📸 **$cleanTitle**\n\n*Extracted Text:*\n```\n$preview\n```"
        } else {
            "📸 **$cleanTitle**"
        }
        val apiQuery = if (rawText.isNotBlank()) "$cleanTitle\nIngredients/Text: $rawText" else cleanTitle
        sendQuery(displayText = displayText, apiQuery = apiQuery, imageUri = imageUri)
    }

    private fun analyzeLink(url: String) {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return
        val displayText = "🔗 $trimmed"
        sendQuery(displayText = displayText, apiQuery = trimmed, linkUrl = trimmed)
    }

    private fun sendMessage(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        val isLink = trimmed.startsWith("http://") || trimmed.startsWith("https://")
        val linkUrl = if (isLink) trimmed else null
        sendQuery(displayText = trimmed, apiQuery = trimmed, linkUrl = linkUrl)
    }

    private fun sendQuery(
        displayText: String,
        apiQuery: String,
        imageUri: String? = null,
        linkUrl: String? = null
    ) {
        if (_uiState.value.isLoading) return

        val userMessage = ChatMessage(
            text = displayText,
            isUser = true,
            imageUri = imageUri,
            linkUrl = linkUrl
        )
        _uiState.update { state ->
            state.copy(
                messages = state.messages + userMessage,
                inputText = "",
                showCameraScanner = false,
                showLinkDialog = false,
                isLoading = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                val response = api.analyzeProduct(
                    ScanApiRequest(
                        product_name = apiQuery,
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
                    text = "⚠️ **Error analyzing food item:**\n$errorText\n\nPlease make sure your backend is running and try again.",
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