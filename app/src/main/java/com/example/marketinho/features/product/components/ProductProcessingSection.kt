package com.example.marketinho.features.product.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.marketinho.features.ocr.OcrProcessor
import com.example.marketinho.features.product.Product

@Composable
fun ProductProcessingSection(
    imageUri: Uri?,
    onAddProduct: (Product) -> Unit,
    onManualSelection: () -> Unit
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (imageUri != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUri)
                    .crossfade(true)
                    .build(),
                contentDescription = "Foto do produto",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color.LightGray.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Text("Nenhuma imagem selecionada", color = Color.Gray)
            }
        }

        Button(
            onClick = {
                isLoading = true
                OcrProcessor.processImageWithOCR(
                    imageUri = imageUri!!,
                    context = context,
                    onSuccess = { name, price ->
                        onAddProduct(
                            Product(
                                name = name,
                                price = price.toDoubleOrNull() ?: 0.0,
                                imageUri = imageUri.toString() // Convertemos Uri para String aqui
                            )
                        )
                        isLoading = false
                    },
                    onFailure = {
                        isLoading = false
                        onManualSelection()
                    }
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = imageUri != null && !isLoading // Desabilita se não tiver imagem
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White)
            } else {
                Text(if (imageUri == null) "Selecione uma imagem primeiro" else "Ler Automaticamente")
            }
        }
    }
}