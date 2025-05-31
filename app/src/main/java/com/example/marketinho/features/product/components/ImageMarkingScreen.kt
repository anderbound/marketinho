package com.example.marketinho.features.product.components

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.marketinho.features.ocr.OcrProcessor
import com.example.marketinho.features.product.ProductViewModel
import com.example.marketinho.features.product.utils.GeometryUtils.toImageCoordinates
import com.example.marketinho.features.product.utils.GeometryUtils.transformToCanvasBounds
import kotlin.math.roundToInt
import com.example.marketinho.features.product.utils.ImageUtils // Importe ImageUtils


@Composable
fun ImageMarkingScreen(
    viewModel: ProductViewModel,
    imageUri: Uri,
    onSelectionDone: (String, String, Int) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var selectionStep by remember { mutableStateOf("name") } // "name" ou "price"
    var detectedTexts by remember { mutableStateOf<List<Pair<Rect, String>>>(emptyList()) }
    var selectedName by remember { mutableStateOf<Pair<Rect, String>?>(null) }
    var selectedPrice by remember { mutableStateOf<Pair<Rect, String>?>(null) }
    var quantity by remember { mutableStateOf(1) }
    var showQuantityDialog by remember { mutableStateOf(false) }

    // Use remember para carregar o bitmap e rotacioná-lo apenas uma vez
    val bitmap = remember(imageUri) {
        try {
            // AQUI ESTÁ A MUDANÇA: Use a nova função getRotatedBitmap
            ImageUtils.getRotatedBitmap(context, imageUri)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    val imageSize = remember(bitmap) {
        if (bitmap != null) {
            // O imageSize agora reflete o tamanho do bitmap JÁ ROTACIONADO
            Size(bitmap.width.toFloat(), bitmap.height.toFloat())
        } else {
            Size.Zero
        }
    }

    LaunchedEffect(imageUri) {
        OcrProcessor.processImageWithOCRForSelection(
            imageUri = imageUri,
            context = context,
            onSuccess = { androidTexts ->
                // Converter List<Pair<android.graphics.Rect, String>> para List<Pair<androidx.compose.ui.geometry.Rect, String>>
                detectedTexts = androidTexts.map { (rect, text) ->
                    rect.toComposeRect() to text
                }
            },
            onFailure = { Toast.makeText(context, "Falha ao ler texto", Toast.LENGTH_SHORT).show() }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = when (selectionStep) {
                "name" -> "Toque para selecionar o NOME do produto"
                else -> "Toque para selecionar o PREÇO do produto"
            },
            style = MaterialTheme.typography.titleMedium
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                // A proporção agora usa o tamanho do bitmap já rotacionado
                .aspectRatio(imageSize.width / imageSize.height)
                .pointerInput(selectionStep, detectedTexts) {
                    detectTapGestures { tapOffset ->
                        // As coordenadas de toque precisam ser transformadas para a imagem rotacionada
                        // Se o ML Kit já lida com a orientação, as coordenadas do OCR
                        // já devem estar corretas em relação à imagem "lógica".
                        val canvasSize = Size(size.width.toFloat(), size.height.toFloat())
                        val imageOffset = tapOffset.toImageCoordinates(imageSize, canvasSize)

                        if (imageOffset != Offset.Zero) {
                            detectedTexts.firstOrNull { (rect, _) ->
                                rect.contains(imageOffset)
                            }?.let { (rect, text) ->
                                when (selectionStep) {
                                    "name" -> {
                                        selectedName = rect to text
                                        selectionStep = "price"
                                    }

                                    "price" -> {
                                        // Garante a conversão correta do preço
                                        val cleanPrice = ImageUtils.extractPriceValue(text)
                                        selectedPrice = rect to cleanPrice
                                    }
                                }
                            }
                        }
                    }
                }
                .drawBehind {
                    val canvasSize = Size(size.width.toFloat(), size.height.toFloat())

                    bitmap?.let { bmp ->
                        val imageBitmap = bmp.asImageBitmap()
                        val imageRatio = bmp.width.toFloat() / bmp.height.toFloat()
                        val canvasRatio = canvasSize.width / canvasSize.height

                        val (width, height) = if (imageRatio > canvasRatio) {
                            Pair(canvasSize.width, canvasSize.width / imageRatio)
                        } else {
                            Pair(canvasSize.height * imageRatio, canvasSize.height)
                        }

                        val offsetX = (canvasSize.width - width) / 2
                        val offsetY = (canvasSize.height - height) / 2

                        drawImage(
                            image = imageBitmap,
                            dstOffset = IntOffset(offsetX.roundToInt(), offsetY.roundToInt()),
                            dstSize = IntSize(width.roundToInt(), height.roundToInt())
                        )
                    }

                    // 4. Destacar apenas o que é relevante para a etapa atual
                    detectedTexts.forEach { (rect, text) ->
                        val transformedRect = rect.transformToCanvasBounds(imageSize, canvasSize)
                        drawRect(
                            color = Color.Green.copy(alpha = 0.3f),
                            topLeft = transformedRect.topLeft,
                            size = transformedRect.size,
                            style = Fill
                        )
                        drawRect(
                            color = Color.Green,
                            topLeft = transformedRect.topLeft,
                            size = transformedRect.size,
                            style = Stroke(width = 1f)
                        )
                    }

                    // Destaca as seleções
                    selectedName?.let { (rect, _) ->
                        val transformedRect = rect.transformToCanvasBounds(imageSize, canvasSize)
                        drawRect(
                            color = Color.Green.copy(alpha = 0.5f),
                            topLeft = transformedRect.topLeft,
                            size = transformedRect.size,
                            style = Fill
                        )
                        drawRect(
                            color = Color.Green,
                            topLeft = transformedRect.topLeft,
                            size = transformedRect.size,
                            style = Stroke(width = 3f)
                        )
                    }

                    selectedPrice?.let { (rect, _) ->
                        val transformedRect = rect.transformToCanvasBounds(imageSize, canvasSize)
                        drawRect(
                            color = Color.Blue.copy(alpha = 0.5f),
                            topLeft = transformedRect.topLeft,
                            size = transformedRect.size,
                            style = Fill
                        )
                        drawRect(
                            color = Color.Blue,
                            topLeft = transformedRect.topLeft,
                            size = transformedRect.size,
                            style = Stroke(width = 3f)
                        )
                    }
                }
        )

        // Mostra as seleções
        selectedName?.let { (_, text) ->
            Text("Nome selecionado: $text", style = MaterialTheme.typography.bodyLarge)
        }

        selectedPrice?.let { (_, text) ->
            Text("Preço selecionado: $text", style = MaterialTheme.typography.bodyLarge)
        }

        // Botões de navegação
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text("Cancelar")
            }

            when (selectionStep) {
                "price" -> {
                    Button(
                        onClick = { selectionStep = "name" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Text("Voltar ao Nome")
                    }
                }

                "quantity" -> {
                    Button(
                        onClick = { selectionStep = "price" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Text("Voltar ao Preço")
                    }
                }

                else -> {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            Button(
                onClick = {
                    if (selectedName != null && selectedPrice != null) {
                        showQuantityDialog = true
                    }
                },
                enabled = selectedName != null && selectedPrice != null
            ) {
                Text("Confirmar")
            }

            // Diálogo de quantidade
            if (showQuantityDialog) {
                QuantityDialog(
                    initialQuantity = quantity,
                    onConfirm = { newQuantity ->
                        quantity = newQuantity
                        showQuantityDialog = false
                        onSelectionDone(
                            selectedName!!.second,
                            selectedPrice!!.second,
                            newQuantity
                        )
                    },
                    onDismiss = { showQuantityDialog = false }
                )
            }
        }
    }
}