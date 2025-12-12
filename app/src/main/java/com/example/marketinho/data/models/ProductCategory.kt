package com.example.marketinho.data.models

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class ProductCategory(
    val displayName: String,
    val icon: ImageVector,
    val color: Color,
    val keywords: List<String>
) {
    ALIMENTOS(
        "Alimentos",
        Icons.Default.Restaurant,
        Color(0xFF4CAF50),
        listOf("arroz", "feijão", "macarrão", "massa", "farinha", "açúcar", "sal",
            "óleo", "azeite", "vinagre", "molho", "tempero", "condimento")
    ),

    BEBIDAS(
        "Bebidas",
        Icons.Default.LocalBar,
        Color(0xFF2196F3),
        listOf("água", "suco", "refrigerante", "coca", "pepsi", "guaraná", "fanta",
            "cerveja", "vinho", "bebida", "café", "chá", "energético")
    ),

    ACOUGUE(
        "Açougue",
        Icons.Default.Restaurant,
        Color(0xFFE91E63),
        listOf("carne", "frango", "peixe", "linguiça", "bacon", "salsicha", "bife",
            "costela", "alcatra", "picanha", "filé", "coxa", "peito")
    ),

    LATICINIOS(
        "Laticínios",
        Icons.Default.EmojiFoodBeverage,
        Color(0xFFFFC107),
        listOf("leite", "queijo", "iogurte", "manteiga", "margarina", "requeijão",
            "creme", "nata", "ricota", "mussarela", "prato")
    ),

    PADARIA(
        "Padaria",
        Icons.Default.Cake,
        Color(0xFFFF9800),
        listOf("pão", "bolo", "torta", "biscoito", "bolacha", "cookie", "rosca",
            "croissant", "sonho", "pãozinho")
    ),

    HIGIENE(
        "Higiene",
        Icons.Default.CleanHands,
        Color(0xFF00BCD4),
        listOf("sabonete", "shampoo", "condicionador", "creme dental", "pasta de dente",
            "escova", "desodorante", "perfume", "papel higiênico", "absorvente",
            "fralda", "lenço")
    ),

    LIMPEZA(
        "Limpeza",
        Icons.Default.CleaningServices,
        Color(0xFF9C27B0),
        listOf("detergente", "sabão", "amaciante", "alvejante", "cloro", "desinfetante",
            "esponja", "vassoura", "rodo", "pano", "água sanitária", "limpa", "lava")
    ),

    CONGELADOS(
        "Congelados",
        Icons.Default.AcUnit,
        Color(0xFF03A9F4),
        listOf("sorvete", "picolé", "congelado", "pizza congelada", "lasanha congelada",
            "hambúrguer", "nuggets", "batata congelada", "frozen")
    ),

    DOCES(
        "Doces",
        Icons.Default.Cake,
        Color(0xFFE91E63),
        listOf("chocolate", "bala", "chiclete", "bombom", "doce", "açúcar", "mel",
            "geleia", "goiabada", "paçoca", "pirulito")
    ),

    CASA(
        "Casa",
        Icons.Default.Home,
        Color(0xFF795548),
        listOf("pilha", "bateria", "vela", "fósforo", "lâmpada", "cola", "fita",
            "sacola", "saco", "papel alumínio", "filme plástico")
    ),

    OUTROS(
        "Outros",
        Icons.Default.MoreHoriz,
        Color(0xFF607D8B),
        listOf()
    );

    companion object {
        /**
         * Sugere uma categoria baseada no nome do produto
         */
        fun suggestFromName(productName: String): ProductCategory {
            val nameLower = productName.lowercase()

            // Tenta encontrar categoria por palavras-chave
            values().forEach { category ->
                if (category.keywords.any { keyword -> nameLower.contains(keyword) }) {
                    return category
                }
            }

            return OUTROS
        }

        /**
         * Converte String para enum
         */
        fun fromString(value: String?): ProductCategory? {
            return values().find { it.name == value || it.displayName == value }
        }
    }
}