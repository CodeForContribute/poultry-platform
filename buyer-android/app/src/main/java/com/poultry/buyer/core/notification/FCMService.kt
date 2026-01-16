package com.poultry.buyer.core.notification

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.poultry.buyer.core.data.preferences.SecureTokenManager
import com.poultry.buyer.core.network.ApiService
import com.poultry.buyer.domain.model.FcmTokenRequest
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Firebase Cloud Messaging Service for handling push notifications.
 *
 * Handles:
 * - New FCM token generation
 * - Incoming notification messages
 * - Incoming data messages
 */
@AndroidEntryPoint
class FCMService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"

        // Notification data keys
        const val KEY_NOTIFICATION_TYPE = "type"
        const val KEY_ORDER_ID = "orderId"
        const val KEY_ORDER_NUMBER = "orderNumber"
        const val KEY_ORDER_STATUS = "orderStatus"
        const val KEY_PAYMENT_ID = "paymentId"
        const val KEY_PAYMENT_STATUS = "paymentStatus"
        const val KEY_TITLE = "title"
        const val KEY_BODY = "body"
        const val KEY_IMAGE_URL = "imageUrl"
        const val KEY_CLICK_ACTION = "clickAction"

        // Notification types
        const val TYPE_ORDER_STATUS = "ORDER_STATUS"
        const val TYPE_ORDER_CREATED = "ORDER_CREATED"
        const val TYPE_ORDER_CONFIRMED = "ORDER_CONFIRMED"
        const val TYPE_ORDER_SHIPPED = "ORDER_SHIPPED"
        const val TYPE_ORDER_DELIVERED = "ORDER_DELIVERED"
        const val TYPE_ORDER_CANCELLED = "ORDER_CANCELLED"
        const val TYPE_PAYMENT_SUCCESS = "PAYMENT_SUCCESS"
        const val TYPE_PAYMENT_FAILED = "PAYMENT_FAILED"
        const val TYPE_PROMOTION = "PROMOTION"
        const val TYPE_GENERAL = "GENERAL"
    }

    @Inject
    lateinit var notificationManager: NotificationManager

    @Inject
    lateinit var tokenManager: SecureTokenManager

    @Inject
    lateinit var apiService: ApiService

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Called when a new FCM token is generated.
     * This happens on initial app start and when the token is refreshed.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token received")

        // Save token locally
        tokenManager.saveFcmToken(token)

        // Send token to server if user is logged in
        if (tokenManager.isLoggedIn()) {
            sendTokenToServer(token)
        }
    }

    /**
     * Called when a message is received.
     *
     * There are two types of messages:
     * 1. Notification messages - automatically displayed by system when app is in background
     * 2. Data messages - always delivered to app
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "Message received from: ${message.from}")

        // Handle notification payload (displayed automatically when app is in background)
        message.notification?.let { notification ->
            handleNotificationMessage(notification, message.data)
        }

        // Handle data payload
        if (message.data.isNotEmpty()) {
            handleDataMessage(message.data)
        }
    }

    /**
     * Handle notification message payload
     */
    private fun handleNotificationMessage(
        notification: RemoteMessage.Notification,
        data: Map<String, String>
    ) {
        val title = notification.title ?: data[KEY_TITLE] ?: "Poultry Platform"
        val body = notification.body ?: data[KEY_BODY] ?: ""
        val imageUrl = notification.imageUrl?.toString() ?: data[KEY_IMAGE_URL]

        val notificationType = data[KEY_NOTIFICATION_TYPE] ?: TYPE_GENERAL

        when (notificationType) {
            TYPE_ORDER_STATUS,
            TYPE_ORDER_CREATED,
            TYPE_ORDER_CONFIRMED,
            TYPE_ORDER_SHIPPED,
            TYPE_ORDER_DELIVERED,
            TYPE_ORDER_CANCELLED -> {
                notificationManager.showOrderNotification(
                    title = title,
                    body = body,
                    orderId = data[KEY_ORDER_ID],
                    orderNumber = data[KEY_ORDER_NUMBER],
                    orderStatus = data[KEY_ORDER_STATUS],
                    imageUrl = imageUrl
                )
            }

            TYPE_PAYMENT_SUCCESS,
            TYPE_PAYMENT_FAILED -> {
                notificationManager.showPaymentNotification(
                    title = title,
                    body = body,
                    orderId = data[KEY_ORDER_ID],
                    paymentId = data[KEY_PAYMENT_ID],
                    isSuccess = notificationType == TYPE_PAYMENT_SUCCESS
                )
            }

            TYPE_PROMOTION -> {
                notificationManager.showPromotionalNotification(
                    title = title,
                    body = body,
                    imageUrl = imageUrl,
                    clickAction = data[KEY_CLICK_ACTION]
                )
            }

            else -> {
                notificationManager.showGeneralNotification(
                    title = title,
                    body = body,
                    imageUrl = imageUrl
                )
            }
        }
    }

    /**
     * Handle data message payload (silent notification)
     */
    private fun handleDataMessage(data: Map<String, String>) {
        Log.d(TAG, "Data message: $data")

        val notificationType = data[KEY_NOTIFICATION_TYPE]
        val title = data[KEY_TITLE]
        val body = data[KEY_BODY]

        // If title and body are present, show notification
        if (!title.isNullOrEmpty() && !body.isNullOrEmpty()) {
            handleNotificationMessage(
                notification = object : RemoteMessage.Notification("", "") {
                    override fun getTitle(): String = title
                    override fun getBody(): String = body
                    override fun getImageUrl(): android.net.Uri? =
                        data[KEY_IMAGE_URL]?.let { android.net.Uri.parse(it) }
                },
                data = data
            )
        }

        // Handle silent data updates
        when (notificationType) {
            "SYNC_ORDERS" -> {
                // Trigger order sync in app
                Log.d(TAG, "Sync orders requested")
            }
            "SYNC_CART" -> {
                // Trigger cart sync in app
                Log.d(TAG, "Sync cart requested")
            }
            "LOGOUT" -> {
                // Force logout the user
                Log.d(TAG, "Force logout requested")
                tokenManager.clearAll()
            }
        }
    }

    /**
     * Send FCM token to backend server
     */
    private fun sendTokenToServer(token: String) {
        serviceScope.launch {
            try {
                val deviceId = tokenManager.getDeviceId()
                val response = apiService.updateFcmToken(
                    FcmTokenRequest(
                        fcmToken = token,
                        deviceId = deviceId
                    )
                )
                if (response.isSuccessful) {
                    Log.d(TAG, "FCM token sent to server successfully")
                    tokenManager.markFcmTokenSent()
                } else {
                    Log.e(TAG, "Failed to send FCM token: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending FCM token to server", e)
            }
        }
    }

    override fun onDeletedMessages() {
        super.onDeletedMessages()
        Log.d(TAG, "Messages deleted on FCM server")
        // Could trigger a full sync here if needed
    }

    override fun onMessageSent(msgId: String) {
        super.onMessageSent(msgId)
        Log.d(TAG, "Upstream message sent: $msgId")
    }

    override fun onSendError(msgId: String, exception: Exception) {
        super.onSendError(msgId, exception)
        Log.e(TAG, "Upstream message error: $msgId", exception)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up if needed
    }
}

/**
 * Data class for parsed notification payload
 */
data class NotificationPayload(
    val type: String,
    val title: String,
    val body: String,
    val imageUrl: String?,
    val orderId: String?,
    val orderNumber: String?,
    val orderStatus: String?,
    val paymentId: String?,
    val paymentStatus: String?,
    val clickAction: String?,
    val extras: Map<String, String>
)
