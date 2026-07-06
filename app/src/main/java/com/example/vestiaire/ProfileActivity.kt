package com.example.vestiaire

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var etUsername: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var tvName: TextView
    private lateinit var tvClothingCount: TextView // 1. Tambahan Deklarasi Variabel
    private lateinit var btnUpdate: Button
    private lateinit var btnLogout: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Inisialisasi Firebase Auth & Cloud Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Komponen UI Utama
        etUsername = findViewById(R.id.etUsername)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        tvName = findViewById(R.id.tvName)
        tvClothingCount = findViewById(R.id.tvClothingCount) // 2. Hubungkan ID XML
        btnUpdate = findViewById(R.id.btnUpdate)
        btnLogout = findViewById(R.id.btnLogout)

        // Hubungkan area menu navigasi bawah dari layout yang di-include
        val layoutMenuAdd = findViewById<LinearLayout>(R.id.layoutMenuAdd)
        val layoutClothingCount = findViewById<com.google.android.material.card.MaterialCardView>(R.id.layoutClothingCount)

        // Tampilkan data user & jumlah clothing dari Firestore
        loadProfile()
        fetchClothingCount() // 3. Jalankan fungsi hitung data

        // Tombol update
        btnUpdate.setOnClickListener {
            updateProfile()
        }

        // Tombol logout
        btnLogout.setOnClickListener {
            logout()
        }

        // Logika Navigasi Bawah
        layoutMenuAdd?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
            finish()
        }

        layoutClothingCount?.setOnClickListener {
            loadProfile()
            fetchClothingCount() // Segarkan hitungan data saat kotak disentuh
        }
    }

    private fun loadProfile() {
        val currentUser = auth.currentUser ?: return

        etEmail.setText(currentUser.email)

        db.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val username = document.getString("username")
                    etUsername.setText(username)

                    if (!username.isNullOrEmpty()) {
                        tvName.text = "$username 👋"
                    } else {
                        tvName.text = "User 👋"
                    }
                } else {
                    tvName.text = "User 👋"
                }
            }
            .addOnFailureListener { e ->
                Log.w("FIRESTORE", "Gagal memuat profil", e)
                Toast.makeText(this, "Gagal memuat profil dari database", Toast.LENGTH_SHORT).show()
            }
    }

    // 4. FUNGSI BARU: Mengambil dan Menampilkan Jumlah Pakaian User
    private fun fetchClothingCount() {
        val currentUser = auth.currentUser ?: return

        db.collection("clothes")
            .whereEqualTo("userId", currentUser.uid)
            .get()
            .addOnSuccessListener { result ->
                val count = result.size()
                tvClothingCount.text = count.toString()
            }
            .addOnFailureListener { e ->
                Log.e("FIRESTORE_COUNT", "Gagal menghitung jumlah pakaian", e)
                tvClothingCount.text = "-"
            }
    }

    private fun updateProfile() {
        val currentUser = auth.currentUser ?: return
        val username = etUsername.text.toString().trim()
        val newEmail = etEmail.text.toString().trim()
        val newPassword = etPassword.text.toString().trim()

        // 1. Validasi Dasar
        if (username.isEmpty() || newEmail.isEmpty()) {
            Toast.makeText(this, "Isi field yang kosong", Toast.LENGTH_SHORT).show()
            return
        }

        // 2. Update Username (Selalu bisa dilakukan)
        val userMap = hashMapOf<String, Any>("username" to username)
        db.collection("users").document(currentUser.uid)
            .set(userMap, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener { tvName.text = "$username 👋" }

        // 3. LOGIKA EMAIL & PASSWORD
        // Jika email berubah atau password diisi, kita harus pastikan Firebase tidak menolak

        // Update Password (Jika diisi)
        if (newPassword.isNotEmpty()) {
            if (newPassword.length < 6) {
                etPassword.error = "Minimal 6 karakter"
                return
            }
            currentUser.updatePassword(newPassword)
                .addOnSuccessListener {
                    etPassword.setText("")
                    Toast.makeText(this, "Password updated", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Gagal update password: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }

        // Update Email (Jika berubah)
        if (newEmail != currentUser.email) {
            // Penting: Firebase membutuhkan sesi yang sangat baru untuk update email
            currentUser.verifyBeforeUpdateEmail(newEmail)
                .addOnSuccessListener {
                    Toast.makeText(this, "Email verifikasi dikirim ke $newEmail. Cek inbox/spam!", Toast.LENGTH_LONG).show()
                }
                .addOnFailureListener { e ->
                    // Jika muncul error "requires-recent-login", artinya user harus Logout/Login dulu
                    if (e.message?.contains("requires-recent-login", ignoreCase = true) == true) {
                        Toast.makeText(this, "Sesi sudah kadaluarsa. Silakan Logout dan Login kembali untuk mengubah email.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }

    private fun logout() {
        auth.signOut()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}