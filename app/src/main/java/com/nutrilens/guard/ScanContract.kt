package com.nutrilens.guard

sealed interface ScanIntent {
    data class OnProductScanned(val productName: String) : ScanIntent
    data class ToggleDiabetic(val enabled: Boolean) : ScanIntent
    data class ToggleHypertension(val enabled: Boolean) : ScanIntent
    data class TogglePeanutAllergy(val enabled: Boolean) : ScanIntent
    data class ToggleDairyAllergy(val enabled: Boolean) : ScanIntent
    data class ToggleGlutenIntolerance(val enabled: Boolean) : ScanIntent
    object ResetScan : ScanIntent
}

data class ScanUiState(
    val scannedText: String = "",
    val isDiabetic: Boolean = true,
    val isHypertension: Boolean = true,
    val isPeanutAllergy: Boolean = false,
    val isDairyAllergy: Boolean = false,
    val isGlutenIntolerance: Boolean = false,
    val isLoading: Boolean = false,
    val analysisResult: String? = null,
    val errorMessage: String? = null
)