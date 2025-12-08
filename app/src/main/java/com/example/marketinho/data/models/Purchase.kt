package com.example.marketinho.data.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint

// ========== ITEM DE COMPRA ==========
data class PurchaseItem(
    val name: String = "",
    val price: Double = 0.0,
    val quantity: Int = 0,
    val category: String? = null,
    val brand: String? = null
) {
    // Calcula subtotal automaticamente
    val subtotal: Double
        get() = price * quantity
}

// ========== COMPRA COMPLETA ==========
data class Purchase(
    @DocumentId
    val id: String = "",

    // USUÁRIO
    val userId: String = "",

    // DATA E HORA
    val date: Timestamp = Timestamp.now(),

    // VALORES
    val total: Double = 0.0,
    val discount: Double = 0.0,
    val finalTotal: Double = total - discount,

    // ========== INFORMAÇÕES DO LOCAL ==========
    val marketName: String = "Mercado Não Identificado",
    val marketLocation: GeoPoint? = null,  // latitude, longitude
    val address: String? = null,           // "Rua XYZ, 123"
    val city: String? = null,              // "São Paulo"
    val state: String? = null,             // "SP"

    // INFORMAÇÕES ADICIONAIS
    val paymentMethod: String? = null,     // "Dinheiro", "Cartão", "PIX"
    val notes: String? = null,             // Observações do usuário

    // ITENS COMPRADOS
    val items: List<PurchaseItem> = emptyList(),

    // METADADOS
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp? = null
) {
    // Quantidade total de itens
    val totalItems: Int
        get() = items.sumOf { it.quantity }

    // Calcula total dos itens (caso total não esteja preenchido)
    val calculatedTotal: Double
        get() = items.sumOf { it.subtotal }
}