package com.poultry.buyer.core.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager as SystemNotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.poultry.buyer.MainActivity
import com.poultry.buyer.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NotificationManager handles all notification-related operations.
 *
 * Features:
 * - Creates and manages notification channels
 * - Shows different types of notifications (order, payment, promotional)
 * - Handles notification actions and deep links
 * - Supports big picture style for promotional notifications
 */
@Singleton
class NotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        // Notification channel IDs
        const val CHANNEL_ORDERS = "orders"
        const val CHANNEL_PAYMENTS = "payments"
        const val CHANNEL_PROMOTIONS = "promotions"
        const val CHANNEL_GENERAL = "general"

        // Notification group keys
        const val GROUP_ORDERS = "group_orders"
        const val GROUP_PAYMENTS = "group_payments"

        // Notification ID generator
        private val notificationIdCounter = AtomicInteger(1000)

        // Intent extras
        const val EXTRA_ORDER_ID = "order_id"
        const val EXTRA_PAYMENT_ID = "payment_id"
        const val EXTRA_NOTIFICATION_TYPE = "notification_type"
        const val EXTRA_DEEP_LINK = "deep_link"
    }

    private val systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
            as SystemNotificationManager

    init {
        createNotificationChannels()
    }

    /**
     * Create notification channels for Android O+
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_ORDERS,
                    "Order Updates",
                    SystemNotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications about your order status updates"
                    enableLights(true)
                    enableVibration(true)
                    setShowBadge(true)
                },

                NotificationChannel(
                    CHANNEL_PAYMENTS,
                    "Payment Updates",
                    SystemNotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications about payment status"
                    enableLights(true)
                    enableVibration(true)
                    setShowBadge(true)
                },

                NotificationChannel(
                    CHANNEL_PROMOTIONS,
                    "Offers & Promotions",
                    SystemNotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Special offers and promotional notifications"
                    enableLights(false)
                    enableVibration(false)
                    setShowBadge(false)
                },

                NotificationChannel(
                    CHANNEL_GENERAL,
                    "General",
                    SystemNotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "General notifications"
                    enableLights(true)
                    enableVibration(true)
                    setShowBadge(true)
                }
            )

            systemNotificationManager.createNotificationChannels(channels)
        }
    }

    /**
     * Check if notification permission is granted
     */
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Show notification for order status updates
     */
    fun showOrderNotification(
        title: String,
        body: String,
        orderId: String?,
        orderNumber: String?,
        orderStatus: String?,
        imageUrl: String? = null
    ) {
        if (!hasNotificationPermission()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            orderId?.let { putExtra(EXTRA_ORDER_ID, it) }
            putExtra(EXTRA_NOTIFICATION_TYPE, "ORDER")
            putExtra(EXTRA_DEEP_LINK, "poultry://orders/$orderId")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            generateNotificationId(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusEmoji = getOrderStatusEmoji(orderStatus)
        val enhancedTitle = "$statusEmoji $title"

        val builder = NotificationCompat.Builder(context, CHANNEL_ORDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(enhancedTitle)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setGroup(GROUP_ORDERS)

        // Add order number as subtext if available
        orderNumber?.let {
            builder.setSubText("Order #$it")
        }

        showNotification(builder.build())
    }

    /**
     * Show notification for payment updates
     */
    fun showPaymentNotification(
        title: String,
        body: String,
        orderId: String?,
        paymentId: String?,
        isSuccess: Boolean
    ) {
        if (!hasNotificationPermission()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            orderId?.let { putExtra(EXTRA_ORDER_ID, it) }
            paymentId?.let { putExtra(EXTRA_PAYMENT_ID, it) }
            putExtra(EXTRA_NOTIFICATION_TYPE, "PAYMENT")
            putExtra(EXTRA_DEEP_LINK, "poultry://orders/$orderId")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            generateNotificationId(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_PAYMENTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setGroup(GROUP_PAYMENTS)

        // Set color based on success/failure
        if (isSuccess) {
            builder.setColor(0xFF4CAF50.toInt()) // Green
        } else {
            builder.setColor(0xFFF44336.toInt()) // Red
        }

        showNotification(builder.build())
    }

    /**
     * Show promotional notification with optional large image
     */
    fun showPromotionalNotification(
        title: String,
        body: String,
        imageUrl: String? = null,
        clickAction: String? = null
    ) {
        if (!hasNotificationPermission()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NOTIFICATION_TYPE, "PROMOTION")
            clickAction?.let { putExtra(EXTRA_DEEP_LINK, it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            generateNotificationId(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_PROMOTIONS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_PROMO)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        // Try to load and set big picture style if image URL is provided
        // Note: In production, use Coil or Glide for image loading
        if (!imageUrl.isNullOrEmpty()) {
            builder.setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(body)
            )
        } else {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(body))
        }

        showNotification(builder.build())
    }

    /**
     * Show general notification
     */
    fun showGeneralNotification(
        title: String,
        body: String,
        imageUrl: String? = null
    ) {
        if (!hasNotificationPermission()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NOTIFICATION_TYPE, "GENERAL")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            generateNotificationId(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_GENERAL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        showNotification(builder.build())
    }

    /**
     * Show a notification
     */
    private fun showNotification(notification: android.app.Notification) {
        try {
            NotificationManagerCompat.from(context).notify(
                generateNotificationId(),
                notification
            )
        } catch (e: SecurityException) {
            // Notification permission not granted
        }
    }

    /**
     * Cancel a specific notification
     */
    fun cancelNotification(notificationId: Int) {
        systemNotificationManager.cancel(notificationId)
    }

    /**
     * Cancel all notifications
     */
    fun cancelAllNotifications() {
        systemNotificationManager.cancelAll()
    }

    /**
     * Cancel all notifications for a specific group
     */
    fun cancelNotificationGroup(groupKey: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val activeNotifications = systemNotificationManager.activeNotifications
            activeNotifications
                .filter { it.notification.group == groupKey }
                .forEach { systemNotificationManager.cancel(it.id) }
        }
    }

    /**
     * Generate unique notification ID
     */
    private fun generateNotificationId(): Int {
        return notificationIdCounter.incrementAndGet()
    }

    /**
     * Get emoji for order status
     */
    private fun getOrderStatusEmoji(status: String?): String {
        return when (status?.uppercase()) {
            "CREATED", "PENDING" -> ""
            "CONFIRMED" -> ""
            "PROCESSING" -> ""
            "READY", "PACKED" -> ""
            "SHIPPED", "OUT_FOR_DELIVERY" -> ""
            "DELIVERED" -> ""
            "CANCELLED" -> ""
            "REFUNDED" -> ""
            else -> ""
        }
    }

    /**
     * Load bitmap from URL (for big picture notifications)
     * Note: In production, use proper image loading library
     */
    @Suppress("unused")
    private suspend fun loadBitmapFromUrl(url: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = URL(url).openStream()
                BitmapFactory.decodeStream(inputStream)
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * Notification action types
 */
enum class NotificationAction {
    VIEW_ORDER,
    VIEW_PAYMENT,
    REORDER,
    TRACK_ORDER,
    CONTACT_SUPPORT
}
