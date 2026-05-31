package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val rate: Double,
    val unit: String,
    val lastUpdated: Long = System.currentTimeMillis(),
    
    // Connected Rate Parameters
    val isDependent: Boolean = false,
    val parentProductId: Int = 0,
    val depType: String = "", // "DIFFERENCE" or "PROPORTION"
    val depValue: Double = 0.0, // e.g. -100.0
    val depParentFactor: Double = 1.0, // e.g. 16.0 (for dividing by 16)
    val depOwnFactor: Double = 1.0 // e.g. 12.0 (for multiplying by 12)
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerName: String,
    val customerPhone: String,
    val itemsSummary: String,
    val totalAmount: Double,
    val paymentType: String, // "WhatsApp", "Direct", "Digital Wallet", "Credit Card"
    val paymentStatus: String, // "Pending", "Paid"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "cash_sessions")
data class CashSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val countsString: String, // denomination/label:count:multiplier:isCheque:remarks
    val totalAmount: Double,
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "counter_items")
data class CounterItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val label: String,
    val multiplier: Double,
    val isCheque: Boolean = false,
    val isCustom: Boolean = false
)

@Entity(tableName = "price_history")
data class PriceHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productId: Int,
    val productName: String,
    val oldRate: Double,
    val newRate: Double,
    val timestamp: Long = System.currentTimeMillis()
)
