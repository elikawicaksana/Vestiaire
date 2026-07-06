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
    private lateinit var tvName: TextView
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
        tvName = findViewById(R.id.tvName)
        btnUpdate = findViewById(R.id.btnUpdate)
        btnLogout = findViewById(R.id.btnLogout)

        // Hubungkan area menu navigasi bawah dari layout yang di-include
        val layoutMenuAdd = findViewById<LinearLayout>(R.id.layoutMenuAdd)
//        val layoutMenuProfile = findViewById<LinearLayout>(R.id.layoutMenuProfile)
        val layoutClothingCount = findViewById<com.google.android.material.card.MaterialCardView>(R.id.layoutClothingCount)

        // Tampilkan data user dari Firestore
        loadProfile()

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
            // Karena ini halaman profile, jika klik menu Add (+), dia akan pindah ke katalog utama / halaman add
            // Sesuai alur activity_main yang mengarahkan ke MainActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
            finish() // Menutup halaman profil agar tumpukan activity rapi
        }

        layoutClothingCount?.setOnClickListener {
            // Sudah berada di halaman profile, abaikan atau refresh data
            loadProfile()
        }
    }

    private fun loadProfile() {
        val currentUser = auth.currentUser ?: return

        // Email langsung dipasang dari Firebase Auth session awal
        etEmail.setText(currentUser.email)

        // Mengambil Username dari Cloud Firestore collection "users"
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

    private fun updateProfile() {
        val currentUser = auth.currentUser ?: return

        val username = etUsername.text.toString().trim()
        val newEmail = etEmail.text.toString().trim()

        if (username.isEmpty()) {
            etUsername.error = "Username wajib diisi"
            return
        }

        if (newEmail.isEmpty()) {
            etEmail.error = "Email wajib diisi"
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            etEmail.error = "Format email tidak valid"
            return
        }

        // Update username di Cloud Firestore
        val userMap = hashMapOf<String, Any>("username" to username)
        db.collection("users").document(currentUser.uid)
            .update(userMap)
            .addOnSuccessListener {
                tvName.text = "$username 👋"
                Toast.makeText(this, "Username updated!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal update username: ${e.message}", Toast.LENGTH_SHORT).show()
            }

        // Update email dengan verifikasi link (Fitur Firebase Auth)
        if (newEmail != currentUser.email) {
            currentUser.verifyBeforeUpdateEmail(newEmail)
                .addOnSuccessListener {
                    // Email benar-benar berhasil terkirim dari sisi server Firebase
                    loadProfile()
                    Toast.makeText(
                        this,
                        "Verification email sent to $newEmail. Please check your inbox and spam folder.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                .addOnFailureListener { e ->
                    // Menangkap error asli dari Firebase (Misal: Butuh login ulang / re-auth)
                    Log.e("FIREBASE_AUTH", "Gagal verifikasi email baru", e)

                    if (e.message?.contains("RECENTLY_LOGGED_IN", ignoreCase = true) == true) {
                        Toast.makeText(
                            this,
                            "Keamanan: Silakan logout lalu login kembali sebelum mengubah email.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            this,
                            "Gagal mengirim email verifikasi: ${e.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
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