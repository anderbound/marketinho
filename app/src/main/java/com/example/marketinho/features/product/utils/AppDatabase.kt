package com.example.marketinho.features.product.utils

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.marketinho.data.converters.StringListConverter
import com.example.marketinho.data.dao.StandardProductDao
import com.example.marketinho.data.models.StandardProduct
import com.example.marketinho.features.product.Product
import com.example.marketinho.features.product.utils.ProductDao

@Database(
    entities = [
        Product::class,
        StandardProduct::class // ✅ NOVA ENTIDADE
    ],
    version = 4 // ✅ NOVA VERSÃO
)
@TypeConverters(StringListConverter::class) // ✅ Conversor para List<String>
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun standardProductDao(): StandardProductDao // ✅ NOVO DAO

    companion object {
        // Migration 1 → 2 (createdAt)
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE products ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        // Migration 2 → 3 (category e brand)
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

        // ✅ NOVA MIGRATION 3 → 4 (standard_products)
        // Room cria os índices automaticamente da anotação @Entity
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Cria apenas a tabela - índices são criados pelo Room
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `standard_products` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `canonicalName` TEXT NOT NULL,
                        `brand` TEXT,
                        `category` TEXT,
                        `variants` TEXT NOT NULL,
                        `aliases` TEXT NOT NULL,
                        `avgPrice` REAL NOT NULL,
                        `usageCount` INTEGER NOT NULL,
                        `lastUsed` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                """)
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
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4
                    )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}