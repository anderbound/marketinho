package com.example.marketinho.data.models

import com.google.firebase.firestore.GeoPoint
import com.google.firebase.Timestamp

/**
 * Modelo de dados para representar um mercado/supermercado
 */
data class Market(
    val id: String = "",
    val name: String = "",
    val address: String = "",
    val location: GeoPoint = GeoPoint(0.0, 0.0),
    val placeId: String? = null, // ID do Google Places
    val types: List<String> = emptyList(),
    val rating: Double? = null,
    val vicinity: String? = null,
    val geoHash: String = "", // Para busca geoespacial

    // Metadados
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val timesUsed: Int = 0,
    val lastUsedBy: String? = null
) {
    /**
     * Calcula distância até um ponto em metros
     */
    fun distanceTo(point: GeoPoint): Double {
        return calculateDistance(location, point)
    }

    companion object {
        /**
         * Calcula distância entre dois pontos usando Haversine
         */
        private fun calculateDistance(point1: GeoPoint, point2: GeoPoint): Double {
            val earthRadius = 6371000.0 // metros

            val lat1 = Math.toRadians(point1.latitude)
            val lat2 = Math.toRadians(point2.latitude)
            val deltaLat = Math.toRadians(point2.latitude - point1.latitude)
            val deltaLng = Math.toRadians(point2.longitude - point1.longitude)

            val a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                    Math.cos(lat1) * Math.cos(lat2) *
                    Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2)

            val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

            return earthRadius * c
        }
    }
}