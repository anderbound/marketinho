package com.example.marketinho.data.dao

import androidx.room.*
import com.example.marketinho.data.models.StandardProduct
import kotlinx.coroutines.flow.Flow

@Dao
interface StandardProductDao {

    /**
     * Busca todos os produtos padronizados
     * Ordenados por mais usados
     */
    @Query("SELECT * FROM standard_products ORDER BY usageCount DESC, lastUsed DESC")
    fun getAllStandardProducts(): Flow<List<StandardProduct>>

    /**
     * Busca produtos por texto (busca simples)
     */
    @Query("""
        SELECT * FROM standard_products 
        WHERE canonicalName LIKE '%' || :query || '%' 
        OR aliases LIKE '%' || :query || '%'
        ORDER BY usageCount DESC
        LIMIT :limit
    """)
    suspend fun searchProducts(query: String, limit: Int = 10): List<StandardProduct>

    /**
     * Busca produto por ID
     */
    @Query("SELECT * FROM standard_products WHERE id = :id")
    suspend fun getProductById(id: String): StandardProduct?

    /**
     * Busca produtos por categoria
     */
    @Query("SELECT * FROM standard_products WHERE category = :category ORDER BY usageCount DESC")
    fun getProductsByCategory(category: String): Flow<List<StandardProduct>>

    /**
     * Busca os produtos mais usados
     */
    @Query("SELECT * FROM standard_products ORDER BY usageCount DESC LIMIT :limit")
    suspend fun getMostUsedProducts(limit: Int = 20): List<StandardProduct>

    /**
     * Busca produtos usados recentemente
     */
    @Query("SELECT * FROM standard_products ORDER BY lastUsed DESC LIMIT :limit")
    suspend fun getRecentProducts(limit: Int = 10): List<StandardProduct>

    /**
     * Insere ou atualiza produto
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: StandardProduct)

    /**
     * Insere múltiplos produtos
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<StandardProduct>)

    /**
     * Atualiza produto
     */
    @Update
    suspend fun updateProduct(product: StandardProduct)

    /**
     * Deleta produto
     */
    @Delete
    suspend fun deleteProduct(product: StandardProduct)

    /**
     * Incrementa contador de uso
     */
    @Query("""
        UPDATE standard_products 
        SET usageCount = usageCount + 1, 
            lastUsed = :timestamp 
        WHERE id = :productId
    """)
    suspend fun incrementUsage(productId: String, timestamp: Long = System.currentTimeMillis())

    /**
     * Atualiza preço médio
     */
    @Query("UPDATE standard_products SET avgPrice = :avgPrice WHERE id = :productId")
    suspend fun updateAvgPrice(productId: String, avgPrice: Double)

    /**
     * Adiciona alias (nome alternativo)
     */
    @Query("UPDATE standard_products SET aliases = :aliases WHERE id = :productId")
    suspend fun updateAliases(productId: String, aliases: String)

    /**
     * Conta total de produtos
     */
    @Query("SELECT COUNT(*) FROM standard_products")
    suspend fun getProductCount(): Int
}