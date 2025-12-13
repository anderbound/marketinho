package com.example.marketinho.features.location

import android.content.Context
import android.util.Log
import com.example.marketinho.data.models.Market
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.tasks.await

/**
 * Serviço para buscar mercados usando Google Places API
 */
class GooglePlacesService(context: Context) {

    private val placesClient: PlacesClient

    init {
        // Inicializa Places API
        if (!Places.isInitialized()) {
            Places.initialize(context.applicationContext, getApiKey(context))
        }
        placesClient = Places.createClient(context)
    }

    /**
     * Busca mercados próximos a uma localização
     */
    suspend fun searchNearbyMarkets(
        location: GeoPoint,
        radiusMeters: Double = 500.0
    ): List<Market> {
        return try {
            Log.d("GooglePlaces", "Buscando mercados próximos a $location")

            // Cria bounds (retângulo) ao redor do ponto
            val bounds = createBounds(location, radiusMeters)

            // Request para buscar supermercados
            val request = FindAutocompletePredictionsRequest.builder()
                .setLocationRestriction(bounds)
                .setQuery("supermercado")
                .build()

            val response = placesClient.findAutocompletePredictions(request).await()

            Log.d("GooglePlaces", "Encontrados ${response.autocompletePredictions.size} resultados")

            // Converte para nosso modelo
            response.autocompletePredictions.mapNotNull { prediction ->
                try {
                    Market(
                        id = prediction.placeId,
                        name = prediction.getPrimaryText(null).toString(),
                        address = prediction.getFullText(null).toString(),
                        placeId = prediction.placeId,
                        vicinity = prediction.getSecondaryText(null).toString(),
                        location = location // Aproximado, idealmente buscar detalhes
                    )
                } catch (e: Exception) {
                    Log.e("GooglePlaces", "Erro ao converter prediction", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("GooglePlaces", "Erro ao buscar mercados", e)
            emptyList()
        }
    }

    /**
     * Cria um retângulo (bounds) ao redor de um ponto
     */
    private fun createBounds(center: GeoPoint, radiusMeters: Double): RectangularBounds {
        // Aproximação: 1 grau de latitude ≈ 111km
        val deltaLat = radiusMeters / 111000.0
        val deltaLng = radiusMeters / (111000.0 * Math.cos(Math.toRadians(center.latitude)))

        val southwest = LatLng(
            center.latitude - deltaLat,
            center.longitude - deltaLng
        )
        val northeast = LatLng(
            center.latitude + deltaLat,
            center.longitude + deltaLng
        )

        return RectangularBounds.newInstance(southwest, northeast)
    }

    /**
     * Obtém a API Key do google-services.json
     */
    private fun getApiKey(context: Context): String {
        // A API Key deve estar em strings.xml ou BuildConfig
        // Por enquanto retorna uma string, você deve adicionar no seu projeto
        return context.getString(
            context.resources.getIdentifier(
                "google_maps_key",
                "string",
                context.packageName
            )
        )
    }
}