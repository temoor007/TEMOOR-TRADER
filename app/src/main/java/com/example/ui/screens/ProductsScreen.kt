package com.example.ui.screens

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ProductEntity
import com.example.ui.viewmodel.AppViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredProducts by viewModel.filteredProducts.collectAsState()
    val cart by viewModel.cart.collectAsState()
    val productsList by viewModel.products.collectAsState()
    val priceHistory by viewModel.priceHistoryToObserve.collectAsState()

    var showCartDialog by remember { mutableStateOf(false) }
    var selectedHistoryProduct by remember { mutableStateOf<ProductEntity?>(null) }

    val cartTotalCount = cart.values.sum()
    val cartTotalPrice = cart.entries.sumOf { (pId, qty) ->
        val prod = productsList.find { it.id == pId }
        (prod?.rate ?: 0.0) * qty
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp) // space for FAB/Actions
        ) {
            // Screen Header in Green Theme
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                Color(0xFF137333)
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Live Rates Display",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${filteredProducts.size} Products available | Click 🕒 to see Old Rates",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
                
                // Live Indicator Dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00FF66))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("LIVE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("product_search_input"),
                placeholder = { Text("Search product by name...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                ),
                shape = RoundedCornerShape(12.dp)
            )

            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Empty icon",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No products found matching \"$searchQuery\"",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredProducts) { product ->
                        ProductGridItem(
                            product = product,
                            qtyInCart = cart[product.id] ?: 0,
                            onAdd = { viewModel.addToCart(product.id) },
                            onSub = { viewModel.removeFromCart(product.id) },
                            onViewHistory = { selectedHistoryProduct = product }
                        )
                    }
                }
            }
        }

        // Floating Action Button for Cart
        if (cartTotalCount > 0) {
            FilledTonalButton(
                onClick = { showCartDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 16.dp, end = 16.dp)
                    .height(56.dp)
                    .testTag("floating_cart_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ),
                elevation = ButtonDefaults.filledTonalButtonElevation(defaultElevation = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = "Cart Icon"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$cartTotalCount Items (Rs. ${cartTotalPrice.toInt()})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }

    // Cart and Checkout Dialogue
    if (showCartDialog) {
        Dialog(
            onDismissRequest = { showCartDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 24.dp)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                color = MaterialTheme.colorScheme.background
            ) {
                CheckoutFlow(
                    viewModel = viewModel,
                    cartTotalPrice = cartTotalPrice,
                    onDismiss = { showCartDialog = false }
                )
            }
        }
    }

    // Historical rates dialogue
    selectedHistoryProduct?.let { prod ->
        val historyForProd = priceHistory.filter { it.productId == prod.id }
        AlertDialog(
            onDismissRequest = { selectedHistoryProduct = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sabqa Rate Diary", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = prod.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Text(
                        text = "Current Rate: Rs. ${prod.rate.toInt()} / ${prod.unit}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Divider(modifier = Modifier.padding(bottom = 12.dp))

                    if (historyForProd.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Is item ka koi sabqa rate save nahi hai.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .heightIn(max = 240.dp)
                                .fillMaxWidth()
                        ) {
                            items(historyForProd) { log ->
                                val df = SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.getDefault())
                                val timeStr = df.format(Date(log.timestamp))

                                val diff = log.newRate - log.oldRate
                                val isDrop = diff < 0

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "Rs. ${log.oldRate.toInt()}",
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                                                    ),
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                                    fontSize = 13.sp
                                                )
                                                
                                                Text(
                                                    text = "  ➔  ",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                                )

                                                Text(
                                                    text = "Rs. ${log.newRate.toInt()}",
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isDrop) Color(0xFFC5221F) else Color(0xFF137333),
                                                    fontSize = 14.sp
                                                )
                                            }

                                            Text(
                                                text = if (isDrop) "Rs. ${diff.absoluteValue.toInt()} Sasta" else "Rs. ${diff.toInt()} Mehnga",
                                                color = if (isDrop) Color(0xFF137333) else Color(0xFFC5221F),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                modifier = Modifier
                                                    .background(
                                                        if (isDrop) Color(0xFFE6F4EA) else Color(0xFFFCE8E6),
                                                        shape = RoundedCornerShape(4.dp)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))
                                        
                                        Text(
                                            text = "Updated: $timeStr",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedHistoryProduct = null }) {
                    Text("Deak Liya", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun ProductGridItem(
    product: ProductEntity,
    qtyInCart: Int,
    onAdd: () -> Unit,
    onSub: () -> Unit,
    onViewHistory: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("product_item_${product.id}"),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (qtyInCart > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (qtyInCart > 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Main title + history diary clicker
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = product.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onViewHistory,
                    modifier = Modifier
                        .size(24.dp)
                        .testTag("rate_diary_trigger_${product.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "View Price History",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Unit/Pack display
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Per ${product.unit}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }

                if (product.isDependent) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Auto",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF137333),
                        modifier = Modifier
                            .background(Color(0xFFE6F4EA), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Price rates
            Text(
                text = "Rs. ${product.rate.toInt()}",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Cart Adjustments
            if (qtyInCart > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onSub,
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Minus",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = qtyInCart.toString(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    IconButton(
                        onClick = onAdd,
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Plus",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else {
                Button(
                    onClick = onAdd,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .testTag("add_to_cart_${product.id}"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add To Order", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutFlow(
    viewModel: AppViewModel,
    cartTotalPrice: Double,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val cart by viewModel.cart.collectAsState()
    val productsList by viewModel.products.collectAsState()

    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var orderTypeWhatsApp by remember { mutableStateOf(true) } // true: WhatsApp message, false: Direct Save
    var directWhatsAppNum by remember { mutableStateOf("03001234567") } // Customizable merchant contact

    // Payment modes: Cash, EasyPaisa, JazzCash, Card
    var selectedPaymentMode by remember { mutableStateOf("Cash") }

    // Digital Payment Gate State Simulations
    var isProcessingPayment by remember { mutableStateOf(false) }
    var isPaymentSuccess by remember { mutableStateOf(false) }
    var paymentGateMessage by remember { mutableStateOf("") }

    // Card Simulation inputs
    var cardNumber by remember { mutableStateOf("") }
    var cardExpiry by remember { mutableStateOf("") }
    var cardCvv by remember { mutableStateOf("") }

    // Digital Wallet details
    var walletPhone by remember { mutableStateOf("") }
    var otpSubmitted by remember { mutableStateOf(false) }
    var otpValue by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Order Details",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close dialog")
            }
        }

        Divider(modifier = Modifier.padding(vertical = 12.dp))

        if (cart.isEmpty() && !isPaymentSuccess) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("Your shopping cart is currently empty.")
            }
        } else if (isPaymentSuccess) {
            // Payment success confirmation UI
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    tint = Color(0xFF137333),
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Payment Captured Digitally!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF137333)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Amount: Rs. ${cartTotalPrice.toInt()}/-",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Your digital order is ready. It will now be saved locally.",
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        if (orderTypeWhatsApp) {
                            viewModel.shareOrderToWhatsApp(
                                context = context,
                                customerName = customerName,
                                customerPhone = customerPhone,
                                whatsappNum = directWhatsAppNum,
                                paymentMethod = selectedPaymentMode,
                                paymentStatus = "Paid",
                                onComplete = onDismiss
                            )
                        } else {
                            viewModel.submitDirectOrder(
                                customerName = customerName,
                                customerPhone = customerPhone,
                                paymentType = selectedPaymentMode,
                                paymentStatus = "Paid",
                                onComplete = onDismiss
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(if (orderTypeWhatsApp) "Proceed to WhatsApp Receipt" else "Save & Finish Receipt")
                }
            }
        } else if (isProcessingPayment) {
            // Gateway animation
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(54.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = paymentGateMessage.ifBlank { "Talking to Payments Gateway..." },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Please do not close this window.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        } else {
            // Active interactive form
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = "Products Selected",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                items(cart.entries.toList()) { (pId, qty) ->
                    val prod = productsList.find { it.id == pId } ?: return@items
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(prod.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                "Qty: $qty ${prod.unit} @ Rs. ${prod.rate.toInt()}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Text(
                            "Rs. ${(prod.rate * qty).toInt()}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Customer information
                item {
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = "Customer Info",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.size(4.dp))

                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("checkout_name"),
                        label = { Text("Customer Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = customerPhone,
                        onValueChange = { customerPhone = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("checkout_phone"),
                        label = { Text("Customer Phone Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // WhatsApp redirection controls
                item {
                    Spacer(modifier = Modifier.size(4.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = orderTypeWhatsApp,
                                    onCheckedChange = { orderTypeWhatsApp = it },
                                    modifier = Modifier.testTag("whatsapp_checkbox")
                                )
                                Column(modifier = Modifier.clickable { orderTypeWhatsApp = !orderTypeWhatsApp }) {
                                    Text("Send Bill Draft to WhatsApp", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(
                                        "Automated forwarding with product list, live rates, total.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }

                            if (orderTypeWhatsApp) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = directWhatsAppNum,
                                    onValueChange = { directWhatsAppNum = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Receive WhatsApp Number (Merchant)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }

                // Digital simulation gateway
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Digital Payment Gateway Options",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Cash", "EasyPaisa", "JazzCash", "Card").forEach { mode ->
                            val isSelected = selectedPaymentMode == mode
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedPaymentMode = mode },
                                label = { Text(mode, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("payment_chip_$mode"),
                                elevation = FilterChipDefaults.filterChipElevation(3.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                // Credit Card forms
                if (selectedPaymentMode == "Card") {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Secured PCI-DSS Card checkout", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = cardNumber,
                                    onValueChange = {
                                        cardNumber = it.filter { c -> c.isDigit() }.take(16)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("16-Digit Credit Card Number") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = cardExpiry,
                                        onValueChange = { cardExpiry = it.take(5) },
                                        modifier = Modifier.weight(1f),
                                        label = { Text("Expiry (MM/YY)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    OutlinedTextField(
                                        value = cardCvv,
                                        onValueChange = { cardCvv = it.filter { c -> c.isDigit() }.take(3) },
                                        modifier = Modifier.weight(1f),
                                        label = { Text("CVV") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }
                    }
                } else if (selectedPaymentMode == "EasyPaisa" || selectedPaymentMode == "JazzCash") {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.PhoneAndroid,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "$selectedPaymentMode Instant Wallet Pay",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = walletPhone,
                                    onValueChange = { walletPhone = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Registered $selectedPaymentMode Wallet Number") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    placeholder = { Text("e.g. 03xxxxxxxxx") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp)
                                )

                                if (otpSubmitted) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        "SMS OTP code sent! Check your phone & enter below:",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedTextField(
                                        value = otpValue,
                                        onValueChange = { otpValue = it.filter { c -> c.isDigit() }.take(4) },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("4-Digit OTP Code") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Grand Bill Total:", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "Rs. ${cartTotalPrice.toInt()}/-",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // CTA payment completion
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        val isDigital = selectedPaymentMode != "Cash"
                        if (isDigital) {
                            if (selectedPaymentMode == "Card") {
                                if (cardNumber.length < 16 || cardExpiry.isEmpty() || cardCvv.length < 3) {
                                    viewModel.showStatus("Please enter valid Credit Card details!")
                                    return@Button
                                }
                            } else {
                                if (walletPhone.length < 10) {
                                    viewModel.showStatus("Please enter a valid wallet active phone number!")
                                    return@Button
                                }
                                if (!otpSubmitted) {
                                    otpSubmitted = true
                                    viewModel.showStatus("OTP Dispatched to $walletPhone!")
                                    return@Button
                                } else if (otpValue.length < 4) {
                                    viewModel.showStatus("Please input the 4-digit numeric verification code!")
                                    return@Button
                                }
                            }

                            isProcessingPayment = true
                            paymentGateMessage = "Validating digits with bank API gateway securely..."
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(1200)
                                paymentGateMessage = "Completing settlement of Rs. ${cartTotalPrice.toInt()} with $selectedPaymentMode network..."
                                kotlinx.coroutines.delay(1000)
                                isProcessingPayment = false
                                isPaymentSuccess = true
                            }
                        } else {
                            if (orderTypeWhatsApp) {
                                viewModel.shareOrderToWhatsApp(
                                    context = context,
                                    customerName = customerName,
                                    customerPhone = customerPhone,
                                    whatsappNum = directWhatsAppNum,
                                    paymentMethod = "Cash On Delivery",
                                    paymentStatus = "Pending",
                                    onComplete = onDismiss
                                )
                            } else {
                                viewModel.submitDirectOrder(
                                    customerName = customerName,
                                    customerPhone = customerPhone,
                                    paymentType = "Direct Cash",
                                    paymentStatus = "Pending",
                                    onComplete = onDismiss
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(2.9f)
                        .testTag("submit_checkout_action"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    val isDigitalToken = selectedPaymentMode != "Cash"
                    if (isDigitalToken) {
                        if (otpSubmitted && selectedPaymentMode != "Card") {
                            Text("Verify OTP & Complete Digital Pay")
                        } else {
                            Text("Authorize Digital Pay (Rs. ${cartTotalPrice.toInt()})")
                        }
                    } else {
                        if (orderTypeWhatsApp) {
                            Text("Forward Order via WhatsApp")
                        } else {
                            Text("Confirm & Log Direct Order")
                        }
                    }
                }
            }
        }
    }
}
