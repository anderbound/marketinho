package com.example.marketinho.data.repositories

import android.content.Context
import android.util.Log
import com.example.marketinho.data.dao.StandardProductDao
import com.example.marketinho.data.models.StandardProduct
import com.example.marketinho.features.product.utils.AppDatabase
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow

/**
 * Repository para gerenciar produtos padronizados
 * Implementa lógica de matching e sugestões
 */
class StandardProductRepository(context: Context) {

    private val dao: StandardProductDao = AppDatabase.getDatabase(context).standardProductDao()
    private val gson = Gson()

    /**
     * Busca sugestões para um texto digitado pelo usuário
     * Retorna produtos ordenados por relevância
     */
    suspend fun findSuggestions(input: String, limit: Int = 5): List<StandardProduct> {
        if (input.length < 2) return emptyList()

        // Busca no banco
        val candidates = dao.searchProducts(input, limit = 20)

        // Calcula similaridade e ordena
        val ranked = candidates
            .map { product -> product to product.similarity(input) }
            .filter { (_, score) -> score > 0.5 } // Só mostra se tiver >50% similaridade
            .sortedByDescending { (_, score) -> score }
            .take(limit)
            .map { (product, _) -> product }

        Log.d("StandardProductRepo", "Sugestões para '$input': ${ranked.size} produtos")
        return ranked
    }

    /**
     * Busca o melhor match para um texto
     * Retorna null se não encontrar nada parecido
     */
    suspend fun findBestMatch(input: String, minSimilarity: Double = 0.7): StandardProduct? {
        val suggestions = findSuggestions(input, limit = 1)
        val best = suggestions.firstOrNull() ?: return null

        // Verifica se a similaridade é boa o suficiente
        val similarity = best.similarity(input)

        return if (similarity >= minSimilarity) {
            Log.d("StandardProductRepo", "Match encontrado: '${best.canonicalName}' (${(similarity * 100).toInt()}%)")
            best
        } else {
            Log.d("StandardProductRepo", "Nenhum match bom para '$input' (melhor: ${(similarity * 100).toInt()}%)")
            null
        }
    }

    /**
     * Cria um novo produto padronizado
     */
    suspend fun createStandardProduct(
        name: String,
        brand: String? = null,
        category: String? = null,
        price: Double = 0.0
    ): StandardProduct {
        val product = StandardProduct(
            canonicalName = name.trim(),
            brand = brand?.trim(),
            category = category,
            avgPrice = price,
            usageCount = 1,
            lastUsed = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis()
        )

        dao.insertProduct(product)
        Log.d("StandardProductRepo", "✅ Produto criado: ${product.canonicalName}")

        return product
    }

    /**
     * Registra uso de um produto existente
     * Atualiza contador e adiciona alias se necessário
     */
    suspend fun recordUsage(
        productId: String,
        usedName: String? = null,
        price: Double? = null
    ) {
        // Incrementa uso
        dao.incrementUsage(productId, System.currentTimeMillis())

        val product = dao.getProductById(productId) ?: return

        // Adiciona alias se for um nome diferente
        if (usedName != null && usedName.trim() != product.canonicalName) {
            val normalizedName = usedName.trim().lowercase()
            val currentAliases = product.aliases.toMutableList()

            if (!currentAliases.any { it.lowercase() == normalizedName }) {
                currentAliases.add(usedName.trim())

                // Atualiza aliases no banco
                val aliasesJson = gson.toJson(currentAliases)
                dao.updateAliases(productId, aliasesJson)

                Log.d("StandardProductRepo", "✅ Alias adicionado: '$usedName' → '${product.canonicalName}'")
            }
        }

        // Atualiza preço médio se fornecido
        if (price != null && price > 0) {
            val newAvg = if (product.avgPrice == 0.0) {
                price
            } else {
                // Média ponderada (favorece preços recentes)
                (product.avgPrice * 0.7 + price * 0.3)
            }
            dao.updateAvgPrice(productId, newAvg)
        }
    }

    /**
     * Busca produtos mais usados (para autocomplete)
     */
    suspend fun getMostUsedProducts(limit: Int = 20): List<StandardProduct> {
        return dao.getMostUsedProducts(limit)
    }

    /**
     * Busca produtos usados recentemente
     */
    suspend fun getRecentProducts(limit: Int = 10): List<StandardProduct> {
        return dao.getRecentProducts(limit)
    }

    /**
     * Busca produtos por categoria
     */
    fun getProductsByCategory(category: String): Flow<List<StandardProduct>> {
        return dao.getProductsByCategory(category)
    }

    /**
     * Busca todos os produtos
     */
    fun getAllProducts(): Flow<List<StandardProduct>> {
        return dao.getAllStandardProducts()
    }

    /**
     * Conta quantos produtos padronizados existem
     */
    suspend fun getProductCount(): Int {
        return dao.getProductCount()
    }

    /**
     * Popular banco com produtos iniciais (para teste)
     */
    suspend fun seedInitialProducts() {
        val count = getProductCount()
        if (count > 0) {
            Log.d("StandardProductRepo", "Banco já tem $count produtos, pulando seed")
            return
        }

        Log.d("StandardProductRepo", "Populando banco com produtos iniciais...")

        val initialProducts = listOf(
            StandardProduct(
                canonicalName = "Arroz Branco Tipo 1",
                category = "ALIMENTOS",
                variants = listOf("1kg", "5kg"),
                aliases = listOf("arroz", "arroz branco")
            ),
            StandardProduct(
                canonicalName = "Feijão Preto",
                category = "ALIMENTOS",
                variants = listOf("1kg", "2kg"),
                aliases = listOf("feijao", "feijão")
            ),
            StandardProduct(
                canonicalName = "Óleo de Soja",
                category = "ALIMENTOS",
                variants = listOf("900ml"),
                aliases = listOf("oleo", "óleo")
            ),
            StandardProduct(
                canonicalName = "Açúcar Cristal",
                category = "ALIMENTOS",
                variants = listOf("1kg", "2kg"),
                aliases = listOf("acucar", "açúcar")
            ),
            StandardProduct(
                canonicalName = "Café em Pó",
                category = "BEBIDAS",
                variants = listOf("250g", "500g"),
                aliases = listOf("cafe", "café")
            ),
            StandardProduct(
                canonicalName = "Leite Integral",
                category = "LATICINIOS",
                variants = listOf("1L"),
                aliases = listOf("leite")
            ),
            StandardProduct(
                canonicalName = "Macarrão Espaguete",
                category = "ALIMENTOS",
                variants = listOf("500g"),
                aliases = listOf("macarrao", "massa", "espaguete")
            ),
            StandardProduct(
                canonicalName = "Sal Refinado",
                category = "ALIMENTOS",
                variants = listOf("1kg"),
                aliases = listOf("sal")
            )
        )

        dao.insertProducts(initialProducts)
        Log.d("StandardProductRepo", "✅ ${initialProducts.size} produtos iniciais adicionados")
    }
}