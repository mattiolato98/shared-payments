package com.example.turtle.data

import com.google.firebase.firestore.DocumentId
import java.math.BigDecimal
import java.time.LocalDate


data class Expense (
    @DocumentId
    var documentId: String? = null,

    var title: String = "",
    var amount: BigDecimal = BigDecimal(0.00),
    var date: LocalDate = LocalDate.now(),

    var userPayingId: String? = null,
    var userPaidForIds: List<String>? = null
)