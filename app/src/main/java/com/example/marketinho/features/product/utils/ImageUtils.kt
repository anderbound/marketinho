// features/product/utils/ImageUtils.kt
package com.example.marketinho.features.product.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface // Importação necessária
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
     * Este método será aprimorado com a rotação.
     */
    @Throws(IOException::class)
    fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
        return context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it)
        }
    }

    /**
     * Carrega um Bitmap de uma Uri e rotaciona-o de acordo com os metadados EXIF.
     */
    fun getRotatedBitmap(context: Context, imageUri: Uri): Bitmap? {
        var bitmap: Bitmap? = null
        var inputStream = context.contentResolver.openInputStream(imageUri)

        try {
            // Decodifica o Bitmap
            bitmap = BitmapFactory.decodeStream(inputStream)

            // Reseta o inputStream para ler os metadados EXIF
            inputStream?.close() // Feche o anterior
            inputStream = context.contentResolver.openInputStream(imageUri) // Abra um novo para ExifInterface

            if (inputStream != null && bitmap != null) {
                val exifInterface = ExifInterface(inputStream)
                val orientation = exifInterface.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )

                val matrix = Matrix()
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90F)
                    ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180F)
                    ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270F)
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                    ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                    ExifInterface.ORIENTATION_TRANSPOSE -> {
                        matrix.postRotate(90f)
                        matrix.postScale(-1f, 1f)
                    }
                    ExifInterface.ORIENTATION_TRANSVERSE -> {
                        matrix.postRotate(270f)
                        matrix.postScale(-1f, 1f)
                    }
                    else -> {} // Sem rotação ou rotação normal
                }

                if (!matrix.isIdentity) {
                    // Crie um novo bitmap rotacionado
                    val rotatedBitmap = Bitmap.createBitmap(
                        bitmap,
                        0,
                        0,
                        bitmap.width,
                        bitmap.height,
                        matrix,
                        true
                    )
                    bitmap.recycle() // Recicle o bitmap original para liberar memória
                    return rotatedBitmap
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            // Lidar com o erro
        } finally {
            inputStream?.close() // Garanta que o InputStream seja fechado
        }
        return bitmap
    }
}