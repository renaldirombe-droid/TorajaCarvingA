package com.example.torajacarvingai

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView

class MainMenuActivity : AppCompatActivity() {

    private lateinit var tvKatalogBadge: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        // 1. Hubungkan ID dari XML ke Kotlin
        val btnPindai = findViewById<MaterialCardView>(R.id.btnMenuPindai)
        val btnKatalog = findViewById<MaterialCardView>(R.id.btnMenuKatalog)
        val btnRiwayat = findViewById<MaterialCardView>(R.id.btnMenuRiwayat)
        val btnTentang = findViewById<MaterialCardView>(R.id.btnMenuTentang)
        val btnFavorit = findViewById<MaterialCardView>(R.id.btnMenuFavorit)
        tvKatalogBadge = findViewById(R.id.tvKatalogBadge)

        // 2. Beri perintah saat tombol "Mulai Klasifikasi Motif" diklik
        btnPindai.setOnClickListener {
            // Pindah ke halaman Scanner / Kamera (MainActivity milikmu yang sudah ada)
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        // 3. Tombol lainnya (Karena belum kita buat halamannya, kita beri pesan Toast dulu)
        btnKatalog.setOnClickListener {
            val intent = Intent(this, KatalogActivity::class.java)
            startActivity(intent)
        }

        // UBAH BAGIAN INI
        btnRiwayat.setOnClickListener {
            val intent = Intent(this, RiwayatActivity::class.java)
            startActivity(intent)
        }

        btnTentang.setOnClickListener {
            val intent = Intent(this, AboutActivity::class.java)
            startActivity(intent)
        }

        btnFavorit.setOnClickListener {
            val intent = Intent(this, FavoritActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        // Dihitung ulang tiap kali kembali ke menu utama, supaya langsung ikut jumlah
        // motif aktif yang sebenarnya (bisa berubah kalau ada update online baru).
        val count = MotifDisplayBuilder.buildCatalogList(this).size
        tvKatalogBadge.text = "$count Motif"
    }
}