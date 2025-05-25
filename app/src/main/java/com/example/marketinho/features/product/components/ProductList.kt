package com.example.marketinho.features.product.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.marketinho.features.product.Product

@Composable
fun ProductList(
    products: List<Product>,
    onProductUpdated: (Product) -> Unit,
    onProductRemoved: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        items(products) { product ->
            ProductCard(
                product = product,
                onEditClick = { updatedProduct -> onProductUpdated(updatedProduct) },
                onRemoveClick = { onProductRemoved(product) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}