package com.poultry.buyer.core.payment

import android.app.Activity
import com.poultry.buyer.BuildConfig
import com.poultry.buyer.domain.model.Payment
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sealed class representing payment result states
 */
sealed class PaymentResult {
    data object Idle : PaymentResult()
    data object Processing : PaymentResult()
    data class Success(
        val razorpayPaymentId: String,
        val razorpayOrderId: String,
        val razorpaySignature: String
    ) : PaymentResult()
    data class Failure(
        val errorCode: Int,
        val errorDescription: String,
        val paymentData: PaymentData?
    ) : PaymentResult()
    data class ExternalWallet(
        val walletName: String
    ) : PaymentResult()
}

/**
 * Data class for payment options
 */
data class PaymentOptions(
    val orderId: String,
    val razorpayOrderId: String,
    val amount: Long, // Amount in paise (INR subunit)
    val currency: String = "INR",
    val name: String = "Poultry Platform",
    val description: String,
    val prefillEmail: String? = null,
    val prefillPhone: String? = null,
    val prefillName: String? = null,
    val notes: Map<String, String>? = null,
    val theme: PaymentTheme = PaymentTheme()
)

/**
 * Payment theme customization
 */
data class PaymentTheme(
    val color: String = "#4CAF50", // Green theme
    val backdropColor: String = "#ffffff"
)

/**
 * PaymentManager handles Razorpay payment integration.
 *
 * Usage:
 * 1. Call startPayment() with payment options from an Activity
 * 2. Activity must implement PaymentResultWithDataListener
 * 3. Handle results via the paymentResult StateFlow
 */
@Singleton
class PaymentManager @Inject constructor() {

    companion object {
        private const val TAG = "PaymentManager"

        /**
         * Call this during Application.onCreate() to preload Razorpay
         */
        fun preloadCheckout(context: android.content.Context) {
            Checkout.preload(context)
        }
    }

    private val _paymentResult = MutableStateFlow<PaymentResult>(PaymentResult.Idle)
    val paymentResult: StateFlow<PaymentResult> = _paymentResult.asStateFlow()

    private var currentPayment: Payment? = null

    /**
     * Initialize and start a Razorpay payment
     *
     * @param activity The activity context (must implement PaymentResultWithDataListener)
     * @param payment The payment object from API
     * @param buyerName Optional buyer name for prefill
     * @param buyerPhone Optional buyer phone for prefill
     * @param buyerEmail Optional buyer email for prefill
     */
    fun startPayment(
        activity: Activity,
        payment: Payment,
        buyerName: String? = null,
        buyerPhone: String? = null,
        buyerEmail: String? = null
    ) {
        currentPayment = payment
        _paymentResult.value = PaymentResult.Processing

        val checkout = Checkout()
        checkout.setKeyID(payment.razorpayKey ?: BuildConfig.RAZORPAY_KEY_ID)

        try {
            val options = JSONObject().apply {
                // Order details
                put("name", "Poultry Platform")
                put("description", "Order Payment")
                put("order_id", payment.razorpayOrderId ?: payment.gatewayOrderId)
                put("currency", payment.currency)

                // Amount in paise (multiply by 100)
                put("amount", (payment.amount * 100).toLong())

                // Prefill details
                val prefill = JSONObject()
                buyerEmail?.let { prefill.put("email", it) }
                buyerPhone?.let { prefill.put("contact", it) }
                buyerName?.let { prefill.put("name", it) }
                put("prefill", prefill)

                // Notes for reference
                val notes = JSONObject()
                notes.put("payment_id", payment.id)
                notes.put("order_id", payment.orderId)
                put("notes", notes)

                // Theme customization
                val theme = JSONObject()
                theme.put("color", "#4CAF50")
                theme.put("backdrop_color", "#ffffff")
                put("theme", theme)

                // Retry configuration
                val retry = JSONObject()
                retry.put("enabled", true)
                retry.put("max_count", 4)
                put("retry", retry)

                // Payment methods
                val modal = JSONObject()
                modal.put("confirm_close", true)
                modal.put("ondismiss", true)
                put("modal", modal)
            }

            checkout.open(activity, options)
        } catch (e: Exception) {
            _paymentResult.value = PaymentResult.Failure(
                errorCode = -1,
                errorDescription = e.message ?: "Failed to initialize payment",
                paymentData = null
            )
        }
    }

