package com.example.marketinho.features.product

import java.util.UUID
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "products")
data class Product(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val quantity: Int = 1,
    val price: Double,
    val imageUri: String?,
    val createdAt: Long = System.currentTimeMillis()
)