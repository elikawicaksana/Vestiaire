package com.example.vestiaire

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.database.database

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        // Ambil instance database
        val database = Firebase.database

        // Reference node
        val myRef = database.getReference("message")

        // Simpan data
        myRef.setValue("Hello, World!")

        // Ambil data
        myRef.get().addOnSuccessListener {

            Log.d("FIREBASE", it.value.toString())

        }.addOnFailureListener {

            Log.e("FIREBASE", "Error", it)

        }
    }
}