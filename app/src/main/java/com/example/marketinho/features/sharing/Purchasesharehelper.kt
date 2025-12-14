package com.example.marketinho.features.sharing

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.marketinho.data.models.Purchase
import com.example.marketinho.features.product.Product
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Helper para compartilhar compras do histórico
 */
object PurchaseShareHelper {

    /**
     * Compartilha uma compra do histórico
     */
    fun sharePurchase(
        purchase: Purchase,
        context: Context,
        scope: CoroutineScope,
        onSuccess: (String, String) -> Unit = { _, _ -> },
        onError: (String) -> Unit = {}
    ) {
        val sharingRepository = SharingRepository()

        scope.launch {
            try {
                // Converte produtos da compra para formato Product
                val products = purchase.items.map { item ->
                    Product(
                        name = item.name,
                        quantity = item.quantity,
                        price = item.price,
                        category = item.category,
                        imageUri = null
                    )
                }

                // Cria lista compartilhada
                val result = sharingRepository.createSharedList(
                    products = products,
                    total = purchase.total,
                    marketName = purchase.marketName,
                    marketLocation = purchase.marketLocation
                )

                result.onSuccess { shareId ->
                    // Gera URL
                    val shareUrl = sharingRepository.generateShareUrl(shareId)

                    // Busca lista para gerar texto
                    sharingRepository.getSharedList(shareId).onSuccess { sharedList ->
                        val shareText = sharingRepository.generateShareText(sharedList, shareId)

                        Log.d("PurchaseShare", "✅ Link criado: $shareUrl")

                        // Callback com URL e texto
                        onSuccess(shareUrl, shareText)

                        // Abre compartilhamento
                        shareViaIntent(context, shareText)
                    }
                }.onFailure { error ->
                    Log.e("PurchaseShare", "❌ Erro ao compartilhar", error)
                    onError(error.message ?: "Erro ao gerar link")
                }

            } catch (e: Exception) {
                Log.e("PurchaseShare", "❌ Erro ao compartilhar", e)
                onError(e.message ?: "Erro desconhecido")
            }
        }
    }

    /**
     * Abre dialog de compartilhamento do Android
     */
    private fun shareViaIntent(context: Context, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }

        val chooser = Intent.createChooser(intent, "Compartilhar lista via")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    /**
     * Compartilha via WhatsApp especificamente
     */
    fun shareViaWhatsApp(context: Context, text: String) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                setPackage("com.whatsapp")
            }

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)

        } catch (e: Exception) {
            Log.e("PurchaseShare", "WhatsApp não instalado, usando compartilhamento genérico")
            shareViaIntent(context, text)
        }
    }
}