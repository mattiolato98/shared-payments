package com.example.turtle.data

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Profile (
    @DocumentId
    var documentId: String? = null,

    var userId: String? = null,
    var displayName: String? = null,
    var email: String? = null,
    var profilePictureUrl: String? = null,

    @ServerTimestamp
    var createDateTime: Date? = null,
) {
    override fun toString(): String {
        return email!!.split("@")[0]
    }
}