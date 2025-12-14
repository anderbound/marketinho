package com.example.marketinho.features.product

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
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
import com.example.marketinho.data.models.Market
import com.example.marketinho.data.models.SharedList
import com.example.marketinho.data.models.SharedProduct
import com.example.marketinho.features.camera.CameraSection
import com.example.marketinho.features.location.MarketRepository
import com.example.marketinho.features.product.components.*
import com.example.marketinho.features.auth.AuthViewModel
import com.example.marketinho.features.auth.LoginScreen
import com.example.marketinho.features.auth.RegisterScreen
import com.example.marketinho.features.location.components.LocationConfirmationDialog
import com.example.marketinho.features.sharing.SharingRepository
import com.example.marketinho.features.sharing.components.ShareListDialog
import com.example.marketinho.features.sharing.components.SharedListViewScreen
import com.example.marketinho.ui.theme.MarketinhoTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Captura Deep Link quando app abre
        val deepLinkShareId = extractShareIdFromIntent(intent)

        setContent {
            MarketinhoTheme {
                MainContent(initialShareId = deepLinkShareId)
            }
        }
    }

    // ✅ Captura Deep Link quando app já está aberto
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        val shareId = extractShareIdFromIntent(intent)
        if (shareId != null) {
            Log.d("MainActivity", "🔗 Deep Link capturado (app já aberto): $shareId")
            // Recarrega o Content com novo shareId
            setContent {
                MarketinhoTheme {
                    MainContent(initialShareId = shareId)
                }
            }
        }
    }

    /**
     * Extrai o shareId do Deep Link
     * Exemplos suportados:
     * - https://marketinho.app/shared/A3X9K2
     * - marketinho://shared/A3X9K2
     */
    private fun extractShareIdFromIntent(intent: Intent?): String? {
        val data: Uri? = intent?.data

        if (data != null) {
            Log.d("MainActivity", "🔗 Deep Link recebido: $data")

            // Extrai shareId do path
            val pathSegments = data.pathSegments
            if (pathSegments.isNotEmpty()) {
                // Pega o último segmento (ABC123)
                val shareId = pathSegments.last()
                if (shareId != "shared" && shareId.isNotBlank()) {
                    Log.d("MainActivity", "✅ ShareId extraído: $shareId")
                    return shareId
                }
            }
        }

        return null
    }
}

