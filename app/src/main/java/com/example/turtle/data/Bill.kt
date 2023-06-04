package com.example.turtle.data

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.time.LocalDateTime
import java.util.Calendar
import java.util.Date

data class Bill(
    @DocumentId
    var documentId: String? = null,

    var title: String = "",
    var description: String? = null,

    @ServerTimestamp
    var createDateTime: Date? = null,
)