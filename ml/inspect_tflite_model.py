"""
Menampilkan detail arsitektur model .tflite (format yang benar-benar
dipakai di aplikasi Android) beserta daftar library/dependency.

File .tflite tidak menyimpan nama layer Keras yang rapi (mis. "dense_1"),
karena saat konversi setiap layer Keras dipecah jadi operasi low-level
(CONV_2D, DEPTHWISE_CONV_2D, dll). Script ini menampilkan level operasi
tersebut sebagai gantinya, plus input/output tensor model.

Cara pakai:
    python inspect_tflite_model.py "app/src/main/assets/model_best_final_float32 (1).tflite"
"""

import os
import sys
import platform

# Dipakai kalau script dijalankan tanpa argumen (mis. lewat tombol Run ▷ di VS Code).
DEFAULT_MODEL_PATH = os.path.join(
    os.path.dirname(os.path.abspath(__file__)),
    "app", "src", "main", "assets", "model_best_final_float32 (1).tflite",
)


def print_dependencies():
    print("=" * 60)
    print("DEPENDENCY / LIBRARY YANG DIGUNAKAN")
    print("=" * 60)
    print(f"Python      : {platform.python_version()}")

    try:
        import tensorflow as tf
        print(f"TensorFlow  : {tf.__version__}")
    except ImportError:
        print("TensorFlow  : (tidak terpasang)")

    try:
        import numpy as np
        print(f"NumPy       : {np.__version__}")
    except ImportError:
        pass

    print()


def print_tflite_details(model_path: str):
    import tensorflow as tf

    print("=" * 60)
    print(f"DETAIL MODEL TFLITE: {model_path}")
    print("=" * 60)

    interpreter = tf.lite.Interpreter(model_path=model_path)
    interpreter.allocate_tensors()

    print("\n--- INPUT TENSOR ---")
    for d in interpreter.get_input_details():
        print(f"  name  : {d['name']}")
        print(f"  shape : {d['shape']}")
        print(f"  dtype : {d['dtype'].__name__}")

    print("\n--- OUTPUT TENSOR ---")
    for d in interpreter.get_output_details():
        print(f"  name  : {d['name']}")
        print(f"  shape : {d['shape']}")
        print(f"  dtype : {d['dtype'].__name__}")

    tensor_details = interpreter.get_tensor_details()
    print(f"\n--- RINGKASAN ---")
    print(f"Total tensor internal : {len(tensor_details)}")
    print("Catatan: jumlah parameter (spt di model.summary() Keras) TIDAK bisa")
    print("dihitung akurat dari .tflite -- nama tensor sudah berubah saat konversi.")
    print("Untuk angka Param # yang presisi, pakai show_model_summary.py dengan")
    print("file model asli (.h5 / .keras) dari hasil training di Colab.")

    print("\n--- DAFTAR OPERASI (LAYER LEVEL RENDAH) ---")
    ops = interpreter._get_ops_details()  # noqa: SLF001 (API internal, tapi paling praktis utk debug)
    for op in ops:
        print(f"  [{op['index']:>3}] {op['op_name']}")

    print(f"\nTotal operasi: {len(ops)}")


def main():
    if len(sys.argv) >= 2:
        model_path = sys.argv[1]
    else:
        # Tidak ada argumen (mis. di-run lewat tombol ▷ VS Code) -> pakai default.
        model_path = DEFAULT_MODEL_PATH
        print(f"(Tidak ada argumen, pakai default: {model_path})\n")

    print_dependencies()
    print_tflite_details(model_path)


if __name__ == "__main__":
    main()
