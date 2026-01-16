package com.poultry.buyer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.poultry.buyer.core.payment.PaymentManager
import com.poultry.buyer.ui.navigation.AppNavigation
import com.poultry.buyer.ui.theme.PoultryBuyerTheme
import com.razorpay.ExternalWalletListener
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity(), PaymentResultWithDataListener, ExternalWalletListener {

    @Inject
    lateinit var paymentManager: PaymentManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PoultryBuyerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        paymentManager.onPaymentSuccess(
            razorpayPaymentId = razorpayPaymentId,
            razorpayOrderId = paymentData?.orderId,
            razorpaySignature = paymentData?.signature
        )
    }

    override fun onPaymentError(code: Int, description: String?, paymentData: PaymentData?) {
        paymentManager.onPaymentError(code, description, paymentData)
    }

    override fun onExternalWalletSelected(walletName: String?, paymentData: PaymentData?) {
        paymentManager.onExternalWallet(walletName)
    }
}
