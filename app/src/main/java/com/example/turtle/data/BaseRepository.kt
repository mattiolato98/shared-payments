package com.example.turtle.data

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map

open class BaseRepository {
    @ExperimentalCoroutinesApi
    fun <T> Query.getDataFlow(mapper: (QuerySnapshot?) -> T): Flow<T> {
        return getQuerySnapshotFlow()
            .map {
                return@map mapper(it)
            }
    }

    private fun DocumentReference.getQuerySnapshotFlow(): Flow<DocumentSnapshot?> {
        return callbackFlow {
            val listenerRegistration =
                addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                    firebaseFirestoreException?.let {
                        cancel(
                            message = "Error fetching data",
                            cause = firebaseFirestoreException
                        )
                        return@addSnapshotListener
                    }
                    trySend(documentSnapshot)
                }
            awaitClose {
                listenerRegistration.remove()
            }
        }
    }

    fun <T> DocumentReference.getDataFlow(mapper: (DocumentSnapshot?) -> T): Flow<T> {
        return getQuerySnapshotFlow()
            .map {
                return@map mapper(it)
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
}