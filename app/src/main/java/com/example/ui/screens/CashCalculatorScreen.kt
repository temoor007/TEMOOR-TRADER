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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CashSessionEntity
import com.example.data.CounterItemEntity
import com.example.ui.viewmodel.AppViewModel
import com.example.ui.viewmodel.CounterActiveInput
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CashCalculatorScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val counterItems by viewModel.counterItems.collectAsState()
    val counterCounts by viewModel.counterCounts.collectAsState()
    val totalCashCalculated by viewModel.totalCashCalculated.collectAsState()
    val cashSessions by viewModel.cashSessions.collectAsState()

    var remarksText by remember { mutableStateOf("") }
    var expandedHistory by remember { mutableStateOf(false) }
    var showAddCustomDialog by remember { mutableStateOf(false) }

    // Custom add dialog state variables
    var customLabel by remember { mutableStateOf("") }
    var customValueStr by remember { mutableStateOf("") }
    var customIsCheque by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 16.dp),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Distinct Screen Banner Header
        item {
            Box(
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
                    .padding(vertical = 20.dp, horizontal = 16.dp)
            ) {
                Column {
                    Text(
                        text = "Cash Count Calculator",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Flexible Shop drawer counter with cheques & custom row items",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Live Grand Total display
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "TOTAL DRAWER CALCULATION",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Rs. ${totalCashCalculated.toInt()}/-",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("calculated_cash_total")
                    )
                    Text(
                        text = "Auto-calculated based on entered PKR notes & cheques below",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }

        // Note Count Form Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cash & Cheque Balances",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )

                // Trigger to add dynamic item
                TextButton(
                    onClick = { showAddCustomDialog = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Row Item", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // List of all items (banknotes, cheques, custom ones)
        items(counterItems) { item ->
            val activeState = counterCounts[item.id] ?: CounterActiveInput()

            if (item.isCheque) {
                // Unique layout for cheque inputs
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = item.label,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                if (item.isCustom) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Custom",
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            if (item.isCustom) {
                                IconButton(
                                    onClick = { viewModel.deleteCounterItem(item.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete item",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Cheque Amount
                            OutlinedTextField(
                                value = activeState.chequeAmount,
                                onValueChange = { viewModel.updateChequeAmount(item.id, it) },
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(52.dp),
                                label = { Text("Amount (Rs.)", fontSize = 11.sp) },
                                placeholder = { Text("0", fontSize = 12.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            )

                            // Cheque Details/Remarks
                            OutlinedTextField(
                                value = activeState.remarks,
                                onValueChange = { viewModel.updateChequeRemarks(item.id, it) },
                                modifier = Modifier
                                    .weight(2f)
                                    .height(52.dp),
                                label = { Text("Bank / Cheque No.", fontSize = 11.sp) },
                                placeholder = { Text("e.g. HBL #92837", fontSize = 12.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                            )

                            // Optional Qty multiplier
                            OutlinedTextField(
                                value = activeState.count,
                                onValueChange = { viewModel.updateCounterCount(item.id, it) },
                                modifier = Modifier
                                    .weight(0.8f)
                                    .height(52.dp),
                                label = { Text("Qty", fontSize = 11.sp) },
                                placeholder = { Text("1", fontSize = 12.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, textAlign = TextAlign.Center)
                            )
                        }
                    }
                }
            } else {
                // Banknote row
                val liveMultiplierResult = (activeState.count.toIntOrNull() ?: 0) * item.multiplier.toInt()

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left Label
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.width(120.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                tint = if (liveMultiplierResult > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = item.label,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    if (item.isCustom) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "C",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 3.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "multiplier: ${item.multiplier.toInt()}",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }

                        // Middle Input Controllers
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.adjustCounterCountBy(item.id, -1) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrement", modifier = Modifier.size(18.dp))
                            }

                            OutlinedTextField(
                                value = activeState.count,
                                onValueChange = { viewModel.updateCounterCount(item.id, it) },
                                modifier = Modifier
                                    .width(64.dp)
                                    .height(48.dp)
                                    .testTag("counter_input_${item.id}"),
                                placeholder = { Text("0", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), fontSize = 12.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                ),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                )
                            )

                            IconButton(
                                onClick = { viewModel.adjustCounterCountBy(item.id, 1) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increment", modifier = Modifier.size(18.dp))
                            }
                        }

                        // Right line results
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.width(90.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Rs. $liveMultiplierResult",
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (liveMultiplierResult > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    fontSize = 13.sp
                                )
                            }
                            
                            if (item.isCustom) {
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { viewModel.deleteCounterItem(item.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete item",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Remarks Input
        item {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)) {
                OutlinedTextField(
                    value = remarksText,
                    onValueChange = { remarksText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("session_remarks_input"),
                    label = { Text("Counter Session Notes / Remarks") },
                    placeholder = { Text("e.g. Counter closing balance, Evening drawer verification") },
                    maxLines = 2,
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        // Actions Footer
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.clearCashCalculator()
                        remarksText = ""
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear")
                }

                Button(
                    onClick = {
                        viewModel.saveCashSession(remarksText)
                        remarksText = ""
                    },
                    modifier = Modifier
                        .weight(2.5f)
                        .testTag("save_cash_session_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save & Log Balance")
                }
            }
        }

        // Expanded History Drawer Toggle
        item {
            Divider(modifier = Modifier.padding(top = 16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandedHistory = !expandedHistory }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Historical Cash Logs (${cashSessions.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Icon(
                    imageVector = if (expandedHistory) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Historical sessions
        if (expandedHistory) {
            if (cashSessions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No saved cash entries in repository history.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                items(cashSessions) { session ->
                    AdvancedCashSessionItem(
                        session = session,
                        onPrint = { viewModel.printCashSessionReceipt(context, session) },
                        onDelete = { viewModel.deleteCashSession(session.id) }
                    )
                }
            }
        }
    }

    // Dynamic Row Item Creator Dialog
    if (showAddCustomDialog) {
        AlertDialog(
            onDismissRequest = { showAddCustomDialog = false },
            title = { Text("➕ Add Row Item", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Add a custom denomination, coin, or dynamic bank cheque slot to your calculator layout.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    OutlinedTextField(
                        value = customLabel,
                        onValueChange = { customLabel = it },
                        label = { Text("Name / Label") },
                        placeholder = { Text("e.g. 20 PKR Note, HBL Cheque") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = customIsCheque,
                            onCheckedChange = { customIsCheque = it }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text("Is bank cheque row?", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Adds inputs for exact cheque values and bank names on the fly.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    }

                    if (!customIsCheque) {
                        OutlinedTextField(
                            value = customValueStr,
                            onValueChange = { customValueStr = it },
                            label = { Text("Denomination multiplier") },
                            placeholder = { Text("e.g. 20") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalMult = if (customIsCheque) 1.0 else (customValueStr.toDoubleOrNull() ?: 1.0)
                        if (customLabel.isNotBlank()) {
                            viewModel.addCustomCounterItem(customLabel, finalMult, customIsCheque)
                            customLabel = ""
                            customValueStr = ""
                            customIsCheque = false
                            showAddCustomDialog = false
                        }
                    }
                ) {
                    Text("Add Row")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AdvancedCashSessionItem(
    session: CashSessionEntity,
    onPrint: () -> Unit,
    onDelete: () -> Unit
) {
    val df = SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.getDefault())
    val formattedDate = df.format(Date(session.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("cash_session_item_${session.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = session.description,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formattedDate,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                Text(
                    text = "Rs. ${session.totalAmount.toInt()}/-",
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Parse counts String properly
            val linesStringList = mutableListOf<String>()
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

                        val cleanLabel = if (isCheque && remarks != "none" && remarks != "N/A") "$label($remarks)" else label
                        linesStringList.add("${qty}x $cleanLabel @ Rs.${rate.toInt()}")
                    } catch (e: Exception) {
                        val parts = serializedItem.split(":")
                        if (parts.size >= 2) {
                            linesStringList.add("${parts[1]}x Rs.${parts[0]}")
                        }
                    }
                }
            }

            Text(
                text = "Breakdown:\n" + linesStringList.joinToString("\n"),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                lineHeight = 15.sp
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .height(1.dp)
                    .background(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete log entry",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                FilledIconButton(
                    onClick = onPrint,
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Print,
                        contentDescription = "Print cash bill",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
