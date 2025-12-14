package com.example.marketinho.data.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint

/**
 * Modelo para lista compartilhada
 * Armazenada no Firestore em: /shared_lists/{shareId}
 */
data class SharedList(
    val id: String = "",  // ID único para o link (ex: "ABC123")
    val ownerId: String = "",  // UID do dono da lista
    val ownerName: String = "",  // Nome do dono
    val products: List<SharedProduct> = emptyList(),
    val total: Double = 0.0,
    val marketName: String? = null,
    val marketLocation: GeoPoint? = null,
    val createdAt: Timestamp? = null,
    val expiresAt: Timestamp? = null,  // Link expira após X dias
    val viewCount: Int = 0,  // Quantas vezes foi visualizado
    val status: String = "active"  // active, expired, deleted
)

/**
 * Produto simplificado para compartilhamento
 */
data class SharedProduct(
    val name: String = "",
    val quantity: Int = 1,
    val price: Double = 0.0,
    val category: String? = null,
    val imageUri: String? = null
) {
    // Calcula subtotal
    fun subtotal(): Double = price * quantity
}