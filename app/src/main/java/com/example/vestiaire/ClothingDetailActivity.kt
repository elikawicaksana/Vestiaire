package com.example.vestiaire

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class ClothingDetailActivity : AppCompatActivity() {

    private lateinit var ivDetailPreview: ImageView
    private lateinit var etDetailName: EditText
    private lateinit var etDetailColor: EditText
    private lateinit var tvDetailTitle: TextView

    private lateinit var tvDetailCategory: TextView
    private lateinit var tvDetailOccasion: TextView
    private lateinit var spinnerDetailCategory: Spinner
    private lateinit var spinnerDetailOccasion: Spinner

    private lateinit var layoutReadMode: LinearLayout
    private lateinit var layoutEditMode: LinearLayout

    private lateinit var btnBack: Button
    private lateinit var btnEditMode: Button
    private lateinit var btnUpdate: Button
    private lateinit var btnCancel: Button
    private lateinit var btnDelete: Button

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private var clothingId: String = ""
    private var imageUrl: String = ""

    private var oldName = ""
    private var oldColor = ""
    private var oldCategory = ""
    private var oldOccasion = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_clothing_detail)

        ivDetailPreview = findViewById(R.id.ivDetailPreview)
        etDetailName = findViewById(R.id.etDetailName)
        etDetailColor = findViewById(R.id.etDetailColor)
        tvDetailTitle = findViewById(R.id.tvDetailTitle)

        tvDetailCategory = findViewById(R.id.tvDetailCategory)
        tvDetailOccasion = findViewById(R.id.tvDetailOccasion)

        spinnerDetailCategory = findViewById(R.id.spinnerDetailCategory)
        spinnerDetailOccasion = findViewById(R.id.spinnerDetailOccasion)

        layoutReadMode = findViewById(R.id.layoutReadMode)
        layoutEditMode = findViewById(R.id.layoutEditMode)

        btnBack = findViewById(R.id.btnBack)
        btnEditMode = findViewById(R.id.btnEditMode)
        btnUpdate = findViewById(R.id.btnUpdate)
        btnCancel = findViewById(R.id.btnCancel)
        btnDelete = findViewById(R.id.btnDelete)

        setupSpinners()

        clothingId = intent.getStringExtra("CLOTHING_ID") ?: ""
        oldName = intent.getStringExtra("CLOTHING_NAME") ?: ""
        oldColor = intent.getStringExtra("CLOTHING_COLOR") ?: ""
        oldCategory = intent.getStringExtra("CLOTHING_CATEGORY") ?: ""
        oldOccasion = intent.getStringExtra("CLOTHING_OCCASION") ?: ""
        imageUrl = intent.getStringExtra("CLOTHING_IMAGE_URL") ?: ""

        resetFieldsToOldData()

        Glide.with(this)
            .load(imageUrl)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .into(ivDetailPreview)

        btnBack.setOnClickListener { finish() }
        btnEditMode.setOnClickListener { toggleEditMode(true) }
        btnCancel.setOnClickListener {
            resetFieldsToOldData()
            toggleEditMode(false)
        }
        btnUpdate.setOnClickListener { updateClothingData() }

        btnDelete.setOnClickListener { confirmDelete() }
    }

    private fun setupSpinners() {
        val categories = arrayOf("Tops", "Bottoms", "Dresses", "Outerwear", "Shoes", "Accessories")
        spinnerDetailCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)

        val occasions = arrayOf("Casual", "Formal", "Sport", "Party", "Work")
        spinnerDetailOccasion.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, occasions)
    }

    private fun toggleEditMode(isEditMode: Boolean) {
        etDetailName.isEnabled = isEditMode
        etDetailColor.isEnabled = isEditMode

        if (isEditMode) {
            tvDetailTitle.text = "Edit Clothing Info"
            tvDetailCategory.visibility = View.GONE
            spinnerDetailCategory.visibility = View.VISIBLE
            tvDetailOccasion.visibility = View.GONE
            spinnerDetailOccasion.visibility = View.VISIBLE
            layoutReadMode.visibility = View.GONE
            layoutEditMode.visibility = View.VISIBLE
        } else {
            tvDetailTitle.text = "Clothing Detail"
            tvDetailCategory.visibility = View.VISIBLE
            spinnerDetailCategory.visibility = View.GONE
            tvDetailOccasion.visibility = View.VISIBLE
            spinnerDetailOccasion.visibility = View.GONE
            layoutReadMode.visibility = View.VISIBLE
            layoutEditMode.visibility = View.GONE
        }
    }

    private fun resetFieldsToOldData() {
        etDetailName.setText(oldName)
        etDetailColor.setText(oldColor)
        tvDetailCategory.text = oldCategory
        tvDetailOccasion.text = oldOccasion

        val catAdapter = spinnerDetailCategory.adapter as ArrayAdapter<String>
        val catPos = catAdapter.getPosition(oldCategory)
        if (catPos >= 0) spinnerDetailCategory.setSelection(catPos)

        val occAdapter = spinnerDetailOccasion.adapter as ArrayAdapter<String>
        val occPos = occAdapter.getPosition(oldOccasion)
        if (occPos >= 0) spinnerDetailOccasion.setSelection(occPos)
    }

    private fun updateClothingData() {
        val name = etDetailName.text.toString().trim()
        val color = etDetailColor.text.toString().trim()
        val category = spinnerDetailCategory.selectedItem.toString()
        val occasion = spinnerDetailOccasion.selectedItem.toString()

        if (name.isEmpty() || color.isEmpty()) {
            Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Confirm Changes")
            .setMessage("Are you sure you want to update this clothing info?")
            .setPositiveButton("Yes") { _, _ ->
                val updatedData = mapOf(
                    "name" to name,
                    "color" to color,
                    "category" to category,
                    "occasion" to occasion,
                    "updatedAt" to Timestamp.now()
                )

                db.collection("clothes").document(clothingId)
                    .update(updatedData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Changes saved successfully!", Toast.LENGTH_SHORT).show()
                        oldName = name
                        oldColor = color
                        oldCategory = category
                        oldOccasion = occasion
                        resetFieldsToOldData()
                        toggleEditMode(false)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to update: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun confirmDelete() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Clothing")
            .setMessage("Are you sure you want to delete this item? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteClothing()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteClothing() {
        db.collection("clothes").document(clothingId)
            .delete()
            .addOnSuccessListener {
                if (imageUrl.isNotEmpty()) {
                    val imageRef = storage.getReferenceFromUrl(imageUrl)
                    imageRef.delete().addOnSuccessListener {
                        Toast.makeText(this, "Item & Image deleted", Toast.LENGTH_SHORT).show()
                        finish()
                    }.addOnFailureListener {
                        Toast.makeText(this, "Item deleted, but failed to remove image", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    Toast.makeText(this, "Item deleted successfully", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to delete: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}