package com.example.turtle.data

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Profile (
    @DocumentId
    var documentId: String? = null,

    var userId: String,
    var username: String? = null,
    var profilePictureUrl: String? = null,

    @ServerTimestamp
    var createDateTime: Date? = null,
)