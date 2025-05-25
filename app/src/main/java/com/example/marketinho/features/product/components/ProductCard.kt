package com.example.marketinho.features.product.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import com.example.marketinho.features.product.Product
import com.example.marketinho.features.product.utils.PriceUtils
import androidx.compose.runtime.remember

@Composable
fun ProductCard(
    product: Product,
    onEditClick: (Product) -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Converte a String de imageUri para Uri quando necessário
    val imageUri = remember(product.imageUri) {
        product.imageUri?.let { Uri.parse(it) }
    }

    var showEditDialog by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf(product.name) }
    var editedPrice by remember(product.price) {
        mutableStateOf(PriceUtils.format(product.price))
    }
    var editedQuantity by remember(product.quantity) {
        mutableStateOf(product.quantity.toString())
    }
    var priceError by remember { mutableStateOf(false) }
    var quantityError by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ProductImage(product.imageUri)
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
            editedName = editedName,
            editedPrice = editedPrice,
            editedQuantity = editedQuantity,
            priceError = priceError,
            quantityError = quantityError,
            onNameChange = { editedName = it },
            onPriceChange = {
                editedPrice = it
                priceError = !PriceUtils.isValid(editedPrice)
            },
            onQuantityChange = {
                editedQuantity = it
                quantityError = it.toIntOrNull()?.let { qty -> qty <= 0 } ?: true
            },
            onSave = {
                onEditClick(
                    product.copy(
                        name = editedName,
                        price = PriceUtils.parse(editedPrice) ?: product.price,
                        quantity = editedQuantity.toIntOrNull() ?: product.quantity
                    )
                )
                showEditDialog = false
            },
            onDismiss = { showEditDialog = false }
        )
    }
}

@Composable
private fun ProductImage(
    imageUri: String?,  // Alterado para String?
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
                text = product.name,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = PriceUtils.format(product.price),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
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
                    Icon(Icons.Default.Delete, "Diminuir quantidade")
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
    editedName: String,
    editedPrice: String,
    editedQuantity: String,
    priceError: Boolean,
    quantityError: Boolean,
    onNameChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar ${product.name}") },
        text = {
            Column {
                TextField(
                    value = editedName,
                    onValueChange = onNameChange,
                    label = { Text("Nome") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                TextField(
                    value = editedPrice,
                    onValueChange = onPriceChange,
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

                Spacer(Modifier.height(16.dp))

                TextField(
                    value = editedQuantity,
                    onValueChange = onQuantityChange,
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
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
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