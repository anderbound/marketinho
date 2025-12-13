package com.example.marketinho.features.location

import android.content.Context
import android.util.Log
import com.example.marketinho.data.models.Market
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.tasks.await

/**
 * Repositório híbrido: Firebase (cache) + Google Places (fallback)
 *
 * Estratégia:
 * 1. Busca no Firebase (rápido, grátis)
 * 2. Se não encontrar, busca no Google Places
 * 3. Salva resultado no Firebase para próximas vezes
 */
class MarketRepository(private val context: Context) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val googlePlaces = GooglePlacesService(context)

    private val marketsCollection = firestore.collection("markets")

    /**
     * Busca mercados próximos (híbrido: Firebase → Google)
     */
    suspend fun findNearbyMarkets(
        location: GeoPoint,
        radiusMeters: Double = 500.0
    ): List<Market> {
        Log.d("MarketRepository", "Buscando mercados próximos")

        // 1️⃣ Tenta Firebase primeiro
        val firebaseMarkets = searchFirestore(location, radiusMeters)

        if (firebaseMarkets.isNotEmpty()) {
            Log.d("MarketRepository", "✅ ${firebaseMarkets.size} mercados encontrados no Firebase")
            return firebaseMarkets.sortedBy { it.distanceTo(location) }
        }

        Log.d("MarketRepository", "⚠️ Nenhum mercado no Firebase, buscando Google Places...")

        // 2️⃣ Fallback: Google Places API
        val googleMarkets = googlePlaces.searchNearbyMarkets(location, radiusMeters)

        if (googleMarkets.isEmpty()) {
            Log.d("MarketRepository", "❌ Nenhum mercado encontrado")
            return emptyList()
        }

        Log.d("MarketRepository", "✅ ${googleMarkets.size} mercados encontrados no Google")

        // 3️⃣ Salva no Firebase para próxima vez
        googleMarkets.forEach { market ->
            saveToFirestore(market)
        }

        return googleMarkets.sortedBy { it.distanceTo(location) }
    }

    /**
     * Busca no Firestore usando GeoHash
     */
    private suspend fun searchFirestore(
        center: GeoPoint,
        radiusMeters: Double
    ): List<Market> {
        return try {
            val radiusKm = radiusMeters / 1000.0

            // Calcula bounds para a query
            val bounds = GeoFireUtils.getGeoHashQueryBounds(
                GeoLocation(center.latitude, center.longitude),
                radiusKm
            )

            val tasks = bounds.map { bound ->
                marketsCollection
                    .orderBy("geoHash")
                    .startAt(bound.startHash)
                    .endAt(bound.endHash)
                    .get()
            }

            // Executa todas as queries em paralelo
            val snapshots = tasks.map { it.await() }

            // Combina resultados e filtra por distância real
            snapshots.flatMap { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(Market::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e("MarketRepository", "Erro ao converter documento", e)
                        null
                    }
                }
            }.filter { market ->
                market.distanceTo(center) <= radiusMeters
            }

        } catch (e: Exception) {
            Log.e("MarketRepository", "Erro ao buscar no Firestore", e)
            emptyList()
        }
    }

    /**
     * Salva mercado no Firestore
     */
    private suspend fun saveToFirestore(market: Market) {
        try {
            val geoHash = GeoFireUtils.getGeoHashForLocation(
                GeoLocation(market.location.latitude, market.location.longitude)
            )

            val marketData = hashMapOf(
                "name" to market.name,
                "address" to market.address,
                "location" to market.location,
                "placeId" to market.placeId,
                "types" to market.types,
                "rating" to market.rating,
                "vicinity" to market.vicinity,
                "geoHash" to geoHash,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
                "timesUsed" to 0,
                "lastUsedBy" to auth.currentUser?.uid
            )

            // Se tem placeId, usa como ID do documento
            val docRef = if (!market.placeId.isNullOrEmpty()) {
                marketsCollection.document(market.placeId)
            } else {
                marketsCollection.document()
            }

            docRef.set(marketData).await()

            Log.d("MarketRepository", "✅ Mercado salvo: ${market.name}")

        } catch (e: Exception) {
            Log.e("MarketRepository", "Erro ao salvar mercado", e)
        }
    }

    /**
     * Incrementa contador de uso do mercado
     */
    suspend fun markAsUsed(marketId: String) {
        try {
            marketsCollection.document(marketId).update(
                mapOf(
                    "timesUsed" to FieldValue.increment(1),
                    "lastUsedBy" to auth.currentUser?.uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            Log.e("MarketRepository", "Erro ao atualizar uso do mercado", e)
        }
    }

    /**
     * Busca mercado por ID
     */
    suspend fun getMarketById(marketId: String): Market? {
        return try {
            val doc = marketsCollection.document(marketId).get().await()
            doc.toObject(Market::class.java)?.copy(id = doc.id)
        } catch (e: Exception) {
            Log.e("MarketRepository", "Erro ao buscar mercado por ID", e)
            null
        }
    }
}