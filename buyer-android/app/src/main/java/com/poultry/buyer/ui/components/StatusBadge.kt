package com.poultry.buyer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class StatusColors(
    val background: Color,
    val text: Color
)

@Composable
fun OrderStatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val colors = getOrderStatusColors(status)

    Text(
        text = formatStatus(status),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Medium,
        color = colors.text,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(colors.background)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
fun ProductStatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val colors = getProductStatusColors(status)

    Text(
        text = formatStatus(status),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Medium,
        color = colors.text,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(colors.background)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
private fun getOrderStatusColors(status: String): StatusColors {
    return when (status.uppercase()) {
        "DRAFT" -> StatusColors(
            background = Color(0xFFE0E0E0),
            text = Color(0xFF616161)
        )
        "PLACED" -> StatusColors(
            background = Color(0xFFE3F2FD),
            text = Color(0xFF1565C0)
        )
        "SELLER_CONFIRMED", "CONFIRMED" -> StatusColors(
            background = Color(0xFFE8F5E9),
            text = Color(0xFF2E7D32)
        )
        "SELLER_REJECTED", "REJECTED" -> StatusColors(
            background = Color(0xFFFFEBEE),
            text = Color(0xFFC62828)
        )
        "DISPATCHED", "SHIPPED" -> StatusColors(
            background = Color(0xFFFFF3E0),
            text = Color(0xFFEF6C00)
        )
        "DELIVERED" -> StatusColors(
            background = Color(0xFFE8F5E9),
            text = Color(0xFF1B5E20)
        )
        "CANCELLED_BY_BUYER", "CANCELLED_BY_SELLER", "CANCELLED" -> StatusColors(
            background = Color(0xFFFFEBEE),
            text = Color(0xFFB71C1C)
        )
        "PAYMENT_PENDING" -> StatusColors(
            background = Color(0xFFFFF8E1),
            text = Color(0xFFF57F17)
        )
        else -> StatusColors(
            background = Color(0xFFF5F5F5),
            text = Color(0xFF757575)
        )
    }
}

@Composable
private fun getProductStatusColors(status: String): StatusColors {
    return when (status.uppercase()) {
        "ACTIVE" -> StatusColors(
            background = Color(0xFFE8F5E9),
            text = Color(0xFF2E7D32)
        )
        "INACTIVE" -> StatusColors(
            background = Color(0xFFE0E0E0),
            text = Color(0xFF616161)
        )
        "OUT_OF_STOCK" -> StatusColors(
            background = Color(0xFFFFEBEE),
            text = Color(0xFFC62828)
        )
        "DELETED" -> StatusColors(
            background = Color(0xFFFFEBEE),
            text = Color(0xFFB71C1C)
        )
        else -> StatusColors(
            background = Color(0xFFF5F5F5),
            text = Color(0xFF757575)
        )
    }
}

private fun formatStatus(status: String): String {
    return status
        .replace("_", " ")
        .lowercase()
        .replaceFirstChar { it.uppercase() }
}
