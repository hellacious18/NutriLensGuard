package com.nutrilens.guard

import java.util.UUID

sealed interface ScanIntent {
    data class SendMessage(val message: String) : ScanIntent
    data class UpdateInputText(val text: String) : ScanIntent
    data class OnProductScanned(val productName: String) : ScanIntent
    data class ToggleDiabetic(val enabled: Boolean) : ScanIntent
    data class ToggleHypertension(val enabled: Boolean) : ScanIntent
    data class TogglePeanutAllergy(val enabled: Boolean) : ScanIntent
    data class ToggleDairyAllergy(val enabled: Boolean) : ScanIntent
    data class ToggleGlutenIntolerance(val enabled: Boolean) : ScanIntent
    data class ToggleProfileExpanded(val expanded: Boolean? = null) : ScanIntent
    object ClearChat : ScanIntent
    object ResetScan : ScanIntent
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false
)

data class ScanUiState(
    val messages: List<ChatMessage> = listOf(
        ChatMessage(
            text = "👋 **Welcome to NutriLens Guard!**\n\nI can analyze packaged foods for hidden ingredients, misleading marketing, and health risks tailored to your dietary profile.\n\nType a product name below or choose a suggestion to get started!",
            isUser = false
        )
    ),
    val inputText: String = "",
    val isProfileExpanded: Boolean = false,
    val isDiabetic: Boolean = true,
    val isHypertension: Boolean = true,
    val isPeanutAllergy: Boolean = false,
    val isDairyAllergy: Boolean = false,
    val isGlutenIntolerance: Boolean = false,
    val isLoading: Boolean = false,
    val analysisResult: String? = null,
    val errorMessage: String? = null
)