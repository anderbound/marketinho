package com.example.marketinho.features.product

import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.marketinho.features.camera.CameraSection
import com.example.marketinho.features.product.components.*
import com.example.marketinho.ui.theme.MarketinhoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MarketinhoTheme {
                MarketinhoApp()
            }
        }
    }
}

@Composable
fun MarketinhoApp(
    viewModel: ProductViewModel = viewModel(
        factory = ProductViewModelFactory(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    val products by viewModel.products.collectAsState(initial = emptyList())
    val total by viewModel.total.collectAsState(initial = 0.0)
    val context = LocalContext.current

    // Modificação 1: Remova a verificação do imageUri (por enquanto)
    if (viewModel.showMarkingScreen && viewModel.currentImageUri != null) {
        ImageMarkingScreen(
            viewModel = viewModel,
            imageUri = viewModel.currentImageUri!!,
            onSelectionDone = { name, price, quantity ->
                viewModel.addProduct(
                    name = name,
                    price = price,
                    quantity = quantity
                )
                viewModel.setShowMarkingScreen(false)
            },
            onCancel = { viewModel.setShowMarkingScreen(false) }
        )
    } else {
        ProductMainScreen(viewModel, products, total, context)
    }
}

@Composable
private fun ProductMainScreen(
    viewModel: ProductViewModel,
    products: List<Product>,
    total: Double,
    context: Context
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CameraSection(
            onImageCaptured = viewModel::captureImage,
            modifier = Modifier.fillMaxWidth()
        )

        if (viewModel.imageUri != null) {
            ProductProcessingSection(
                imageUri = viewModel.imageUri!!,
                onAddProduct = {
                    viewModel.processImageWithOCR(
                        context = context,
                        onManualSelection = { viewModel.setShowMarkingScreen(true) }
                    )
                },
                onManualSelection = { viewModel.setShowMarkingScreen(true) }
            )
        }

        TotalCard(total = total)

        ProductList(
            products = products,
            onProductUpdated = viewModel::updateProduct,
            onProductRemoved = viewModel::removeProduct,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// Adicione esta classe no mesmo arquivo ou em um novo
class ProductViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProductViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProductViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMarketinhoApp() {
    MarketinhoTheme {
        MarketinhoApp()
    }
}