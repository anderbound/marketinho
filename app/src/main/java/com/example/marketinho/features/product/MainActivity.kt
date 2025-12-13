package com.example.marketinho.features.product

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.marketinho.data.models.Market  // ✅ NOVO
import com.example.marketinho.features.camera.CameraSection
import com.example.marketinho.features.location.MarketRepository  // ✅ NOVO
import com.example.marketinho.features.product.components.*
import com.example.marketinho.features.auth.AuthViewModel
import com.example.marketinho.features.auth.LoginScreen
import com.example.marketinho.features.auth.RegisterScreen
import com.example.marketinho.features.location.components.LocationConfirmationDialog
import com.example.marketinho.ui.theme.MarketinhoTheme
import kotlinx.coroutines.launch

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

    key(isAuthenticated) {
        when {
            isAuthenticated -> {
                MarketinhoApp(authViewModel = authViewModel)
            }
            showRegisterScreen -> {
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
    val isAuthenticated by authViewModel.isAuthenticated.collectAsState()

    // Abre tela de seleção automaticamente após capturar foto
    LaunchedEffect(productViewModel.imageUri) {
        if (productViewModel.imageUri != null && !productViewModel.showMarkingScreen) {
            Log.d("MainActivity", "📸 Foto capturada, abrindo tela de seleção")
            productViewModel.setShowMarkingScreen(true)
        }
    }

    LaunchedEffect(isAuthenticated) {
        if (!isAuthenticated) {
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
                    onSelectionDone = { name, price, quantity, category ->
                        Log.d("MainActivity", "📝 Produto: $name, R$ $price (x$quantity), Categoria: ${category ?: "sem categoria"}")

                        productViewModel.addProduct(
                            name = name,
                            price = price,
                            quantity = quantity,
                            category = category
                        )
                    },
                    onCancel = {
                        Log.d("MainActivity", "❌ Seleção cancelada")
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
                authViewModel = authViewModel
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

        composable("purchase_details/{purchaseId}") { backStackEntry ->
            val purchaseId = backStackEntry.arguments?.getString("purchaseId")
            val purchaseViewModel: PurchaseViewModel = viewModel(
                factory = PurchaseViewModelFactory(
                    LocalContext.current.applicationContext as Application
                )
            )

            val purchases by purchaseViewModel.userPurchases.collectAsState()
            val purchase = purchases.find { it.id == purchaseId }

            if (purchase != null) {
                PurchaseDetailsScreen(
                    purchase = purchase,
                    navController = navController
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
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
    authViewModel: AuthViewModel
) {
    val searchQuery by viewModel.searchQuery.collectAsState()

    val purchaseViewModel: PurchaseViewModel = viewModel(
        factory = PurchaseViewModelFactory(
            LocalContext.current.applicationContext as Application
        )
    )

    // ✅ NOVO: Repository para buscar mercados
    val marketRepository = remember { MarketRepository(context) }

    var showPaymentDialog by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }
    var selectedPaymentMethod by remember { mutableStateOf<String?>(null) }
    var locationInfo by remember { mutableStateOf<com.example.marketinho.features.location.LocationInfo?>(null) }
    var isLoadingLocation by remember { mutableStateOf(false) }

    // ✅ NOVO: Estados para mercados
    var nearbyMarkets by remember { mutableStateOf<List<Market>?>(null) }
    var isSearchingMarkets by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        if (granted) {
            isLoadingLocation = true
            kotlinx.coroutines.MainScope().launch {
                // 1️⃣ Obtém localização
                locationInfo = com.example.marketinho.features.location.LocationHelper.getLocationInfo(context)
                isLoadingLocation = false

                // 2️⃣ Busca mercados próximos
                locationInfo?.geoPoint?.let { geoPoint ->
                    isSearchingMarkets = true
                    try {
                        nearbyMarkets = marketRepository.findNearbyMarkets(
                            location = geoPoint,
                            radiusMeters = 500.0
                        )
                        Log.d("MainActivity", "✅ ${nearbyMarkets?.size ?: 0} mercados encontrados")
                    } catch (e: Exception) {
                        Log.e("MainActivity", "❌ Erro ao buscar mercados", e)
                        nearbyMarkets = emptyList()
                    } finally {
                        isSearchingMarkets = false
                    }
                }
            }
        } else {
            isLoadingLocation = false
            android.widget.Toast.makeText(
                context,
                "Permissão de localização negada",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

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
            onImageCaptured = { uri ->
                Log.d("MainActivity", "📸 Imagem capturada: $uri")
                viewModel.captureImage(uri)
            },
            modifier = Modifier.fillMaxWidth()
        )

        TotalCard(total = total)

        SearchBar(
            query = searchQuery,
            onQueryChange = viewModel::updateSearchQuery,
            onClearClick = viewModel::clearSearch
        )

        if (products.size >= 2) {
            Button(
                onClick = {
                    showPaymentDialog = true
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

    if (showPaymentDialog) {
        PaymentMethodDialog(
            onDismiss = { showPaymentDialog = false },
            onConfirm = { paymentMethod ->
                showPaymentDialog = false
                selectedPaymentMethod = paymentMethod.displayName

                // ✅ Reseta estados
                locationInfo = null
                nearbyMarkets = null

                showLocationDialog = true

                if (com.example.marketinho.features.location.LocationHelper.hasLocationPermission(context)) {
                    isLoadingLocation = true
                    kotlinx.coroutines.MainScope().launch {
                        // 1️⃣ GPS
                        locationInfo = com.example.marketinho.features.location.LocationHelper.getLocationInfo(context)
                        isLoadingLocation = false

                        // 2️⃣ Mercados
                        locationInfo?.geoPoint?.let { geoPoint ->
                            isSearchingMarkets = true
                            try {
                                nearbyMarkets = marketRepository.findNearbyMarkets(
                                    location = geoPoint,
                                    radiusMeters = 500.0
                                )
                                Log.d("MainActivity", "✅ ${nearbyMarkets?.size ?: 0} mercados encontrados")
                            } catch (e: Exception) {
                                Log.e("MainActivity", "❌ Erro ao buscar mercados", e)
                                nearbyMarkets = emptyList()
                            } finally {
                                isSearchingMarkets = false
                            }
                        }
                    }
                } else {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            }
        )
    }

    if (showLocationDialog) {
        LocationConfirmationDialog(
            locationInfo = locationInfo,
            isLoading = isLoadingLocation,
            nearbyMarkets = nearbyMarkets,  // ✅ NOVO
            isSearchingMarkets = isSearchingMarkets,  // ✅ NOVO
            onConfirm = { marketName, address ->
                showLocationDialog = false

                // ✅ NOVO: Incrementa contador de uso do mercado
                nearbyMarkets?.find { it.name == marketName }?.let { market ->
                    kotlinx.coroutines.MainScope().launch {
                        marketRepository.markAsUsed(market.id)
                    }
                }

                purchaseViewModel.savePurchase(
                    products = products,
                    total = total,
                    paymentMethod = selectedPaymentMethod,
                    marketName = marketName,
                    marketLocation = locationInfo?.geoPoint,
                    address = address
                )

                viewModel.clearAllProducts()
                navController.navigate("history_screen")
            },
            onDismiss = {
                showLocationDialog = false
                locationInfo = null
                nearbyMarkets = null  // ✅ NOVO
                isLoadingLocation = false
                isSearchingMarkets = false  // ✅ NOVO
            },
            onRetry = {
                if (com.example.marketinho.features.location.LocationHelper.hasLocationPermission(context)) {
                    isLoadingLocation = true
                    kotlinx.coroutines.MainScope().launch {
                        locationInfo = com.example.marketinho.features.location.LocationHelper.getLocationInfo(context)
                        isLoadingLocation = false

                        // ✅ NOVO: Busca mercados no retry também
                        locationInfo?.geoPoint?.let { geoPoint ->
                            isSearchingMarkets = true
                            try {
                                nearbyMarkets = marketRepository.findNearbyMarkets(geoPoint, 500.0)
                            } catch (e: Exception) {
                                nearbyMarkets = emptyList()
                            } finally {
                                isSearchingMarkets = false
                            }
                        }
                    }
                } else {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            }
        )
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