@Composable
private fun MainContent(initialShareId: String? = null) {
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(LocalContext.current.applicationContext as Application)
    )
    val isAuthenticated by authViewModel.isAuthenticated.collectAsState()
    var showRegisterScreen by remember { mutableStateOf(false) }

    key(isAuthenticated) {
        when {
            isAuthenticated -> {
                MarketinhoApp(
                    authViewModel = authViewModel,
                    initialShareId = initialShareId
                )
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
    initialShareId: String? = null,
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

    // ✅ Navega para lista compartilhada se houver Deep Link
    LaunchedEffect(initialShareId) {
        if (initialShareId != null) {
            Log.d("MainActivity", "🔗 Navegando para lista compartilhada: $initialShareId")
            navController.navigate("shared_list_view/$initialShareId")
        }
    }

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

        // ✅ NOVA ROTA: Visualizar lista compartilhada
        composable("shared_list_view/{shareId}") { backStackEntry ->
            val shareId = backStackEntry.arguments?.getString("shareId") ?: return@composable
            val sharingRepository = remember { SharingRepository() }

            var sharedList by remember { mutableStateOf<SharedList?>(null) }
            var isLoading by remember { mutableStateOf(true) }
            var errorMessage by remember { mutableStateOf<String?>(null) }

            // Carrega lista compartilhada
            LaunchedEffect(shareId) {
                Log.d("MainActivity", "📥 Carregando lista compartilhada: $shareId")
                val result = sharingRepository.getSharedList(shareId)

                result.onSuccess { list ->
                    sharedList = list
                    isLoading = false
                    Log.d("MainActivity", "✅ Lista carregada: ${list.products.size} itens")
                }.onFailure { error ->
                    errorMessage = error.message
                    isLoading = false
                    Log.e("MainActivity", "❌ Erro ao carregar lista", error)
                }
            }

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text("Carregando lista compartilhada...")
                        }
                    }
                }

                errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Text(
                                "❌ $errorMessage",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Button(onClick = { navController.navigate("main_screen") }) {
                                Text("Voltar")
                            }
                        }
                    }
                }

                sharedList != null -> {
                    SharedListViewScreen(
                        sharedList = sharedList!!,
                        onBack = { navController.popBackStack() },
                        onCopyToMyList = { products ->
                            // Copia produtos para a lista do usuário
                            products.forEach { sharedProduct ->
                                productViewModel.addProduct(
                                    name = sharedProduct.name,
                                    price = sharedProduct.price.toString(),
                                    quantity = sharedProduct.quantity,
                                    category = sharedProduct.category
                                )
                            }

                            android.widget.Toast.makeText(
                                context,
                                "${products.size} produtos copiados!",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()

                            navController.navigate("main_screen")
                        }
                    )
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

    val marketRepository = remember { MarketRepository(context) }
    val sharingRepository = remember { SharingRepository() }

    var showPaymentDialog by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }
    var selectedPaymentMethod by remember { mutableStateOf<String?>(null) }
    var locationInfo by remember { mutableStateOf<com.example.marketinho.features.location.LocationInfo?>(null) }
    var isLoadingLocation by remember { mutableStateOf(false) }
    var nearbyMarkets by remember { mutableStateOf<List<Market>?>(null) }
    var isSearchingMarkets by remember { mutableStateOf(false) }

    var showShareDialog by remember { mutableStateOf(false) }
    var isGeneratingLink by remember { mutableStateOf(false) }
    var shareId by remember { mutableStateOf<String?>(null) }
    var shareUrl by remember { mutableStateOf<String?>(null) }
    var shareText by remember { mutableStateOf<String?>(null) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        if (granted) {
            isLoadingLocation = true
            kotlinx.coroutines.MainScope().launch {
                locationInfo = com.example.marketinho.features.location.LocationHelper.getLocationInfo(context)
                isLoadingLocation = false

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

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (products.size >= 2) {
                    IconButton(
                        onClick = { showShareDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Compartilhar lista",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
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

    if (showShareDialog) {
        ShareListDialog(
            isGeneratingLink = isGeneratingLink,
            shareId = shareId,
            shareUrl = shareUrl,
            shareText = shareText,
            onDismiss = {
                showShareDialog = false
                shareId = null
                shareUrl = null
                shareText = null
            },
            onGenerateLink = {
                isGeneratingLink = true
                kotlinx.coroutines.MainScope().launch {
                    val result = sharingRepository.createSharedList(
                        products = products,
                        total = total,
                        marketName = locationInfo?.marketName,
                        marketLocation = locationInfo?.geoPoint
                    )

                    result.onSuccess { id ->
                        shareId = id
                        // ✅ USA GITHUB PAGES - Link clicável no WhatsApp!
                        shareUrl = sharingRepository.generateShareUrl(id)

                        sharingRepository.getSharedList(id).onSuccess { sharedList ->
                            // ✅ Texto com link do GitHub Pages
                            shareText = sharingRepository.generateShareText(sharedList, id)
                        }

                        Log.d("MainActivity", "✅ Link criado: $shareUrl")
                    }.onFailure { error ->
                        Log.e("MainActivity", "❌ Erro ao gerar link", error)
                        android.widget.Toast.makeText(
                            context,
                            "Erro ao gerar link: ${error.message}",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }

                    isGeneratingLink = false
                }
            }
        )
    }

    if (showPaymentDialog) {
        PaymentMethodDialog(
            onDismiss = { showPaymentDialog = false },
            onConfirm = { paymentMethod ->
                showPaymentDialog = false
                selectedPaymentMethod = paymentMethod.displayName

                locationInfo = null
                nearbyMarkets = null

                showLocationDialog = true

                if (com.example.marketinho.features.location.LocationHelper.hasLocationPermission(context)) {
                    isLoadingLocation = true
                    kotlinx.coroutines.MainScope().launch {
                        locationInfo = com.example.marketinho.features.location.LocationHelper.getLocationInfo(context)
                        isLoadingLocation = false

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
            nearbyMarkets = nearbyMarkets,
            isSearchingMarkets = isSearchingMarkets,
            onConfirm = { marketName, address ->
                showLocationDialog = false

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
                nearbyMarkets = null
                isLoadingLocation = false
                isSearchingMarkets = false
            },
            onRetry = {
                if (com.example.marketinho.features.location.LocationHelper.hasLocationPermission(context)) {
                    isLoadingLocation = true
                    kotlinx.coroutines.MainScope().launch {
                        locationInfo = com.example.marketinho.features.location.LocationHelper.getLocationInfo(context)
                        isLoadingLocation = false

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