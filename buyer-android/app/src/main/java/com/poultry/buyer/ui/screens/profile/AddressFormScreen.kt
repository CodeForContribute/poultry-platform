package com.poultry.buyer.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressFormScreen(
    addressId: String? = null,
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.addressFormUiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    val isEditing = addressId != null

    // Handle save success
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            snackbarHostState.showSnackbar(
                if (isEditing) "Address updated successfully" else "Address added successfully"
            )
            viewModel.clearAddressFormSuccess()
            onBack()
        }
    }

    // Handle errors
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Address" else "Add Address") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = viewModel::saveAddress,
                        enabled = !uiState.isSaving && !uiState.isLoading
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Save")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(scrollState)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Address Label Section
                    Text(
                        text = "Address Type",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )

                    AddressLabelSelector(
                        selectedLabel = uiState.label,
                        onLabelSelected = viewModel::updateAddressLabel
                    )

                    // Custom label field (if "Other" is selected or for custom names)
                    if (uiState.label.isNotBlank() &&
                        uiState.label !in listOf("Home", "Work", "Office")) {
                        OutlinedTextField(
                            value = uiState.label,
                            onValueChange = viewModel::updateAddressLabel,
                            label = { Text("Custom Label") },
                            placeholder = { Text("e.g., Parents' House") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next
                            )
                        )
                    }

                    HorizontalDivider()

                    // Address Details Section
                    Text(
                        text = "Address Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )

                    OutlinedTextField(
                        value = uiState.line1,
                        onValueChange = viewModel::updateAddressLine1,
                        label = { Text("Address Line 1 *") },
                        placeholder = { Text("House/Building No., Street Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.LocationOn, contentDescription = null)
                        },
                        isError = uiState.errors.containsKey("line1"),
                        supportingText = uiState.errors["line1"]?.let {
                            { Text(it, color = MaterialTheme.colorScheme.error) }
                        },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        )
                    )

                    OutlinedTextField(
                        value = uiState.line2,
                        onValueChange = viewModel::updateAddressLine2,
                        label = { Text("Address Line 2") },
                        placeholder = { Text("Area, Colony, Sector (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.city,
                            onValueChange = viewModel::updateAddressCity,
                            label = { Text("City *") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            isError = uiState.errors.containsKey("city"),
                            supportingText = uiState.errors["city"]?.let {
                                { Text(it, color = MaterialTheme.colorScheme.error) }
                            },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next
                            )
                        )

                        OutlinedTextField(
                            value = uiState.state,
                            onValueChange = viewModel::updateAddressState,
                            label = { Text("State *") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            isError = uiState.errors.containsKey("state"),
                            supportingText = uiState.errors["state"]?.let {
                                { Text(it, color = MaterialTheme.colorScheme.error) }
                            },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.pincode,
                            onValueChange = viewModel::updateAddressPincode,
                            label = { Text("Pincode *") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            isError = uiState.errors.containsKey("pincode"),
                            supportingText = uiState.errors["pincode"]?.let {
                                { Text(it, color = MaterialTheme.colorScheme.error) }
                            } ?: {
                                Text(
                                    "6 digit pincode",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            )
                        )

                        OutlinedTextField(
                            value = uiState.landmark,
                            onValueChange = viewModel::updateAddressLandmark,
                            label = { Text("Landmark") },
                            placeholder = { Text("Optional") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next
                            )
                        )
                    }

                    HorizontalDivider()

                    // Contact Details Section
                    Text(
                        text = "Contact Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )

                    OutlinedTextField(
                        value = uiState.contactName,
                        onValueChange = viewModel::updateAddressContactName,
                        label = { Text("Contact Name *") },
                        placeholder = { Text("Name of person to contact") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null)
                        },
                        isError = uiState.errors.containsKey("contactName"),
                        supportingText = uiState.errors["contactName"]?.let {
                            { Text(it, color = MaterialTheme.colorScheme.error) }
                        },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        )
                    )

                    OutlinedTextField(
                        value = uiState.contactPhone,
                        onValueChange = viewModel::updateAddressContactPhone,
                        label = { Text("Contact Phone *") },
                        placeholder = { Text("10 digit mobile number") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = null)
                        },
                        prefix = { Text("+91 ") },
                        isError = uiState.errors.containsKey("contactPhone"),
                        supportingText = uiState.errors["contactPhone"]?.let {
                            { Text(it, color = MaterialTheme.colorScheme.error) }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Done
                        )
                    )

                    HorizontalDivider()

                    // Default Address Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Set as default address",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Use this address by default when placing orders",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.isDefault,
                            onCheckedChange = viewModel::updateAddressIsDefault
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Save Button
                    Button(
                        onClick = viewModel::saveAddress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Saving...")
                        } else {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isEditing) "Update Address" else "Save Address")
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun AddressLabelSelector(
    selectedLabel: String,
    onLabelSelected: (String) -> Unit
) {
    val labels = listOf(
        "Home" to Icons.Default.Home,
        "Work" to Icons.Default.Work,
        "Office" to Icons.Default.Business,
        "Other" to Icons.Default.MoreHoriz
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        labels.forEach { (label, icon) ->
            val isSelected = when {
                label == "Other" && selectedLabel.isNotBlank() &&
                    selectedLabel !in listOf("Home", "Work", "Office") -> true
                else -> selectedLabel.equals(label, ignoreCase = true)
            }

            FilterChip(
                selected = isSelected,
                onClick = {
                    onLabelSelected(if (label == "Other") "" else label)
                },
                label = { Text(label) },
                leadingIcon = {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
    }
}
