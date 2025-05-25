package com.example.marketinho.features.ocr

import android.content.Context
import android.graphics.Rect
import android.net.Uri
import android.util.Log
import androidx.compose.ui.geometry.Offset
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import kotlin.math.abs

object OcrProcessor {
    suspend fun recognizeTextFromRects(
        imageUri: Uri,
        nameRect: android.graphics.Rect,
        priceRect: android.graphics.Rect,
        context: Context
    ): Pair<String, String> {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromFilePath(context, imageUri)

        return try {
            val visionText = recognizer.process(image).await()
            val name = extractTextFromRect(visionText, nameRect)
            val price = extractTextFromRect(visionText, priceRect)
            Pair(name, price)
        } catch (e: Exception) {
            Log.e("OCR", "Erro no reconhecimento", e)
            Pair("", "")
        }
    }

    fun processImageWithOCRForSelection(
        imageUri: Uri,
        context: Context,
        onSuccess: (List<Pair<android.graphics.Rect, String>>) -> Unit,
        onFailure: () -> Unit
    ) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromFilePath(context, imageUri)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val results = mutableListOf<Pair<android.graphics.Rect, String>>()
                for (block in visionText.textBlocks) {
                    block.lines.forEach { line ->
                        line.boundingBox?.let { rect ->
                            results.add(
                                android.graphics.Rect(
                                    rect.left,
                                    rect.top,
                                    rect.right,
                                    rect.bottom
                                ) to line.text
                            )
                        }
                    }
                }
                if (results.isNotEmpty()) {
                    onSuccess(results)
                } else {
                    onFailure()
                }
            }
            .addOnFailureListener {
                onFailure()
            }
    }

    fun processImageWithOCR(
        imageUri: Uri,
        context: Context,
        onSuccess: (String, String) -> Unit,
        onFailure: () -> Unit
    ) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromFilePath(context, imageUri)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val (name, price) = findPriceAndName(visionText.text)
                if (price != "0.00") {
                    onSuccess(name, price)
                } else {
                    onFailure()
                }
            }
            .addOnFailureListener {
                onFailure()
            }
    }

    private fun extractTextFromRect(
        visionText: Text,
        rect: android.graphics.Rect
    ): String {
        return visionText.textBlocks
            .filter { block ->
                block.boundingBox?.let { box ->
                    rect.contains(box.left, box.top) ||
                            rect.contains(box.right, box.bottom)
                } ?: false
            }
            .joinToString(" ") { it.text }
    }

    private fun findPriceAndName(fullText: String): Pair<String, String> {
        val priceRegex = """(R\$?\s?)(\d+[\.,]\d{1,2})""".toRegex()
        val priceMatch = priceRegex.find(fullText)

        val price = priceMatch?.groups?.get(2)?.value?.replace(',', '.') ?: "0.00"

        val name = fullText.lines()
            .filterNot { line ->
                line.contains(priceRegex) ||
                        line.trim().isEmpty() ||
                        line.trim().matches("""^\d+[\.,]\d{2}$""".toRegex())
            }
            .joinToString(" ")
            .trim()
            .takeIf { it.isNotBlank() } ?: "Produto Desconhecido"

        return Pair(name, price)
    }

    fun isLikelyPrice(text: String): Boolean {
        val cleanText = text.replace(",", ".").replace("R", "").replace("$", "").trim()
        return cleanText.matches("""^\d+\.\d{2}$""".toRegex()) ||
                cleanText.matches("""^\d+$""".toRegex())
    }

    fun extractPriceValue(text: String): String {
        val cleanText = text.replace(Regex("[^0-9,.]"), "")
        val normalizedText = cleanText.replace(',', '.')

        return if (normalizedText.contains('.')) {
            val parts = normalizedText.split('.')
            when {
                parts.size == 1 -> parts[0] + ".00"
                parts.size > 1 -> {
                    val integerPart = parts[0].takeIf { it.isNotEmpty() } ?: "0"
                    val decimalPart = parts[1].take(2).padEnd(2, '0')
                    "$integerPart.$decimalPart"
                }
                else -> "0.00"
            }
        } else {
            if (normalizedText.isEmpty()) "0.00" else "$normalizedText.00"
        }
    }
}