    /**
     * Start payment with custom options
     */
    fun startPaymentWithOptions(
        activity: Activity,
        options: PaymentOptions
    ) {
        _paymentResult.value = PaymentResult.Processing

        val checkout = Checkout()
        checkout.setKeyID(BuildConfig.RAZORPAY_KEY_ID)

        try {
            val jsonOptions = JSONObject().apply {
                put("name", options.name)
                put("description", options.description)
                put("order_id", options.razorpayOrderId)
                put("currency", options.currency)
                put("amount", options.amount)

                // Prefill
                val prefill = JSONObject()
                options.prefillEmail?.let { prefill.put("email", it) }
                options.prefillPhone?.let { prefill.put("contact", it) }
                options.prefillName?.let { prefill.put("name", it) }
                put("prefill", prefill)

                // Notes
                options.notes?.let { notes ->
                    val notesJson = JSONObject()
                    notes.forEach { (key, value) ->
                        notesJson.put(key, value)
                    }
                    put("notes", notesJson)
                }

                // Theme
                val theme = JSONObject()
                theme.put("color", options.theme.color)
                theme.put("backdrop_color", options.theme.backdropColor)
                put("theme", theme)

                // Retry
                val retry = JSONObject()
                retry.put("enabled", true)
                retry.put("max_count", 4)
                put("retry", retry)
            }

            checkout.open(activity, jsonOptions)
        } catch (e: Exception) {
            _paymentResult.value = PaymentResult.Failure(
                errorCode = -1,
                errorDescription = e.message ?: "Failed to initialize payment",
                paymentData = null
            )
        }
    }

    /**
     * Handle successful payment callback
     * Called by Activity implementing PaymentResultWithDataListener
     */
    fun onPaymentSuccess(paymentData: PaymentData) {
        onPaymentSuccess(
            razorpayPaymentId = paymentData.paymentId,
            razorpayOrderId = paymentData.orderId,
            razorpaySignature = paymentData.signature
        )
    }

    /**
     * Handle successful payment callback when only raw values are available
     */
    fun onPaymentSuccess(
        razorpayPaymentId: String?,
        razorpayOrderId: String?,
        razorpaySignature: String?
    ) {
        _paymentResult.value = PaymentResult.Success(
            razorpayPaymentId = razorpayPaymentId ?: "",
            razorpayOrderId = razorpayOrderId ?: "",
            razorpaySignature = razorpaySignature ?: ""
        )
    }

    /**
     * Handle payment failure callback
     * Called by Activity implementing PaymentResultWithDataListener
     */
    fun onPaymentError(code: Int, description: String?, paymentData: PaymentData?) {
        _paymentResult.value = PaymentResult.Failure(
            errorCode = code,
            errorDescription = description ?: getErrorMessage(code),
            paymentData = paymentData
        )
    }

    /**
     * Handle external wallet selection
     */
    fun onExternalWallet(walletName: String?) {
        _paymentResult.value = PaymentResult.ExternalWallet(
            walletName = walletName ?: "Unknown Wallet"
        )
    }

    /**
     * Reset payment state to idle
     */
    fun resetPaymentState() {
        _paymentResult.value = PaymentResult.Idle
        currentPayment = null
    }

    /**
     * Get current payment being processed
     */
    fun getCurrentPayment(): Payment? = currentPayment

    /**
     * Get human-readable error message for Razorpay error codes
     */
    private fun getErrorMessage(code: Int): String {
        return when (code) {
            Checkout.NETWORK_ERROR -> "Network error. Please check your connection and try again."
            Checkout.INVALID_OPTIONS -> "Invalid payment options. Please try again."
            Checkout.PAYMENT_CANCELED -> "Payment was cancelled."
            Checkout.TLS_ERROR -> "Security error. Please update your app or device."
            else -> "Payment failed. Please try again."
        }
    }
}

/**
 * Extension function to convert Payment amount to paise
 */
fun Payment.amountInPaise(): Long = (amount * 100).toLong()

/**
 * Data class for payment verification request
 */
data class PaymentVerificationRequest(
    val paymentId: String,
    val razorpayPaymentId: String,
    val razorpayOrderId: String,
    val razorpaySignature: String
)
