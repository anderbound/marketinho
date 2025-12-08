// features/product/CheckoutScreen.kt
package com.example.marketinho.features.product

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CheckoutScreen(
    // Por enquanto, não vamos passar a lista de produtos aqui.
    // Vamos discutir a melhor forma de fazer isso no próximo passo (navegação).
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "Tela de Finalização de Compras (Checkout)")
        Text(text = "Aqui você verá o resumo da sua compra e opções para finalizar.")
        // TODO: Adicionar a lógica para exibir os produtos e o total.
    }
}