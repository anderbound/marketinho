// features/product/utils/ImageUtils.kt
package com.example.marketinho.features.product.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.IOException

object ImageUtils {

    /**
     * Cria um URI na galeria para salvar uma nova imagem
     */
    fun createImageUri(context: Context): Uri? {
        val contentResolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "marketinho_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        return contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    }

    /**
     * Extrai o valor numérico de um texto reconhecido pelo OCR
     */
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

    /**
     * Converte Uri para Bitmap (opcional)
     */
    @Throws(IOException::class)
    fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
        return context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it)
        }
    }
}