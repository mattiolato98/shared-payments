package com.example.turtle.data

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.math.BigDecimal
import java.util.Date

data class Bill(
    @DocumentId
    var documentId: String? = null,

    var userOwnerId: String? = null,
    var users: List<Profile>? = null,

    var title: String = "",
    var description: String? = null,

    @ServerTimestamp
    var createDateTime: Date? = null,

    var expenses: List<Expense>? = null,

    ) {
    fun groupTotal(): BigDecimal {
        var result: BigDecimal = BigDecimal.ZERO

        expenses?.map { it.bigDecimalAmount }?.also {
            result = it.fold(BigDecimal.ZERO, BigDecimal::add)
        }

        return result
    }

    fun userTotal(userId: String): BigDecimal {
        var result: BigDecimal = BigDecimal.ZERO

        expenses?.forEach { expense ->
            expense.usersPaidFor?.filter { it.key == userId }?.forEach {
                   result += BigDecimal(it.value)
            }
        }
        
        return result
    }
}