// features/product/utils/PriceUtils.kt
package com.example.marketinho.features.product.utils

import java.util.*

object PriceUtils {
    // Formata Double para R$ 0,00
    fun format(price: Double): String {
        return String.format(Locale("pt", "BR"), "R$ %.2f", price)
    }

    // Converte String (como "9,99") para Double
    fun parse(priceString: String): Double? {
        return try {
            priceString.replace("R$", "")
                .replace(",", ".")
                .trim()
                .toDouble()
        } catch (e: Exception) {
            null
        }
    }

    // Valida se o texto é um preço válido
    fun isValid(priceText: String): Boolean {
        return parse(priceText) != null
    }
}