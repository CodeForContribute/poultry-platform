package com.poultry.buyer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.poultry.buyer.core.error.*

/**
 * Reusable error dialog component with support for different error types and retry actions.
 */
@Composable
fun ErrorDialog(
    error: AppError?,
    onDismiss: () -> Unit,
    onRetry: (() -> Unit)? = null,
    onLogin: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (error == null) return

    val errorStyle = remember(error) { getErrorStyle(error) }
    val isRecoverable = error.isRecoverable()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Error Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(errorStyle.backgroundColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = errorStyle.icon,
                        contentDescription = null,
                        tint = errorStyle.iconTint,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = errorStyle.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Message
                Text(
                    text = error.userMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Dismiss Button
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Dismiss")
                    }

                    // Primary Action Button
                    when {
                        error is AppError.Auth && error.type == AuthErrorType.SESSION_EXPIRED && onLogin != null -> {
                            Button(
                                onClick = {
                                    onDismiss()
                                    onLogin()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Login")
                            }
                        }
                        isRecoverable && onRetry != null -> {
                            Button(
                                onClick = {
                                    onDismiss()
                                    onRetry()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Retry")
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Inline error banner for displaying errors within content.
 */
@Composable
fun ErrorBanner(
    error: AppError?,
    onDismiss: () -> Unit,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (error == null) return

    val errorStyle = remember(error) { getErrorStyle(error) }
    val isRecoverable = error.isRecoverable()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = errorStyle.backgroundColor.copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = errorStyle.icon,
                contentDescription = null,
                tint = errorStyle.iconTint,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = errorStyle.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = errorStyle.iconTint
                )
                Text(
                    text = error.userMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isRecoverable && onRetry != null) {
                IconButton(onClick = onRetry) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry",
                        tint = errorStyle.iconTint
                    )
                }
            }

            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Full screen error state component.
 */
@Composable
fun ErrorScreen(
    error: AppError,
    onRetry: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val errorStyle = remember(error) { getErrorStyle(error) }
    val isRecoverable = error.isRecoverable()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(errorStyle.backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = errorStyle.icon,
                contentDescription = null,
                tint = errorStyle.iconTint,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = errorStyle.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = error.userMessage,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (isRecoverable && onRetry != null) {
            Button(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Try Again")
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        if (onBack != null) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Go Back")
            }
        }
    }
}

/**
 * Snackbar wrapper for error messages.
 */
@Composable
fun ErrorSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier,
        snackbar = { data ->
            Snackbar(
                modifier = Modifier.padding(12.dp),
                shape = RoundedCornerShape(8.dp),
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                action = {
                    data.visuals.actionLabel?.let { label ->
                        TextButton(
                            onClick = { data.performAction() }
                        ) {
                            Text(
                                text = label,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            ) {
                Text(data.visuals.message)
            }
        }
    )
}

/**
 * Data class representing visual style for an error type.
 */
private data class ErrorStyle(
    val icon: ImageVector,
    val iconTint: Color,
    val backgroundColor: Color,
    val title: String
)

/**
 * Gets the visual style for a given error type.
 */
@Composable
private fun getErrorStyle(error: AppError): ErrorStyle {
    return when (error) {
        is AppError.Network -> when (error.type) {
            NetworkErrorType.NO_CONNECTION -> ErrorStyle(
                icon = Icons.Outlined.WifiOff,
                iconTint = Color(0xFFFF6B6B),
                backgroundColor = Color(0xFFFFE5E5),
                title = "No Connection"
            )
            NetworkErrorType.TIMEOUT -> ErrorStyle(
                icon = Icons.Outlined.Schedule,
                iconTint = Color(0xFFFFA726),
                backgroundColor = Color(0xFFFFF3E0),
                title = "Timeout"
            )
            NetworkErrorType.SSL_ERROR -> ErrorStyle(
                icon = Icons.Outlined.GppBad,
                iconTint = Color(0xFFEF5350),
                backgroundColor = Color(0xFFFFEBEE),
                title = "Security Error"
            )
        }
        is AppError.Server -> ErrorStyle(
            icon = Icons.Outlined.CloudOff,
            iconTint = Color(0xFFEF5350),
            backgroundColor = Color(0xFFFFEBEE),
            title = "Server Error"
        )
        is AppError.Auth -> ErrorStyle(
            icon = Icons.Outlined.Lock,
            iconTint = Color(0xFFFFA726),
            backgroundColor = Color(0xFFFFF3E0),
            title = when (error.type) {
                AuthErrorType.SESSION_EXPIRED -> "Session Expired"
                AuthErrorType.FORBIDDEN -> "Access Denied"
                AuthErrorType.INVALID_CREDENTIALS -> "Login Failed"
            }
        )
        is AppError.Validation -> ErrorStyle(
            icon = Icons.Outlined.Error,
            iconTint = Color(0xFFFFA726),
            backgroundColor = Color(0xFFFFF3E0),
            title = "Invalid Input"
        )
        is AppError.NotFound -> ErrorStyle(
            icon = Icons.Outlined.SearchOff,
            iconTint = Color(0xFF78909C),
            backgroundColor = Color(0xFFECEFF1),
            title = "Not Found"
        )
        is AppError.RateLimited -> ErrorStyle(
            icon = Icons.Outlined.Speed,
            iconTint = Color(0xFFFFA726),
            backgroundColor = Color(0xFFFFF3E0),
            title = "Too Many Requests"
        )
        is AppError.Business -> ErrorStyle(
            icon = Icons.Outlined.Info,
            iconTint = Color(0xFF42A5F5),
            backgroundColor = Color(0xFFE3F2FD),
            title = "Notice"
        )
        is AppError.Unknown -> ErrorStyle(
            icon = Icons.Outlined.Error,
            iconTint = Color(0xFFEF5350),
            backgroundColor = Color(0xFFFFEBEE),
            title = "Error"
        )
    }
}

/**
 * Extension function to check if error is recoverable.
 */
private fun AppError.isRecoverable(): Boolean {
    return when (this) {
        is AppError.Network -> this.isRecoverable
        is AppError.Server -> this.isRecoverable
        is AppError.RateLimited -> true
        is AppError.Unknown -> true
        else -> false
    }
}
