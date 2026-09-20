package com.example.torajacarvingai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/**
 * Wrapper tipis untuk satu model TFLite: preprocessing citra persegi (resize + RGB)
 * dan jalankan inferensi. Dipakai untuk Model 1 (gate ukiran Toraja vs bukan) maupun
 * Model 2 (klasifikasi jenis motif) -- keduanya berbentuk input sama, cuma beda
 * kebutuhan normalisasi piksel: Model 2 sudah punya lapisan Rescaling internal
 * (kirim 0..255 mentah), sedangkan Model 1 butuh normalisasi (px/127.5 - 1.0)
 * diterapkan di luar model.
 */
class TFLiteClassifierHelper(
    private val interpreter: Interpreter,
    val labels: List<String>,
    private val normalizeExternally: Boolean
) {
    /** Jalankan inferensi, kembalikan probabilitas mentah sepanjang labels.size (tidak hardcode). */
    fun classify(bitmap: Bitmap, imgSize: Int): FloatArray {
        val resized = if (bitmap.width == imgSize && bitmap.height == imgSize) {
            bitmap
        } else {
            Bitmap.createScaledBitmap(bitmap, imgSize, imgSize, true)
        }

        val byteBuffer = ByteBuffer.allocateDirect(4 * imgSize * imgSize * 3)
        byteBuffer.order(ByteOrder.nativeOrder())
        val intValues = IntArray(imgSize * imgSize)
        resized.getPixels(intValues, 0, imgSize, 0, 0, imgSize, imgSize)

        var pixel = 0
        for (i in 0 until imgSize) {
            for (j in 0 until imgSize) {
                val valInt = intValues[pixel++]
                val r = Color.red(valInt).toFloat()
                val g = Color.green(valInt).toFloat()
                val b = Color.blue(valInt).toFloat()
                if (normalizeExternally) {
                    byteBuffer.putFloat(r / 127.5f - 1.0f)
                    byteBuffer.putFloat(g / 127.5f - 1.0f)
                    byteBuffer.putFloat(b / 127.5f - 1.0f)
                } else {
                    byteBuffer.putFloat(r)
                    byteBuffer.putFloat(g)
                    byteBuffer.putFloat(b)
                }
            }
        }
        byteBuffer.rewind()

        val output = Array(1) { FloatArray(labels.size) }
        interpreter.run(byteBuffer, output)
        return output[0]
    }

    fun close() {
        interpreter.close()
    }

    companion object {
        /** Load model + label langsung dari folder assets APK (dipakai khusus untuk Model 1 -- gate). */
        fun loadFromAssets(
            context: Context,
            modelFileName: String,
            labelsFileName: String,
            normalizeExternally: Boolean,
            numThreads: Int = 4
        ): TFLiteClassifierHelper {
            val afd = context.assets.openFd(modelFileName)
            val modelBuffer = FileInputStream(afd.fileDescriptor).channel.map(
                FileChannel.MapMode.READ_ONLY, afd.startOffset, afd.declaredLength
            )
            val interpreter = Interpreter(modelBuffer, Interpreter.Options().setNumThreads(numThreads))
            val labels = context.assets.open(labelsFileName).bufferedReader().readLines()
                .map { it.trim() }.filter { it.isNotEmpty() }
            return TFLiteClassifierHelper(interpreter, labels, normalizeExternally)
        }
    }
}
