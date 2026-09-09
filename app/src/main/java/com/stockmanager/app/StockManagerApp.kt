package com.stockmanager.app

import android.app.Application
import com.google.android.material.color.DynamicColors
import com.stockmanager.app.repository.StockRepository

class StockManagerApp : Application() {

    val repository: StockRepository by lazy { StockRepository(this) }

    override fun onCreate() {
        super.onCreate()
        // Tints the whole app to the user's wallpaper-based Material You palette on Android 12+.
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
