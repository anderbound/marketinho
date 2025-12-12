package com.example.marketinho.features.product

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.marketinho.features.product.utils.AppDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ProductViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val productDao = database.productDao()

    private var _imageUri by mutableStateOf<Uri?>(null)
    private var _showMarkingScreen by mutableStateOf(false)
    private var _isLoading by mutableStateOf(false)
    private var _errorMessage by mutableStateOf<String?>(null)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val imageUri: Uri? get() = _imageUri
    val showMarkingScreen: Boolean get() = _showMarkingScreen
    val products: Flow<List<Product>> = productDao.getAllProducts()
    val isLoading: Boolean get() = _isLoading
    val errorMessage: String? get() = _errorMessage
    val currentImageUri: Uri? get() = _imageUri

    val filteredProducts: Flow<List<Product>> = combine(
        products,
        _searchQuery
    ) { productsList, query ->
        if (query.isBlank()) {
            productsList
        } else {
            productsList.filter { product ->
                product.name.contains(query, ignoreCase = true) ||
                        product.price.toString().contains(query) ||
                        product.quantity.toString().contains(query)
            }
        }
    }

    val total: Flow<Double> = products.map { productsList ->
        productsList.sumOf { it.price * it.quantity }
    }

    fun captureImage(uri: Uri) {
        _imageUri = uri
        _errorMessage = null
        Log.d("ProductViewModel", "Imagem capturada: $uri")
    }

    fun setShowMarkingScreen(value: Boolean) {
        _showMarkingScreen = value
        Log.d("ProductViewModel", "ShowMarkingScreen: $value")

        // ✅ Limpa a imagem ao fechar
        if (!value) {
            _imageUri = null
            Log.d("ProductViewModel", "ImageUri limpa")
        }
    }

    fun addProduct(name: String, price: String, quantity: Int = 1, category: String? = null) {
        viewModelScope.launch {
            val product = Product(
                name = name,
                price = price.toDoubleOrNull() ?: 0.0,
                quantity = quantity,
                imageUri = _imageUri?.toString(),
                createdAt = System.currentTimeMillis(),
                category = category
            )
            productDao.insertProduct(product)
            Log.d("ProductViewModel", "✅ Produto adicionado: $name")

            // ✅ IMPORTANTE: Limpa a imagem após adicionar
            _imageUri = null
            _showMarkingScreen = false
        }
    }

    fun updateProduct(updatedProduct: Product) {
        viewModelScope.launch {
            productDao.updateProduct(updatedProduct)
            Log.d("ProductViewModel", "Produto atualizado: ${updatedProduct.name}")
        }
    }

    fun removeProduct(product: Product) {
        viewModelScope.launch {
            productDao.deleteProduct(product)
            Log.d("ProductViewModel", "Produto removido: ${product.name}")
        }
    }

    fun clearAllProducts() {
        viewModelScope.launch {
            productDao.deleteAllProducts()
            Log.d("ProductViewModel", "Todos os produtos limpos")
        }
    }

    fun clearErrorMessage() {
        _errorMessage = null
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }
}