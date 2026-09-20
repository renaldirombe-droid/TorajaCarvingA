# TorajaCarvingAI

Aplikasi Android untuk **klasifikasi motif ukiran Toraja** dari foto menggunakan *on-device* **TensorFlow Lite** (arsitektur **MobileNetV2**). Foto motif (foto asli, dari kamera/gallery) diproses di perangkat → model menghasilkan label + confidence → aturan pasca-inferensi menyempurnakan hasil.

```
Foto ukiran (kamera / galeri, belum di-crop)
  -> preprocess 320x320 (resize + normalisasi)
  -> inferensi TFLite MobileNetV2 di perangkat
  -> confidence 3 label teratas
  -> kalibrasi pasca-inferensi (boost/penalti berdasarkan mode crop)
  -> label motif + persentase keyakinan
```

### Fitur

| Fitur | Keterangan |
|---|---|
| 🎨 Deteksi motif | Kamera & galeri, hasil real-time di perangkat (tanpa koneksi) |
| 🗂️ Katalog motif | Katalog lengkap motif + kelas, pencarian cepat (`MotifCatalogBridge`) |
| ⭐ Favorit | Simpan motif favorit (`FavoritesManager`) |
| 🕘 Riwayat | Riwayat deteksi tersimpan lokal (`HistoryManager`) |
| 📦 Update model | Unduh & manajemen model dari server (`UpdateManager` / `UpdateModels`) |
| 🎬 Onboarding | Panduan awal penggunaan aplikasi |

### Struktur folder

```
TorajaCarvingAI/
├── app/                  # Aplikasi Android (Kotlin, TFLite)
├── ml/                   # Skrip training & inspeksi model (Python)
│   ├── Model_Toraja.py           # training MobileNetV2
│   ├── inspect_tflite_model.py   # inspeksi TFLite
│   ├── show_model_summary.py     # ringkasan arsitektur
│   └── arsitektur_model_lengkap.txt
├── web/                  # Web portofolio (sumber, single-file)
└── docs/                 # GitHub Pages (publish dari folder ini)
    └── desain/           # ERD, use case diagram, dump UI
```

### Setup (Android)

1. Buka folder ini di **Android Studio** (versi terbaru, JDK 17+).
2. Pastikan `local.properties` (sdk.dir) dan `keystore.properties` sudah ada — keduanya **tidak** di-commit (lihat `.gitignore`).
3. Model TFLite sudah tersedia di `app/src/main/assets/` (float16 & float32), tidak perlu unduh manual.
4. Build & run — **Run ▶** → pilih emulator/device.

### Skrip ML

```
pip install tensorflow pandas scikit-learn   # Python 3.10+
python ml/Model_Toraja.py                     # training
python ml/inspect_tflite_model.py             # cek input/output model .tflite
python ml/show_model_summary.py               # ringkasan arsitektur
```

### Web portofolio

Portofolio single-file (HTML+CSS+JS, semua gambar & sertifikat tertanam):

- Live: **https://renaldirombe-droid.github.io/TorajaCarvingA/**
- Sumber: `web/index.html`, salinan publish: `docs/index.html`

### Teknologi

- **Android** — Kotlin, Jetpack (Room, Preferences, ViewBinding)
- **TensorFlow Lite** — MobileNetV2 (float16/float32), input 320×320
- **Python** — skrip training & inspeksi model
- **GitHub Pages** — hosting web portofolio