package com.example.marketinho.features.product.components

import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
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
import androidx.compose.ui.text.font.FontWeight
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
    var selectionStep by remember { mutableStateOf("name") }
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
            .windowInsetsPadding(WindowInsets.systemBars) // ✅ Evita botões do sistema
            .padding(horizontal = 16.dp)
            .padding(top = 24.dp, bottom = 16.dp), // ✅ Espaçamento extra
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ✅ Card com instruções visuais melhoradas
        InstructionCard(
            step = selectionStep,
            selectedName = selectedName?.second,
            selectedPrice = selectedPrice?.second
        )

        // Canvas com imagem
        if (selectionStep != "category") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
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

                            // Desenha imagem
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

                            // ✅ Destaca apenas textos relevantes para o passo atual
                            detectedTexts.forEach { (rect, text) ->
                                val transformedRect = rect.transformToCanvasBounds(imageSize, canvasSize)

                                // Cor baseada no passo e tipo de texto
                                val (borderColor, fillColor) = when {
                                    selectionStep == "name" -> {
                                        Pair(Color(0xFF4CAF50), Color(0xFF4CAF50).copy(alpha = 0.2f))
                                    }
                                    selectionStep == "price" && OcrProcessor.isLikelyPrice(text) -> {
                                        Pair(Color(0xFF2196F3), Color(0xFF2196F3).copy(alpha = 0.2f))
                                    }
                                    selectionStep == "price" -> {
                                        Pair(Color.Gray.copy(alpha = 0.3f), Color.Gray.copy(alpha = 0.1f))
                                    }
                                    else -> {
                                        Pair(Color.Gray.copy(alpha = 0.3f), Color.Transparent)
                                    }
                                }

                                drawRect(
                                    color = fillColor,
                                    topLeft = transformedRect.topLeft,
                                    size = transformedRect.size,
                                    style = Fill
                                )
                                drawRect(
                                    color = borderColor,
                                    topLeft = transformedRect.topLeft,
                                    size = transformedRect.size,
                                    style = Stroke(width = 2f)
                                )
                            }

                            // ✅ Destaca seleção do NOME com borda verde grossa
                            selectedName?.let { (rect, _) ->
                                val transformedRect = rect.transformToCanvasBounds(imageSize, canvasSize)
                                drawRect(
                                    color = Color(0xFF4CAF50).copy(alpha = 0.4f),
                                    topLeft = transformedRect.topLeft,
                                    size = transformedRect.size,
                                    style = Fill
                                )
                                drawRect(
                                    color = Color(0xFF4CAF50),
                                    topLeft = transformedRect.topLeft,
                                    size = transformedRect.size,
                                    style = Stroke(width = 4f)
                                )
                            }

                            // ✅ Destaca seleção do PREÇO com borda azul grossa
                            selectedPrice?.let { (rect, _) ->
                                val transformedRect = rect.transformToCanvasBounds(imageSize, canvasSize)
                                drawRect(
                                    color = Color(0xFF2196F3).copy(alpha = 0.4f),
                                    topLeft = transformedRect.topLeft,
                                    size = transformedRect.size,
                                    style = Fill
                                )
                                drawRect(
                                    color = Color(0xFF2196F3),
                                    topLeft = transformedRect.topLeft,
                                    size = transformedRect.size,
                                    style = Stroke(width = 4f)
                                )
                            }
                        }
                )
            }
        }

        // ✅ Preview das seleções (sempre visível quando há seleções)
        AnimatedVisibility(
            visible = selectedName != null || selectedPrice != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            SelectionPreviewCard(
                selectedName = selectedName?.second,
                selectedPrice = selectedPrice?.second
            )
        }

        // ✅ Seleção de Categoria
        AnimatedVisibility(
            visible = selectionStep == "category",
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            CategorySelectionCard(
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // ✅ Botões de ação melhorados (com padding do fundo)
        Column(modifier = Modifier.padding(bottom = 8.dp)) {
            ActionButtons(
                selectionStep = selectionStep,
                canConfirm = selectedName != null && selectedPrice != null,
                onCancel = onCancel,
                onBack = {
                    selectionStep = when (selectionStep) {
                        "price" -> "name"
                        "category" -> "price"
                        else -> "name"
                    }
                },
                onConfirm = {
                    if (selectionStep == "category") {
                        showQuantityDialog = true
                    }
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

// ✅ Card com instruções visuais
@Composable
private fun InstructionCard(
    step: String,
    selectedName: String?,
    selectedPrice: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (step) {
                "name" -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                "price" -> Color(0xFF2196F3).copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.tertiaryContainer
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ícone animado
            val scale by rememberInfiniteTransition(label = "pulse").animateFloat(
                initialValue = 1f,
                targetValue = 1.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .scale(if (step != "category") scale else 1f)
                    .background(
                        color = when (step) {
                            "name" -> Color(0xFF4CAF50)
                            "price" -> Color(0xFF2196F3)
                            else -> MaterialTheme.colorScheme.tertiary
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (step) {
                        "name" -> Icons.Default.Label
                        "price" -> Icons.Default.AttachMoney
                        else -> Icons.Default.Category
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (step) {
                        "name" -> "PASSO 1: Selecione o Nome"
                        "price" -> "PASSO 2: Selecione o Preço"
                        else -> "PASSO 3: Categoria (opcional)"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = when (step) {
                        "name" -> "Toque na área verde com o nome do produto"
                        "price" -> "Toque na área azul com o valor em R$"
                        else -> "Escolha uma categoria para organizar melhor"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ✅ Card com preview das seleções
@Composable
private fun SelectionPreviewCard(
    selectedName: String?,
    selectedPrice: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Selecionado:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            selectedName?.let {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "Nome: $it",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            selectedPrice?.let {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "Preço: R$ $it",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF2196F3)
                    )
                }
            }
        }
    }
}

// ✅ Card de seleção de categoria
@Composable
private fun CategorySelectionCard(
    selectedCategory: ProductCategory?,
    onCategorySelected: (ProductCategory?) -> Unit
) {
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
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            CategoryDropdown(
                selectedCategory = selectedCategory,
                onCategorySelected = onCategorySelected,
                modifier = Modifier.fillMaxWidth()
            )

            if (selectedCategory != null) {
                CategoryBadge(
                    category = selectedCategory,
                    modifier = Modifier.align(Alignment.Start)
                )
            }
        }
    }
}

// ✅ Botões de ação melhorados
@Composable
private fun ActionButtons(
    selectionStep: String,
    canConfirm: Boolean,
    onCancel: () -> Unit,
    onBack: () -> Unit,
    onConfirm: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Botão Cancelar
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            ),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp) // ✅ Padding reduzido
        ) {
            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Cancelar", maxLines = 1) // ✅ Força uma linha
        }

        // Botão Voltar (aparece quando necessário)
        if (selectionStep != "name") {
            FilledTonalButton(
                onClick = onBack,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp) // ✅ Padding reduzido
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Voltar", maxLines = 1) // ✅ Força uma linha
            }
        }

        // Botão Confirmar
        Button(
            onClick = onConfirm,
            enabled = canConfirm && selectionStep == "category",
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50)
            ),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp) // ✅ Padding reduzido
        ) {
            // ✅ Ícone DEPOIS do texto para economizar espaço
            Text(
                if (selectionStep == "category") "Confirmar" else "Aguarde",
                fontWeight = FontWeight.Bold,
                maxLines = 1 // ✅ Força uma linha
            )
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}