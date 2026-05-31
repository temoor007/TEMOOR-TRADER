package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class AppRepository(private val dao: AppDao) {

    val allProducts: Flow<List<ProductEntity>> = dao.getAllProducts()
    val allOrders: Flow<List<OrderEntity>> = dao.getAllOrders()
    val allCashSessions: Flow<List<CashSessionEntity>> = dao.getAllCashSessions()
    val allCounterItems: Flow<List<CounterItemEntity>> = dao.getAllCounterItems()
    val allPriceHistory: Flow<List<PriceHistoryEntity>> = dao.getAllPriceHistory()

    fun getPriceHistoryForProduct(productId: Int): Flow<List<PriceHistoryEntity>> {
        return dao.getPriceHistoryForProduct(productId)
    }

    suspend fun insertProduct(product: ProductEntity): Long {
        val newId = dao.insertProduct(product)
        // If product is newly added and is dependent, run initial calculation
        if (product.isDependent && product.parentProductId > 0) {
            val parent = dao.getProductById(product.parentProductId)
            if (parent != null) {
                val computedRate = calculateRate(parent.rate, product)
                dao.updateProductRate(newId.toInt(), computedRate, System.currentTimeMillis())
            }
        }
        return newId
    }

    private fun calculateRate(parentRate: Double, dep: ProductEntity): Double {
        return when (dep.depType) {
            "DIFFERENCE" -> parentRate + dep.depValue
            "PROPORTION" -> {
                val pf = if (dep.depParentFactor > 0.0) dep.depParentFactor else 1.0
                (parentRate / pf) * dep.depOwnFactor
            }
            else -> parentRate
        }
    }

    suspend fun updateProductRate(id: Int, rate: Double) {
        updateProductRateAndConnected(id, rate)
    }

    suspend fun updateProductRateAndConnected(id: Int, newRate: Double, timestamp: Long = System.currentTimeMillis()) {
        val product = dao.getProductById(id) ?: return
        val oldRate = product.rate

        // Update main product
        dao.updateProductRate(id, newRate, timestamp)

        // Write price history log if rate changed
        if (oldRate != newRate) {
            dao.insertPriceHistory(
                PriceHistoryEntity(
                    productId = id,
                    productName = product.name,
                    oldRate = oldRate,
                    newRate = newRate,
                    timestamp = timestamp
                )
            )
        }

        // Fetch all current products to recompute descendants
        val allProds = dao.getAllProducts().first()
        updateDependentsRecursive(id, newRate, allProds, timestamp)
    }

    private suspend fun updateDependentsRecursive(parentId: Int, parentRate: Double, allProds: List<ProductEntity>, timestamp: Long) {
        allProds.filter { it.isDependent && it.parentProductId == parentId }.forEach { depProd ->
            val calculatedRate = calculateRate(parentRate, depProd)
            val oldDepRate = depProd.rate

            if (oldDepRate != calculatedRate) {
                dao.updateProductRate(depProd.id, calculatedRate, timestamp)
                dao.insertPriceHistory(
                    PriceHistoryEntity(
                        productId = depProd.id,
                        productName = depProd.name,
                        oldRate = oldDepRate,
                        newRate = calculatedRate,
                        timestamp = timestamp
                    )
                )
                // Recursive propagation
                updateDependentsRecursive(depProd.id, calculatedRate, allProds, timestamp)
            }
        }
    }

    suspend fun deleteProduct(id: Int) {
        dao.deleteProduct(id)
        dao.deletePriceHistoryForProduct(id)
    }

    suspend fun insertOrder(order: OrderEntity) {
        dao.insertOrder(order)
    }

    suspend fun insertCashSession(session: CashSessionEntity) {
        dao.insertCashSession(session)
    }

    suspend fun deleteCashSession(id: Int) {
        dao.deleteCashSession(id)
    }

    // --- Counter Items Configuration ---
    suspend fun insertCounterItem(item: CounterItemEntity) {
        dao.insertCounterItem(item)
    }

    suspend fun deleteCounterItem(id: Int) {
        dao.deleteCounterItem(id)
    }

    suspend fun seedDatabaseIfEmpty() {
        // Clear or seed products
        val currentProducts = dao.getAllProducts().first()
        if (currentProducts.isEmpty()) {
            val defaultProducts = listOf(
                ProductEntity(name = "Cooking Oil Tint 16kg (Teen)", rate = 7800.0, unit = "canister"),
                ProductEntity(name = "Basmati Rice Super", rate = 320.0, unit = "kg"),
                ProductEntity(name = "Wheat Flour (Atta)", rate = 140.0, unit = "kg"),
                ProductEntity(name = "Refined Sugar (Cheeni)", rate = 160.0, unit = "kg"),
                ProductEntity(name = "Fresh Milk", rate = 210.0, unit = "litre")
            )
            dao.insertProducts(defaultProducts)
            
            // To fulfill formula constraints, let's create a 16kg Teen Oil product
            // and seed dependent products! This is extremely cool for onboarding demonstration.
            val allNow = dao.getAllProducts().first()
            val teen16 = allNow.find { it.name.contains("Teen", ignoreCase = true) }
            teen16?.let { parent ->
                // "16 kg ke teen se 16kg peeti satsi hoti ha 100 rupe"
                val peeti16 = ProductEntity(
                    name = "Cooking Oil Peeti 16kg",
                    rate = parent.rate - 100.0,
                    unit = "peeti",
                    isDependent = true,
                    parentProductId = parent.id,
                    depType = "DIFFERENCE",
                    depValue = -100.0
                )
                val peeti16Id = dao.insertProduct(peeti16).toInt()

                // "12kg ke peeti 16kg pr taqseem kr ke rate niklate ha"
                val peeti12 = ProductEntity(
                    name = "Cooking Oil Peeti 12kg",
                    rate = ((parent.rate - 100.0) / 16.0) * 12.0,
                    unit = "peeti",
                    isDependent = true,
                    parentProductId = peeti16Id,
                    depType = "PROPORTION",
                    depParentFactor = 16.0,
                    depOwnFactor = 12.0
                )
                dao.insertProduct(peeti12)
            }
        }

        // Seed Counter Items
        val currentCounter = dao.getAllCounterItems().first()
        if (currentCounter.isEmpty()) {
            val defaultCounterItems = listOf(
                CounterItemEntity(label = "5000 PKR Note", multiplier = 5000.0, isCheque = false),
                CounterItemEntity(label = "1000 PKR Note", multiplier = 1000.0, isCheque = false),
                CounterItemEntity(label = "500 PKR Note", multiplier = 500.0, isCheque = false),
                CounterItemEntity(label = "100 PKR Note", multiplier = 100.0, isCheque = false),
                CounterItemEntity(label = "50 PKR Note", multiplier = 50.0, isCheque = false),
                CounterItemEntity(label = "10 PKR Note", multiplier = 10.0, isCheque = false),
                CounterItemEntity(label = "Bank Cheque A", multiplier = 1.0, isCheque = true),
                CounterItemEntity(label = "Bank Cheque B", multiplier = 1.0, isCheque = true)
            )
            dao.insertCounterItems(defaultCounterItems)
        }
    }
}
