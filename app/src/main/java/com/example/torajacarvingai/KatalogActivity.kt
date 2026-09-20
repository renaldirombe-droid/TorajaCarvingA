package com.example.torajacarvingai

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class KatalogActivity : AppCompatActivity() {

    private lateinit var allMotifs: List<MotifToraja>
    private lateinit var adapter: KatalogAdapter
    private lateinit var tvEmptyState: TextView
    private lateinit var rvKatalog: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_katalog)

        rvKatalog = findViewById(R.id.rvKatalog)
        rvKatalog.layoutManager = LinearLayoutManager(this)

        tvEmptyState = findViewById(R.id.tvEmptyState)

        val btnBack = findViewById<android.widget.ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener { finish() }

        // Bangun daftar dari labels + motif_catalog AKTIF (online jika ada, fallback assets),
        // bukan hardcode SumberData.kt, supaya motif baru dari update online ikut muncul.
        allMotifs = MotifDisplayBuilder.buildCatalogList(this)

        adapter = KatalogAdapter(allMotifs) { motifTerpilih ->
            showMotifInfoDialog(motifTerpilih)
        }
        rvKatalog.adapter = adapter

        // Setup Search
        val etSearch = findViewById<EditText>(R.id.etSearch)
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterList(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun normalizeForSearch(text: String): String {
        return text.lowercase()
            .replace(Regex("[_'\"()\\-]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun compactSearch(text: String): String {
        return normalizeForSearch(text).replace(" ", "")
    }

    private fun filterList(query: String) {
        val normalizedQuery = normalizeForSearch(query)
        val compactQuery = compactSearch(query)

        if (normalizedQuery.isEmpty()) {
            adapter.updateData(allMotifs)
            rvKatalog.visibility = View.VISIBLE
            tvEmptyState.visibility = View.GONE
            return
        }

        val filtered = allMotifs.filter { motif ->
            val normalizedTitle = normalizeForSearch(motif.namaLayar)
            val compactTitle = compactSearch(motif.namaLayar)

            val normalizedId = normalizeForSearch(motif.idModel)
            val compactId = compactSearch(motif.idModel)

            val normalizedDesc = normalizeForSearch(motif.deskripsiLengkap)
            val compactDesc = compactSearch(motif.deskripsiLengkap)

            val normalizedShortDesc = normalizeForSearch(motif.deskripsiSingkat)
            val compactShortDesc = compactSearch(motif.deskripsiSingkat)

            val aliasMatches = motif.aliases.any { alias ->
                val normalizedAlias = normalizeForSearch(alias)
                val compactAlias = compactSearch(alias)
                normalizedAlias.contains(normalizedQuery) || compactAlias.contains(compactQuery)
            }

            normalizedTitle.contains(normalizedQuery) || compactTitle.contains(compactQuery) ||
                    normalizedId.contains(normalizedQuery) || compactId.contains(compactQuery) ||
                    normalizedDesc.contains(normalizedQuery) || compactDesc.contains(compactQuery) ||
                    normalizedShortDesc.contains(normalizedQuery) || compactShortDesc.contains(compactQuery) ||
                    aliasMatches
        }

        adapter.updateData(filtered)

        if (filtered.isEmpty()) {
            rvKatalog.visibility = View.GONE
            tvEmptyState.visibility = View.VISIBLE
        } else {
            rvKatalog.visibility = View.VISIBLE
            tvEmptyState.visibility = View.GONE
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

        // Langsung tampilkan teks LENGKAP di Katalog
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

        // Sembunyikan tombol kuning karena teksnya sudah lengkap
        btnDetail.visibility = View.GONE

        btnTutup.text = "Tutup"
        btnTutup.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }
    override fun onResume() {
        super.onResume()
        // Segarkan data adapter setiap kali kembali ke halaman ini
        val rvKatalog = findViewById<RecyclerView>(R.id.rvKatalog)
        rvKatalog.adapter?.notifyDataSetChanged()
    }
}

// ==========================================
// KELAS ADAPTER
// ==========================================
class KatalogAdapter(
    private var listMotif: List<MotifToraja>,
    private val onItemClick: (MotifToraja) -> Unit
) : RecyclerView.Adapter<KatalogAdapter.MotifViewHolder>() {

    fun updateData(newList: List<MotifToraja>) {
        this.listMotif = newList
        notifyDataSetChanged()
    }

    class MotifViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNama: TextView = itemView.findViewById(R.id.tvNamaMotif)
        val tvDeskripsi: TextView = itemView.findViewById(R.id.tvDeskripsiMotif)
        val ivGambar: ImageView = itemView.findViewById(R.id.ivGambarMotif)
        val btnFavorite: android.widget.ImageButton = itemView.findViewById(R.id.btnFavorite)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MotifViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_katalog, parent, false)
        return MotifViewHolder(view)
    }

    override fun onBindViewHolder(holder: MotifViewHolder, position: Int) {
        val motif = listMotif[position]
        holder.tvNama.text = motif.namaLayar
        // Tampilkan teks SINGKAT di kartu daftar
        holder.tvDeskripsi.text = motif.deskripsiSingkat
        loadMotifImage(holder.ivGambar, motif.imageUrl, motif.gambar)

        // Logika Favorit
        val favoritesManager = FavoritesManager(holder.itemView.context)
        val isFav = favoritesManager.isFavorite(motif.idModel)
        holder.btnFavorite.setImageResource(
            if (isFav) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border
        )
        
        holder.btnFavorite.setOnClickListener {
            val isNowFav = favoritesManager.toggleFavorite(motif.idModel)
            holder.btnFavorite.setImageResource(
                if (isNowFav) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border
            )
        }

        holder.itemView.setOnClickListener {
            onItemClick(motif)
        }
    }

    override fun getItemCount(): Int {
        return listMotif.size
    }
}