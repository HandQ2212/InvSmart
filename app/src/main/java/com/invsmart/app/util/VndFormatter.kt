package com.invsmart.app.util

import java.text.NumberFormat
import java.util.Locale

object VndFormatter {
    private val numberFormatter = NumberFormat.getNumberInstance(Locale("vi", "VN")).apply {
        maximumFractionDigits = 0
    }

    fun format(amount: Double): String {
        return "${numberFormatter.format(amount)} đ"
    }
}
