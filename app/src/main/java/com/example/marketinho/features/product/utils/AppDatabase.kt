// features/product/utils/AppDatabase.kt
package com.example.marketinho.features.product.utils

import com.example.marketinho.features.product.ProductDao



import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.marketinho.features.product.Product

@Database(
    entities = [Product::class],
    version = 2 // Aumente a versão
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao

    companion object {
        // Adicione esta migration
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE products ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "marketinho_db"
            )
                .addMigrations(MIGRATION_1_2) // Adicione a migration
                .build()
        }
    }
}