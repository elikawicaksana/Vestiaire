package com.example.vestiaire

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

data class ClothingItem(
    @get:Exclude var id: String = "",
    var userId: String = "",
    var name: String = "",
    var category: String = "",
    var occasion: String = "",
    var color: String = "",
    var imageUrl: String = "",
    var createdAt: Timestamp? = null,
    var updatedAt: Timestamp? = null
)