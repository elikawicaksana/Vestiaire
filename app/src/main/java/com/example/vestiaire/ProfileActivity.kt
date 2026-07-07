package com.example.vestiaire

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var etUsername: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var tvName: TextView
    private lateinit var tvClothingCount: TextView
    private lateinit var btnUpdate: Button
    private lateinit var btnLogout: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        etUsername = findViewById(R.id.etUsername)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        tvName = findViewById(R.id.tvName)
        tvClothingCount = findViewById(R.id.tvClothingCount)
        btnUpdate = findViewById(R.id.btnUpdate)
        btnLogout = findViewById(R.id.btnLogout)

        val layoutMenuCatalog = findViewById<LinearLayout>(R.id.layoutMenuCatalog)
        val layoutMenuAdd = findViewById<LinearLayout>(R.id.layoutMenuAdd)
        val layoutMenuProfile = findViewById<LinearLayout>(R.id.layoutMenuProfile)
        val layoutClothingCount = findViewById<MaterialCardView>(R.id.layoutClothingCount)

        val btnProfile = findViewById<MaterialButton>(R.id.btnProfile)
        val tvProfileLabel = findViewById<TextView>(R.id.tvProfileLabel)

        /*
         * Mengubah state visual komponen navbar secara dinamis melalui runtime
         * untuk merepresentasikan status aktif halaman profil.
         */
        val activeColor = ContextCompat.getColor(this, R.color.vestiaire_text)
        btnProfile?.setIconTintResource(R.color.vestiaire_text)
        tvProfileLabel?.setTextColor(activeColor)
        tvProfileLabel?.setTypeface(null, Typeface.BOLD)

        loadProfile()
        fetchClothingCount()

        btnUpdate.setOnClickListener {
            updateProfile()
        }

        btnLogout.setOnClickListener {
            logout()
        }

        layoutMenuCatalog?.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }

        layoutMenuAdd?.setOnClickListener {
            startActivity(Intent(this, AddClothingActivity::class.java))
            overridePendingTransition(0, 0)
        }

        layoutClothingCount?.setOnClickListener {
            loadProfile()
            fetchClothingCount()
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
                        tvName.text = username
                    } else {
                        tvName.text = "User"
                    }
                } else {
                    tvName.text = "User"
                }
            }
            .addOnFailureListener { e ->
                Log.w("FIRESTORE", "Gagal memuat profil", e)
                Toast.makeText(this, "Gagal memuat profil dari database", Toast.LENGTH_SHORT).show()
            }
    }

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

        if (username.isEmpty() || newEmail.isEmpty()) {
            Toast.makeText(this, "Isi field yang kosong", Toast.LENGTH_SHORT).show()
            return
        }

        /*
         * Menggunakan SetOptions.merge() untuk memperbarui field tertentu (username)
         * tanpa menimpa atau menghapus field lain yang sudah ada pada dokumen user terkait.
         */
        val userMap = hashMapOf<String, Any>("username" to username)
        db.collection("users").document(currentUser.uid)
            .set(userMap, SetOptions.merge())
            .addOnSuccessListener { tvName.text = username }

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

        if (newEmail != currentUser.email) {
            /*
             * Firebase Auth mendeteksi perubahan email sebagai tindakan sensitif. Jika token sesi
             * kedaluwarsa (requires-recent-login), pengguna diwajibkan untuk re-autentikasi (logout-login ulang).
             */
            currentUser.verifyBeforeUpdateEmail(newEmail)
                .addOnSuccessListener {
                    Toast.makeText(this, "Email verifikasi dikirim ke $newEmail. Cek inbox/spam!", Toast.LENGTH_LONG).show()
                }
                .addOnFailureListener { e ->
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