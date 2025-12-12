package com.example.marketinho.features.product.components

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.marketinho.data.models.ProductCategory
import com.example.marketinho.features.ocr.OcrProcessor
import com.example.marketinho.features.product.ProductViewModel
import com.example.marketinho.features.product.utils.GeometryUtils.toImageCoordinates
import com.example.marketinho.features.product.utils.GeometryUtils.transformToCanvasBounds
import com.example.marketinho.features.product.utils.ImageUtils
import kotlin.math.roundToInt

@Composable
fun ImageMarkingScreen(
    viewModel: ProductViewModel,
    imageUri: Uri,
    onSelectionDone: (String, String, Int, String?) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var selectionStep by remember { mutableStateOf("name") } // "name", "price" ou "category"
    var detectedTexts by remember { mutableStateOf<List<Pair<Rect, String>>>(emptyList()) }
    var selectedName by remember { mutableStateOf<Pair<Rect, String>?>(null) }
    var selectedPrice by remember { mutableStateOf<Pair<Rect, String>?>(null) }
    var selectedCategory by remember { mutableStateOf<ProductCategory?>(null) }
    var quantity by remember { mutableStateOf(1) }
    var showQuantityDialog by remember { mutableStateOf(false) }

    val bitmap = remember(imageUri) {
        try {
            ImageUtils.getRotatedBitmap(context, imageUri)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    val imageSize = remember(bitmap) {
        if (bitmap != null) {
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
                detectedTexts = androidTexts.map { (rect, text) ->
                    rect.toComposeRect() to text
                }
            },
            onFailure = {
                Toast.makeText(context, "Falha ao ler texto", Toast.LENGTH_SHORT).show()
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Título do passo atual
        Text(
            text = when (selectionStep) {
                "name" -> "1️⃣ Toque para selecionar o NOME do produto"
                "price" -> "2️⃣ Toque para selecionar o PREÇO do produto"
                else -> "3️⃣ Selecione a categoria (opcional)"
            },
            style = MaterialTheme.typography.titleMedium
        )

        // Canvas com imagem (apenas para nome e preço)
        if (selectionStep != "category") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(imageSize.width / imageSize.height)
                    .pointerInput(selectionStep, detectedTexts) {
                        detectTapGestures { tapOffset ->
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
                                            val cleanPrice = ImageUtils.extractPriceValue(text)
                                            selectedPrice = rect to cleanPrice
                                            selectionStep = "category"
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

                        detectedTexts.forEach { (rect, _) ->
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
        }

        // Informações selecionadas
        selectedName?.let { (_, text) ->
            Text("✅ Nome: $text", style = MaterialTheme.typography.bodyLarge)
        }

        selectedPrice?.let { (_, text) ->
            Text("✅ Preço: R$ $text", style = MaterialTheme.typography.bodyLarge)
        }

        // ✅ Dropdown de Categoria (aparece após selecionar nome e preço)
        if (selectionStep == "category") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Categoria (opcional):",
                        style = MaterialTheme.typography.titleSmall
                    )

                    // ✅ Usando seu CategoryDropdown existente
                    CategoryDropdown(
                        selectedCategory = selectedCategory,
                        onCategorySelected = { selectedCategory = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (selectedCategory != null) {
                        CategoryBadge(
                            category = selectedCategory!!,
                            modifier = Modifier.align(androidx.compose.ui.Alignment.Start)
                        )
                    }
                }
            }
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
                        Text("← Voltar")
                    }
                }
                "category" -> {
                    Button(
                        onClick = { selectionStep = "price" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Text("← Voltar")
                    }
                }
                else -> {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            Button(
                onClick = {
                    if (selectionStep == "category" && selectedName != null && selectedPrice != null) {
                        showQuantityDialog = true
                    }
                },
                enabled = selectionStep == "category" && selectedName != null && selectedPrice != null,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    when (selectionStep) {
                        "category" -> "Confirmar →"
                        else -> "Selecione na imagem"
                    }
                )
            }
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
                        newQuantity,
                        selectedCategory?.name
                    )
                },
                onDismiss = { showQuantityDialog = false }
            )
        }
    }
}