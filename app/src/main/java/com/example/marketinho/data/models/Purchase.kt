// com.example.marketinho.data/models/Purchase.kt (ou o caminho que preferir)
package com.example.marketinho.data.models

import com.google.firebase.firestore.DocumentId
import java.util.Date

// Representa um item individual dentro de uma compra (para ser aninhado ou em subcoleção)
data class PurchaseItem(
    val name: String = "",
    val price: Double = 0.0,
    val quantity: Int = 0
)

// Representa uma compra completa no Firestore
data class Purchase(
    @DocumentId // Isso mapeia o ID do documento do Firestore para esta propriedade
    val id: String = "", // O ID será gerado pelo Firestore
    val date: Date = Date(), // Data e hora da compra
    val total: Double = 0.0,
    val marketName: String = "", // Nome do mercado, a ser preenchido pela geolocalização
    val userId: String = "", // Para associar a compra ao usuário autenticado
    val items: List<PurchaseItem> = emptyList() // A lista de produtos comprados
)