package com.poultry.buyer

import android.app.Application
import com.poultry.buyer.core.payment.PaymentManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PoultryBuyerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PaymentManager.preloadCheckout(this)
    }
}
