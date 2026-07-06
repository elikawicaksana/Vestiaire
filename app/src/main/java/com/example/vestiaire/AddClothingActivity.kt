package com.example.vestiaire

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class AddClothingActivity : AppCompatActivity() {

    private lateinit var ivPreview: ImageView
    private lateinit var etName: EditText
    private lateinit var etColor: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var spinnerOccasion: Spinner
    private lateinit var btnSelectImage: Button
    private lateinit var btnSave: Button
    private lateinit var progressBar: ProgressBar

    private var imageUri: Uri? = null

    // Initialize Firebase Services
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            imageUri = uri
            ivPreview.setImageURI(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_clothing)

        // Connect UI
        ivPreview = findViewById(R.id.ivPreview)
        etName = findViewById(R.id.etName)
        etColor = findViewById(R.id.etColor)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        spinnerOccasion = findViewById(R.id.spinnerOccasion)
        btnSelectImage = findViewById(R.id.btnSelectImage)
        btnSave = findViewById(R.id.btnSave)
        progressBar = findViewById(R.id.progressBar)

        val layoutMenuCatalog = findViewById<LinearLayout>(R.id.layoutMenuCatalog)
        val layoutMenuProfile = findViewById<LinearLayout>(R.id.layoutMenuProfile)

        layoutMenuCatalog?.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }

        layoutMenuProfile?.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }

        setupSpinners()

        btnSelectImage.setOnClickListener {
            pickImage.launch("image/*")
        }

        btnSave.setOnClickListener {
            uploadData()
        }
    }

    private fun setupSpinners() {
        // ENUM options for Category
        val categories = arrayOf("Tops", "Bottoms", "Dresses", "Outerwear", "Shoes", "Accessories")
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        spinnerCategory.adapter = categoryAdapter

        // ENUM options for Occasion
        val occasions = arrayOf("Casual", "Formal", "Sport", "Party", "Work")
        val occasionAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, occasions)
        spinnerOccasion.adapter = occasionAdapter
    }

    private fun uploadData() {
        val name = etName.text.toString().trim()
        val color = etColor.text.toString().trim()
        val category = spinnerCategory.selectedItem.toString()
        val occasion = spinnerOccasion.selectedItem.toString()
        val userId = auth.currentUser?.uid

        // Validasi teks tetap wajib
        if (name.isEmpty() || color.isEmpty()) {
            Toast.makeText(this, "Please fill all text fields", Toast.LENGTH_SHORT).show()
            return
        }
        if (userId == null) {
            Toast.makeText(this, "Error: User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        btnSave.isEnabled = false

        // CEK KONDISI: Apakah user memilih gambar atau tidak?
        if (imageUri != null) {
            // KONDISI 1: User milih gambar -> Upload ke Storage dulu
            val fileName = UUID.randomUUID().toString() + ".jpg"
            val storageRef = storage.reference.child("clothing_images/$fileName")

            storageRef.putFile(imageUri!!)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        val imageUrl = uri.toString()
                        // Lanjut simpan ke Firestore membawa link gambar
                        saveToFirestore(userId, name, category, occasion, color, imageUrl)
                    }
                }
                .addOnFailureListener { e ->
                    progressBar.visibility = View.GONE
                    btnSave.isEnabled = true
                    Toast.makeText(this, "Image Upload Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            // KONDISI 2: User TIDAK milih gambar -> Langsung tembak ke Firestore
            // Parameter imageUrl kita isi dengan string kosong ("")
            saveToFirestore(userId, name, category, occasion, color, "")
        }
    }

    private fun saveToFirestore(userId: String, name: String, category: String, occasion: String, color: String, imageUrl: String) {

        val currentTime = Timestamp.now() // Ambil waktu saat ini

        val clothingItem = ClothingItem(
            userId = userId,
            name = name,
            category = category,
            occasion = occasion,
            color = color,
            imageUrl = imageUrl,
            createdAt = currentTime,
            updatedAt = currentTime
        )

        db.collection("clothes")
            .add(clothingItem)
            .addOnSuccessListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Clothing added successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                btnSave.isEnabled = true
                Toast.makeText(this, "Database Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}