package com.example.marketinho.features.location.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.marketinho.data.models.Market
import com.example.marketinho.features.location.LocationInfo
import com.google.firebase.firestore.GeoPoint

/**
 * Dialog melhorado que busca mercados automaticamente
 */
@Composable
fun LocationConfirmationDialog(
    locationInfo: LocationInfo?,
    isLoading: Boolean,
    nearbyMarkets: List<Market>?, // ✅ NOVO: Lista de mercados próximos
    isSearchingMarkets: Boolean,  // ✅ NOVO: Estado de busca de mercados
    onConfirm: (marketName: String, address: String) -> Unit,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    var selectedMarket by remember { mutableStateOf<String?>(null) }
    var showManualInput by remember { mutableStateOf(false) }
    var manualMarketName by remember { mutableStateOf("") }
    var manualAddress by remember { mutableStateOf(locationInfo?.address ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("Confirmar Local da Compra")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when {
                    // 🔄 Carregando localização
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator()
                                Text(
                                    "Obtendo sua localização...",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    // ❌ Erro ao obter localização
                    locationInfo == null -> {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    "Não foi possível obter sua localização",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Verifique se o GPS está ativado e tente novamente",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    // 🔍 Buscando mercados
                    isSearchingMarkets -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            LocationInfoCard(locationInfo)

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator()
                                    Text(
                                        "🔍 Buscando mercados próximos...",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }

                    // ✅ Mercados encontrados
                    !nearbyMarkets.isNullOrEmpty() && !showManualInput -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            LocationInfoCard(locationInfo)

                            Text(
                                "Selecione o mercado:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )

                            // Lista de mercados
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                nearbyMarkets.take(5).forEach { market ->
                                    MarketOptionCard(
                                        market = market,
                                        isSelected = selectedMarket == market.name,
                                        userLocation = locationInfo.geoPoint,
                                        onClick = { selectedMarket = market.name }
                                    )
                                }

                                // Opção manual
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { showManualInput = true },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = null
                                        )
                                        Text(
                                            "Outro mercado (digitar)",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ✏️ Input manual ou nenhum mercado encontrado
                    else -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            LocationInfoCard(locationInfo)

                            if (nearbyMarkets?.isEmpty() == true) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null
                                        )
                                        Text(
                                            "Nenhum mercado encontrado por perto. Digite manualmente:",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = manualMarketName,
                                onValueChange = { manualMarketName = it },
                                label = { Text("Nome do Mercado") },
                                placeholder = { Text("Ex: Extra, Carrefour...") },
                                leadingIcon = {
                                    Icon(Icons.Default.Store, contentDescription = null)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = manualAddress,
                                onValueChange = { manualAddress = it },
                                label = { Text("Endereço (opcional)") },
                                leadingIcon = {
                                    Icon(Icons.Default.LocationOn, contentDescription = null)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 3
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalMarketName = if (showManualInput || nearbyMarkets.isNullOrEmpty()) {
                        manualMarketName
                    } else {
                        selectedMarket ?: ""
                    }

                    val finalAddress = if (showManualInput) {
                        manualAddress.ifBlank { locationInfo?.address ?: "" }
                    } else {
                        locationInfo?.address ?: ""
                    }

                    if (finalMarketName.isNotBlank()) {
                        onConfirm(finalMarketName, finalAddress)
                    }
                },
                enabled = when {
                    isLoading || isSearchingMarkets -> false
                    showManualInput || nearbyMarkets.isNullOrEmpty() -> manualMarketName.isNotBlank()
                    else -> selectedMarket != null
                }
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Confirmar")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (locationInfo == null && !isLoading) {
                    TextButton(onClick = onRetry) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Tentar Novamente")
                    }
                }

                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        }
    )
}

@Composable
private fun LocationInfoCard(locationInfo: LocationInfo?) {
    locationInfo?.let { info ->
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Localização atual",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    info.address,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun MarketOptionCard(
    market: Market,
    isSelected: Boolean,
    userLocation: GeoPoint?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(
                2.dp,
                MaterialTheme.colorScheme.primary
            )
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Store,
                    contentDescription = null,
                    tint = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.size(24.dp)
                )

                Column {
                    Text(
                        text = market.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )

                    if (!market.vicinity.isNullOrEmpty()) {
                        Text(
                            text = market.vicinity,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Distância
            userLocation?.let { location ->
                val distanceMeters = market.distanceTo(location)
                val distanceText = when {
                    distanceMeters < 1000 -> "${distanceMeters.toInt()}m"
                    else -> "${"%.1f".format(distanceMeters / 1000)}km"
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = distanceText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}