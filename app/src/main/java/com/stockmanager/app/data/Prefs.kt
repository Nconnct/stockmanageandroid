package com.stockmanager.app.data

import android.content.Context

class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("stock_manager_prefs", Context.MODE_PRIVATE)

    var defaultReorderLevel: Int
        get() = sp.getInt(KEY_REORDER_LEVEL, 5)
        set(value) = sp.edit().putInt(KEY_REORDER_LEVEL, value).apply()

    companion object {
        private const val KEY_REORDER_LEVEL = "default_reorder_level"
    }
}
