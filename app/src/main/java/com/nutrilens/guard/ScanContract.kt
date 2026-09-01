package com.nutrilens.guard

import java.util.UUID

sealed interface ScanIntent {
    data class SendMessage(val message: String) : ScanIntent
    data class UpdateInputText(val text: String) : ScanIntent
    data class OnProductScanned(val productName: String) : ScanIntent
    data class AnalyzeExtractedText(val text: String, val title: String? = null) : ScanIntent
    data class AnalyzeLink(val url: String) : ScanIntent
    data class ToggleCameraScanner(val show: Boolean) : ScanIntent
    data class ToggleLinkDialog(val show: Boolean) : ScanIntent
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
            text = "👋 **Welcome to NutriLens Guard!**\n\nI can analyze packaged foods for hidden ingredients, misleading marketing, and health risks tailored to your dietary profile.\n\n✨ **Options to scan:**\n- 📷 **Live Camera**: Scan food packaging & nutrition labels in real-time\n- 🖼️ **Photo Upload**: Analyze food labels from your gallery with offline AI\n- 🔗 **Product Link**: Paste an e-commerce food link\n- 💬 **Type or Tap Suggestions**: Type any product name below!",
            isUser = false
        )
    ),
    val inputText: String = "",
    val isProfileExpanded: Boolean = false,
    val showCameraScanner: Boolean = false,
    val showLinkDialog: Boolean = false,
    val isDiabetic: Boolean = true,
    val isHypertension: Boolean = true,
    val isPeanutAllergy: Boolean = false,
    val isDairyAllergy: Boolean = false,
    val isGlutenIntolerance: Boolean = false,
    val isLoading: Boolean = false,
    val analysisResult: String? = null,
    val errorMessage: String? = null
)