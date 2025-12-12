package com.example.marketinho.features.product.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.marketinho.data.models.ProductCategory
import com.example.marketinho.features.product.Product
import com.example.marketinho.features.product.utils.PriceUtils

@Composable
fun ProductCard(
    product: Product,
    onEditClick: (Product) -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val imageUri = remember(product.imageUri) {
        product.imageUri?.let { Uri.parse(it) }
    }

    var showEditDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ProductImage(product.imageUri)
            Spacer(modifier = Modifier.height(8.dp))

            // Nome e categoria
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Badge de categoria
                product.category?.let { categoryName ->
                    ProductCategory.fromString(categoryName)?.let { category ->
                        CategoryBadge(category = category)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            ProductInfo(
                product = product,
                onEditClick = { showEditDialog = true },
                onRemoveClick = onRemoveClick
            )
            Spacer(modifier = Modifier.height(8.dp))
            QuantityControls(
                quantity = product.quantity,
                price = product.price,
                onQuantityChange = { newQuantity ->
                    onEditClick(product.copy(quantity = newQuantity))
                }
            )
        }
    }

    if (showEditDialog) {
        ProductEditDialog(
            product = product,
            onSave = { updatedProduct ->
                onEditClick(updatedProduct)
                showEditDialog = false
            },
            onDismiss = { showEditDialog = false }
        )
    }
}

@Composable
private fun ProductImage(
    imageUri: String?,
    modifier: Modifier = Modifier
) {
    val uri = remember(imageUri) {
        imageUri?.let { Uri.parse(it) }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (uri != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(uri)
                    .crossfade(true)
                    .build(),
                contentDescription = "Imagem do produto",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.AccountBox,
                contentDescription = "Sem imagem",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProductInfo(
    product: Product,
    onEditClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = PriceUtils.format(product.price),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        Row {
            IconButton(
                onClick = onEditClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Edit, "Editar produto")
            }
            IconButton(
                onClick = onRemoveClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remover produto",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun QuantityControls(
    quantity: Int,
    price: Double,
    onQuantityChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Quantidade:", style = MaterialTheme.typography.bodyMedium)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { if (quantity > 1) onQuantityChange(quantity - 1) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Remove, "Diminuir quantidade")
                }

                Text(text = quantity.toString())

                IconButton(
                    onClick = { onQuantityChange(quantity + 1) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Add, "Aumentar quantidade")
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Subtotal: ${PriceUtils.format(price * quantity)}",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
private fun ProductEditDialog(
    product: Product,
    onSave: (Product) -> Unit,
    onDismiss: () -> Unit
) {
    var editedName by remember { mutableStateOf(product.name) }
    var editedPrice by remember(product.price) {
        mutableStateOf(PriceUtils.format(product.price))
    }
    var editedQuantity by remember(product.quantity) {
        mutableStateOf(product.quantity.toString())
    }
    var editedCategory by remember {
        mutableStateOf(
            product.category?.let { ProductCategory.fromString(it) }
        )
    }

    var priceError by remember { mutableStateOf(false) }
    var quantityError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar ${product.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                TextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    label = { Text("Nome") },
                    modifier = Modifier.fillMaxWidth()
                )

                TextField(
                    value = editedPrice,
                    onValueChange = {
                        editedPrice = it
                        priceError = !PriceUtils.isValid(editedPrice)
                    },
                    label = { Text("Preço") },
                    isError = priceError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = {
                        if (priceError) {
                            Text("Preço inválido")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                TextField(
                    value = editedQuantity,
                    onValueChange = {
                        editedQuantity = it
                        quantityError = it.toIntOrNull()?.let { qty -> qty <= 0 } ?: true
                    },
                    label = { Text("Quantidade") },
                    isError = quantityError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = {
                        if (quantityError) {
                            Text("Quantidade deve ser maior que zero")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Dropdown de categoria
                CategoryDropdown(
                    selectedCategory = editedCategory,
                    onCategorySelected = { editedCategory = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        product.copy(
                            name = editedName,
                            price = PriceUtils.parse(editedPrice) ?: product.price,
                            quantity = editedQuantity.toIntOrNull() ?: product.quantity,
                            category = editedCategory?.name
                        )
                    )
                },
                enabled = !priceError && !quantityError
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}