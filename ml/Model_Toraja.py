"""
Merekonstruksi arsitektur model MobileNetV2 + classifier head PERSIS seperti
STEP 7 pada notebook training (Kaggle/Colab), lalu menampilkan model.summary()
beserta seluruh dependency/library yang digunakan.

Script ini TIDAK butuh dataset, TIDAK training, dan TIDAK butuh file
model_best_final.keras -- arsitekturnya dibangun ulang langsung dari kode,
karena summary() hanya bergantung pada struktur layer, bukan hasil training.

Cara pakai (VS Code, tombol Run Python File ▷):
    Pastikan interpreter yang dipakai adalah Python 3.10 (yang ada TensorFlow-nya).
    Klik ▷, atau jalankan manual:
        python build_and_show_architecture.py
"""

# ==============================================================================
# STEP 1: IMPORT SEMUA LIBRARY YANG DIBUTUHKAN
# ==============================================================================
import sys
import platform

# Console Windows (PowerShell/cmd) default-nya cp1252, tidak bisa menampilkan
# karakter box-drawing (--, dst.) yang dipakai tabel model.summary() Keras 3.
# Paksa stdout pakai UTF-8 supaya tidak UnicodeEncodeError.
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

import tensorflow as tf
from tensorflow.keras import layers

# ==============================================================================
# STEP 2: PARAMETER UTAMA (harus sama dengan konfigurasi training)
# ==============================================================================
INPUT_SIZE = 320
INPUT_SHAPE = (INPUT_SIZE, INPUT_SIZE, 3)

# Sesuai skripsi & notebook training: dataset_tidak_terdeteksi sudah dihapus
# dari training set, sehingga classifier head dilatih untuk 30 kelas motif.
# (Model .tflite hasil deployment di app Android punya 31 output -- 30 motif
#  + 1 kelas "tidak terdeteksi" -- karena ditambahkan di tahap lain / versi lain.
#  Untuk merekonstruksi summary yang SAMA seperti screenshot, dipakai 30.)
JUMLAH_KELAS = 30

# Bobot ImageNet butuh koneksi internet untuk diunduh (~14MB) dan TIDAK
# mengubah bentuk/summary model sama sekali -- hanya isi bobotnya.
# Set True kalau memang mau replikasi persis proses training (unduh bobot asli).
GUNAKAN_BOBOT_IMAGENET = False

# ==============================================================================
# STEP 3: MEMBANGUN MODEL (identik dengan STEP 7 notebook training)
# ==============================================================================
print("Membangun arsitektur model...\n")

base_model = tf.keras.applications.MobileNetV2(
    input_shape=INPUT_SHAPE,
    include_top=False,
    weights="imagenet" if GUNAKAN_BOBOT_IMAGENET else None,
)

inputs = tf.keras.Input(shape=INPUT_SHAPE)
x = layers.Rescaling(1.0 / 127.5, offset=-1.0)(inputs)
x = base_model(x)
x = layers.GlobalAveragePooling2D()(x)
x = layers.BatchNormalization()(x)
x = layers.Dropout(0.5)(x)
x = layers.Dense(512, activation="relu")(x)
x = layers.BatchNormalization()(x)
x = layers.Dropout(0.4)(x)
outputs = layers.Dense(JUMLAH_KELAS, activation="softmax")(x)

model = tf.keras.Model(inputs, outputs)

# ==============================================================================
# STEP 4: TAMPILKAN ARSITEKTUR MODEL (model.summary())
# ==============================================================================
# expand_nested=True membongkar submodel MobileNetV2 (yang biasanya diringkas
# jadi satu baris "Functional") sehingga seluruh 154 layer internalnya
# (conv, depthwise conv, batch norm, dst.) ikut tercetak satu per satu.
print("=" * 70)
print("ARSITEKTUR MODEL LENGKAP (classifier head + seluruh layer MobileNetV2)")
print("=" * 70)
model.summary(expand_nested=True, show_trainable=True, line_length=110)

# Tabel lengkap ini panjang (150+ baris) sehingga juga disimpan ke file teks
# agar mudah dibuka/scroll terpisah dari output terminal.
import io
import contextlib

buffer = io.StringIO()
with contextlib.redirect_stdout(buffer):
    model.summary(expand_nested=True, show_trainable=True, line_length=110)

output_txt_path = "arsitektur_model_lengkap.txt"
with open(output_txt_path, "w", encoding="utf-8") as f:
    f.write(buffer.getvalue())

print(f"\n(Tabel lengkap juga disimpan ke: {output_txt_path})")

total_params = model.count_params()
trainable = sum(tf.keras.backend.count_params(w) for w in model.trainable_weights)
non_trainable = total_params - trainable

print()
print(f"Total params      : {total_params:,}")
print(f"Trainable params  : {trainable:,}")
print(f"Non-trainable     : {non_trainable:,}")

# ==============================================================================
# STEP 5: DEPENDENCY / LIBRARY YANG DIGUNAKAN
# ==============================================================================
print()
print("=" * 70)
print("DEPENDENCY / LIBRARY YANG DIGUNAKAN")
print("=" * 70)
print(f"Python       : {platform.python_version()}")
print(f"TensorFlow   : {tf.__version__}")
print(f"Keras        : {tf.keras.__version__}")

for pkg_name in ("numpy", "pandas", "seaborn", "matplotlib", "sklearn", "h5py", "gdown"):
    try:
        mod = __import__(pkg_name)
        version = getattr(mod, "__version__", "?")
        print(f"{pkg_name:<12} : {version}")
    except ImportError:
        print(f"{pkg_name:<12} : (tidak terpasang)")

print()
print("Selesai.")
