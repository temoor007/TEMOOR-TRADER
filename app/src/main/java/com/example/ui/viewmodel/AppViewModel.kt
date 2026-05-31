package com.example.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

data class CounterActiveInput(
    val count: String = "",
    val chequeAmount: String = "",
    val remarks: String = ""
)

class AppViewModel(private val repository: AppRepository) : ViewModel() {

    // Bottom Navigation tab selection
    private val _currentTab = MutableStateFlow(0) // 0: Products, 1: Cash Box, 2: Manager
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    // Products Search Query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Base Products Reactive Flow
    val products: StateFlow<List<ProductEntity>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Products list
    val filteredProducts: StateFlow<List<ProductEntity>> = combine(products, searchQuery) { prodList, query ->
        if (query.isBlank()) {
            prodList
        } else {
            prodList.filter { it.name.contains(query, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Orders Flow
    val orders: StateFlow<List<OrderEntity>> = repository.allOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Saved Cash Sessions
    val cashSessions: StateFlow<List<CashSessionEntity>> = repository.allCashSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Counter items (PKR notes, Cheques, and Custom)
    val counterItems: StateFlow<List<CounterItemEntity>> = repository.allCounterItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Historical price updates diary
    val priceHistoryToObserve: StateFlow<List<PriceHistoryEntity>> = repository.allPriceHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cart state: Map of ProductId -> Quantity
    private val _cart = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val cart: StateFlow<Map<Int, Int>> = _cart.asStateFlow()

    // Dynamic Counter Active Inputs map (CounterItemId -> CounterActiveInput)
    private val _counterCounts = MutableStateFlow<Map<Int, CounterActiveInput>>(emptyMap())
    val counterCounts: StateFlow<Map<Int, CounterActiveInput>> = _counterCounts.asStateFlow()

    // Total Amount Cash Live Calculation
    val totalCashCalculated: StateFlow<Double> = combine(counterItems, _counterCounts) { items, counts ->
        items.sumOf { item ->
            val active = counts[item.id] ?: CounterActiveInput()
            if (item.isCheque) {
                val amt = active.chequeAmount.toDoubleOrNull() ?: 0.0
                val qty = if (active.count.isEmpty()) 1 else (active.count.toIntOrNull() ?: 1)
                amt * qty
            } else {
                val qty = active.count.toIntOrNull() ?: 0
                item.multiplier * qty
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Saved Status Message (for UI Snacking)
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedDatabaseIfEmpty()
        }
    }

    fun selectTab(tabIndex: Int) {
        _currentTab.value = tabIndex
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun showStatus(message: String) {
        _statusMessage.value = message
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            if (_statusMessage.value == message) {
                _statusMessage.value = null
            }
        }
    }

    // --- CART OPERATIONS ---
    fun addToCart(productId: Int) {
        val current = _cart.value.toMutableMap()
        val currentQty = current[productId] ?: 0
        current[productId] = currentQty + 1
        _cart.value = current
    }

    fun removeFromCart(productId: Int) {
        val current = _cart.value.toMutableMap()
        val currentQty = current[productId] ?: 0
        if (currentQty <= 1) {
            current.remove(productId)
        } else {
            current[productId] = currentQty - 1
        }
        _cart.value = current
    }

    fun updateCartQty(productId: Int, qty: Int) {
        val current = _cart.value.toMutableMap()
        if (qty <= 0) {
            current.remove(productId)
        } else {
            current[productId] = qty
        }
        _cart.value = current
    }

    fun clearCart() {
        _cart.value = emptyMap()
    }

    // --- PRODUCT RATE UPDATES (ADMIN) ---
    fun updateProductRate(productId: Int, newRate: Double) {
        viewModelScope.launch {
            repository.updateProductRate(productId, newRate)
            showStatus("Rate updated successfully!")
        }
    }

    fun addNewProduct(name: String, rate: Double, unit: String) {
        viewModelScope.launch {
            repository.insertProduct(ProductEntity(name = name, rate = rate, unit = unit))
            showStatus("New product added!")
        }
    }

    fun deleteProduct(id: Int) {
        viewModelScope.launch {
            repository.deleteProduct(id)
            showStatus("Product removed!")
        }
    }

    // --- CONNECTED PRODUCT RATE FORMULAS ---
    fun saveProductConnection(
        productId: Int,
        parentProductId: Int,
        depType: String,
        depValue: Double,
        depParentFactor: Double,
        depOwnFactor: Double
    ) {
        viewModelScope.launch {
            val pList = products.value
            val product = pList.find { it.id == productId } ?: return@launch
            val parent = pList.find { it.id == parentProductId } ?: return@launch

            val updated = product.copy(
                isDependent = true,
                parentProductId = parentProductId,
                depType = depType,
                depValue = depValue,
                depParentFactor = depParentFactor,
                depOwnFactor = depOwnFactor
            )

            // Save relationship metadata
            repository.insertProduct(updated)

            // Calculate & propagate immediately
            val computedRate = if (depType == "DIFFERENCE") {
                parent.rate + depValue
            } else {
                val pf = if (depParentFactor > 0.0) depParentFactor else 1.0
                (parent.rate / pf) * depOwnFactor
            }

            repository.updateProductRateAndConnected(productId, computedRate)
            showStatus("Rate connection rule established!")
        }
    }

    fun removeProductConnection(productId: Int) {
        viewModelScope.launch {
            val product = products.value.find { it.id == productId } ?: return@launch
            val updated = product.copy(
                isDependent = false,
                parentProductId = 0,
                depType = "",
                depValue = 0.0,
                depParentFactor = 1.0,
                depOwnFactor = 1.0
            )
            repository.insertProduct(updated)
            showStatus("Formula connection removed successfully.")
        }
    }

    // --- CASH CALCULATOR METHODS ---
    fun updateCounterCount(itemId: Int, countStr: String) {
        val filteredStr = countStr.filter { it.isDigit() }
        val updatedMap = _counterCounts.value.toMutableMap()
        val active = updatedMap[itemId] ?: CounterActiveInput()
        updatedMap[itemId] = active.copy(count = filteredStr)
        _counterCounts.value = updatedMap
    }

    fun adjustCounterCountBy(itemId: Int, delta: Int) {
        val updatedMap = _counterCounts.value.toMutableMap()
        val active = updatedMap[itemId] ?: CounterActiveInput()
        val currentQty = active.count.toIntOrNull() ?: 0
        val newQty = (currentQty + delta).coerceAtLeast(0)
        updatedMap[itemId] = active.copy(count = if (newQty == 0) "" else newQty.toString())
        _counterCounts.value = updatedMap
    }

    fun updateChequeAmount(itemId: Int, amountStr: String) {
        val filteredStr = amountStr.filter { it.isDigit() }
        val updatedMap = _counterCounts.value.toMutableMap()
        val active = updatedMap[itemId] ?: CounterActiveInput()
        updatedMap[itemId] = active.copy(chequeAmount = filteredStr)
        _counterCounts.value = updatedMap
    }

    fun updateChequeRemarks(itemId: Int, remarks: String) {
        val updatedMap = _counterCounts.value.toMutableMap()
        val active = updatedMap[itemId] ?: CounterActiveInput()
        updatedMap[itemId] = active.copy(remarks = remarks)
        _counterCounts.value = updatedMap
    }

    fun addCustomCounterItem(label: String, multiplier: Double, isCheque: Boolean) {
        viewModelScope.launch {
            val item = CounterItemEntity(
                label = label,
                multiplier = multiplier,
                isCheque = isCheque,
                isCustom = true
            )
            repository.insertCounterItem(item)
            showStatus("Custom item '$label' added to counter!")
        }
    }

    fun deleteCounterItem(itemId: Int) {
        viewModelScope.launch {
            repository.deleteCounterItem(itemId)
            val updatedMap = _counterCounts.value.toMutableMap()
            updatedMap.remove(itemId)
            _counterCounts.value = updatedMap
            showStatus("Counter item deleted!")
        }
    }

    fun clearCashCalculator() {
        _counterCounts.value = emptyMap()
        showStatus("Calculator cleared!")
    }

    fun saveCashSession(description: String) {
        val total = totalCashCalculated.value
        if (total <= 0) {
            showStatus("Cannot save empty session!")
            return
        }

        val list = mutableListOf<String>()
        val items = counterItems.value
        val counts = _counterCounts.value

        items.forEach { item ->
            val active = counts[item.id] ?: CounterActiveInput()
            if (item.isCheque) {
                val amt = active.chequeAmount.toDoubleOrNull() ?: 0.0
                if (amt > 0) {
                    val qty = if (active.count.isEmpty()) 1 else (active.count.toIntOrNull() ?: 1)
                    val rem = active.remarks.ifBlank { "N/A" }.replace(",", ";").replace(":", " ")
                    list.add("CHEQUE~${item.label}:$qty:$amt:$rem")
                }
            } else {
                val qty = active.count.toIntOrNull() ?: 0
                if (qty > 0) {
                    list.add("NOTE~${item.label}:$qty:${item.multiplier}:none")
                }
            }
        }

        val countsStr = list.joinToString(",")

        viewModelScope.launch {
            val session = CashSessionEntity(
                countsString = countsStr,
                totalAmount = total,
                description = description.ifBlank { "Daily Cash Count" },
                timestamp = System.currentTimeMillis()
            )
            repository.insertCashSession(session)
            clearCashCalculator()
            showStatus("Cash session saved to history!")
        }
    }

    fun deleteCashSession(id: Int) {
        viewModelScope.launch {
            repository.deleteCashSession(id)
            showStatus("Session deleted successfully!")
        }
    }

    // --- CHECKOUT & ORDERING FLOWS ---
    fun submitDirectOrder(
        customerName: String,
        customerPhone: String,
        paymentType: String,
        paymentStatus: String,
        onComplete: () -> Unit
    ) {
        val cartItems = _cart.value
        val productList = products.value
        if (cartItems.isEmpty()) {
            showStatus("Your cart is empty!")
            return
        }

        val itemsSummaryList = mutableListOf<String>()
        var calculatedTotal = 0.0

        for ((pId, qty) in cartItems) {
            val p = productList.find { it.id == pId } ?: continue
            val cost = p.rate * qty
            calculatedTotal += cost
            itemsSummaryList.add("${p.name} (${qty} ${p.unit} @ Rs.${p.rate}/- = Rs.${cost.toInt()})")
        }

        val itemsSummary = itemsSummaryList.joinToString(", ")

        viewModelScope.launch {
            val order = OrderEntity(
                customerName = customerName.ifBlank { "Walk-in Customer" },
                customerPhone = customerPhone.ifBlank { "N/A" },
                itemsSummary = itemsSummary,
                totalAmount = calculatedTotal,
                paymentType = paymentType,
                paymentStatus = paymentStatus,
                timestamp = System.currentTimeMillis()
            )
            repository.insertOrder(order)
            clearCart()
            showStatus("Order recorded successfully!")
            onComplete()
        }
    }

    // Forwarding to WhatsApp with beautiful formatted message
    fun shareOrderToWhatsApp(
        context: Context,
        customerName: String,
        customerPhone: String,
        whatsappNum: String, // Merchant's WhatsApp where orders go
        paymentMethod: String,
        paymentStatus: String,
        onComplete: () -> Unit
    ) {
        val cartItems = _cart.value
        val productList = products.value
        if (cartItems.isEmpty()) {
            showStatus("Cart is empty!")
            return
        }

        val df = SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.getDefault())
        val dateStr = df.format(Date())

        val sb = StringBuilder()
        sb.append("🛍️ *NEW ORDER RECEIVED* 🛍️\n")
        sb.append("📅 *Date*: $dateStr\n")
        sb.append("────────────────────\n")
        sb.append("👤 *Customer*: ${customerName.ifBlank { "Valued Customer" }}\n")
        sb.append("📞 *Phone*: ${customerPhone.ifBlank { "N/A" }}\n")
        sb.append("────────────────────\n\n")

        var calculatedTotal = 0.0
        var itemIndex = 1
        for ((pId, qty) in cartItems) {
            val p = productList.find { it.id == pId } ?: continue
            val cost = p.rate * qty
            calculatedTotal += cost
            sb.append("$itemIndex) *${p.name}*\n")
            sb.append("   Qty: $qty ${p.unit} @ Rs. ${p.rate} = *Rs. ${cost.toInt()}*\n")
            itemIndex++
        }
        sb.append("\n────────────────────\n")
        sb.append("💰 *TOTAL BILL*: *Rs. ${calculatedTotal.toInt()}*\n")
        sb.append("💳 *Payment*: $paymentMethod ($paymentStatus)\n")
        sb.append("────────────────────\n")
        sb.append("Thank you for ordering!")

        val completeMessage = sb.toString()
        val cleanPhone = whatsappNum.filter { it.isDigit() }.let {
            if (it.startsWith("0")) "92" + it.drop(1) else it // default to +92 for Pakistan
        }

        try {
            val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=" + URLEncoder.encode(completeMessage, "UTF-8")
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
            }
            context.startActivity(intent)
            // Save order locally too for logs!
            submitDirectOrder(customerName, customerPhone, "WhatsApp", paymentStatus, onComplete)
        } catch (e: Exception) {
            showStatus("WhatsApp is not installed on this device.")
        }
    }

    // --- HTML RECEIPT PRINT MANAGER ---
    fun printCashSessionReceipt(context: Context, session: CashSessionEntity) {
        val df = SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.getDefault())
        val dateString = df.format(Date(session.timestamp))

        val tableRows = StringBuilder()
        if (session.countsString.isNotEmpty()) {
            session.countsString.split(",").forEach { serializedItem ->
                try {
                    val typeParts = serializedItem.split("~")
                    val isCheque = typeParts[0] == "CHEQUE"
                    val parts = typeParts[1].split(":")
                    val label = parts[0]
                    val qty = parts[1].toInt()
                    val rate = parts[2].toDouble()
                    val remarks = if (isCheque) parts[3] else ""

                    val lineTotal = qty * rate
                    val descName = if (isCheque && remarks != "none") "$label ($remarks)" else label

                    tableRows.append("""
                        <tr>
                            <td style="padding: 6px; border-bottom: 1px dashed #ccc; font-family: monospace;">$descName</td>
                            <td align="center" style="padding: 6px; border-bottom: 1px dashed #ccc; font-family: monospace;">$qty</td>
                            <td align="right" style="padding: 6px; border-bottom: 1px dashed #ccc; font-family: monospace;">Rs. ${lineTotal.toInt()}/-</td>
                        </tr>
                    """.trimIndent())
                } catch (e: Exception) {
                    // Fallback parse support for old string format (denom:count) if any
                    val parts = serializedItem.split(":")
                    if (parts.size >= 2) {
                        val denom = parts[0]
                        val qty = parts[1].toInt()
                        val lineTotal = denom.toDouble() * qty
                        tableRows.append("""
                            <tr>
                                <td style="padding: 6px; border-bottom: 1px dashed #ccc; font-family: monospace;">Rs. $denom Notes</td>
                                <td align="center" style="padding: 6px; border-bottom: 1px dashed #ccc; font-family: monospace;">$qty</td>
                                <td align="right" style="padding: 6px; border-bottom: 1px dashed #ccc; font-family: monospace;">Rs. ${lineTotal.toInt()}/-</td>
                            </tr>
                        """.trimIndent())
                    }
                }
            }
        }

        val html = """
            <html>
            <head>
                <style>
                    body { font-family: 'Courier New', Courier, monospace; margin: 15px; color: #333; }
                    .header { text-align: center; margin-bottom: 20px; }
                    .divider { border-top: 2px dashed #000; margin: 12px 0; }
                    .footer { text-align: center; margin-top: 30px; font-size: 11px; }
                    table { width: 100%; border-collapse: collapse; }
                </style>
            </head>
            <body>
                <div class="header">
                    <h2 style="margin: 0; font-size: 20px;">CASH SESSION RECEIPT</h2>
                    <p style="margin: 5px 0 0 0; font-size: 12px;">Rate & Cash Manager</p>
                    <p style="margin: 2px 0; font-size: 11px;">$dateString</p>
                </div>
                <div class="divider"></div>
                <p style="font-size: 13px; margin: 5px 0;"><strong>Session ID:</strong> #${session.id}</p>
                <p style="font-size: 13px; margin: 5px 0;"><strong>Remarks:</strong> ${session.description}</p>
                <div class="divider"></div>
                <table>
                    <thead>
                        <tr style="background-color: #f0f0f0;">
                            <th align="left" style="padding: 6px;">Denomination / Cheque</th>
                            <th align="center" style="padding: 6px;">Qty</th>
                            <th align="right" style="padding: 6px;">Total</th>
                        </tr>
                    </thead>
                    <tbody>
                        $tableRows
                    </tbody>
                </table>
                <div class="divider"></div>
                <div style="font-size: 16px; margin-top: 10px;">
                    <table style="width:100%;">
                        <tr>
                            <td align="left"><strong>GRAND TOTAL:</strong></td>
                            <td align="right"><strong>Rs. ${session.totalAmount.toInt()}/-</strong></td>
                        </tr>
                    </table>
                </div>
                <div class="divider"></div>
                <div class="footer">
                    <p>Cash Verification Complete</p>
                    <p style="font-size: 9px; color:#666;">Generated via Rate & Cash Manager App</p>
                </div>
            </body>
            </html>
        """.trimIndent()

        printHTMLContent(context, "Cash_Session_Receipt_${session.id}", html)
    }

    fun printOrderReceipt(context: Context, order: OrderEntity) {
        val df = SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.getDefault())
        val dateString = df.format(Date(order.timestamp))

        // Breakdown order text
        val itemsList = order.itemsSummary.split(", ")
        val tableRows = StringBuilder()
        itemsList.forEach { item ->
            tableRows.append("""
                <tr>
                    <td style="padding: 6px; border-bottom: 1px dashed #ccc; font-family: monospace; font-size: 12px;">$item</td>
                </tr>
            """.trimIndent())
        }

        val html = """
            <html>
            <head>
                <style>
                    body { font-family: 'Courier New', Courier, monospace; margin: 15px; color: #333; }
                    .header { text-align: center; margin-bottom: 20px; }
                    .divider { border-top: 2px dashed #000; margin: 12px 0; }
                    .footer { text-align: center; margin-top: 30px; font-size: 11px; }
                    table { width: 100%; border-collapse: collapse; }
                </style>
            </head>
            <body>
                <div class="header">
                    <h2 style="margin: 0; font-size: 20px;">MERCHANT SALES RECEIPT</h2>
                    <p style="margin: 5px 0 0 0; font-size: 12px;">Rate & Cash Manager</p>
                    <p style="margin: 2px 0; font-size: 11px;">$dateString</p>
                </div>
                <div class="divider"></div>
                <p style="font-size: 13px; margin: 5px 0;"><strong>Order ID:</strong> #${order.id}</p>
                <p style="font-size: 13px; margin: 5px 0;"><strong>Customer:</strong> ${order.customerName}</p>
                <p style="font-size: 13px; margin: 5px 0;"><strong>Phone:</strong> ${order.customerPhone}</p>
                <div class="divider"></div>
                <table>
                    <thead>
                        <tr style="background-color: #f0f0f0;">
                            <th align="left" style="padding: 6px;">Items & Calculation</th>
                        </tr>
                    </thead>
                    <tbody>
                        $tableRows
                    </tbody>
                </table>
                <div class="divider"></div>
                <table style="width:100%;">
                    <tr>
                        <td align="left"><strong>Payment Type:</strong></td>
                        <td align="right"><strong>${order.paymentType}</strong></td>
                    </tr>
                    <tr>
                        <td align="left"><strong>Status:</strong></td>
                        <td align="right"><strong style="color: #137333;">${order.paymentStatus}</strong></td>
                    </tr>
                    <tr>
                        <td align="left" style="font-size:16px;"><strong>GRAND TOTAL:</strong></td>
                        <td align="right" style="font-size:16px;"><strong style="text-decoration: underline;">Rs. ${order.totalAmount.toInt()}</strong></td>
                    </tr>
                </table>
                <div class="divider"></div>
                <div class="footer">
                    <p>Thank You For Your Business!</p>
                    <p style="font-size: 9px; color:#666;">Rate & Cash Manager App</p>
                </div>
            </body>
            </html>
        """.trimIndent()

        printHTMLContent(context, "Order_Receipt_${order.id}", html)
    }

    private fun printHTMLContent(context: Context, docName: String, htmlContent: String) {
        try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
            val webView = WebView(context)
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    val printAdapter = webView.createPrintDocumentAdapter(docName)
                    printManager.print("Receipt Document", printAdapter, PrintAttributes.Builder().build())
                }
            }
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
        } catch (e: Exception) {
            showStatus("Failed to open print manager: ${e.localizedMessage}")
        }
    }
}

class AppViewModelFactory(private val repository: AppRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
            return AppViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
