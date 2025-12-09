package com.example.marketinho.features.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

data class LocationInfo(
    val geoPoint: GeoPoint,
    val address: String,
    val marketName: String
)

object LocationHelper {

    private const val TAG = "LocationHelper"

    /**
     * Verifica se tem permissão de localização
     */
    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Pega localização atual do usuário
     */
    suspend fun getCurrentLocation(context: Context): Location? {
        if (!hasLocationPermission(context)) {
            Log.w(TAG, "Sem permissão de localização")
            return null
        }

        val fusedLocationClient: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(context)

        return try {
            suspendCancellableCoroutine { continuation ->
                val cancellationTokenSource = CancellationTokenSource()

                continuation.invokeOnCancellation {
                    cancellationTokenSource.cancel()
                }

                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                ).addOnSuccessListener { location ->
                    Log.d(TAG, "Localização obtida: ${location?.latitude}, ${location?.longitude}")
                    continuation.resume(location)
                }.addOnFailureListener { e ->
                    Log.e(TAG, "Erro ao obter localização: ${e.message}", e)
                    continuation.resume(null)
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: ${e.message}", e)
            null
        }
    }

    /**
     * Converte GPS em endereço (Reverse Geocoding)
     */
    suspend fun getAddressFromLocation(
        context: Context,
        latitude: Double,
        longitude: Double
    ): LocationInfo? {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())

            suspendCancellableCoroutine { continuation ->
                try {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(latitude, longitude, 1)?.let { addresses ->
                        if (addresses.isNotEmpty()) {
                            val address = addresses[0]

                            // Extrai nome do local (tenta pegar nome comercial)
                            val marketName = address.featureName
                                ?: address.subThoroughfare
                                ?: address.thoroughfare
                                ?: address.subLocality
                                ?: "Mercado Não Identificado"

                            // Monta endereço completo
                            val fullAddress = buildString {
                                address.thoroughfare?.let { append(it) }
                                address.subThoroughfare?.let { append(", $it") }
                                address.subLocality?.let { append(" - $it") }
                                address.locality?.let { append(" - $it") }
                                address.adminArea?.let { append("/$it") }
                            }.ifBlank { "Endereço não disponível" }

                            val locationInfo = LocationInfo(
                                geoPoint = GeoPoint(latitude, longitude),
                                address = fullAddress,
                                marketName = marketName
                            )

                            Log.d(TAG, "Geocoding bem-sucedido: $locationInfo")
                            continuation.resume(locationInfo)
                        } else {
                            Log.w(TAG, "Nenhum endereço encontrado")
                            continuation.resume(null)
                        }
                    } ?: run {
                        Log.w(TAG, "Geocoder retornou null")
                        continuation.resume(null)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Erro no geocoding: ${e.message}", e)
                    continuation.resume(null)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao criar Geocoder: ${e.message}", e)
            null
        }
    }

    /**
     * Função completa: Pega localização + endereço
     */
    suspend fun getLocationInfo(context: Context): LocationInfo? {
        val location = getCurrentLocation(context) ?: return null
        return getAddressFromLocation(context, location.latitude, location.longitude)
    }
}