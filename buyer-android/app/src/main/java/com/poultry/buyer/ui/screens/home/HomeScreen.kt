package com.poultry.buyer.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Poultry") },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Search bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search products...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.large
                )
            }

            // Categories
            item {
                Column {
                    Text(
                        text = "Categories",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(categories) { category ->
                            CategoryCard(category)
                        }
                    }
                }
            }

            // Featured Products
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Featured Products",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = {}) {
                            Text("See All")
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ProductCard(modifier = Modifier.weight(1f))
                        ProductCard(modifier = Modifier.weight(1f))
                    }
                }
            }

            // Recent Orders
            item {
                Column {
                    Text(
                        text = "Recent Orders",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    RecentOrderCard(orderNumber = "ORD-2025-001", status = "Delivered", amount = "4,500")
                    Spacer(modifier = Modifier.height(8.dp))
                    RecentOrderCard(orderNumber = "ORD-2025-002", status = "In Transit", amount = "2,800")
                }
            }
        }
    }
}

data class CategoryItem(val name: String, val icon: @Composable () -> Unit)

val categories = listOf(
    CategoryItem("Chicken") { Icon(Icons.Default.SetMeal, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
    CategoryItem("Eggs") { Icon(Icons.Default.Egg, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
    CategoryItem("Duck") { Icon(Icons.Default.SetMeal, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
    CategoryItem("Feed") { Icon(Icons.Default.Grass, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
)

@Composable
fun CategoryCard(category: CategoryItem) {
    Card(
        onClick = {},
        modifier = Modifier.width(80.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            category.icon()
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun ProductCard(modifier: Modifier = Modifier) {
    Card(
        onClick = {},
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Image, contentDescription = null)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Fresh Chicken",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "280/kg",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                Text("Add to Cart", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun RecentOrderCard(orderNumber: String, status: String, amount: String) {
    Card(
        onClick = {},
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = orderNumber, fontWeight = FontWeight.Medium)
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "$amount", fontWeight = FontWeight.Bold)
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
