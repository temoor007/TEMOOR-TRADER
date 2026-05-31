package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.CashCalculatorScreen
import com.example.ui.screens.ProductsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppViewModel
import com.example.ui.viewmodel.AppViewModelFactory

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Initialize database & repository references
    val database = AppDatabase.getDatabase(applicationContext)
    val repository = AppRepository(database.dao)

    setContent {
      MyApplicationTheme {
        val viewModel: AppViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
          factory = AppViewModelFactory(repository)
        )

        MainAppLayout(viewModel)
      }
    }
  }
}

@Composable
fun MainAppLayout(viewModel: AppViewModel) {
  val currentTab by viewModel.currentTab.collectAsState()
  val statusMessage by viewModel.statusMessage.collectAsState()

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    bottomBar = {
      NavigationBar(
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
      ) {
        NavigationBarItem(
          selected = currentTab == 0,
          onClick = { viewModel.selectTab(0) },
          label = { Text("Display Rates", style = MaterialTheme.typography.labelSmall) },
          icon = { Icon(Icons.Default.TrendingUp, contentDescription = "Products Rate Display") },
          modifier = Modifier.testTag("nav_tab_rates")
        )
        NavigationBarItem(
          selected = currentTab == 1,
          onClick = { viewModel.selectTab(1) },
          label = { Text("Cash Counter", style = MaterialTheme.typography.labelSmall) },
          icon = { Icon(Icons.Default.Calculate, contentDescription = "Cash Count Screen") },
          modifier = Modifier.testTag("nav_tab_cash")
        )
        NavigationBarItem(
          selected = currentTab == 2,
          onClick = { viewModel.selectTab(2) },
          label = { Text("Manager Hub", style = MaterialTheme.typography.labelSmall) },
          icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin Screen") },
          modifier = Modifier.testTag("nav_tab_admin")
        )
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      // Swappable Main Sections
      when (currentTab) {
        0 -> ProductsScreen(viewModel)
        1 -> CashCalculatorScreen(viewModel)
        2 -> AdminScreen(viewModel)
      }

      // Live Dynamic Banner Message (eg "Rate updated successfully!")
      statusMessage?.let { msg ->
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.TopCenter)
            .padding(16.dp),
          contentAlignment = Alignment.TopCenter
        ) {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.testTag("ui_status_banner")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.Info, contentDescription = null, tint = Color.White)
              Spacer(modifier = Modifier.width(10.dp))
              Text(text = msg, color = Color.White, fontSize = 14.sp)
            }
          }
        }
      }
    }
  }
}
