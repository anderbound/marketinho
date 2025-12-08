package com.example.marketinho.features.product

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CheckoutScreen(
    viewModel: ProductViewModel,
    onFinalizePurchase: () -> Unit, // Callback para finalizar a compra
    onCancel: () -> Unit // Callback para cancelar e voltar
) {
    // Observa os produtos e o total do Room via Flow
    val products by viewModel.products.collectAsState(initial = emptyList())
    val total by viewModel.total.collectAsState(initial = 0.0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "Resumo da Compra", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        if (products.isEmpty()) {
            Text(text = "Sua lista de compras está vazia.")
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
            ) {
                Text("Voltar")
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) { // Usa weight para empurrar os botões para baixo
                items(products) { product ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "${product.name} (${product.quantity}x)")
                        Text(text = "R$ ${String.format("%.2f", product.price * product.quantity)}")
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Total: R$ ${String.format("%.2f", total)}", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Button(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                ) {
                    Text("Voltar")
                }
                Button(
                    onClick = onFinalizePurchase, // Chama o callback para finalizar
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                ) {
                    Text("Confirmar Compra")
                }
            }
        }
    }
}