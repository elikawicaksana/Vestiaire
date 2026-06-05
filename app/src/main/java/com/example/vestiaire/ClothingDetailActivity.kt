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

class ClothingDetailActivity : AppCompatActivity() {

    private lateinit var ivDetailPreview: ImageView
    private lateinit var etDetailName: EditText
    private lateinit var etDetailColor: EditText
    private lateinit var tvDetailTitle: TextView

    // Variabel Baru untuk Teks Detail (Read Mode)
    private lateinit var tvDetailCategory: TextView
    private lateinit var tvDetailOccasion: TextView

    // Variabel Spinner (Edit Mode)
    private lateinit var spinnerDetailCategory: Spinner
    private lateinit var spinnerDetailOccasion: Spinner

    // Layout container untuk tombol
    private lateinit var layoutReadMode: LinearLayout
    private lateinit var layoutEditMode: LinearLayout

    // Tombol-tombol
    private lateinit var btnBack: Button
    private lateinit var btnEditMode: Button
    private lateinit var btnUpdate: Button
    private lateinit var btnCancel: Button

    private val db = FirebaseFirestore.getInstance()
    private var clothingId: String = ""

    // Backup data lama
    private var oldName = ""
    private var oldColor = ""
    private var oldCategory = ""
    private var oldOccasion = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_clothing_detail)

        // Hubungkan ke XML
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

        setupSpinners()

        // 1. TANGKAP DATA AWAL DARI KATALOG
        clothingId = intent.getStringExtra("CLOTHING_ID") ?: ""
        oldName = intent.getStringExtra("CLOTHING_NAME") ?: ""
        oldColor = intent.getStringExtra("CLOTHING_COLOR") ?: ""
        oldCategory = intent.getStringExtra("CLOTHING_CATEGORY") ?: ""
        oldOccasion = intent.getStringExtra("CLOTHING_OCCASION") ?: ""
        val imageUrl = intent.getStringExtra("CLOTHING_IMAGE_URL") ?: ""

        // Set data awal ke komponen layar
        resetFieldsToOldData()

        Glide.with(this)
            .load(imageUrl)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .into(ivDetailPreview)

        // 2. NAVIGASI TOMBOL
        btnBack.setOnClickListener { finish() }

        btnEditMode.setOnClickListener {
            toggleEditMode(true) // Nyalakan Mode Edit (TextView sembunyi, Spinner muncul)
        }

        btnCancel.setOnClickListener {
            resetFieldsToOldData()
            toggleEditMode(false) // Kembalikan ke Mode Detail (TextView muncul, Spinner sembunyi)
            Toast.makeText(this, "Edit canceled", Toast.LENGTH_SHORT).show()
        }

        btnUpdate.setOnClickListener { updateClothingData() }
    }

    private fun setupSpinners() {
        val categories = arrayOf("Tops", "Bottoms", "Dresses", "Outerwear", "Shoes", "Accessories")
        spinnerDetailCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)

        val occasions = arrayOf("Casual", "Formal", "Sport", "Party", "Work")
        spinnerDetailOccasion.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, occasions)
    }

    // DISINI LOGIKA SWITCHING ANTARA TEXTVIEW DAN DROPDOWN JADI SANGAT RAPI!
    private fun toggleEditMode(isEditMode: Boolean) {
        etDetailName.isEnabled = isEditMode
        etDetailColor.isEnabled = isEditMode

        if (isEditMode) {
            tvDetailTitle.text = "Edit Clothing Info"

            // Sembunyikan TextView baca, Munculkan Dropdown Spinner
            tvDetailCategory.visibility = View.GONE
            spinnerDetailCategory.visibility = View.VISIBLE

            tvDetailOccasion.visibility = View.GONE
            spinnerDetailOccasion.visibility = View.VISIBLE

            layoutReadMode.visibility = View.GONE
            layoutEditMode.visibility = View.VISIBLE
        } else {
            tvDetailTitle.text = "Clothing Detail"

            // Munculkan TextView baca, Sembunyikan Dropdown Spinner
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

        // Pasang teks ke TextView Mode Baca
        tvDetailCategory.text = oldCategory
        tvDetailOccasion.text = oldOccasion

        // Set posisi default Spinner agar pas dengan data lama saat mode edit dibuka
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

        // Pop-up Konfirmasi
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

                        // Update backup data lama dengan data baru
                        oldName = name
                        oldColor = color
                        oldCategory = category
                        oldOccasion = occasion

                        resetFieldsToOldData() // Perbarui teks tampilan baca
                        toggleEditMode(false)  // Kunci kembali form dan sembunyikan dropdown
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to update: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("No", null)
            .show()
    }
}