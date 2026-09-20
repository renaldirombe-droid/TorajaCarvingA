package com.example.torajacarvingai

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class RiwayatActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_riwayat)

        val rvRiwayat = findViewById<RecyclerView>(R.id.rvRiwayat)
        rvRiwayat.layoutManager = LinearLayoutManager(this)

        val btnBack = findViewById<android.widget.ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener { finish() }

        // Ambil data dari penyimpanan lokal
        val dataRiwayat = HistoryManager(this).getHistory()
        val catalog = ModelBundleManager(this).loadActiveCatalog()

        rvRiwayat.adapter = RiwayatAdapter(dataRiwayat, catalog)
    }
}

class RiwayatAdapter(
    private val list: List<HistoryModel>,
    private val catalog: Map<String, MotifInfo>
) : RecyclerView.Adapter<RiwayatAdapter.ViewHolder>() {
    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val tvNama = v.findViewById<TextView>(R.id.tvNamaRiwayat)
        val tvTanggal = v.findViewById<TextView>(R.id.tvTanggalRiwayat)
        val tvNomor = v.findViewById<TextView>(R.id.tvNomor)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_riwayat, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = list[position]
        // Entri lama (sebelum field "label" ada) fallback resolve pakai "nama" yang tersimpan.
        val resolveKey = entry.label ?: entry.nama
        val resolved = MotifCatalogResolver.resolve(resolveKey, catalog)
        // Kalau resolve gagal total (label kosong/tak dikenal sama sekali), tetap pakai nama asli tersimpan.
        holder.tvNama.text = if (resolved.source == MotifDataSource.FALLBACK_LABEL) entry.nama else resolved.displayName
        holder.tvTanggal.text = entry.tanggal
        holder.tvNomor.text = "${position + 1}"
    }

    override fun getItemCount() = list.size
}