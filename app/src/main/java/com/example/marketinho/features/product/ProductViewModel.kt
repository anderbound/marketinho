package com.example.marketinho.features.product

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.marketinho.features.ocr.OcrProcessor
import com.example.marketinho.features.product.utils.AppDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ProductViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val productDao = database.productDao()

    // Estado privado
    private var _imageUri by mutableStateOf<Uri?>(null)
    private var _showMarkingScreen by mutableStateOf(false)
    private var _isLoading by mutableStateOf(false)
    private var _errorMessage by mutableStateOf<String?>(null)

    // Estado público (somente leitura)
    val imageUri: Uri? get() = _imageUri
    val showMarkingScreen: Boolean get() = _showMarkingScreen
    val products: Flow<List<Product>> = productDao.getAllProducts()
    val isLoading: Boolean get() = _isLoading
    val errorMessage: String? get() = _errorMessage
    val currentImageUri: Uri? get() = _imageUri
    val total: Flow<Double> = products.map { productsList ->
        productsList.sumOf { it.price * it.quantity }
    }

    // Ações
    fun captureImage(uri: Uri) {
        _imageUri = uri
        _errorMessage = null
    }

    fun setShowMarkingScreen(value: Boolean) {
        _showMarkingScreen = value
    }

    fun addProduct(name: String, price: String, quantity: Int = 1) {
        viewModelScope.launch {
            val product = Product(
                name = name,
                price = price.toDoubleOrNull() ?: 0.0,
                quantity = quantity,
                imageUri = _imageUri?.toString(),
                createdAt = System.currentTimeMillis() // Novo campo
            )
            productDao.insertProduct(product)
        }
    }

    fun updateProduct(updatedProduct: Product) {
        viewModelScope.launch {
            productDao.updateProduct(updatedProduct)
        }
    }

    fun removeProduct(product: Product) {
        viewModelScope.launch {
            productDao.deleteProduct(product)
        }
    }

    fun processImageWithOCR(
        context: Context,
        onManualSelection: () -> Unit = {}
    ) {
        _imageUri?.let { uri ->
            _isLoading = true
            viewModelScope.launch {
                try {
                    OcrProcessor.processImageWithOCR(
                        imageUri = uri,
                        context = context,
                        onSuccess = { name, price ->
                            addProduct(name, price) // Adiciona automaticamente após OCR
                        },
                        onFailure = {
                            onManualSelection()
                        }
                    )
                } catch (e: Exception) {
                    onManualSelection()
                } finally {
                    _isLoading = false
                }
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage = null
    }
}