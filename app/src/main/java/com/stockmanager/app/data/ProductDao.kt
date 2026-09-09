package com.stockmanager.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ProductDao {

    @Insert
    suspend fun insert(product: Product): Long

    @Update
    suspend fun update(product: Product)

    @Delete
    suspend fun delete(product: Product)

    @Query("SELECT * FROM products ORDER BY name COLLATE NOCASE")
    suspend fun getAll(): List<Product>

    @Query(
        """SELECT * FROM products
           WHERE lower(name) LIKE '%' || lower(:search) || '%'
              OR lower(category) LIKE '%' || lower(:search) || '%'
              OR lower(quality) LIKE '%' || lower(:search) || '%'
           ORDER BY name COLLATE NOCASE"""
    )
    suspend fun search(search: String): List<Product>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Product?

    @Query("SELECT * FROM products WHERE lower(name) = lower(:name) LIMIT 1")
    suspend fun findByName(name: String): Product?
}
