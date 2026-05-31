package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.OrderEntity
import com.example.data.ProductEntity
import com.example.ui.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminScreen(viewModel: AppViewModel) {
    val productsList by viewModel.products.collectAsState()
    val ordersList by viewModel.orders.collectAsState()

    var activeManagerTab by remember { mutableStateOf(0) } // 0: Live Rates Editor, 1: Product Manager & Formulas, 2: Client Orders

    Column(modifier = Modifier.fillMaxSize()) {
        // Manager tab row selector
        TabRow(
            selectedTabIndex = activeManagerTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = activeManagerTab == 0,
                onClick = { activeManagerTab = 0 },
                text = { Text("Rates Editor", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            Tab(
                selected = activeManagerTab == 1,
                onClick = { activeManagerTab = 1 },
                text = { Text("Product/Formulas", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            Tab(
                selected = activeManagerTab == 2,
                onClick = { activeManagerTab = 2 },
                text = { Text("Client Orders", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
        }

        when (activeManagerTab) {
            0 -> LiveRatesSubsection(viewModel, productsList)
            1 -> ProductMaintenanceSubsection(viewModel, productsList)
            2 -> ClientOrdersSubsection(viewModel, ordersList)
        }
    }
}

@Composable
fun LiveRatesSubsection(viewModel: AppViewModel, productsList: List<ProductEntity>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentPadding = PaddingValues(bottom = 70.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Edit main rates below. Connected items (e.g. 12kg or sasti peeti) update automatically using parent rules!",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        items(productsList) { product ->
            RateEditRowCard(
                product = product,
                onUpdateRate = { newRate ->
                    viewModel.updateProductRate(product.id, newRate)
                }
            )
        }
    }
}

@Composable
fun RateEditRowCard(product: ProductEntity, onUpdateRate: (Double) -> Unit) {
    var isEditing by remember { mutableStateOf(false) }
    var rateInput by remember { mutableStateOf(product.rate.toInt().toString()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1.3f)) {
                Text(product.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                    Text(
                        "Per ${product.unit}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    if (product.isDependent) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Formula Live (AUTO)",
                            fontSize = 9.sp,
                            color = Color(0xFF137333),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(Color(0xFFE6F4EA), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (isEditing) {
                Row(
                    modifier = Modifier.weight(2f),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = rateInput,
                        onValueChange = { rateInput = it.filter { c -> c.isDigit() || c == '.' } },
                        modifier = Modifier
                            .width(85.dp)
                            .height(50.dp)
                            .testTag("rate_edit_input_${product.id}"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = {
                            val doubleRate = rateInput.toDoubleOrNull()
                            if (doubleRate != null && doubleRate >= 0) {
                                onUpdateRate(doubleRate)
                                isEditing = false
                            }
                        }
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save rate", tint = MaterialTheme.colorScheme.primary)
                    }

                    IconButton(onClick = { isEditing = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel rate editing", tint = MaterialTheme.colorScheme.error)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Rs. ${product.rate.toInt()}",
                        fontWeight = FontWeight.ExtraBold,
                        color = if (product.isDependent) Color(0xFF137333) else MaterialTheme.colorScheme.primary,
                        fontSize = 16.sp
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            rateInput = product.rate.toInt().toString()
                            isEditing = true
                        },
                        modifier = Modifier.testTag("edit_rate_trigger_${product.id}")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit price", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductMaintenanceSubsection(viewModel: AppViewModel, productsList: List<ProductEntity>) {
    var newProdName by remember { mutableStateOf("") }
    var newProdRate by remember { mutableStateOf("") }
    var newProdUnit by remember { mutableStateOf("kg") }

    var showFormulaSetupDialog by remember { mutableStateOf(false) }

    // Dialog Input state
    var selectedDependentId by remember { mutableStateOf(-1) }
    var selectedParentId by remember { mutableStateOf(-1) }
    var selectedFormulaType by remember { mutableStateOf("DIFFERENCE") } // "DIFFERENCE" or "PROPORTION"
    
    var offsetValueStr by remember { mutableStateOf("") } // depValue
    var divFactorStr by remember { mutableStateOf("16.0") } // depParentFactor
    var multFactorStr by remember { mutableStateOf("12.0") } // depOwnFactor

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentPadding = PaddingValues(bottom = 70.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- 🔗 CONNECTED RATE FORMULAS SECTION ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Connected Rate Formulas (Owner)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Button(
                            onClick = { showFormulaSetupDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Formula", fontSize = 11.sp)
                        }
                    }

                    Text(
                        "Setup automatic formulas: e.g. 16kg Peeti is always Rs. 100 sasti than 16kg Teen; or 12kg rates calculated by dividing 16kg by 16 and multiplying by 12.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    // Display active formulas list
                    val dependentProducts = productsList.filter { it.isDependent && it.parentProductId > 0 }
                    if (dependentProducts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No formula relations active. Click 'New Formula' to setup.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            dependentProducts.forEach { item ->
                                val parent = productsList.find { it.id == item.parentProductId }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Target: ${item.name}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            "Connected Parent: ${parent?.name ?: "Deleted Product"}",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                        val ruleDesc = if (item.depType == "DIFFERENCE") {
                                            if (item.depValue >= 0) "Parent rate + Rs. ${item.depValue.toInt()}" else "Parent rate - Rs. ${if (item.depValue < 0) -item.depValue.toInt() else item.depValue.toInt()} (SASTI)"
                                        } else {
                                            "Proportion: (Parent / ${item.depParentFactor}) * ${item.depOwnFactor}"
                                        }
                                        Text(
                                            "Formula: $ruleDesc",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = Color(0xFF137333)
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.removeProductConnection(item.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.LinkOff, contentDescription = "Remove relation", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Form to add a new product
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Add New Standalone Product",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = newProdName,
                        onValueChange = { newProdName = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_product_name"),
                        label = { Text("Product Description / Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newProdRate,
                            onValueChange = { newProdRate = it.filter { c -> c.isDigit() || c == '.' } },
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("new_product_rate"),
                            label = { Text("Rate Rs.") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )

                        OutlinedTextField(
                            value = newProdUnit,
                            onValueChange = { newProdUnit = it },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("new_product_unit"),
                            label = { Text("Unit (eg Can, kg, L)") },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val rateVal = newProdRate.toDoubleOrNull()
                            if (newProdName.isBlank()) {
                                viewModel.showStatus("Name cannot be blank!")
                                return@Button
                            }
                            if (rateVal == null || rateVal <= 0) {
                                viewModel.showStatus("Please enter a valid rate!")
                                return@Button
                            }
                            viewModel.addNewProduct(newProdName, rateVal, newProdUnit.ifBlank { "pcs" })
                            newProdName = ""
                            newProdRate = ""
                            newProdUnit = "kg"
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_new_product_action"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save New Product Item")
                    }
                }
            }
        }

        item {
            Text(
                text = "Remove Current Products (${productsList.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // List of products with direct deletes
        items(productsList) { p ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(p.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            if (p.isDependent) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Formula",
                                    fontSize = 8.sp,
                                    color = Color(0xFF137333),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .background(Color(0xFFE6F4EA), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text("Per ${p.unit} - Rs. ${p.rate.toInt()}/-", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                    }

                    IconButton(onClick = { viewModel.deleteProduct(p.id) }) {
                        Icon(Icons.Default.DeleteForever, contentDescription = "Delete product", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    // Owner Connection Setup Dialogue
    if (showFormulaSetupDialog) {
        val nonDependent = productsList.filter { it.id != selectedParentId }
        val parentSelections = productsList.filter { it.id != selectedDependentId }

        AlertDialog(
            onDismissRequest = { showFormulaSetupDialog = false },
            title = { Text("🔗 Connection Formula Setup", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Design dynamic cascading price rates for any product.", fontSize = 12.sp)

                    // 1. Selector for Dependent Product
                    Text("Select Target Product (Gets auto-updated):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    var targetExpanded by remember { mutableStateOf(false) }
                    val targetProd = productsList.find { it.id == selectedDependentId }
                    Box {
                        OutlinedButton(
                            onClick = { targetExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(targetProd?.name ?: "Click to Choose Product ➜")
                        }
                        DropdownMenu(expanded = targetExpanded, onDismissRequest = { targetExpanded = false }) {
                            nonDependent.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(item.name) },
                                    onClick = {
                                        selectedDependentId = item.id
                                        targetExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // 2. Selector for Parent Product
                    Text("Select Driver Main Product (Base rate source):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    var parentExpanded by remember { mutableStateOf(false) }
                    val parentProd = productsList.find { it.id == selectedParentId }
                    Box {
                        OutlinedButton(
                            onClick = { parentExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(parentProd?.name ?: "Click to Choose Parent ➜")
                        }
                        DropdownMenu(expanded = parentExpanded, onDismissRequest = { parentExpanded = false }) {
                            parentSelections.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(item.name) },
                                    onClick = {
                                        selectedParentId = item.id
                                        parentExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // 3. Select Formula Type
                    Text("Formula Type Logic:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedFormulaType == "DIFFERENCE",
                            onClick = { selectedFormulaType = "DIFFERENCE" },
                            label = { Text("Offset / sasti difference") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedFormulaType == "PROPORTION",
                            onClick = { selectedFormulaType = "PROPORTION" },
                            label = { Text("Proportional fraction (12/16)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // 4. Input criteria fields
                    if (selectedFormulaType == "DIFFERENCE") {
                        OutlinedTextField(
                            value = offsetValueStr,
                            onValueChange = { offsetValueStr = it },
                            label = { Text("Offset value (e.g. -100 for 100 sasti)") },
                            placeholder = { Text("e.g. -100") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = divFactorStr,
                                onValueChange = { divFactorStr = it },
                                label = { Text("Parent Weight factor") },
                                placeholder = { Text("16") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            OutlinedTextField(
                                value = multFactorStr,
                                onValueChange = { multFactorStr = it },
                                label = { Text("Target Weight factor") },
                                placeholder = { Text("12") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedDependentId <= 0 || selectedParentId <= 0) {
                            viewModel.showStatus("Select both target and parent products!")
                            return@Button
                        }
                        if (selectedDependentId == selectedParentId) {
                            viewModel.showStatus("Target and parent cannot be same product!")
                            return@Button
                        }

                        val offsetVal = offsetValueStr.toDoubleOrNull() ?: 0.0
                        val divVal = divFactorStr.toDoubleOrNull() ?: 16.0
                        val multVal = multFactorStr.toDoubleOrNull() ?: 12.0

                        viewModel.saveProductConnection(
                            productId = selectedDependentId,
                            parentProductId = selectedParentId,
                            depType = selectedFormulaType,
                            depValue = offsetVal,
                            depParentFactor = divVal,
                            depOwnFactor = multVal
                        )

                        // Clear setup
                        selectedDependentId = -1
                        selectedParentId = -1
                        offsetValueStr = ""
                        divFactorStr = "16.0"
                        multFactorStr = "12.0"
                        showFormulaSetupDialog = false
                    }
                ) {
                    Text("Apply Rule")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        selectedDependentId = -1
                        selectedParentId = -1
                        showFormulaSetupDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

private val Double.absoluteValue: Double
    get() = if (this < 0) -this else this

@Composable
fun ClientOrdersSubsection(viewModel: AppViewModel, ordersList: List<OrderEntity>) {
    val context = LocalContext.current

    if (ordersList.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No direct client orders placed yet.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentPadding = PaddingValues(bottom = 70.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(ordersList) { order ->
                OrderHistoryRow(
                    order = order,
                    onPrintReceipt = { viewModel.printOrderReceipt(context, order) }
                )
            }
        }
    }
}

@Composable
fun OrderHistoryRow(order: OrderEntity, onPrintReceipt: () -> Unit) {
    val df = SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.getDefault())
    val formattedDate = df.format(Date(order.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_client_order_${order.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(order.customerName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Phone: ${order.customerPhone}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Text(formattedDate, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Rs. ${order.totalAmount.toInt()}/-", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, fontSize = 15.sp)
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .background(
                                color = if (order.paymentStatus == "Paid") Color(0xFFE6F4EA) else Color(0xFFFCE8E6),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = order.paymentStatus,
                            color = if (order.paymentStatus == "Paid") Color(0xFF137333) else Color(0xFFC5221F),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body summary of items
            Text(
                text = "Items Ordered:",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = order.itemsSummary,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )

            DivideDash()

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Method: ${order.paymentType}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                FilledTonalButton(
                    onClick = onPrintReceipt,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Print Receipt", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun DivideDash() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .height(1.dp)
            .background(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
    )
}
