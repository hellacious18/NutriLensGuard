package com.nutrilens.guard

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors

object FoodOcrAnalyzer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    val executor = Executors.newSingleThreadExecutor()

    fun analyzeImageUri(
        context: Context,
        uri: Uri,
        onSuccess: (extractedText: String, candidateTitle: String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            val inputImage = InputImage.fromFilePath(context, uri)
            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val (title, fullText) = parseFoodPackagingText(visionText)
                    onSuccess(fullText, title)
                }
                .addOnFailureListener { e ->
                    onError(e)
                }
        } catch (e: Exception) {
            onError(e)
        }
    }

    fun parseFoodPackagingText(visionText: Text): Pair<String, String> {
        val fullText = visionText.text.trim()
        if (fullText.isEmpty()) {
            return Pair("Unknown Product", "")
        }

        // Heuristic to find the product name/title from text blocks:
        // 1. Look for lines that don't look like barcode, weight, date, or nutritional facts header
        val ignoredKeywords = listOf(
            "net wt", "g", "kg", "ml", "mfg", "exp", "batch", "pkd", "mrp", "rs", "₹",
            "ingredients:", "nutrition facts", "per 100g", "servings", "fssai", "lic no"
        )

        var candidateTitle = ""
        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                val lineText = line.text.trim()
                val lower = lineText.lowercase()
                val isIgnored = ignoredKeywords.any { lower.contains(it) }
                if (!isIgnored && lineText.length >= 3 && lineText.any { it.isLetter() }) {
                    if (candidateTitle.isEmpty() || lineText.length > candidateTitle.length) {
                        candidateTitle = lineText
                    }
                }
            }
        }

        if (candidateTitle.isEmpty()) {
            candidateTitle = visionText.textBlocks.firstOrNull()?.text?.lines()?.firstOrNull() ?: "Scanned Product"
        }

        return Pair(candidateTitle, fullText)
    }

    class LiveCameraAnalyzer(
        private val onTextDetected: (candidateTitle: String, fullText: String) -> Unit
    ) : ImageAnalysis.Analyzer {

        private var isProcessing = false

        @OptIn(ExperimentalGetImage::class)
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null && !isProcessing) {
                isProcessing = true
                val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        if (visionText.text.isNotBlank()) {
                            val (title, fullText) = parseFoodPackagingText(visionText)
                            onTextDetected(title, fullText)
                        }
                    }
                    .addOnCompleteListener {
                        isProcessing = false
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }
        }
    }
}
