package com.example.vestiaire

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var rvClothing: RecyclerView
    private lateinit var clothingList: ArrayList<ClothingItem>
    private lateinit var adapter: ClothingAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // 2. Connect UI
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        val fabAdd = findViewById<FloatingActionButton>(R.id.fabAdd)
        val btnProfile = findViewById<Button>(R.id.btnProfile)
        rvClothing = findViewById(R.id.rvClothing)

        // 3. Setup RecyclerView
        rvClothing.layoutManager = LinearLayoutManager(this)
        clothingList = arrayListOf()
        adapter = ClothingAdapter(clothingList)
        rvClothing.adapter = adapter

        // 4. Fetch data from Firestore
        fetchClothingData()

        // 5. Action Buttons
        btnLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
        btnProfile.setOnClickListener {
            startActivity(
                Intent(this, ProfileActivity::class.java)
            )
        }

        fabAdd.setOnClickListener {
//            Toast.makeText(this, "Will redirect to Add Clothing page later", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, AddClothingActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        fetchClothingData()
    }

    private fun fetchClothingData() {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            db.collection("clothes")
                .whereEqualTo("userId", currentUser.uid)
                .get()
                .addOnSuccessListener { result ->
                    clothingList.clear()

                    for (document in result) {
                        val clothingItem = document.toObject(ClothingItem::class.java)
                        clothingItem.id = document.id
                        clothingList.add(clothingItem)
                    }

                    adapter.notifyDataSetChanged()
                }
                .addOnFailureListener { exception ->
                    Log.w("FIRESTORE", "Error getting documents.", exception)
                    Toast.makeText(this, "Failed to fetch data", Toast.LENGTH_SHORT).show()
                }
        }
    }
}