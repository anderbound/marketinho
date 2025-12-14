package com.example.marketinho.features.sharing

import android.util.Log
import com.example.marketinho.data.models.SharedList
import com.example.marketinho.data.models.SharedProduct
import com.example.marketinho.features.product.Product
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.tasks.await
import java.util.*
import kotlin.random.Random

/**
 * Repository para gerenciar compartilhamento de listas
 */
class SharingRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val sharedListsCollection = firestore.collection("shared_lists")

    /**
     * Gera ID único de 6 caracteres (ex: "A3X9K2")
     */
    private fun generateShareId(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6)
            .map { chars[Random.nextInt(chars.length)] }
            .joinToString("")
    }

    /**
     * Cria uma lista compartilhada
     */
    suspend fun createSharedList(
        products: List<Product>,
        total: Double,
        marketName: String?,
        marketLocation: GeoPoint?,
        expirationDays: Int = 7  // Link expira em 7 dias por padrão
    ): Result<String> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("Usuário não autenticado"))

            // Gera ID único
            var shareId = generateShareId()

            // Verifica se já existe (improvável, mas garante unicidade)
            var exists = sharedListsCollection.document(shareId).get().await().exists()
            while (exists) {
                shareId = generateShareId()
                exists = sharedListsCollection.document(shareId).get().await().exists()
            }

            // Converte produtos para formato compartilhável
            val sharedProducts = products.map { product ->
                SharedProduct(
                    name = product.name,
                    quantity = product.quantity,
                    price = product.price,
                    category = product.category,
                    imageUri = null  // Não compartilha imagens (URI local)
                )
            }

            // Calcula data de expiração
            val now = Timestamp.now()
            val expiresAt = Timestamp(
                Date(now.seconds * 1000 + expirationDays * 24 * 60 * 60 * 1000)
            )

            // Cria documento
            val sharedList = SharedList(
                id = shareId,
                ownerId = currentUser.uid,
                ownerName = currentUser.displayName ?: "Usuário",
                products = sharedProducts,
                total = total,
                marketName = marketName,
                marketLocation = marketLocation,
                createdAt = now,
                expiresAt = expiresAt,
                viewCount = 0,
                status = "active"
            )

            // Salva no Firestore
            sharedListsCollection.document(shareId).set(sharedList).await()

            Log.d("SharingRepository", "✅ Lista compartilhada criada: $shareId")
            Result.success(shareId)

        } catch (e: Exception) {
            Log.e("SharingRepository", "❌ Erro ao criar lista compartilhada", e)
            Result.failure(e)
        }
    }

    /**
     * Busca lista compartilhada por ID
     */
    suspend fun getSharedList(shareId: String): Result<SharedList> {
        return try {
            val doc = sharedListsCollection.document(shareId).get().await()

            if (!doc.exists()) {
                return Result.failure(Exception("Lista não encontrada"))
            }

            val sharedList = doc.toObject(SharedList::class.java)
                ?: return Result.failure(Exception("Erro ao converter dados"))

            // Verifica se expirou
            val now = Timestamp.now()
            if (sharedList.expiresAt != null && sharedList.expiresAt!! < now) {
                return Result.failure(Exception("Este link expirou"))
            }

            if (sharedList.status != "active") {
                return Result.failure(Exception("Esta lista não está mais disponível"))
            }

            // Incrementa contador de visualizações
            sharedListsCollection.document(shareId).update(
                "viewCount", FieldValue.increment(1)
            )

            Log.d("SharingRepository", "✅ Lista compartilhada carregada: $shareId")
            Result.success(sharedList)

        } catch (e: Exception) {
            Log.e("SharingRepository", "❌ Erro ao buscar lista compartilhada", e)
            Result.failure(e)
        }
    }

    /**
     * Deleta lista compartilhada (soft delete)
     */
    suspend fun deleteSharedList(shareId: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("Usuário não autenticado"))

            val doc = sharedListsCollection.document(shareId).get().await()
            val sharedList = doc.toObject(SharedList::class.java)

            // Verifica se é o dono
            if (sharedList?.ownerId != currentUser.uid) {
                return Result.failure(Exception("Apenas o dono pode deletar a lista"))
            }

            // Soft delete
            sharedListsCollection.document(shareId).update(
                mapOf(
                    "status" to "deleted",
                    "deletedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Log.d("SharingRepository", "✅ Lista compartilhada deletada: $shareId")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e("SharingRepository", "❌ Erro ao deletar lista compartilhada", e)
            Result.failure(e)
        }
    }

    /**
     * Gera URL de compartilhamento usando GitHub Pages
     * Link será CLICÁVEL no WhatsApp e abrirá o app automaticamente!
     */
    fun generateShareUrl(shareId: String): String {
        return "https://anderbound.github.io/marketinho-links/$shareId"
    }

    /**
     * Gera texto para compartilhar com link do GitHub Pages
     * O link será CLICÁVEL no WhatsApp e abrirá o app automaticamente!
     */
    fun generateShareText(sharedList: SharedList, shareId: String): String {
        val url = generateShareUrl(shareId)

        return buildString {
            appendLine("🛒 *${sharedList.ownerName}* compartilhou uma lista de compras!")
            appendLine()
            appendLine("📦 *${sharedList.products.size} itens* | 💰 *R$ ${"%.2f".format(sharedList.total)}*")

            if (sharedList.marketName != null) {
                appendLine("🏪 ${sharedList.marketName}")
            }

            appendLine()
            appendLine("👇 *Clique para abrir:*")
            appendLine(url)
            appendLine()
            appendLine("⏰ Válido até ${formatExpirationDate(sharedList.expiresAt)}")
        }
    }

    private fun formatExpirationDate(timestamp: Timestamp?): String {
        if (timestamp == null) return "indeterminado"

        val date = Date(timestamp.seconds * 1000)
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
        return sdf.format(date)
    }
}