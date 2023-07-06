package com.example.turtle.data

import android.util.Log
import com.example.turtle.Resource
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Singleton


@Singleton
class BillRepository {
    val tag = "BILL_REPOSITORY"

    private val billCollectionRef = Firebase.firestore.collection("bills")

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getBills(userId: String): Flow<Resource<List<Bill>?>> {
        val msg = "An error occurred while fetching the data. Try again later"

        return billCollectionRef
            .whereArrayContains("usersId", userId)
            .orderBy("createDateTime", Query.Direction.DESCENDING)
            .getDataFlow { querySnapshot ->
                if (querySnapshot != null) {
                    Resource.Success(
                        querySnapshot.documents.map { doc ->
                            doc.toObject(Bill::class.java)!!
                        }
                    )
                } else {
                    Resource.Error(msg)
                }
            }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun getBillAndExpenses(billId: String): Flow<Resource<Bill>> {
        val msg = "An error occurred while fetching the data. Try again later"
        val bill = getBill(billId) ?: return flow { emit(Resource.Error(msg)) }

        return billCollectionRef.document(billId)
            .collection("expenses")
            .orderBy("date", Query.Direction.DESCENDING)
            .getDataFlow { querySnapshot ->
                if (querySnapshot != null) {
                    bill.expenses = querySnapshot.documents.map { doc ->
                        doc.toObject(Expense::class.java)!!
                    }
                    Resource.Success(bill)
                } else {
                    Resource.Error(msg)
                }
            }
    }

    private suspend fun getBill(billId: String): Bill? {
        return try {
            val doc = billCollectionRef.document(billId).get().await()
            doc.toObject(Bill::class.java)!!
        } catch (e: Exception) {
            Log.e(com.example.turtle.ui.billdetail.TAG, e.message.toString())
            if (e is CancellationException) throw e
            return null
        }
    }

    fun getExpense(billId: String, expenseId: String): Flow<Resource<Expense>> {
        val msg = "An error occurred while fetching the data. Try again later"

        return billCollectionRef
            .document(billId)
            .collection("expenses")
            .document(expenseId).getDataFlow { documentSnapshot ->
                if (documentSnapshot != null) {
                    val expense = documentSnapshot.toObject(Expense::class.java)!!
                    Resource.Success(expense)
                } else {
                    Resource.Error(msg)
                }
            }
    }

    fun deleteBill(billId: String): Resource<Unit> {
        return try {
            billCollectionRef.document(billId).delete()
            Resource.Success(Unit, "Bill successfully deleted")
        } catch (e: Exception) {
            Log.d(tag, e.message.toString())
            if (e is CancellationException) throw e
            Resource.Error(
                "An unexpected error occurred while deleting the item. Retry later"
            )
        }
    }

    fun deleteExpense(billId: String, expenseId: String): Resource<Unit> {
        return try {
            billCollectionRef
                .document(billId)
                .collection("expenses")
                .document(expenseId)
                .delete()
            Resource.Success(Unit, "Expense successfully deleted")
        } catch (e: Exception) {
            Log.d(tag, e.message.toString())
            if (e is CancellationException) throw e
            Resource.Error(
                "An unexpected error occurred while deleting the item. Retry later"
            )
        }
    }

    private fun Query.getQuerySnapshotFlow(): Flow<QuerySnapshot?> {
        return callbackFlow {
            val listenerRegistration =
                addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    firebaseFirestoreException?.let {
                        cancel(
                            message = "Error fetching data",
                            cause = firebaseFirestoreException
                        )
                        return@addSnapshotListener
                    }
                    trySend(querySnapshot)
                }
            awaitClose {
                listenerRegistration.remove()
            }
        }
    }

    @ExperimentalCoroutinesApi
    fun <T> Query.getDataFlow(mapper: (QuerySnapshot?) -> T): Flow<T> {
        return getQuerySnapshotFlow()
            .map {
                return@map mapper(it)
            }
    }


}