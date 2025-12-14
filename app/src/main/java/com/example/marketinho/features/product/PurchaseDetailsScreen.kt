package com.example.marketinho.features.product

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.marketinho.data.models.Purchase
import com.example.marketinho.features.sharing.PurchaseShareHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseDetailsScreen(
    purchase: Purchase,
    navController: NavController
) {
    // ✅ ADICIONADO: Estados para compartilhamento
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var isSharing by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Detalhes da Compra",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Voltar")
                    }
                },
                actions = {
                    // ✅ ATUALIZADO: Botão de compartilhar funcional
                    IconButton(
                        onClick = {
                            isSharing = true
                            PurchaseShareHelper.sharePurchase(
                                purchase = purchase,
                                context = context,
                                scope = scope,
                                onSuccess = { url, text ->
                                    isSharing = false
                                    Toast.makeText(
                                        context,
                                        "Link criado! Compartilhe com seus amigos.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onError = { error ->
                                    isSharing = false
                                    Toast.makeText(
                                        context,
                                        "Erro: $error",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        },
                        enabled = !isSharing
                    ) {
                        if (isSharing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        } else {
                            Icon(Icons.Default.Share, "Compartilhar")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Cabeçalho com informações gerais
            item {
                PurchaseHeaderCard(purchase)
            }

            // Título da seção de produtos
            item {
                Text(
                    text = "Produtos (${purchase.totalItems} ${if (purchase.totalItems == 1) "item" else "itens"})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Lista de produtos
            items(purchase.items) { item ->
                ProductItemCard(item)
            }

            // Card do total
            item {
                Spacer(modifier = Modifier.height(8.dp))
                TotalSummaryCard(purchase)
            }
        }
    }
}

@Composable
private fun PurchaseHeaderCard(purchase: Purchase) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Data e hora
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "Data",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                val dateFormatter = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.getDefault())
                Text(
                    text = dateFormatter.format(purchase.date.toDate()),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Mercado
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Store,
                    contentDescription = "Mercado",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Column {
                    Text(
                        text = purchase.marketName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (!purchase.address.isNullOrBlank()) {
                        Text(
                            text = purchase.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Forma de pagamento
            if (!purchase.paymentMethod.isNullOrBlank()) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val paymentIcon = when {
                        purchase.paymentMethod.contains("Dinheiro", ignoreCase = true) ->
                            Icons.Default.AttachMoney
                        purchase.paymentMethod.contains("PIX", ignoreCase = true) ->
                            Icons.Default.AccountBalance
                        purchase.paymentMethod.contains("Vale", ignoreCase = true) ->
                            Icons.Default.CardGiftcard
                        else -> Icons.Default.CreditCard
                    }

                    Icon(
                        imageVector = paymentIcon,
                        contentDescription = "Pagamento",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Pago com ${purchase.paymentMethod}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductItemCard(item: com.example.marketinho.data.models.PurchaseItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Nome e quantidade
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Qtd: ${item.quantity}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "×",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "R$ ${"%.2f".format(item.price)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Categoria e marca (se tiver)
                if (!item.category.isNullOrBlank() || !item.brand.isNullOrBlank()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!item.category.isNullOrBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = MaterialTheme.shapes.extraSmall
                            ) {
                                Text(
                                    text = item.category,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                        if (!item.brand.isNullOrBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = MaterialTheme.shapes.extraSmall
                            ) {
                                Text(
                                    text = item.brand,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }

            // Subtotal
            Text(
                text = "R$ ${"%.2f".format(item.subtotal)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun TotalSummaryCard(purchase: Purchase) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Subtotal (se houver desconto)
            if (purchase.discount > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Subtotal:",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "R$ ${"%.2f".format(purchase.total)}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Desconto:",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "- R$ ${"%.2f".format(purchase.discount)}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)
                )
            }

            // Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TOTAL:",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "R$ ${"%.2f".format(if (purchase.discount > 0) purchase.finalTotal else purchase.total)}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}