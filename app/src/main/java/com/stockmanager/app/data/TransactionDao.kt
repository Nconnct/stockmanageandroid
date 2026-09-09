package com.stockmanager.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TransactionDao {

    @Insert
    suspend fun insert(transaction: StockTransaction): Long

    @Query("SELECT * FROM stock_transactions ORDER BY createdAt DESC, id DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<StockTransaction>

    @Query("SELECT * FROM stock_transactions WHERE type = :type ORDER BY createdAt DESC, id DESC")
    suspend fun getByType(type: String): List<StockTransaction>

    @Query(
        """SELECT * FROM stock_transactions
           WHERE (:type = 'ALL' OR type = :type)
             AND createdAt >= :fromMillis AND createdAt <= :toMillis
             AND lower(productName) LIKE '%' || lower(:search) || '%'
           ORDER BY createdAt DESC, id DESC"""
    )
    suspend fun getFiltered(type: String, fromMillis: Long, toMillis: Long, search: String): List<StockTransaction>
}
