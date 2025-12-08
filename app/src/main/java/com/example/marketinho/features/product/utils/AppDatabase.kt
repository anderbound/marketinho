package com.example.marketinho.features.product.utils

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.marketinho.features.product.Product

@Database(
    entities = [Product::class],
    version = 3 // NOVA VERSÃO
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao

    companion object {
        // Migration 1 → 2 (createdAt)
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE products ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        // NOVA MIGRATION 2 → 3 (category e brand)
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE products ADD COLUMN category TEXT"
                )
                database.execSQL(
                    "ALTER TABLE products ADD COLUMN brand TEXT"
                )
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "marketinho_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3) // Adiciona todas as migrations
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}