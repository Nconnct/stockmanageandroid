package com.stockmanager.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatting {
    private val dateTimeFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    fun currency(amount: Double): String = "₹%,.2f".format(amount)

    fun dateTime(millis: Long): String = dateTimeFormat.format(Date(millis))

    fun date(millis: Long): String = dateFormat.format(Date(millis))
}
