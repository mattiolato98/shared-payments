package com.example.turtle.data

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.math.BigDecimal
import java.math.RoundingMode
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
    fun groupTotal(): String {
        var result: BigDecimal = BigDecimal.ZERO

        expenses?.map { it.bigDecimalAmount }?.also {
            result = it.fold(BigDecimal.ZERO, BigDecimal::add)
        }

        return result.setScale(2, RoundingMode.HALF_UP).toString()
    }

    fun userTotal(userId: String): String {
        var result: BigDecimal = BigDecimal.ZERO

        expenses?.forEach { expense ->
            expense.usersPaidFor?.filter { it.key == userId }?.forEach {
                   result += BigDecimal(it.value)
            }
        }
        
        return result.setScale(2, RoundingMode.HALF_UP).toString()
    }

    fun balance(): Map<String, String> {
        val balance = mutableMapOf<String, String>()

        users!!.forEach { user ->
            val userBalance = userCreditTotal(user.userId!!) - userDebitTotal(user.userId!!)
            balance[user.email!!.split("@")[0]] = userBalance.setScale(2, RoundingMode.HALF_UP).toString()
        }

        return balance
    }

    private fun userCreditTotal(userId: String): BigDecimal {
        var result: BigDecimal = BigDecimal.ZERO

        val a = expenses?.filter { expense ->
            expense.userPayingId == userId
        }
        val b = a?.map { expense ->
            expense.usersPaidFor!!.filter { it.key != userId }
        }

        val c = b?.flatMap { it.values }

        c?.forEach {
            result += BigDecimal(it)
        }

        return result
    }

    private fun userDebitTotal(userId: String): BigDecimal {
        var result: BigDecimal = BigDecimal.ZERO

        expenses?.filter { expense ->
            expense.userPayingId != userId
        }?.map { expense ->
            expense.usersPaidFor!!.filter { it.key == userId }
        }?.flatMap { it.values }?.forEach {
            result += BigDecimal(it)
        }

        return result
    }
}