package com.example.turtle.data

import com.google.firebase.firestore.DocumentId

data class Bill(
    @DocumentId
    var documentId: String? = null,
    var title: String = "",
    var description: String = "",
)