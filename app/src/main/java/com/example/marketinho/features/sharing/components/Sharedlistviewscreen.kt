package com.example.marketinho.features.sharing.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.marketinho.data.models.ProductCategory
import com.example.marketinho.data.models.SharedList
import com.example.marketinho.data.models.SharedProduct
import com.example.marketinho.features.product.components.CategoryBadge

/**
 * Tela para visualizar lista compartilhada (read-only)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedListViewScreen(
    sharedList: SharedList,
    onBack: () -> Unit,
    onCopyToMyList: (List<SharedProduct>) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lista Compartilhada") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header com info do dono
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Compartilhado por ${sharedList.ownerName}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (sharedList.marketName != null) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Store,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    sharedList.marketName,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                "${sharedList.products.size} itens",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        Text(
                            "Total: R$ ${"%.2f".format(sharedList.total)}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Lista de produtos
            items(sharedList.products) { product ->
                SharedProductCard(product)
            }

            // Botão para copiar lista
            item {
                Button(
                    onClick = { onCopyToMyList(sharedList.products) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Copiar para Minha Lista")
                }
            }

            // Info de visualizações
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Visualizado ${sharedList.viewCount} vezes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SharedProductCard(product: SharedProduct) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                if (product.category != null) {
                    ProductCategory.fromString(product.category)?.let { category ->
                        CategoryBadge(category = category)
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Qtd: ${product.quantity}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        "R$ ${"%.2f".format(product.price)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    "Subtotal",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "R$ ${"%.2f".format(product.subtotal())}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}