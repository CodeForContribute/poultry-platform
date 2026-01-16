package com.poultry.buyer.ui.screens.checkout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.poultry.buyer.ui.components.PriceDisplay
import com.poultry.buyer.ui.components.PriceDisplayLarge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    onBack: () -> Unit,
    onOrderPlaced: (String) -> Unit,
    viewModel: CheckoutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    // Handle order placed
    LaunchedEffect(uiState.placedOrder) {
        uiState.placedOrder?.let { order ->
            onOrderPlaced(order.id)
        }
    }

    // Handle errors
    LaunchedEffect(uiState.orderError) {
        uiState.orderError?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearOrderError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Checkout") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            uiState.cart?.let { cart ->
                Surface(shadowElevation = 8.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Total",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            PriceDisplayLarge(amount = cart.totalAmount)
                        }

                        Button(
                            onClick = viewModel::placeOrder,
                            enabled = !uiState.isCreatingOrder && !uiState.isPlacingOrder,
                            modifier = Modifier.height(48.dp)
                        ) {
                            if (uiState.isCreatingOrder || uiState.isPlacingOrder) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (uiState.isCreatingOrder) "Creating..." else "Placing..."
                                )
                            } else {
                                Icon(Icons.Default.Check, "Place Order")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Place Order")
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        when {
            uiState.isLoadingCart -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.cartError != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = uiState.cartError ?: "Error loading cart",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = viewModel::retry) {
                            Text("Retry")
                        }
                    }
                }
            }

            uiState.cart != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(scrollState)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Order Summary
                    OrderSummaryCard(
                        cart = uiState.cart!!
                    )

                    // Delivery Address
                    DeliveryAddressCard(
                        uiState = uiState,
                        onLine1Change = viewModel::updateAddressLine1,
                        onLine2Change = viewModel::updateAddressLine2,
                        onCityChange = viewModel::updateCity,
                        onStateChange = viewModel::updateState,
                        onPincodeChange = viewModel::updatePincode,
                        onLandmarkChange = viewModel::updateLandmark,
                        onContactNameChange = viewModel::updateContactName,
                        onContactPhoneChange = viewModel::updateContactPhone
                    )

                    // Delivery Schedule
                    DeliveryScheduleCard(
                        selectedDate = uiState.deliveryDate,
                        selectedSlot = uiState.deliverySlot,
                        deliveryInstructions = uiState.deliveryInstructions,
                        availableDates = viewModel.getAvailableDates(),
                        deliverySlots = viewModel.deliverySlots,
                        formatDate = viewModel::formatDateForDisplay,
                        onDateSelect = viewModel::updateDeliveryDate,
                        onSlotSelect = viewModel::updateDeliverySlot,
                        onInstructionsChange = viewModel::updateDeliveryInstructions
                    )

                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun OrderSummaryCard(
    cart: com.poultry.buyer.domain.model.Cart
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Order Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "From ${cart.sellerBusinessName ?: cart.sellerName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            cart.items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.productName,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${item.quantity} ${item.productUnit} × ₹${String.format("%.2f", item.unitPrice)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    PriceDisplay(amount = item.lineTotal)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Subtotal (${cart.itemCount} items)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                PriceDisplay(amount = cart.totalAmount)
            }
        }
    }
}

@Composable
private fun DeliveryAddressCard(
    uiState: CheckoutUiState,
    onLine1Change: (String) -> Unit,
    onLine2Change: (String) -> Unit,
    onCityChange: (String) -> Unit,
    onStateChange: (String) -> Unit,
    onPincodeChange: (String) -> Unit,
    onLandmarkChange: (String) -> Unit,
    onContactNameChange: (String) -> Unit,
    onContactPhoneChange: (String) -> Unit
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Delivery Address",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = uiState.addressLine1,
                onValueChange = onLine1Change,
                label = { Text("Address Line 1 *") },
                placeholder = { Text("House/Building No., Street") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = uiState.addressErrors.containsKey("addressLine1"),
                supportingText = uiState.addressErrors["addressLine1"]?.let {
                    { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            )

            OutlinedTextField(
                value = uiState.addressLine2,
                onValueChange = onLine2Change,
                label = { Text("Address Line 2") },
                placeholder = { Text("Area, Colony (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = uiState.city,
                    onValueChange = onCityChange,
                    label = { Text("City *") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    isError = uiState.addressErrors.containsKey("city"),
                    supportingText = uiState.addressErrors["city"]?.let {
                        { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                )

                OutlinedTextField(
                    value = uiState.state,
                    onValueChange = onStateChange,
                    label = { Text("State *") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    isError = uiState.addressErrors.containsKey("state"),
                    supportingText = uiState.addressErrors["state"]?.let {
                        { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = uiState.pincode,
                    onValueChange = onPincodeChange,
                    label = { Text("Pincode *") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = uiState.addressErrors.containsKey("pincode"),
                    supportingText = uiState.addressErrors["pincode"]?.let {
                        { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                )

                OutlinedTextField(
                    value = uiState.landmark,
                    onValueChange = onLandmarkChange,
                    label = { Text("Landmark") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            HorizontalDivider()

            Text(
                text = "Contact Details",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = uiState.contactName,
                    onValueChange = onContactNameChange,
                    label = { Text("Contact Name *") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    isError = uiState.addressErrors.containsKey("contactName"),
                    supportingText = uiState.addressErrors["contactName"]?.let {
                        { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                )

                OutlinedTextField(
                    value = uiState.contactPhone,
                    onValueChange = onContactPhoneChange,
                    label = { Text("Phone *") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    isError = uiState.addressErrors.containsKey("contactPhone"),
                    supportingText = uiState.addressErrors["contactPhone"]?.let {
                        { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                )
            }
        }
    }
}

@Composable
private fun DeliveryScheduleCard(
    selectedDate: String?,
    selectedSlot: String?,
    deliveryInstructions: String,
    availableDates: List<String>,
    deliverySlots: List<String>,
    formatDate: (String) -> String,
    onDateSelect: (String?) -> Unit,
    onSlotSelect: (String?) -> Unit,
    onInstructionsChange: (String) -> Unit
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Delivery Schedule",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Preferred Delivery Date",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(availableDates) { date ->
                    FilterChip(
                        selected = selectedDate == date,
                        onClick = {
                            onDateSelect(if (selectedDate == date) null else date)
                        },
                        label = { Text(formatDate(date)) }
                    )
                }
            }

            Text(
                text = "Preferred Time Slot",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(deliverySlots) { slot ->
                    FilterChip(
                        selected = selectedSlot == slot,
                        onClick = {
                            onSlotSelect(if (selectedSlot == slot) null else slot)
                        },
                        label = { Text(slot) }
                    )
                }
            }

            OutlinedTextField(
                value = deliveryInstructions,
                onValueChange = onInstructionsChange,
                label = { Text("Delivery Instructions") },
                placeholder = { Text("Any special instructions for delivery") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 3
            )
        }
    }
}
