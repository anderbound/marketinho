package com.example.marketinho.features.sharing.components

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Dialog para compartilhar lista
 */
@Composable
fun ShareListDialog(
    isGeneratingLink: Boolean,
    shareId: String?,
    shareUrl: String?,
    shareText: String?,
    onDismiss: () -> Unit,
    onGenerateLink: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                "Compartilhar Lista",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when {
                    // Estado: Carregando
                    isGeneratingLink -> {
                        CircularProgressIndicator()
                        Text(
                            "Gerando link de compartilhamento...",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Estado: Link gerado
                    shareId != null && shareUrl != null -> {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )

                                Text(
                                    "Link criado com sucesso!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Divider(modifier = Modifier.padding(vertical = 8.dp))

                                Text(
                                    "Código:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Text(
                                    shareId,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Text(
                                    "Válido por 7 dias",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            "Compartilhe este link com amigos e família!",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )

                        // Botões de compartilhamento
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // WhatsApp
                            FilledTonalButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareText ?: shareUrl)
                                        setPackage("com.whatsapp")
                                    }
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        // WhatsApp não instalado, use compartilhamento genérico
                                        val genericIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, shareText ?: shareUrl)
                                        }
                                        context.startActivity(
                                            Intent.createChooser(genericIntent, "Compartilhar via")
                                        )
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("WhatsApp")
                            }

                            // Outros apps
                            FilledTonalButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareText ?: shareUrl)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(intent, "Compartilhar via")
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Outros")
                            }
                        }
                    }

                    // Estado: Erro ou inicial
                    else -> {
                        Text(
                            "Compartilhe sua lista de compras com amigos e família!",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        "Link seguro e temporário",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        "Válido por 7 dias",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Visibility,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        "Apenas visualização",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (shareId == null && !isGeneratingLink) {
                Button(
                    onClick = onGenerateLink,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Gerar Link")
                }
            }
        },
        dismissButton = {
            if (shareId != null || !isGeneratingLink) {
                TextButton(onClick = onDismiss) {
                    Text(if (shareId != null) "Fechar" else "Cancelar")
                }
            }
        }
    )
}