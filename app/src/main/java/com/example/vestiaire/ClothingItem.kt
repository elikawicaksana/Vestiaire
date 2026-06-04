package com.example.vestiaire

import com.google.firebase.Timestamp

data class ClothingItem(
    var id: String = "",        // Untuk itemId (Primary Key)
    var userId: String = "",    // Foreign Key dari User yang login
    var name: String = "",
    var category: String = "",
    var occasion: String = "",
    var color: String = "",
    var imageUrl: String = "",
    var createdAt: Timestamp? = null,
    var updatedAt: Timestamp? = null
)