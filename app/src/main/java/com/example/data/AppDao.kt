package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Product queries
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Int): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Query("UPDATE products SET rate = :rate, lastUpdated = :timestamp WHERE id = :id")
    suspend fun updateProductRate(id: Int, rate: Double, timestamp: Long)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProduct(id: Int)

    // Order queries
    @Query("SELECT * FROM orders ORDER BY timestamp DESC")
    fun getAllOrders(): Flow<List<OrderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    // Cash session queries
    @Query("SELECT * FROM cash_sessions ORDER BY timestamp DESC")
    fun getAllCashSessions(): Flow<List<CashSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCashSession(session: CashSessionEntity)

    @Query("DELETE FROM cash_sessions WHERE id = :id")
    suspend fun deleteCashSession(id: Int)

    // Counter Item queries
    @Query("SELECT * FROM counter_items ORDER BY isCheque ASC, multiplier DESC, label ASC")
    fun getAllCounterItems(): Flow<List<CounterItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCounterItem(item: CounterItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCounterItems(items: List<CounterItemEntity>)

    @Query("DELETE FROM counter_items WHERE id = :id")
    suspend fun deleteCounterItem(id: Int)

    // Price History queries
    @Query("SELECT * FROM price_history ORDER BY timestamp DESC")
    fun getAllPriceHistory(): Flow<List<PriceHistoryEntity>>

    @Query("SELECT * FROM price_history WHERE productId = :productId ORDER BY timestamp DESC")
    fun getPriceHistoryForProduct(productId: Int): Flow<List<PriceHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriceHistory(history: PriceHistoryEntity)

    @Query("DELETE FROM price_history WHERE productId = :productId")
    suspend fun deletePriceHistoryForProduct(productId: Int)
}
