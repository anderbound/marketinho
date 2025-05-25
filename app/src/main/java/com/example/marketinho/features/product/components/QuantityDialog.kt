package com.example.marketinho.features.product.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType

// features/product/components/QuantityDialog.kt
// components/QuantityDialog.kt
@Composable
fun QuantityDialog(
    initialQuantity: Int = 1,  // Nome mais claro do parâmetro
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var quantity by remember { mutableStateOf(initialQuantity.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Definir Quantidade") },
        text = {
            Column {
                TextField(
                    value = quantity,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.toIntOrNull() != null) {
                            quantity = newValue
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("Quantidade") }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(quantity.toIntOrNull() ?: 1)
            }) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}