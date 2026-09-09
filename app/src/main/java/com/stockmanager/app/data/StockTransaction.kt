package com.stockmanager.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

const val MOVE_IN = "IN"
const val MOVE_OUT = "OUT"

@Entity(tableName = "stock_transactions")
data class StockTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long,
    val productName: String,
    val type: String, // MOVE_IN or MOVE_OUT
    val quantity: Int,
    val unitPrice: Double,
    val note: String = "",
    val balanceAfter: Int,
    val createdAt: Long = System.currentTimeMillis(),
)
