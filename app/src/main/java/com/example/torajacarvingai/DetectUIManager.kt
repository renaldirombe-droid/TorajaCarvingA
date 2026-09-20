package com.example.torajacarvingai

import android.graphics.Bitmap
import android.util.Log

/**
 * Mengelola alur keputusan klasifikasi dua tahap:
 *   Tahap 1 (gate)  -> apakah citra adalah ukiran Toraja sama sekali?
 *   Tahap 2 (motif) -> kalau ya, motif apa persisnya?
 * Murni logika keputusan, tidak menyentuh Dialog/View Activity secara langsung,
 * supaya bisa dipakai ulang baik oleh jalur kamera live maupun galeri/capture statis.
 */
class DetectUIManager(
    private val gateClassifier: TFLiteClassifierHelper,
    private val motifClassifier: TFLiteClassifierHelper,
    private val gateThreshold: Float = 0.70f,
    private val motifThreshold: Float = 0.70f,
    private val imgSize: Int = 320
) {
    sealed class PipelineResult {
        /** Model 1 bilang ini bukan ukiran Toraja sama sekali (atau confidence-nya kurang). */
        object NotTorajaMotif : PipelineResult()
        /** Lolos Model 1, tapi Model 2 tidak yakin motif spesifiknya (dataset_tidak_terdeteksi/confidence rendah). */
        object NotDetected : PipelineResult()
        /** Lolos kedua tahap -- motif dikenali dengan yakin. */
        data class Detected(val label: String, val confidence: Float) : PipelineResult()
    }

    data class GateOutcome(val isToraja: Boolean, val confidence: Float)

    private fun isUnknownMotifLabel(label: String): Boolean {
        val normalized = label.trim().lowercase().replace(" ", "_")
        return normalized.contains("tidak_terdeteksi") || normalized == "unknown"
    }

    /** Jalankan Model 1 saja -- dipakai jalur kamera live supaya Model 2 (lebih berat) bisa dilewati kalau jelas bukan motif. */
    fun runGateOnly(bitmap: Bitmap): GateOutcome {
        val probs1 = gateClassifier.classify(bitmap, imgSize)
        val idx1 = probs1.indices.sortedByDescending { probs1[it] }
        val top1Label = gateClassifier.labels[idx1[0]]
        val top1Conf = probs1[idx1[0]]
        if (idx1.size > 1) {
            val top2Label = gateClassifier.labels[idx1[1]]
            val top2Conf = probs1[idx1[1]]
            Log.d(
                "MODEL1_GATE",
                "Top-1: $top1Label (${"%.1f".format(top1Conf * 100)}%) | " +
                    "Top-2: $top2Label (${"%.1f".format(top2Conf * 100)}%)"
            )
        }
        val isToraja = top1Label.trim().equals("ukiran_toraja", ignoreCase = true)
        return GateOutcome(isToraja && top1Conf >= gateThreshold, top1Conf)
    }

    /** Jalankan Model 2 saja (asumsi Model 1 sudah lolos sebelumnya) -- dipakai jalur kamera live. */
    fun runMotifOnly(bitmap: Bitmap): PipelineResult {
        val probs2 = motifClassifier.classify(bitmap, imgSize)
        val idx2 = probs2.indices.sortedByDescending { probs2[it] }.take(3)
        val top3Log = idx2.joinToString { i ->
            "${motifClassifier.labels[i]}: ${"%.1f".format(probs2[i] * 100)}%"
        }
        Log.d("MODEL2_MOTIF", "Top-3: $top3Log")

        val top1Label = motifClassifier.labels[idx2[0]]
        val top1Conf = probs2[idx2[0]]

        return if (isUnknownMotifLabel(top1Label) || top1Conf < motifThreshold) {
            PipelineResult.NotDetected
        } else {
            PipelineResult.Detected(top1Label, top1Conf)
        }
    }

    /** Alur lengkap satu tarikan -- dipakai jalur galeri/capture statis (single-shot, bukan live loop). */
    fun runPipeline(bitmap: Bitmap): PipelineResult {
        val gate = runGateOnly(bitmap)
        if (!gate.isToraja) return PipelineResult.NotTorajaMotif
        return runMotifOnly(bitmap)
    }
}
