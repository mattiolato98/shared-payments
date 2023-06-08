package com.example.turtle.data

import com.example.turtle.utils.BigDecimalConverterDelegate
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Date
import java.util.Locale


data class Expense(
    @DocumentId
    var documentId: String? = null,

    var title: String = "",

    @get:Exclude
    @set:Exclude
    var bigDecimalAmount: BigDecimal = BigDecimal.ZERO,

    @ServerTimestamp
    var date: Date? = null,

    var userPayingId: String? = null,
    var userPaidForIds: List<String>? = null
) {
    var amount: String by BigDecimalConverterDelegate(this::bigDecimalAmount)
}