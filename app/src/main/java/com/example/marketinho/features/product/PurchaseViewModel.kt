package com.example.marketinho.features.product

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.marketinho.data.models.Purchase
import com.example.marketinho.data.models.PurchaseItem
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PurchaseViewModel(application: Application) : AndroidViewModel(application) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // ========== ESTADOS ==========

    private val _userPurchases = MutableStateFlow<List<Purchase>>(emptyList())
    val userPurchases: StateFlow<List<Purchase>> = _userPurchases.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    // ========== CARREGAMENTO INICIAL ==========

    init {
        loadUserPurchases()
    }

    // ========== CARREGAR COMPRAS ==========

    fun loadUserPurchases() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.w("PurchaseViewModel", "Usuário não autenticado")
            _errorMessage.value = "Faça login para ver seu histórico"
            return
        }

        _isLoading.value = true

        firestore.collection("purchases")
            .whereEqualTo("userId", userId)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("PurchaseViewModel", "Erro ao carregar compras: ${e.message}", e)
                    _errorMessage.value = "Erro ao carregar histórico: ${e.localizedMessage}"
                    _isLoading.value = false
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    try {
                        val purchases = snapshot.toObjects(Purchase::class.java)
                        _userPurchases.value = purchases
                        Log.d("PurchaseViewModel", "✅ Carregadas ${purchases.size} compras")
                    } catch (e: Exception) {
                        Log.e("PurchaseViewModel", "Erro ao converter compras: ${e.message}", e)
                        _errorMessage.value = "Erro ao processar dados"
                    }
                }
                _isLoading.value = false
            }
    }

    // ========== SALVAR COMPRA ==========

    /**
     * Salva uma compra no Firestore
     *
     * @param products Lista de produtos do carrinho (Room)
     * @param total Valor total da compra
     * @param marketName Nome do mercado (opcional)
     * @param marketLocation Localização GPS (implementar depois)
     * @param address Endereço do mercado (implementar depois)
     * @param paymentMethod Forma de pagamento (implementar depois)
     * @param discount Desconto aplicado (implementar depois)
     * @param notes Observações (implementar depois)
     */
    fun savePurchase(
        products: List<Product>,
        total: Double,
        marketName: String = "Mercado Não Identificado",
        marketLocation: GeoPoint? = null,
        address: String? = null,
        paymentMethod: String? = null,
        discount: Double = 0.0,
        notes: String? = null
    ) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Log.e("PurchaseViewModel", "❌ Usuário não autenticado")
                _errorMessage.value = "Você precisa estar logado para salvar a compra"
                return@launch
            }

            if (products.isEmpty()) {
                Log.w("PurchaseViewModel", "⚠️ Tentou salvar compra vazia")
                _errorMessage.value = "Carrinho vazio"
                return@launch
            }

            _isLoading.value = true
            _saveSuccess.value = false

            try {
                // Converte produtos do Room para PurchaseItem do Firestore
                val purchaseItems = products.map { product ->
                    PurchaseItem(
                        name = product.name,
                        price = product.price,
                        quantity = product.quantity,
                        category = product.category,
                        brand = product.brand
                    )
                }

                val newPurchase = Purchase(
                    userId = userId,
                    date = Timestamp.now(),
                    total = total,
                    discount = discount,
                    marketName = marketName,
                    marketLocation = marketLocation,
                    address = address,
                    paymentMethod = paymentMethod,
                    notes = notes,
                    items = purchaseItems,
                    createdAt = Timestamp.now()
                )

                // Salva no Firestore
                firestore.collection("purchases")
                    .add(newPurchase)
                    .addOnSuccessListener { documentReference ->
                        Log.d("PurchaseViewModel", "✅ Compra salva! ID: ${documentReference.id}")
                        Log.d("PurchaseViewModel", "   📍 Mercado: $marketName")
                        Log.d("PurchaseViewModel", "   💰 Total: R$ ${"%.2f".format(total)}")
                        Log.d("PurchaseViewModel", "   📦 Itens: ${products.size}")

                        _saveSuccess.value = true
                        _isLoading.value = false
                        _errorMessage.value = null
                    }
                    .addOnFailureListener { e ->
                        Log.e("PurchaseViewModel", "❌ Erro ao salvar: ${e.message}", e)
                        _errorMessage.value = "Erro ao salvar compra: ${e.localizedMessage}"
                        _saveSuccess.value = false
                        _isLoading.value = false
                    }

            } catch (e: Exception) {
                Log.e("PurchaseViewModel", "❌ Erro inesperado: ${e.message}", e)
                _errorMessage.value = "Erro inesperado: ${e.localizedMessage}"
                _saveSuccess.value = false
                _isLoading.value = false
            }
        }
    }

    // ========== LIMPAR ESTADOS ==========

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun resetSaveSuccess() {
        _saveSuccess.value = false
    }

    // ========== ESTATÍSTICAS (Implementar depois) ==========

    /**
     * Calcula gasto total de um período
     */
    fun getTotalSpentInPeriod(startDate: Timestamp, endDate: Timestamp): Double {
        return _userPurchases.value
            .filter { it.date >= startDate && it.date <= endDate }
            .sumOf { it.total }
    }

    /**
     * Mercados mais frequentes
     */
    fun getMostFrequentMarkets(): Map<String, Int> {
        return _userPurchases.value
            .groupingBy { it.marketName }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .toMap()
    }

    /**
     * Produto mais comprado
     */
    fun getMostPurchasedProducts(): Map<String, Int> {
        return _userPurchases.value
            .flatMap { it.items }
            .groupingBy { it.name }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .toMap()
    }

    /**
     * Gasto médio por compra
     */
    fun getAverageSpent(): Double {
        return if (_userPurchases.value.isEmpty()) {
            0.0
        } else {
            _userPurchases.value.sumOf { it.total } / _userPurchases.value.size
        }
    }
}