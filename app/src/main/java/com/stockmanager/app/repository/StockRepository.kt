package com.stockmanager.app.repository

import android.content.Context
import android.net.Uri
import com.stockmanager.app.data.AppDatabase
import com.stockmanager.app.data.MOVE_IN
import com.stockmanager.app.data.MOVE_OUT
import com.stockmanager.app.data.Prefs
import com.stockmanager.app.data.Product
import com.stockmanager.app.data.StockTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class DashboardStats(
    val totalProducts: Int,
    val totalStockQty: Int,
    val totalStockValue: Double,
    val lowStock: Int,
    val outOfStock: Int,
)

class StockMoveException(message: String) : Exception(message)

class StockRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val productDao = db.productDao()
    private val transactionDao = db.transactionDao()
    val prefs = Prefs(context)

    // ---------------------------------------------------------- products --
    suspend fun getProducts(search: String = ""): List<Product> =
        if (search.isBlank()) productDao.getAll() else productDao.search(search)

    suspend fun getProduct(id: Long): Product? = productDao.getById(id)

    suspend fun findByName(name: String): Product? = productDao.findByName(name)

    suspend fun addProduct(
        name: String, category: String, quality: String,
        quantity: Int, price: Double, reorderLevel: Int,
    ): Long {
        val now = System.currentTimeMillis()
        return productDao.insert(
            Product(
                name = name.trim(), category = category.trim(), quality = quality.trim(),
                quantity = quantity, price = price, reorderLevel = reorderLevel,
                createdAt = now, updatedAt = now,
            )
        )
    }

    suspend fun updateProductDetails(
        product: Product, name: String, category: String, quality: String,
        price: Double, reorderLevel: Int,
    ) {
        productDao.update(
            product.copy(
                name = name.trim(), category = category.trim(), quality = quality.trim(),
                price = price, reorderLevel = reorderLevel, updatedAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun deleteProduct(product: Product) = productDao.delete(product)

    // ------------------------------------------------------------- moves --
    suspend fun recordStockMove(productId: Long, type: String, quantity: Int, unitPrice: Double, note: String): Int {
        val product = productDao.getById(productId) ?: throw StockMoveException("Product not found.")
        val newQty = when (type) {
            MOVE_IN -> product.quantity + quantity
            MOVE_OUT -> {
                if (quantity > product.quantity) {
                    throw StockMoveException(
                        "Not enough stock. Only ${product.quantity} unit(s) of '${product.name}' available."
                    )
                }
                product.quantity - quantity
            }
            else -> throw StockMoveException("Invalid move type.")
        }
        productDao.update(product.copy(quantity = newQty, updatedAt = System.currentTimeMillis()))
        transactionDao.insert(
            StockTransaction(
                productId = productId, productName = product.name, type = type,
                quantity = quantity, unitPrice = unitPrice, note = note, balanceAfter = newQty,
            )
        )
        return newQty
    }

    // --------------------------------------------------------- dashboard --
    suspend fun getDashboardStats(): DashboardStats {
        val products = productDao.getAll()
        return DashboardStats(
            totalProducts = products.size,
            totalStockQty = products.sumOf { it.quantity },
            totalStockValue = products.sumOf { it.total },
            lowStock = products.count { it.isLowStock },
            outOfStock = products.count { it.isOutOfStock },
        )
    }

    suspend fun getRecentTransactions(limit: Int = 10) = transactionDao.getRecent(limit)

    suspend fun getTransactionsByType(type: String) = transactionDao.getByType(type)

    suspend fun getFilteredTransactions(type: String, fromMillis: Long, toMillis: Long, search: String) =
        transactionDao.getFiltered(type, fromMillis, toMillis, search)

    // ------------------------------------------------------------ backup --
    suspend fun backupTo(uri: Uri) = withContext(Dispatchers.IO) {
        val dbFile = AppDatabase.databaseFile(context)
        context.contentResolver.openOutputStream(uri)?.use { out ->
            dbFile.inputStream().use { input -> input.copyTo(out) }
        }
    }

    suspend fun restoreFrom(uri: Uri) = withContext(Dispatchers.IO) {
        val dbFile = AppDatabase.databaseFile(context)
        AppDatabase.closeAndReset(context)
        context.contentResolver.openInputStream(uri)?.use { input ->
            dbFile.outputStream().use { out -> input.copyTo(out) }
        }
        // Also drop any leftover -wal/-shm from a previous run, just in case.
        File(dbFile.path + "-wal").delete()
        File(dbFile.path + "-shm").delete()
    }
}
