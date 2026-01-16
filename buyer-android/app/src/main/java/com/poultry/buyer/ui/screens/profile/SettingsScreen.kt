package com.poultry.buyer.ui.screens.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val settingsState by viewModel.settingsUiState.collectAsState()
    val profileState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var deleteReason by remember { mutableStateOf("") }
    var confirmPhone by remember { mutableStateOf("") }
    var deletePhoneError by remember { mutableStateOf<String?>(null) }

    // Handle cache cleared
    LaunchedEffect(settingsState.cacheCleared) {
        if (settingsState.cacheCleared) {
            snackbarHostState.showSnackbar("Cache cleared successfully")
            viewModel.clearCacheFlag()
        }
    }

    // Handle errors
    LaunchedEffect(settingsState.error) {
        settingsState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    // Handle account deletion error
    LaunchedEffect(profileState.error) {
        profileState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    // Delete Account Dialog
    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!profileState.isDeletingAccount) {
                    showDeleteAccountDialog = false
                    deleteReason = ""
                    confirmPhone = ""
                    deletePhoneError = null
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Account")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "This action is irreversible. All your data including orders, addresses, and preferences will be permanently deleted.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedTextField(
                        value = deleteReason,
                        onValueChange = { deleteReason = it },
                        label = { Text("Reason for leaving (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 3
                    )

                    Text(
                        text = "Enter your phone number to confirm:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )

                    OutlinedTextField(
                        value = confirmPhone,
                        onValueChange = {
                            confirmPhone = it.filter { c -> c.isDigit() }.take(10)
                            deletePhoneError = null
                        },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = deletePhoneError != null,
                        supportingText = deletePhoneError?.let {
                            { Text(it, color = MaterialTheme.colorScheme.error) }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val userPhone = viewModel.getUserPhone().takeLast(10)
                        if (confirmPhone != userPhone) {
                            deletePhoneError = "Phone number doesn't match"
                        } else {
                            viewModel.deleteAccount(
                                reason = deleteReason.takeIf { it.isNotBlank() },
                                confirmPhone = confirmPhone
                            ) {
                                showDeleteAccountDialog = false
                                onLogout()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    enabled = !profileState.isDeletingAccount
                ) {
                    if (profileState.isDeletingAccount) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onError
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Delete My Account")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteAccountDialog = false
                        deleteReason = ""
                        confirmPhone = ""
                        deletePhoneError = null
                    },
                    enabled = !profileState.isDeletingAccount
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            // Notifications Section
            SettingsSectionHeader(title = "Notifications")

            SettingsToggleItem(
                icon = Icons.Default.Notifications,
                title = "All Notifications",
                subtitle = "Enable or disable all notifications",
                checked = settingsState.notificationsEnabled,
                onCheckedChange = viewModel::updateNotificationsEnabled,
                enabled = !settingsState.isUpdating
            )

            if (settingsState.notificationsEnabled) {
                SettingsToggleItem(
                    icon = Icons.Default.Email,
                    title = "Email Notifications",
                    subtitle = "Order updates and promotions via email",
                    checked = settingsState.emailNotificationsEnabled,
                    onCheckedChange = viewModel::updateEmailNotificationsEnabled,
                    enabled = !settingsState.isUpdating
                )

                SettingsToggleItem(
                    icon = Icons.Default.Sms,
                    title = "SMS Notifications",
                    subtitle = "Order updates via text messages",
                    checked = settingsState.smsNotificationsEnabled,
                    onCheckedChange = viewModel::updateSmsNotificationsEnabled,
                    enabled = !settingsState.isUpdating
                )

                SettingsToggleItem(
                    icon = Icons.Default.PhoneAndroid,
                    title = "Push Notifications",
                    subtitle = "Real-time updates on your device",
                    checked = settingsState.pushNotificationsEnabled,
                    onCheckedChange = viewModel::updatePushNotificationsEnabled,
                    enabled = !settingsState.isUpdating
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // App Section
            SettingsSectionHeader(title = "App")

            SettingsClickItem(
                icon = Icons.Default.CleaningServices,
                title = "Clear Cache",
                subtitle = "Free up storage space",
                onClick = viewModel::clearCache
            )

            SettingsInfoItem(
                icon = Icons.Default.Info,
                title = "App Version",
                value = ProfileViewModel.APP_VERSION
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Legal Section
            SettingsSectionHeader(title = "Legal")

            SettingsLinkItem(
                icon = Icons.Default.Description,
                title = "Terms of Service",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://poultryplatform.com/terms"))
                    context.startActivity(intent)
                }
            )

            SettingsLinkItem(
                icon = Icons.Default.PrivacyTip,
                title = "Privacy Policy",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://poultryplatform.com/privacy"))
                    context.startActivity(intent)
                }
            )

            SettingsLinkItem(
                icon = Icons.Default.Policy,
                title = "Refund Policy",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://poultryplatform.com/refund-policy"))
                    context.startActivity(intent)
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Danger Zone
            SettingsSectionHeader(
                title = "Account",
                color = MaterialTheme.colorScheme.error
            )

            ListItem(
                headlineContent = {
                    Text(
                        "Delete Account",
                        color = MaterialTheme.colorScheme.error
                    )
                },
                supportingContent = {
                    Text(
                        "Permanently delete your account and all data",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                },
                leadingContent = {
                    Icon(
                        Icons.Default.DeleteForever,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                trailingContent = {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                },
                modifier = Modifier.clickable { showDeleteAccountDialog = true }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsSectionHeader(
    title: String,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Medium,
        color = color,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        }
    )
}

@Composable
private fun SettingsClickItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingContent = {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun SettingsInfoItem(
    icon: ImageVector,
    title: String,
    value: String
) {
    ListItem(
        headlineContent = { Text(title) },
        leadingContent = {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingContent = {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

@Composable
private fun SettingsLinkItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        leadingContent = {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingContent = {
            Icon(
                Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
