package com.example.vestiaire

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
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

        // 2. Connect UI (Hanya menghubungkan komponen yang benar-benar ada di activity_main.xml)
        rvClothing = findViewById(R.id.rvClothing)

        // Hubungkan area menu klik dari layout_bottom_nav yang di-include
        val layoutMenuCatalog = findViewById<LinearLayout>(R.id.layoutMenuCatalog)
        val layoutMenuAdd = findViewById<LinearLayout>(R.id.layoutMenuAdd)
        val layoutMenuProfile = findViewById<LinearLayout>(R.id.layoutMenuProfile)
        val btnCatalog = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCatalog)
        val tvCatalogLabel = findViewById<TextView>(R.id.tvCatalogLabel)

        val activeColor = androidx.core.content.ContextCompat.getColor(this, R.color.vestiaire_text)
        btnCatalog?.setIconTintResource(R.color.vestiaire_text)
        tvCatalogLabel?.setTextColor(activeColor)
        tvCatalogLabel?.setTypeface(null, android.graphics.Typeface.BOLD)

        // 3. Setup RecyclerView
        rvClothing.layoutManager = LinearLayoutManager(this)
        clothingList = arrayListOf()
        adapter = ClothingAdapter(clothingList)
        rvClothing.adapter = adapter

        // 4. Fetch data from Firestore
        fetchClothingData()

        // 5. Action Navigasi Bawah Baru (Membaca klik dari layout kustom include)

        layoutMenuAdd?.setOnClickListener {
            startActivity(Intent(this, AddClothingActivity::class.java))
            overridePendingTransition(0, 0) // Menghilangkan animasi transisi agar smooth
        }

        layoutMenuProfile?.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
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