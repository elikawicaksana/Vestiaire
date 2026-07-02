package com.example.vestiaire

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var etUsername: EditText
    private lateinit var etEmail: EditText
    private lateinit var tvName: TextView
    private lateinit var btnUpdate: Button
    private lateinit var btnLogout: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Firebase
        auth = FirebaseAuth.getInstance()

        // Komponen UI
        etUsername = findViewById(R.id.etUsername)
        etEmail = findViewById(R.id.etEmail)
        tvName = findViewById(R.id.tvName)

        btnUpdate = findViewById(R.id.btnUpdate)
        btnLogout = findViewById(R.id.btnLogout)

        // Tampilkan data user
        loadProfile()

        // Tombol update
        btnUpdate.setOnClickListener {
            updateProfile()
        }

        // Tombol logout
        btnLogout.setOnClickListener {
            logout()
        }
    }

    private fun loadProfile() {

        val currentUser = auth.currentUser ?: return

        // Email dari Firebase Auth
        etEmail.setText(currentUser.email)

        // Username dari Realtime Database
        FirebaseDatabase.getInstance()
            .getReference("users")
            .child(currentUser.uid)
            .child("username")
            .get()
            .addOnSuccessListener { snapshot ->

                val username =
                    snapshot.getValue(String::class.java)

                etUsername.setText(username)
                // 2. Tampilkan sebagai teks nama user biasa jika datanya tidak kosong
                if (!username.isNullOrEmpty()) {
                    tvName.text = "$username 👋"
                } else {
                    tvName.text = "User 👋" // Cadangan jika username di database kosong
                }
            }
    }

    private fun validateInput(): Boolean {

        val username =
            etUsername.text.toString().trim()

        val email =
            etEmail.text.toString().trim()

        if (username.isEmpty()) {
            etUsername.error = "Username wajib diisi"
            return false
        }

        if (email.isEmpty()) {
            etEmail.error = "Email wajib diisi"
            return false
        }

        return true
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

        // Update username
        FirebaseDatabase.getInstance()
            .getReference("users")
            .child(currentUser.uid)
            .child("username")
            .setValue(username)
            .addOnSuccessListener {
                // Langsung ubah teks nama di atas secara real-time
                tvName.text = "$username 👋"
            }

        // Update email
        currentUser.verifyBeforeUpdateEmail(newEmail)
            .addOnSuccessListener {

                loadProfile()

                Toast.makeText(
                    this,
                    "Verification email sent. Check your inbox.",
                    Toast.LENGTH_LONG
                ).show()

            }
            .addOnFailureListener {

                Toast.makeText(
                    this,
                    "Gagal: ${it.message}",
                    Toast.LENGTH_LONG
                ).show()

            }
    }

    private fun logout() {

        auth.signOut()

        val intent = Intent(
            this,
            LoginActivity::class.java
        )

        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish()
    }
}