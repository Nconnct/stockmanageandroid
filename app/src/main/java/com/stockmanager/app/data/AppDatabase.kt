package com.stockmanager.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

const val DATABASE_NAME = "stock_manager.db"

@Database(entities = [Product::class, StockTransaction::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: build(context).also { instance = it }
            }
        }

        private fun build(context: Context): AppDatabase {
            return Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DATABASE_NAME)
                // TRUNCATE (not WAL) keeps everything in the single .db file, which keeps
                // manual backup/restore (Settings screen) simple and reliable.
                .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                .build()
        }

        /** Closes and clears the singleton so a freshly-restored file is picked up on next access. */
        fun closeAndReset(context: Context) {
            synchronized(this) {
                instance?.close()
                instance = null
            }
        }

        fun databaseFile(context: Context) = context.getDatabasePath(DATABASE_NAME)
    }
}
