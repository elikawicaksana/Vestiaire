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

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        rvClothing = findViewById(R.id.rvClothing)

        val layoutMenuCatalog = findViewById<LinearLayout>(R.id.layoutMenuCatalog)
        val layoutMenuAdd = findViewById<LinearLayout>(R.id.layoutMenuAdd)
        val layoutMenuProfile = findViewById<LinearLayout>(R.id.layoutMenuProfile)
        val btnCatalog = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCatalog)
        val tvCatalogLabel = findViewById<TextView>(R.id.tvCatalogLabel)

        /* * Mengubah warna ikon dan gaya teks secara dinamis melalui kode
         * untuk menandakan bahwa menu "Catalog" sedang dalam status aktif/dipilih.
         */
        val activeColor = androidx.core.content.ContextCompat.getColor(this, R.color.vestiaire_text)
        btnCatalog?.setIconTintResource(R.color.vestiaire_text)
        tvCatalogLabel?.setTextColor(activeColor)
        tvCatalogLabel?.setTypeface(null, android.graphics.Typeface.BOLD)

        rvClothing.layoutManager = LinearLayoutManager(this)
        clothingList = arrayListOf()
        adapter = ClothingAdapter(clothingList)
        rvClothing.adapter = adapter

        fetchClothingData()

        layoutMenuAdd?.setOnClickListener {
            startActivity(Intent(this, AddClothingActivity::class.java))
            overridePendingTransition(0, 0)
        }

        /* * finish() digunakan untuk menghancurkan MainActivity dari tumpukan (stack) aktivitas,
         * sehingga ketika pengguna menekan tombol kembali di ProfileActivity,
         * aplikasi langsung keluar dan tidak kembali ke halaman utama yang kosong.
         */
        layoutMenuProfile?.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }
    }

    /* * onResume dipanggil setiap kali aktivitas berinteraksi kembali dengan pengguna.
     * Penempatan fetchClothingData di sini memastikan data katalog selalu diperbarui otomatis
     * setelah pengguna menambahkan pakaian baru dari AddClothingActivity.
     */
    override fun onResume() {
        super.onResume()
        fetchClothingData()
    }

    private fun fetchClothingData() {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            /* * Melakukan query ke Firestore untuk mengambil dokumen dari koleksi "clothes"
             * yang field "userId"-nya cocok dengan UID pengguna yang sedang login saat ini.
             */
            db.collection("clothes")
                .whereEqualTo("userId", currentUser.uid)
                .get()
                .addOnSuccessListener { result ->
                    // Membersihkan list lama untuk mencegah duplikasi data saat memuat ulang
                    clothingList.clear()

                    /* * Memetakan setiap dokumen Firestore menjadi objek beralur tipe data "ClothingItem".
                     * document.id diambil secara manual karena ID dokumen Firestore
                     * berada di luar struktur bodi field data dokumen itu sendiri.
                     */
                    for (document in result) {
                        val clothingItem = document.toObject(ClothingItem::class.java)
                        clothingItem.id = document.id
                        clothingList.add(clothingItem)
                    }

                    // Memaksa RecyclerView untuk menggambar ulang komponen UI sesuai perubahan data terbaru
                    adapter.notifyDataSetChanged()
                }
                .addOnFailureListener { exception ->
                    Log.w("FIRESTORE", "Error getting documents.", exception)
                    Toast.makeText(this, "Failed to fetch data", Toast.LENGTH_SHORT).show()
                }
        }
    }
}