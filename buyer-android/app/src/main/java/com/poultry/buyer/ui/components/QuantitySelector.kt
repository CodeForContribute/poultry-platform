package com.poultry.buyer.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun QuantitySelector(
    quantity: Double,
    onQuantityChange: (Double) -> Unit,
    minQuantity: Double = 1.0,
    maxQuantity: Double? = null,
    step: Double = 1.0,
    unit: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Decrease Button
        IconButton(
            onClick = {
                val newQty = quantity - step
                if (newQty >= minQuantity) {
                    onQuantityChange(newQty)
                }
            },
            enabled = quantity > minQuantity
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "Decrease quantity"
            )
        }

        // Quantity Display
        Column(
            modifier = Modifier.widthIn(min = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (quantity == quantity.toLong().toDouble()) {
                    quantity.toLong().toString()
                } else {
                    String.format("%.2f", quantity)
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            unit?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Increase Button
        IconButton(
            onClick = {
                val newQty = quantity + step
                if (maxQuantity == null || newQty <= maxQuantity) {
                    onQuantityChange(newQty)
                }
            },
            enabled = maxQuantity == null || quantity < maxQuantity
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Increase quantity"
            )
        }
    }
}

@Composable
fun QuantitySelectorCompact(
    quantity: Double,
    onQuantityChange: (Double) -> Unit,
    minQuantity: Double = 1.0,
    maxQuantity: Double? = null,
    step: Double = 1.0,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilledIconButton(
            onClick = {
                val newQty = quantity - step
                if (newQty >= minQuantity) {
                    onQuantityChange(newQty)
                }
            },
            enabled = quantity > minQuantity,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "Decrease",
                modifier = Modifier.size(16.dp)
            )
        }

        Text(
            text = if (quantity == quantity.toLong().toDouble()) {
                quantity.toLong().toString()
            } else {
                String.format("%.1f", quantity)
            },
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.widthIn(min = 32.dp),
            textAlign = TextAlign.Center
        )

        FilledIconButton(
            onClick = {
                val newQty = quantity + step
                if (maxQuantity == null || newQty <= maxQuantity) {
                    onQuantityChange(newQty)
                }
            },
            enabled = maxQuantity == null || quantity < maxQuantity,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Increase",
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
