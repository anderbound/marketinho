package com.example.marketinho.data.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.marketinho.data.converters.StringListConverter
import java.util.UUID

/**
 * Produto padronizado no banco de dados
 * Serve como "referência" para normalizar nomes
 */
@Entity(
    tableName = "standard_products",
    indices = [
        Index(value = ["canonicalName"]),
        Index(value = ["category"]),
        Index(value = ["usageCount"])
    ]
)
@TypeConverters(StringListConverter::class)
data class StandardProduct(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    // Nome canônico (oficial/padronizado)
    val canonicalName: String,

    // Marca (ex: "Tio João", "Nestlé")
    val brand: String? = null,

    // Categoria
    val category: String? = null,

    // Variantes (ex: ["1kg", "5kg", "500g"])
    val variants: List<String> = emptyList(),

    // Nomes alternativos que o usuário já usou
    // Ex: ["Arroz Tio Joao", "Arroz TJ", "Arroz tio joao 5kg"]
    val aliases: List<String> = emptyList(),

    // Preço médio (calculado automaticamente)
    val avgPrice: Double = 0.0,

    // Quantas vezes foi usado
    val usageCount: Int = 0,

    // Última vez que foi usado (timestamp)
    val lastUsed: Long = System.currentTimeMillis(),

    // Data de criação
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Verifica se um texto "bate" com este produto
     * Usa matching fuzzy (tolerante a erros)
     */
    fun matches(input: String): Boolean {
        val normalizedInput = input.lowercase().trim()

        // Verifica nome canônico
        if (canonicalName.lowercase().contains(normalizedInput)) return true

        // Verifica aliases
        if (aliases.any { it.lowercase().contains(normalizedInput) }) return true

        // Matching reverso (input contém o nome)
        if (normalizedInput.contains(canonicalName.lowercase())) return true

        return false
    }

    /**
     * Calcula similaridade com um texto (0.0 a 1.0)
     * Usado para ranking de sugestões
     */
    fun similarity(input: String): Double {
        val normalizedInput = input.lowercase().trim()
        val normalizedCanonical = canonicalName.lowercase().trim()

        // Match exato = 1.0
        if (normalizedInput == normalizedCanonical) return 1.0

        // Contém o texto completo = 0.8
        if (normalizedCanonical.contains(normalizedInput)) return 0.8

        // Calcula Levenshtein distance (similaridade)
        val distance = levenshteinDistance(normalizedInput, normalizedCanonical)
        val maxLength = maxOf(normalizedInput.length, normalizedCanonical.length)

        return 1.0 - (distance.toDouble() / maxLength)
    }

    /**
     * Distância de Levenshtein (quantas mudanças precisa para ficar igual)
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }

        return dp[s1.length][s2.length]
    }
}