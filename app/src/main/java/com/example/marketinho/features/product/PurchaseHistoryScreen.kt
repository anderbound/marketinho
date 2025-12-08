package com.example.marketinho.features.product

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.marketinho.data.models.Purchase // Certifique-se de que este import está correto
import com.example.marketinho.data.models.PurchaseItem // Importe PurchaseItem também
import com.example.marketinho.features.auth.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseHistoryScreen(
    navController: NavController,
    purchaseViewModel: PurchaseViewModel = viewModel(
        factory = PurchaseViewModelFactory(LocalContext.current.applicationContext as Application)
    )
) {
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(LocalContext.current.applicationContext as Application)
    )
    val purchases by purchaseViewModel.userPurchases.collectAsState(initial = emptyList())

    LaunchedEffect(key1 = Unit) {
        purchaseViewModel.loadUserPurchases()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Histórico de Compras") },
                actions = {
                    Button(onClick = { authViewModel.signOut() }) {
                        Text("Sair")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    navController.navigate("main_screen") // Navega para a tela de nova compra (ProductMainScreen)
                }
            ) {
                Icon(Icons.Filled.Add, "Nova Compra")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (purchases.isEmpty()) {
                Text("Nenhuma compra registrada ainda. Clique no '+' para começar!")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(purchases) { purchase ->
                        PurchaseHistoryCard(
                            purchase = purchase,
                            onClick = {
                                // TODO: Implementar navegação para detalhes da compra
                                println("Clicou na compra de ${purchase.date}")
                                // navController.navigate("purchase_details_screen/${purchase.id}")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PurchaseHistoryCard(purchase: Purchase, onClick: (Purchase) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(purchase) }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            Text(text = "Data: ${dateFormatter.format(purchase.date)}")
            Text(text = "Mercado: ${if (purchase.marketName.isNotEmpty()) purchase.marketName else "Mercado Padrão"}") // Usar Mercado Padrão
            Text(text = "Total: R$ ${String.format("%.2f", purchase.total)}")
        }
    }
}

class PurchaseViewModel(application: Application) : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _userPurchases = MutableStateFlow<List<Purchase>>(emptyList())
    val userPurchases: StateFlow<List<Purchase>> = _userPurchases.asStateFlow()

    fun loadUserPurchases() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("purchases")
            .whereEqualTo("userId", userId)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    println("Erro ao carregar compras: $e")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val purchases = snapshot.toObjects(Purchase::class.java)
                    _userPurchases.value = purchases
                }
            }
    }

    fun savePurchase(products: List<Product>, total: Double) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            println("Usuário não autenticado, não foi possível salvar a compra.")
            return
        }

        val purchaseItems = products.map { product ->
            PurchaseItem(
                name = product.name,
                price = product.price,
                quantity = product.quantity
            )
        }

        val newPurchase = Purchase(
            date = Date(),
            total = total,
            marketName = "Mercado Padrão", // Placeholder para o nome do mercado
            userId = userId,
            items = purchaseItems
        )

        firestore.collection("purchases")
            .add(newPurchase)
            .addOnSuccessListener { documentReference ->
                println("Compra salva com sucesso! ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                println("Erro ao salvar compra: $e")
            }
    }
}

class PurchaseViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PurchaseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PurchaseViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}