package com.stockmanager.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: String = "",
    val quality: String = "",
    val quantity: Int = 0,
    val price: Double = 0.0,
    val reorderLevel: Int = 5,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    val total: Double get() = quantity * price
    val isOutOfStock: Boolean get() = quantity <= 0
    val isLowStock: Boolean get() = quantity in 1..reorderLevel
}
