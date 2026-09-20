package com.example.torajacarvingai

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class FavoritActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorit)

        val btnBack = findViewById<android.widget.ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        tampilkanDataFavorit()
    }

    private fun tampilkanDataFavorit() {
        val rvFavorit = findViewById<RecyclerView>(R.id.rvFavorit)
        val tvKosong = findViewById<TextView>(R.id.tvKosong)
        
        rvFavorit.layoutManager = LinearLayoutManager(this)

        val favoritesManager = FavoritesManager(this)
        val favoriteIds = favoritesManager.getFavoriteIds()

        // Bangun dari labels + motif_catalog AKTIF (bukan SumberData.listMotif langsung),
        // lalu filter berdasarkan ID (label mentah) yang tersimpan di favorites.
        val daftarFavorit = MotifDisplayBuilder.buildCatalogList(this).filter { favoriteIds.contains(it.idModel) }

        if (daftarFavorit.isEmpty()) {
            tvKosong.visibility = View.VISIBLE
            rvFavorit.visibility = View.GONE
        } else {
            tvKosong.visibility = View.GONE
            rvFavorit.visibility = View.VISIBLE
            
            rvFavorit.adapter = KatalogAdapter(daftarFavorit) { motifTerpilih ->
                showMotifInfoDialog(motifTerpilih)
            }
        }
    }

    private fun showMotifInfoDialog(motif: MotifToraja) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_motif_info)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val tvTitle = dialog.findViewById<TextView>(R.id.tvDialogTitle)
        val tvKategori = dialog.findViewById<TextView>(R.id.tvDialogKategori)
        val ivDialogMotif = dialog.findViewById<ImageView>(R.id.ivDialogMotif)
        val tvDeskripsi = dialog.findViewById<TextView>(R.id.tvDialogDeskripsi)
        val btnDetail = dialog.findViewById<Button>(R.id.btnDialogDetail)
        val btnTutup = dialog.findViewById<Button>(R.id.btnDialogTutup)

        tvTitle.text = motif.namaLayar
        // Kategori kosong (belum diisi admin): sembunyikan, jangan tampilkan label tanpa isi.
        if (motif.category.isBlank()) {
            tvKategori.text = ""
            tvKategori.visibility = View.GONE
        } else {
            tvKategori.text = motif.category
            tvKategori.visibility = View.VISIBLE
        }
        loadMotifImage(ivDialogMotif, motif.imageUrl, motif.gambar)
        tvDeskripsi.text = motif.deskripsiLengkap

        btnDetail.visibility = View.GONE

        btnTutup.text = "Tutup"
        btnTutup.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }
}
