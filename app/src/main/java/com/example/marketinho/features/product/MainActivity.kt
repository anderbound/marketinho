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
import androidx.compose.ui.Alignment
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
import com.example.marketinho.features.camera.CameraSection
import com.example.marketinho.features.product.components.*
import com.example.marketinho.features.auth.AuthViewModel
import com.example.marketinho.features.auth.LoginScreen
import com.example.marketinho.features.auth.RegisterScreen
import com.example.marketinho.ui.theme.MarketinhoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MarketinhoTheme {
                val authViewModel: AuthViewModel = viewModel(
                    factory = AuthViewModelFactory(application)
                )
                val isAuthenticated by authViewModel.isAuthenticated.collectAsState()

                // Controla qual tela de autenticação mostrar
                var showRegisterScreen by remember { mutableStateOf(false) }

                when {
                    isAuthenticated -> {
                        // Usuário autenticado - mostra o app principal
                        MarketinhoApp()
                    }
                    showRegisterScreen -> {
                        // Mostra tela de cadastro
                        RegisterScreen(
                            authViewModel = authViewModel,
                            onRegisterSuccess = {
                                showRegisterScreen = false
                            },
                            onBackToLogin = {
                                showRegisterScreen = false
                            }
                        )
                    }
                    else -> {
                        // Mostra tela de login
                        LoginScreen(
                            authViewModel = authViewModel,
                            onLoginSuccess = { },
                            onNavigateToRegister = {
                                showRegisterScreen = true
                            }
                        )
                    }
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

    // CORRIGIDO: Usar filteredProducts ao invés de products
    val productsInCart by productViewModel.filteredProducts.collectAsState(initial = emptyList())
    val totalInCart by productViewModel.total.collectAsState(initial = 0.0)

    NavHost(
        navController = navController,
        startDestination = "main_screen" // CORRIGIDO: Começar na tela principal
    ) {
        composable("main_screen") {
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
                        productViewModel.setShowMarkingScreen(false)
                    },
                    onCancel = {
                        productViewModel.setShowMarkingScreen(false)
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
                        // Salva a compra no Firestore
                        purchaseViewModel.savePurchase(
                            products = productsInCart,
                            total = totalInCart
                        )
                        // Limpa o carrinho local (Room)
                        productViewModel.clearAllProducts()
                        // Navega para o histórico
                        navController.navigate("history_screen") {
                            popUpTo("main_screen") { inclusive = false }
                        }
                    }
                )
            }
        }

        composable("history_screen") {
            PurchaseHistoryScreen(
                navController = navController,
                purchaseViewModel = purchaseViewModel
            )
        }

        composable("checkout_screen") {
            CheckoutScreen(
                viewModel = productViewModel,
                onFinalizePurchase = {
                    // Salva a compra
                    purchaseViewModel.savePurchase(
                        products = productsInCart,
                        total = totalInCart
                    )
                    // Limpa o carrinho
                    productViewModel.clearAllProducts()
                    // Volta para a tela principal
                    navController.navigate("main_screen") {
                        popUpTo("main_screen") { inclusive = true }
                    }
                },
                onCancel = {
                    navController.popBackStack()
                }
            )
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
    // Observa o searchQuery como State para reatividade
    val searchQuery by viewModel.searchQuery.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars) // NOVO: Respeita barras do sistema
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header com botões de ação
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val authViewModel: AuthViewModel = viewModel(
                factory = AuthViewModelFactory(LocalContext.current.applicationContext as Application)
            )

            Button(onClick = {
                navController.navigate("history_screen")
            }) {
                Text("Histórico")
            }

            Button(onClick = {
                authViewModel.signOut()
            }) {
                Text("Sair")
            }
        }

        // Seção da câmera
        CameraSection(
            onImageCaptured = viewModel::captureImage,
            modifier = Modifier.fillMaxWidth()
        )

        // Processamento de imagem
        if (viewModel.imageUri != null) {
            ProductProcessingSection(
                imageUri = viewModel.imageUri!!,
                onAddProduct = {
                    viewModel.processImageWithOCR(
                        context = context,
                        onManualSelection = { viewModel.setShowMarkingScreen(true) }
                    )
                },
                onManualSelection = {
                    viewModel.setShowMarkingScreen(true)
                }
            )
        }

        // Card do total
        TotalCard(total = total)

        // ========== NOVO: BARRA DE BUSCA ==========
        SearchBar(
            query = searchQuery, // Agora usa o State observável
            onQueryChange = viewModel::updateSearchQuery,
            onClearClick = viewModel::clearSearch
        )

        // Botão de finalizar compras
        if (products.size >= 2) {
            Button(
                onClick = { navController.navigate("checkout_screen") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text("Finalizar Compras (${products.size} itens)")
            }
        }

        // Lista de produtos (agora com produtos filtrados)
        if (products.isEmpty() && searchQuery.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nenhum produto encontrado para '$searchQuery'",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            ProductList(
                products = products,
                onProductUpdated = viewModel::updateProduct,
                onProductRemoved = viewModel::removeProduct,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
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
