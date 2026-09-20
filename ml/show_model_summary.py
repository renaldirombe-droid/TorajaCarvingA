"""
Menampilkan arsitektur model (persis seperti model.summary() Keras)
beserta daftar library/dependency yang digunakan.

Pakai file INI kalau kamu punya file model asli hasil training
(.h5 atau .keras) dari Google Colab -- bukan file .tflite.

Cara pakai:
    python show_model_summary.py "path/ke/model_best_final.h5"
    python show_model_summary.py "path/ke/model_best_final.keras"
"""

import sys
import platform


def print_dependencies():
    print("=" * 60)
    print("DEPENDENCY / LIBRARY YANG DIGUNAKAN")
    print("=" * 60)
    print(f"Python      : {platform.python_version()}")

    try:
        import tensorflow as tf
        print(f"TensorFlow  : {tf.__version__}")
        print(f"Keras       : {tf.keras.__version__}")
    except ImportError:
        print("TensorFlow  : (tidak terpasang)")

    try:
        import numpy as np
        print(f"NumPy       : {np.__version__}")
    except ImportError:
        pass

    try:
        import h5py
        print(f"h5py        : {h5py.__version__}")
    except ImportError:
        pass

    print()


def print_model_summary(model_path: str):
    import tensorflow as tf

    print("=" * 60)
    print(f"ARSITEKTUR MODEL: {model_path}")
    print("=" * 60)

    model = tf.keras.models.load_model(model_path, compile=False)
    model.summary()

    total_params = model.count_params()
    trainable = sum(tf.keras.backend.count_params(w) for w in model.trainable_weights)
    non_trainable = total_params - trainable

    print()
    print(f"Total params      : {total_params:,}")
    print(f"Trainable params  : {trainable:,}")
    print(f"Non-trainable     : {non_trainable:,}")
    print()


def main():
    if len(sys.argv) < 2:
        print("Cara pakai: python show_model_summary.py <path_ke_model.h5_atau_.keras>")
        sys.exit(1)

    model_path = sys.argv[1]
    print_dependencies()
    print_model_summary(model_path)


if __name__ == "__main__":
    main()
