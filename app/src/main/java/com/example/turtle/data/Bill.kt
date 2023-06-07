package com.example.turtle.data

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
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
)