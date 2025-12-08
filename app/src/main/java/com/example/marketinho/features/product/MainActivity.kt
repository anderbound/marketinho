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
                MainContent()
            }
        }
    }
}

@Composable
private fun MainContent() {
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(LocalContext.current.applicationContext as Application)
    )
    val isAuthenticated by authViewModel.isAuthenticated.collectAsState()
    var showRegisterScreen by remember { mutableStateOf(false) }

    // IMPORTANTE: Re-render completo quando autenticação mudar
    key(isAuthenticated) {
        when {
            isAuthenticated -> {
                // Usuário logado - mostra app
                MarketinhoApp(authViewModel = authViewModel)
            }
            showRegisterScreen -> {
                // Tela de cadastro
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
                // Tela de login
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

@Composable
fun MarketinhoApp(
    authViewModel: AuthViewModel,
    productViewModel: ProductViewModel = viewModel(
        factory = ProductViewModelFactory(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    val navController = rememberNavController()
    val context = LocalContext.current

    val productsInCart by productViewModel.filteredProducts.collectAsState(initial = emptyList())
    val totalInCart by productViewModel.total.collectAsState(initial = 0.0)

    // Observa mudanças de autenticação para limpar dados
    val isAuthenticated by authViewModel.isAuthenticated.collectAsState()

    LaunchedEffect(isAuthenticated) {
        if (!isAuthenticated) {
            // Limpa dados locais quando deslogar
            productViewModel.clearAllProducts()
        }
    }

    NavHost(
        navController = navController,
        startDestination = "main_screen"
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
                    authViewModel = authViewModel
                )
            }
        }

        composable("history_screen") {
            PurchaseHistoryScreen(
                navController = navController,
                authViewModel = authViewModel // NOVO: Passa AuthViewModel
            )
        }

        composable("checkout_screen") {
            val purchaseViewModel: PurchaseViewModel = viewModel(
                factory = PurchaseViewModelFactory(
                    LocalContext.current.applicationContext as Application
                )
            )

            CheckoutScreen(
                viewModel = productViewModel,
                onFinalizePurchase = {
                    purchaseViewModel.savePurchase(
                        products = productsInCart,
                        total = totalInCart
                    )
                    productViewModel.clearAllProducts()
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
    authViewModel: AuthViewModel
) {
    val searchQuery by viewModel.searchQuery.collectAsState()

    val purchaseViewModel: PurchaseViewModel = viewModel(
        factory = PurchaseViewModelFactory(
            LocalContext.current.applicationContext as Application
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = {
                navController.navigate("history_screen")
            }) {
                Text("Histórico")
            }

            Button(
                onClick = {
                    authViewModel.signOut()
                },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Sair")
            }
        }

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
                onManualSelection = {
                    viewModel.setShowMarkingScreen(true)
                }
            )
        }

        TotalCard(total = total)

        SearchBar(
            query = searchQuery,
            onQueryChange = viewModel::updateSearchQuery,
            onClearClick = viewModel::clearSearch
        )

        if (products.size >= 2) {
            Button(
                onClick = {
                    purchaseViewModel.savePurchase(
                        products = products,
                        total = total
                    )
                    viewModel.clearAllProducts()
                    navController.navigate("history_screen")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text("Finalizar Compras (${products.size} itens)")
            }
        }

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