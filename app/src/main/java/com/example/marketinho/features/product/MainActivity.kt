package com.example.marketinho.features.product

import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.marketinho.data.models.Purchase
import com.example.marketinho.data.models.PurchaseItem
import com.example.marketinho.features.camera.CameraSection
import com.example.marketinho.features.product.components.*
import com.example.marketinho.features.auth.AuthViewModel
import com.example.marketinho.features.auth.LoginScreen
import com.example.marketinho.ui.theme.MarketinhoTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Date

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MarketinhoTheme {
                val authViewModel: AuthViewModel = viewModel(
                    factory = AuthViewModelFactory(application)
                )
                val isAuthenticated by authViewModel.isAuthenticated.collectAsState()

                if (isAuthenticated) {
                    MarketinhoApp()
                } else {
                    LoginScreen(
                        authViewModel = authViewModel,
                        onLoginSuccess = { }
                    )
                }
            }
        }
    }
}

class AuthViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@Composable
fun MarketinhoApp(
    productViewModel: ProductViewModel = viewModel(
        factory = ProductViewModelFactory(
            LocalContext.current.applicationContext as Application
        )
    ),
    purchaseViewModel: PurchaseViewModel = viewModel(
        factory = PurchaseViewModelFactory(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    val navController = rememberNavController()
    val context = LocalContext.current

    val productsInCart by productViewModel.products.collectAsState(initial = emptyList())
    val totalInCart by productViewModel.total.collectAsState(initial = 0.0)

    NavHost(navController = navController, startDestination = "history_screen") {
        composable("history_screen") {
            PurchaseHistoryScreen(
                navController = navController,
                purchaseViewModel = purchaseViewModel
            )
        }
        composable("main_screen") {
            // Se _showMarkingScreen for true E tiver uma imagem, mostra a tela de marcação
            if (productViewModel.showMarkingScreen && productViewModel.currentImageUri != null) {
                ImageMarkingScreen(
                    viewModel = productViewModel,
                    imageUri = productViewModel.currentImageUri!!,
                    onSelectionDone = { name, price, quantity ->
                        productViewModel.addProduct(
                            name = name,
                            price = price,
                            quantity = quantity
                        )
                        productViewModel.setShowMarkingScreen(false) // Desativa a tela de marcação e limpa a URI
                    },
                    onCancel = {
                        productViewModel.setShowMarkingScreen(false) // Desativa a tela de marcação e limpa a URI
                    }
                )
            } else {
                ProductMainScreen(
                    viewModel = productViewModel,
                    products = productsInCart,
                    total = totalInCart,
                    context = context,
                    navController = navController,
                    onFinalizePurchase = {
                        purchaseViewModel.savePurchase(
                            products = productsInCart,
                            total = totalInCart
                        )
                        productViewModel.clearAllProducts()
                        navController.navigate("history_screen") {
                            popUpTo("main_screen") { inclusive = true }
                        }
                    }
                )
            }
        }
        composable("checkout_screen") {
            CheckoutScreen(
                viewModel = productViewModel,
                onFinalizePurchase = {
                    purchaseViewModel.savePurchase(
                        products = productsInCart,
                        total = totalInCart
                    )
                    productViewModel.clearAllProducts()
                    navController.navigate("history_screen") {
                        popUpTo("main_screen") { inclusive = true }
                    }
                },
                onCancel = {
                    navController.popBackStack()
                }
            )
        }
        composable("purchase_details_screen/{purchaseId}") { backStackEntry ->
            val purchaseId = backStackEntry.arguments?.getString("purchaseId")
            if (purchaseId != null) {
                // PurchaseDetailsScreen(purchaseId = purchaseId, purchaseViewModel = purchaseViewModel)
            }
        }
    }
}

@Composable
private fun ProductMainScreen(
    viewModel: ProductViewModel,
    products: List<Product>,
    total: Double,
    context: Context,
    navController: NavController,
    onFinalizePurchase: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val authViewModel: AuthViewModel = viewModel(
            factory = AuthViewModelFactory(LocalContext.current.applicationContext as Application)
        )
        Button(onClick = { authViewModel.signOut() }) {
            Text("Sair")
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Voltou ao onImageCaptured original
        CameraSection(
            onImageCaptured = viewModel::captureImage,
            modifier = Modifier.fillMaxWidth()
        )

        // Restaura a ProductProcessingSection
        if (viewModel.imageUri != null) {
            ProductProcessingSection(
                imageUri = viewModel.imageUri!!,
                onAddProduct = {
                    // Esta é a chamada para tentar OCR automático
                    viewModel.processImageWithOCR(
                        context = context,
                        onManualSelection = { viewModel.setShowMarkingScreen(true) } // Vai para manual se OCR falhar
                    )
                },
                onManualSelection = {
                    // Esta é a chamada para ir direto para seleção manual
                    viewModel.setShowMarkingScreen(true)
                }
            )
        }

        TotalCard(total = total)

        if (products.size >= 2) {
            Button(
                onClick = {
                    navController.navigate("checkout_screen")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text("Finalizar Compras")
            }
        }

        ProductList(
            products = products,
            onProductUpdated = viewModel::updateProduct,
            onProductRemoved = viewModel::removeProduct,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

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