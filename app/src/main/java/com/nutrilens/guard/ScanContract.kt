package com.nutrilens.guard

sealed interface ScanIntent {
    data class OnProductScanned(val productName: String) : ScanIntent
    data class ToggleDiabetic(val enabled: Boolean) : ScanIntent
    data class ToggleHypertension(val enabled: Boolean) : ScanIntent
    object ResetScan : ScanIntent
}

data class ScanUiState(
    val scannedText: String = "",
    val isDiabetic: Boolean = true,
    val isHypertension: Boolean = true,
    val isLoading: Boolean = false,
    val analysisResult: String? = null,
    val errorMessage: String? = null
)