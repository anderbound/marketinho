package com.example.marketinho.data.models

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class PaymentMethod(
    val displayName: String,
    val icon: ImageVector
) {
    CASH("Dinheiro", Icons.Default.AttachMoney),
    DEBIT_CARD("Cartão de Débito", Icons.Default.CreditCard),
    CREDIT_CARD("Cartão de Crédito", Icons.Default.CreditCard),
    PIX("PIX", Icons.Default.AccountBalance),
    FOOD_VOUCHER("Vale Alimentação", Icons.Default.CardGiftcard),
    MEAL_VOUCHER("Vale Refeição", Icons.Default.Restaurant);

    companion object {
        fun fromString(value: String?): PaymentMethod? {
            return values().find { it.name == value || it.displayName == value }
        }
    }
}