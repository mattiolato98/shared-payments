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

    var usersId: List<String>? = null,
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
            expense.usersPaidForId?.filter { it.key == userId }?.forEach {
                result += BigDecimal(it.value)
            }
        }

        return result.setScale(2, RoundingMode.HALF_UP).toString()
    }

    suspend fun balance(): Map<String, String> {
        val balance = mutableMapOf<String, String>()

        users!!.forEach { user ->
            val userBalance = userCreditTotal(user.userId!!) - userDebitTotal(user.userId!!)
            balance[user.username!!] = userBalance.setScale(2, RoundingMode.HALF_UP).toString()
        }

        return balance
    }

    suspend fun refunds(): List<Triple<String, String, String>>? {
        val balance = balance()

        val creditors = balance.mapValues { BigDecimal(it.value) }
            .filter { it.value > BigDecimal.ZERO }
            .toList().sortedBy { (_, value) -> -value }.toMap().toMutableMap()
        val debtors = balance.mapValues { BigDecimal(it.value) }
            .filter { it.value < BigDecimal.ZERO }.mapValues { it.value.abs() }
            .toList().sortedBy { (_, value) -> -value }.toMap().toMutableMap()

        val (transaction_number, refunds) = computeRefunds(creditors, debtors)

        return refunds?.sortedByDescending { it.second }
    }

    private fun computeRefunds(
        creditors: MutableMap<String, BigDecimal>,
        debtors: MutableMap<String, BigDecimal>,
    ): Pair<Int, List<Triple<String, String, String>>?> {

        if (creditors.isEmpty() || debtors.isEmpty())
            return Pair(0, listOf())

        var min = Integer.MAX_VALUE
        var refunds = mutableListOf<Triple<String, String, String>>()

        val creditor = creditors.entries.first()

        debtors.entries.forEach { debtor ->

            val creditorsCopy: MutableMap<String, BigDecimal> = mutableMapOf()
            creditorsCopy.putAll(creditors)

            val debtorsCopy: MutableMap<String, BigDecimal> = mutableMapOf()
            debtorsCopy.putAll(debtors)

            val transaction: Triple<String, String, String>


            if (debtor.value > creditor.value) {
                debtorsCopy[debtor.key] = debtor.value - creditor.value
                debtorsCopy.toList().sortedBy { (_, value) -> -value }.toMap().toMutableMap()
                creditorsCopy.remove(creditor.key)

                transaction = Triple(debtor.key, creditor.value.toString(), creditor.key)
            } else if (debtor.value < creditor.value) {
                creditorsCopy[creditor.key] = creditor.value - debtor.value
                creditorsCopy.toList().sortedBy { (_, value) -> -value }.toMap().toMutableMap()
                debtorsCopy.remove(debtor.key)

                transaction = Triple(debtor.key, debtor.value.toString(), creditor.key)
            } else {
                creditorsCopy.remove(creditor.key)
                debtorsCopy.remove(debtor.key)

                transaction = Triple(debtor.key, debtor.value.toString(), creditor.key)
            }

            val (newMin, newRefunds) = computeRefunds(creditorsCopy, debtorsCopy)

            if (newMin < min) {
                min = newMin + 1

                refunds = mutableListOf()
                refunds += transaction
                newRefunds?.also {
                    refunds.addAll(newRefunds)
                }
            }
        }

        return Pair(min, refunds)
    }



    private fun userCreditTotal(userId: String): BigDecimal {
        var result: BigDecimal = BigDecimal.ZERO

        val a = expenses?.filter { expense ->
            expense.userPayingId == userId
        }
        val b = a?.map { expense ->
            expense.usersPaidForId!!.filter { it.key != userId }
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
            expense.usersPaidForId!!.filter { it.key == userId }
        }?.flatMap { it.values }?.forEach {
            result += BigDecimal(it)
        }

        return result
    }
}