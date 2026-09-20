package com.example.torajacarvingai

import android.Manifest
import android.app.Dialog
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.Matrix
import android.os.Bundle
import android.view.ScaleGestureDetector
import android.view.MotionEvent
import android.util.Log
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
class MainActivity : AppCompatActivity() {

    private lateinit var viewFinder: PreviewView
    private lateinit var textStatusKeyakinan: TextView
    private lateinit var textTop1: TextView
    private lateinit var textTop2: TextView
    private lateinit var textTop3: TextView
    private lateinit var btnLihatDetail: android.widget.Button
    private lateinit var textKameraWarning: TextView
    private lateinit var textGuideline: TextView
    private lateinit var scannerFrame: View
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var tts: android.speech.tts.TextToSpeech
    private lateinit var scaleGestureDetector: android.view.ScaleGestureDetector

    // Menggunakan Interpreter (Lebih kebal error untuk model tanpa Metadata)
    private lateinit var interpreter: Interpreter
    private var labels = listOf<String>()
    private lateinit var modelBundleManager: ModelBundleManager
    private var motifCatalog: Map<String, MotifInfo> = emptyMap()

    // Model 1 (gate ukiran Toraja vs bukan) + pipeline dua-tahap
    private lateinit var gateClassifier: TFLiteClassifierHelper
    private lateinit var detectUIManager: DetectUIManager

    private var isDestroyedState = false
    private val aiLock = Any()

    // Konstanta Auto-Popup Cerdas
    private val CONF_POPUP = 0.80f
    private val MARGIN_POPUP = 0.20f
    private val MARGIN_RISKY_PAIR = 0.25f
    private val STABLE_TIME_MS = 1200L // Dioptimalkan dari 2000L
    private val INFERENCE_INTERVAL_MS = 200L // Interval throttling baru
    private val MIN_BLUR_SCORE = 60.0
    private val MIN_BRIGHTNESS = 40.0
    private val MAX_BRIGHTNESS = 220.0
    private val POPUP_COOLDOWN_MS = 10_000L
    private val UNKNOWN_WARNING_TIME_MS = 2_500L // dataset_tidak_terdeteksi beruntun 2-3 detik -> tampilkan panduan
    private val GATE_THRESHOLD = 0.70f // Model 1: ambang batas confidence ukiran_toraja
    private val MOTIF_THRESHOLD = 0.70f // Model 2: ambang batas confidence motif spesifik
    private val NOT_TORAJA_WARNING_TIME_MS = 2_500L // Model 1 bilang "bukan_ukiran_toraja" beruntun 2-3 detik

    // State Auto-Popup
    private var popupStableLabel = ""
    private var popupStableStartTime = 0L
    private var unknownStableStartTime = 0L
    private var notTorajaStableStartTime = 0L
    private var lastPopupLabel = ""
    private var lastPopupTime = 0L

    companion object {
        /** Ukuran input gambar model AI (width = height). Ubah di sini jika model berubah. */
        private const val IMG_SIZE = 320

        // ── Orientation prior: label yang dominan di arah tertentu ──────────────
        // Semua nama harus sama persis dengan labels.txt (huruf kecil, underscore)
        private val HORIZONTAL_ONLY = setOf(
            "pa_bulu_londong",
            "pa_re_po",
            "pa_tanduk_rape",
            "pa_don_bolu"
        )
        private val VERTICAL_ONLY = setOf(
            "pa_lolo_tabang"
        )

        private fun normalizeLabel(label: String): String {
            return label
                .lowercase()
                .replace("'", "")
                .replace("\"", "")
                .replace("(", "")
                .replace(")", "")
                .replace(" ", "_")
        }

        // ── Unknown Class Detection ──
        // Kelas penolak yang tidak boleh memunculkan popup detail motif
        private fun isUnknownLabel(label: String): Boolean {
            val normalized = normalizeLabel(label)
            return normalized == "dataset_tidak_terdeteksi" ||
                   normalized == "tidak_terdeteksi" ||
                   normalized == "unknown"
        }

        // Nama teknis kelas penolak (dataset_tidak_terdeteksi, dsb) tidak boleh tampil
        // apa adanya ke pengguna -- selalu ganti dengan kalimat yang ramah pembaca.
        private fun displayLabelName(label: String): String {
            return if (isUnknownLabel(label)) {
                "Motif Ukiran Tidak Terdeteksi"
            } else {
                label.replace("_", " ")
            }
        }

        private val RISKY_PAIRS = setOf(
            setOf("pa_tangke_lumu", "pa_tangke_lumu_ditoke"),
            setOf("pa_tangke_lumu", "pa_baranaepassape_bai"),
            setOf("pa_tedong_tumuru", "pa_barana"),
            setOf("pa_sekong_kandaure", "pa_baranaepassape_bai"),
            setOf("pa_bulintong_somba", "pa_pollo_gayang"),
            setOf("pa_bulu_londong", "pa_tanduk_rape"),
            setOf("pa_bulu_londong", "pa_don_bolu")
        )

        private val FLEXIBLE = setOf(       // boost ringan di kedua arah
            "pa_batang_lau"
        )
        private val HORIZONTAL_AND_NEUTRAL = setOf( // boost H, netral di KOTAK
            "pa_bulintong_somba",
            "pa_tangki_pattung"
        )

        // ── Debug V1: Kelas rawan yang sering salah deteksi ──
        private val RISKY_CLASSES = setOf(
            "Pa_tedong_tumuru", "pa_tangke_lumu", "pa_tangke_lumu_ditoke", "pa_kapu_baka", "pa_doti_pandin",
            "pa_erong", "pa_bulintong_somba", "pa_don_paria", "pa_doti",
            "pa_re_po", "dataset_tidak_terdeteksi", "pa_gayang", "pa_tedong"
        )

        // ── Debug V1: Threshold & Cooldown ──
        private const val DEBUG_CONF_THRESHOLD = 0.75f
        private const val DEBUG_COOLDOWN_NORMAL_MS = 1500L
        private const val DEBUG_COOLDOWN_RISKY_MS = 3000L
        private const val DEBUG_COOLDOWN_UNSTABLE_MS = 2000L
        private const val DEBUG_SAMPLING_INTERVAL_MS = 30000L
        private const val DEBUG_MAX_CAPTURES_TOTAL = 200
        private const val DEBUG_MAX_CAPTURES_PER_LABEL = 20
        private const val DEBUG_BLUR_THRESHOLD = 60f
        private const val DEBUG_BRIGHTNESS_LOW = 40f
        private const val DEBUG_BRIGHTNESS_HIGH = 220f
        private const val DEBUG_INSTABILITY_WINDOW = 8
        private const val DEBUG_INSTABILITY_MIN_LABELS = 3
    }

    /** Tiga mode bentuk kotak panduan pemindaian */
    enum class CropMode(val label: String, val widthDp: Int, val heightDp: Int) {
        KOTAK("\u2B1B Kotak",       260, 260),
        HORIZONTAL("\u25AC Horizontal", 320, 180),
        VERTIKAL("\u25AE Vertikal",   180, 320)
    }
    private var currentCropMode = CropMode.KOTAK

    private var camera: Camera? = null
    private var isFlashlightOn = false
    private var isGalleryOpen = false // Penanda apakah galeri sedang dibuka

    private var isPopupShowing = false // Mencegah popup muncul menumpuk

    /** Mode debug: simpan crop 320x320 saat AI ragu, untuk keperluan retraining dataset */
    @Volatile private var debugMode = false
    /** Waktu terakhir penyimpanan berdasar jenis debug */
    @Volatile private var lastDebugSaveTime = 0L
    @Volatile private var lastRiskySaveTime = 0L
    @Volatile private var lastUnstableSaveTime = 0L
    @Volatile private var lastNormalSampleTime = 0L

    private val recentDebugLabels = java.util.ArrayDeque<String>()
    
    // Debug V1.5: Hashing dan Kuota
    private val recentDebugHashes = java.util.ArrayDeque<Long>()
    private var totalDebugCaptures = 0
    private val debugCapturesPerLabel = mutableMapOf<String, Int>()

    // Untuk Capture Photo
    private var imageCapture: ImageCapture? = null
    private var isImageAnalysisPaused = false

    /** Alasan kenapa frame debug disimpan */
    private enum class DebugReason(val code: String) {
        LOW_CONFIDENCE("LOW_CONF"),
        LOW_MARGIN("LOW_MARGIN"),
        RISKY_CLASS("RISKY"),
        UNSTABLE("UNSTABLE"),
        BAD_QUALITY("BAD_QUALITY"),
        NORMAL_SAMPLE("SAMPLE"),
        MANUAL_FORCE("MANUAL")
    }

    private var dialogInfo: Dialog? = null

    // Menyimpan agregasi prediksi untuk averaging
    private val predictionHistory = java.util.ArrayDeque<String>()
    private val predictionProbabilityHistory = java.util.ArrayDeque<FloatArray>()

    // Animasi berkedip untuk indikator debug recording
    private var debugBlinkAnimator: android.animation.ValueAnimator? = null

    // Sensor untuk koreksi perspektif otomatis
    private lateinit var sensorManager: SensorManager
    @Volatile private var currentPitchDeg = 0f  // Kemiringan depan/belakang (derajat dari posisi tegak)
    @Volatile private var currentRollDeg = 0f   // Kemiringan kiri/kanan

    // Cache gambar & metadata terbaru untuk Manual Force Debug (long-press textTop1)
    @Volatile private var lastFrameBitmap: Bitmap? = null
    @Volatile private var lastFrameProbabilities: FloatArray? = null
    @Volatile private var lastFrameBrightness = 0f
    @Volatile private var lastFrameBlurScore = 0f

    private val gravitySensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val x = event.values[0] // Kiri/kanan
            val y = event.values[1] // Atas/bawah (saat HP tegak sempurna ≈ -9.8)
            val z = event.values[2] // Depan/belakang (saat HP tegak sempurna ≈ 0)

            // Hitung kemiringan dari posisi tegak sempurna (dalam derajat)
            // pitchDeg = 0° saat HP tegak, positif saat HP mendongak ke atas
            currentPitchDeg = Math.toDegrees(Math.atan2(z.toDouble(), -y.toDouble())).toFloat()
            currentRollDeg = Math.toDegrees(Math.atan2(x.toDouble(), -y.toDouble())).toFloat()
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "Izin kamera ditolak!", Toast.LENGTH_LONG).show()
        }
    }

    // LOKASI 1: LAUNCHER GALERI
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        isGalleryOpen = false

        if (uri != null) {
            // Buka layar pemotongan gambar dengan rasio 1:1
            cropImageLauncher.launch(
                CropImageContractOptions(
                    uri = uri,
                    cropImageOptions = CropImageOptions(
                        guidelines = CropImageView.Guidelines.ON,
                        fixAspectRatio = true,
                        aspectRatioX = 1,
                        aspectRatioY = 1
                    )
                )
            )
        } else {
            // Jika batal memilih gambar, nyalakan kamera kembali
            startCamera()
        }
    }

    // LAUNCHER PEMOTONG GAMBAR
    private val cropImageLauncher = registerForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            val uriContent = result.uriContent
            if (uriContent != null) {
                val bitmapCropped = uriToBitmap(uriContent)
                if (bitmapCropped != null) {
                    // Panggil fungsi AI khusus gambar statis
                    processStaticImage(bitmapCropped)
                } else {
                    Toast.makeText(this, "Gagal memuat gambar hasil pemotongan", Toast.LENGTH_SHORT).show()
                    startCamera() // Nyalakan kamera kembali jika gagal memuat
                }
            } else {
                startCamera() // Nyalakan kamera kembali jika uri null
            }
        } else {
            val exception = result.error
            val errorMsg = exception?.message ?: "Pemotongan gambar dibatalkan"
            Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
            startCamera() // Nyalakan kamera kembali jika batal crop
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewFinder = findViewById(R.id.viewFinder)
        textStatusKeyakinan = findViewById(R.id.textStatusKeyakinan)
        textTop1 = findViewById(R.id.textTop1)
        textTop2 = findViewById(R.id.textTop2)
        textTop3 = findViewById(R.id.textTop3)
        btnLihatDetail = findViewById(R.id.btnLihatDetail)
        textKameraWarning = findViewById(R.id.textKameraWarning)
        scannerFrame = findViewById(R.id.scannerFrame)
        textGuideline = findViewById(R.id.textGuideline)

        cameraExecutor = Executors.newSingleThreadExecutor()

        // 1. Siapkan AI
        setupAI()

        // --- TAMBAHKAN KODE INI ---
        tts = android.speech.tts.TextToSpeech(this) { status ->
            if (status != android.speech.tts.TextToSpeech.ERROR) {
                tts.language = java.util.Locale("id", "ID") // Mengatur bahasa ke Indonesia
            }
        }

        // Tombol Shutter untuk Mode Foto Diam (Fitur dinonaktifkan sementara)
        // val btnCapture = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCapture)
        // btnCapture.setOnClickListener {
        //     takePictureAndProcess()
        // }

        // 2. Siapkan sensor untuk koreksi perspektif otomatis
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gravitySensor?.let {
            sensorManager.registerListener(gravitySensorListener, it, SensorManager.SENSOR_DELAY_UI)
        }

        // 3. Minta Izin Kamera
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        val btnBukaGaleri = findViewById<android.widget.ImageButton>(R.id.btnBukaGaleri)
        btnBukaGaleri.setOnClickListener {
            isGalleryOpen = true // KUNCI KAMERA!
            predictionHistory.clear() // Bersihkan riwayat voting
            predictionProbabilityHistory.clear()
            
            // Hentikan kamera segera saat membuka galeri untuk menghemat sumber daya dan mencegah freeze
            try {
                val cameraProvider = ProcessCameraProvider.getInstance(this).get()
                cameraProvider.unbindAll()
            } catch (e: Exception) { }
            
            pickImageLauncher.launch("image/*")
        }

        // --- TAMBAHKAN KODE SENTER INI DI DALAM onCreate ---
        val btnSenter = findViewById<android.widget.ImageButton>(R.id.btnSenter)
        btnSenter.setOnClickListener {
            if (camera != null) {
                // Membalik status senter (Jika OFF jadi ON, jika ON jadi OFF)
                isFlashlightOn = !isFlashlightOn

                // Perintah CameraX untuk menyalakan/mematikan lampu flash
                camera?.cameraControl?.enableTorch(isFlashlightOn)

                // Ubah warna latar belakang agar pengguna tahu statusnya
                if (isFlashlightOn) {
                    btnSenter.setBackgroundColor(android.graphics.Color.parseColor("#4CAF50")) // Warna Hijau
                } else {
                    btnSenter.setBackgroundColor(android.graphics.Color.TRANSPARENT) // Warna transparan semula
                }
            } else {
                Toast.makeText(this, "Kamera sedang memuat, tunggu sebentar...", Toast.LENGTH_SHORT).show()
            }
        }

        val btnCropMode = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCropMode)
        btnCropMode.setOnClickListener {
            // Pindah ke mode berikutnya secara siklus
            val modes = CropMode.entries.toTypedArray()
            currentCropMode = modes[(currentCropMode.ordinal + 1) % modes.size]
            btnCropMode.text = currentCropMode.label
            // Terapkan ukuran baru ke scannerFrame dengan animasi
            applyCropMode(currentCropMode)
            // Reset voting agar tidak ada sisa prediksi mode lama
            predictionHistory.clear()
            predictionProbabilityHistory.clear()
            popupStableLabel = ""
            popupStableStartTime = 0L
        }

        // 3. Siapkan fitur pinch-to-zoom
        setupCameraZoom()
        val btnDebug = findViewById<android.widget.ImageButton>(R.id.btnDebug)
        btnDebug.setOnClickListener {
            setDebugModeState(!debugMode)
        }

        // Long-press pada kotak panduan dinonaktifkan agar debug mode hanya diakses melalui tombol

        // Long-press pada textTop1 → Manual Force Debug (simpan frame sekarang ke debug folder)
        textTop1.setOnLongClickListener {
            if (debugMode) {
                manualForceDebugCapture()
                true
            } else {
                Toast.makeText(this, "Aktifkan Debug Mode terlebih dahulu", Toast.LENGTH_SHORT).show()
                false
            }
        }
    }

    private fun setDebugModeState(enabled: Boolean) {
        debugMode = enabled
        val btnDebug = findViewById<android.widget.ImageButton>(R.id.btnDebug)
        
        // Stop animasi lama jika ada
        debugBlinkAnimator?.cancel()
        debugBlinkAnimator = null

        if (enabled) {
            // Border merah = debug ON
            scannerFrame.setBackgroundResource(0)
            scannerFrame.background = android.graphics.drawable.GradientDrawable().apply {
                setStroke(6, android.graphics.Color.parseColor("#FF3333"))
                cornerRadius = 16 * resources.displayMetrics.density
                setColor(android.graphics.Color.TRANSPARENT)
            }
            btnDebug.setBackgroundColor(android.graphics.Color.parseColor("#FF3333"))
            
            // Setup Indikator "🔴 REC" Berkedip
            textKameraWarning.text = "🔴 REC - Debug Mode Active"
            textKameraWarning.visibility = android.view.View.VISIBLE
            textKameraWarning.setTextColor(android.graphics.Color.RED)
            
            debugBlinkAnimator = android.animation.ObjectAnimator.ofFloat(textKameraWarning, "alpha", 0.2f, 1f).apply {
                duration = 800
                repeatCount = android.animation.ValueAnimator.INFINITE
                repeatMode = android.animation.ValueAnimator.REVERSE
                start()
            }

            val dir = java.io.File(getExternalFilesDir(null), "debug_crops")
            java.io.File(dir, "input_320").mkdirs()
            java.io.File(dir, "metadata").mkdirs()
            
            Toast.makeText(this,
                "🔴 Debug Mode ON\nLong-press prediksi untuk Manual Save!",
                Toast.LENGTH_LONG).show()
        } else {
            // Kembali ke border putih semula
            scannerFrame.setBackgroundResource(R.drawable.scanner_frame)
            btnDebug.setBackgroundColor(android.graphics.Color.parseColor("#4D000000"))
            
            // Sembunyikan indikator
            textKameraWarning.visibility = android.view.View.GONE
            textKameraWarning.alpha = 1f
            
            Toast.makeText(this, "Debug Mode OFF", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Manual Force Debug: User long-press pada prediksi Top-1 untuk force-save frame,
     * meski tidak memenuhi kondisi otomatis. Berguna untuk "High Confidence Error"
     * (model sangat yakin tapi salah).
     */
    private fun manualForceDebugCapture() {
        val bitmap = lastFrameBitmap
        val probs = lastFrameProbabilities
        
        if (bitmap == null || probs == null) {
            Toast.makeText(this, "Frame belum siap, tunggu sebentar...", Toast.LENGTH_SHORT).show()
            return
        }

        cameraExecutor.execute {
            try {
                saveEnhancedDebugCrop(
                    bitmap,
                    probs,
                    DebugReason.MANUAL_FORCE,
                    lastFrameBrightness,
                    lastFrameBlurScore
                )
                
                // Feedback audio & visual
                runOnUiThread {
                    Toast.makeText(this, "✅ Manual Force Debug Saved!", Toast.LENGTH_SHORT).show()
                    tts.speak("Frame tersimpan untuk audit manual", android.speech.tts.TextToSpeech.QUEUE_FLUSH, null)
                }
            } catch (e: Exception) {
                Log.e("MANUAL_DEBUG", "Gagal save manual debug: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(this, "❌ Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @android.annotation.SuppressLint("ClickableViewAccessibility")
    private fun setupCameraZoom() {
        val listener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val currentZoomRatio = camera?.cameraInfo?.zoomState?.value?.zoomRatio ?: 1f
                val delta = detector.scaleFactor
                camera?.cameraControl?.setZoomRatio(currentZoomRatio * delta)
                return true
            }
        }
        scaleGestureDetector = android.view.ScaleGestureDetector(this, listener)
    }

    /**
     * Ubah ukuran scannerFrame sesuai mode crop yang dipilih, dengan animasi halus.
     * Menggunakan ValueAnimator bawaan Android (tidak butuh dependency tambahan).
     */
    private fun applyCropMode(mode: CropMode) {
        val density = resources.displayMetrics.density
        val targetW = (mode.widthDp  * density).toInt()
        val targetH = (mode.heightDp * density).toInt()
        val startW  = scannerFrame.layoutParams.width
        val startH  = scannerFrame.layoutParams.height

        val animW = android.animation.ValueAnimator.ofInt(startW, targetW).apply { duration = 280 }
        val animH = android.animation.ValueAnimator.ofInt(startH, targetH).apply { duration = 280 }

        animW.addUpdateListener { anim ->
            scannerFrame.layoutParams = scannerFrame.layoutParams.also {
                it.width = anim.animatedValue as Int
            }
        }
        animH.addUpdateListener { anim ->
            scannerFrame.layoutParams = scannerFrame.layoutParams.also {
                it.height = anim.animatedValue as Int
            }
        }

        animW.start()
        animH.start()
    }

    // ── Debug V1: Fungsi pembantu ──────────────────────────────────────────────

    private fun updateRecentDebugLabels(label: String) {
        if (recentDebugLabels.size >= DEBUG_INSTABILITY_WINDOW) {
            recentDebugLabels.removeFirst()
        }
        recentDebugLabels.addLast(label)
    }



    private fun predictionIsUnstable(): Boolean {
        return recentDebugLabels.toSet().size >= DEBUG_INSTABILITY_MIN_LABELS
    }

    /** Hitung rata-rata kecerahan gambar (0=gelap, 255=terang). Sampling tiap 4 piksel agar cepat. */
    private fun calculateBrightness(bitmap: Bitmap): Float {
        val step = 4
        var total = 0L; var count = 0
        for (y in 0 until bitmap.height step step) {
            for (x in 0 until bitmap.width step step) {
                val pixel = bitmap.getPixel(x, y)
                total += (0.299 * Color.red(pixel) + 0.587 * Color.green(pixel) + 0.114 * Color.blue(pixel)).toLong()
                count++
            }
        }
        return if (count > 0) total.toFloat() / count else 128f
    }

    /** Hitung skor ketajaman gambar via Laplacian variance. Semakin rendah = semakin blur. */
    private fun calculateBlurScore(bitmap: Bitmap): Float {
        val step = 2
        var varianceSum = 0.0; var count = 0
        for (y in 1 until bitmap.height - 1 step step) {
            for (x in 1 until bitmap.width - 1 step step) {
                val center = luminance(bitmap.getPixel(x, y))
                val left   = luminance(bitmap.getPixel(x - 1, y))
                val right  = luminance(bitmap.getPixel(x + 1, y))
                val top    = luminance(bitmap.getPixel(x, y - 1))
                val bottom = luminance(bitmap.getPixel(x, y + 1))
                val laplacian = -4f * center + left + right + top + bottom
                varianceSum += laplacian * laplacian
                count++
            }
        }
        return if (count > 0) (varianceSum / count).toFloat() else 100f
    }

    private fun luminance(pixel: Int): Float {
        return 0.299f * Color.red(pixel) + 0.587f * Color.green(pixel) + 0.114f * Color.blue(pixel)
    }

    /**
     * Debug V1: Simpan gambar 320×320 + metadata JSON terstruktur.
     * Folder: debug_crops/input_320/ dan debug_crops/metadata/
     * Nama file: {datetime}__{reason}__top1_{label}_{conf}__top2_{label}_{conf}.jpg/.json
     * Dijalankan di background thread agar tidak memblokir kamera.
     */
    private fun saveEnhancedDebugCrop(
        bitmap: Bitmap,
        probabilities: FloatArray,
        reason: DebugReason,
        brightness: Float,
        blurScore: Float
    ) {
        cameraExecutor.execute {
            try {
                val baseDir = java.io.File(getExternalFilesDir(null), "debug_crops")
                val imgDir = java.io.File(baseDir, "input_320")
                val metaDir = java.io.File(baseDir, "metadata")
                if (!imgDir.exists()) imgDir.mkdirs()
                if (!metaDir.exists()) metaDir.mkdirs()

                // Ambil top 3
                val indexed = probabilities.mapIndexed { i, p -> i to p }
                    .sortedByDescending { it.second }
                val top1 = indexed.getOrNull(0)
                val top2 = indexed.getOrNull(1)
                val top3 = indexed.getOrNull(2)

                val top1Label = if (top1 != null && top1.first < labels.size) labels[top1.first].trim() else "unknown"
                val top1Conf = top1?.second ?: 0f
                val top2Label = if (top2 != null && top2.first < labels.size) labels[top2.first].trim() else "unknown"
                val top2Conf = top2?.second ?: 0f
                val top3Label = if (top3 != null && top3.first < labels.size) labels[top3.first].trim() else "unknown"
                val top3Conf = top3?.second ?: 0f

                // Format timestamp untuk nama file
                val sdf = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
                val dateStr = sdf.format(java.util.Date())
                val safeTop1 = top1Label.replace("[^a-zA-Z0-9_()']" .toRegex(), "_")
                val safeTop2 = top2Label.replace("[^a-zA-Z0-9_()']" .toRegex(), "_")

                val baseName = "${dateStr}__R_${reason.code}__top1_${safeTop1}_${String.format(java.util.Locale.US, "%.2f", top1Conf)}__top2_${safeTop2}_${String.format(java.util.Locale.US, "%.2f", top2Conf)}"

                // Simpan gambar 320x320
                val imgFile = java.io.File(imgDir, "${baseName}.jpg")
                java.io.FileOutputStream(imgFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }

                // Simpan metadata JSON
                val json = org.json.JSONObject().apply {
                    put("timestamp", dateStr)
                    put("top1_label", top1Label)
                    put("top1_conf", String.format(java.util.Locale.US, "%.4f", top1Conf).toDouble())
                    put("top2_label", top2Label)
                    put("top2_conf", String.format(java.util.Locale.US, "%.4f", top2Conf).toDouble())
                    put("top3_label", top3Label)
                    put("top3_conf", String.format(java.util.Locale.US, "%.4f", top3Conf).toDouble())
                    put("margin_top1_top2", String.format(java.util.Locale.US, "%.4f", top1Conf - top2Conf).toDouble())
                    put("reason", reason.code)
                    put("brightness", String.format(java.util.Locale.US, "%.1f", brightness).toDouble())
                    put("blur_score", String.format(java.util.Locale.US, "%.1f", blurScore).toDouble())
                    put("image_size", "${IMG_SIZE}x${IMG_SIZE}")
                    put("crop_mode", currentCropMode.name)
                }
                val metaFile = java.io.File(metaDir, "${baseName}.json")
                metaFile.writeText(json.toString(2))

                Log.d("DEBUG_CROP", "[${reason.code}] ${imgFile.name}")
            } catch (e: Exception) {
                Log.e("DEBUG_CROP", "Gagal simpan debug: ${e.message}", e)
            }
        }
    }

    /**
     * Sesuaikan probabilitas AI berdasarkan mode crop yang sedang aktif.
     * Label yang secara visual cenderung horizontal di-boost saat mode HORIZONTAL,
     * dan sedikit dipenalti saat mode VERTIKAL — begitu pula sebaliknya.
     * Hasil probabilitas selalu dinormalisasi ulang agar tetap berjumlah 1.0.
     */
    private fun applyOrientationBoost(probs: FloatArray): FloatArray {
        val adjusted = probs.copyOf()

        for (i in adjusted.indices) {
            if (i >= labels.size) break
            val label = labels[i].trim()

            val factor = when (currentCropMode) {
                CropMode.HORIZONTAL -> when {
                    label in HORIZONTAL_ONLY         -> 1.10f  // boost
                    label in HORIZONTAL_AND_NEUTRAL  -> 1.07f  // boost
                    label in FLEXIBLE                -> 1.05f  // boost
                    else                             -> 1.00f  // netral (no penalty)
                }
                CropMode.VERTIKAL -> when {
                    label in VERTICAL_ONLY           -> 1.10f  // boost
                    label in FLEXIBLE                -> 1.05f  // boost
                    else                             -> 1.00f  // netral (no penalty)
                }
                CropMode.KOTAK -> 1.00f // mode netral
            }

            adjusted[i] *= factor
        }

        // Normalisasi ulang agar total probabilitas tetap = 1.0
        val sum = adjusted.sum().coerceAtLeast(1e-6f)
        for (i in adjusted.indices) adjusted[i] /= sum

        return adjusted
    }

    private fun sha256Asset(fileName: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")

        assets.open(fileName).use { input ->
            val buffer = ByteArray(8192)

            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }

        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun setupAI() {
        try {
            modelBundleManager = ModelBundleManager(this)

            // Interpreter, labels, dan katalog kini berasal dari ModelBundleManager:
            // pakai bundle online tervalidasi jika ada, kalau tidak fallback ke assets bawaan APK.
            val options = Interpreter.Options().setNumThreads(4)
            val bundle = modelBundleManager.createInterpreter(options)
            interpreter = bundle.interpreter
            labels = modelBundleManager.loadActiveLabels()
            motifCatalog = modelBundleManager.loadActiveCatalog()

            // Log hash asset bawaan APK (fallback utama) tetap dipertahankan seperti semula.
            Log.d("MODEL_HASH", "${ModelBundleManager.DEFAULT_ASSET_MODEL_FILENAME} SHA256=${sha256Asset(ModelBundleManager.DEFAULT_ASSET_MODEL_FILENAME)}")
            Log.d("LABEL_HASH", "${ModelBundleManager.DEFAULT_ASSET_LABELS_FILENAME} SHA256=${sha256Asset(ModelBundleManager.DEFAULT_ASSET_LABELS_FILENAME)}")

            // Verifikasi dimensi tensor input, output, dan labels dari sumber yang BENAR-BENAR aktif
            Log.d("MODEL_CHECK", "Model file: ${bundle.modelFileName}")
            Log.d("MODEL_CHECK", "Input shape: ${bundle.inputShape.contentToString()}")
            Log.d("MODEL_CHECK", "Output shape: ${bundle.outputShape.contentToString()}")
            Log.d("MODEL_CHECK", "Labels count: ${labels.size}")
            Log.d("MODEL_CHECK", "Preprocessing: raw 0..255 because model has internal Rescaling")

            modelBundleManager.logStartupInfo(bundle)

            // Model 1 (gate ukiran Toraja vs bukan) -- selalu dari assets bawaan APK,
            // tidak ikut mekanisme update online seperti Model 2/labels/katalog.
            val motifClassifier = TFLiteClassifierHelper(interpreter, labels, normalizeExternally = false)
            gateClassifier = TFLiteClassifierHelper.loadFromAssets(
                context = this,
                modelFileName = "model_tahap1_float32.tflite",
                labelsFileName = "labels_tahap1.txt",
                normalizeExternally = true
            )
            detectUIManager = DetectUIManager(
                gateClassifier = gateClassifier,
                motifClassifier = motifClassifier,
                gateThreshold = GATE_THRESHOLD,
                motifThreshold = MOTIF_THRESHOLD,
                imgSize = IMG_SIZE
            )
            Log.d("MODEL_CHECK", "Model 1 (gate) labels: ${gateClassifier.labels}")

            Log.d("AI_STATUS", "Otak AI Berhasil Dimuat!")
        } catch (e: Exception) {
            Log.e("AI_ERROR", "Gagal memuat model: ${e.message}")
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImage(imageProxy)
                    }
                }

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture, imageAnalyzer)
            } catch(exc: Exception) {
                Log.e("CAMERA_ERROR", "Kamera gagal dibuka", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    /**
     * Memotong gambar kamera agar hanya area di dalam kotak panduan (scannerFrame)
     * yang dikirim ke model AI. Ini membuat motif terlihat lebih besar di input AI,
     * sehingga deteksi lebih akurat dari jarak yang lebih jauh.
     */
    private fun cropToScannerFrame(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
        // 1. Putar gambar agar sesuai dengan orientasi layar (sensor kamera biasanya mendatar)
        val matrix = Matrix()
        matrix.postRotate(rotationDegrees.toFloat())
        val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

        // 2. Ambil ukuran viewFinder dan scannerFrame
        val viewWidth = viewFinder.width.toFloat()
        val viewHeight = viewFinder.height.toFloat()

        if (viewWidth == 0f || viewHeight == 0f) return rotatedBitmap

        // 3. Hitung skala dan offset karena PreviewView menggunakan FILL_CENTER
        // FILL_CENTER men-scale gambar agar menutupi view, lalu memusatkannya.
        val bitmapWidth = rotatedBitmap.width.toFloat()
        val bitmapHeight = rotatedBitmap.height.toFloat()

        val scale = kotlin.math.max(viewWidth / bitmapWidth, viewHeight / bitmapHeight)

        val displayedWidth = bitmapWidth * scale
        val displayedHeight = bitmapHeight * scale

        // Berapa banyak piksel gambar yang berada di luar layar viewFinder
        val offsetX = (displayedWidth - viewWidth) / 2f
        val offsetY = (displayedHeight - viewHeight) / 2f

        // 4. Hitung posisi aktual scannerFrame secara visual relatif terhadap viewFinder
        // Karena keduanya berada dalam ConstraintLayout yang sama, kita bisa memakai properti x dan y
        val scannerLeftInView = scannerFrame.x - viewFinder.x
        val scannerTopInView = scannerFrame.y - viewFinder.y
        val scannerRightInView = scannerLeftInView + scannerFrame.width.toFloat()
        val scannerBottomInView = scannerTopInView + scannerFrame.height.toFloat()

        // 5. Petakan area aktual scannerFrame kembali ke koordinat asli rotatedBitmap
        val cropLeft = ((scannerLeftInView + offsetX) / scale).toInt().coerceIn(0, rotatedBitmap.width - 1)
        val cropTop = ((scannerTopInView + offsetY) / scale).toInt().coerceIn(0, rotatedBitmap.height - 1)
        
        val cropRight = ((scannerRightInView + offsetX) / scale).toInt().coerceIn(cropLeft + 1, rotatedBitmap.width)
        val cropBottom = ((scannerBottomInView + offsetY) / scale).toInt().coerceIn(cropTop + 1, rotatedBitmap.height)

        val cropWidth = cropRight - cropLeft
        val cropHeight = cropBottom - cropTop

        return Bitmap.createBitmap(rotatedBitmap, cropLeft, cropTop, cropWidth, cropHeight)
    }

    /**
     * Koreksi perspektif otomatis berdasarkan kemiringan HP (sensor gravitasi).
     * Jika HP diarahkan dari bawah ke atas (mendongak), gambar akan "diluruskan"
     * secara digital agar model AI menerima input yang lebih lurus dan akurat.
     */
    private fun correctPerspective(bitmap: Bitmap): Bitmap {
        // Dinonaktifkan sementara untuk pengujian crop normal (mencegah gambar miring/distorsi)
        return bitmap

        // Abaikan jika kemiringan sangat kecil (< 5°) — tidak perlu dikoreksi
        if (Math.abs(currentPitchDeg) < 5f && Math.abs(currentRollDeg) < 5f) {
            return bitmap
        }

        // Faktor koreksi 0.6 agar tidak over-correction (koreksi berlebihan)
        val correctionFactor = 0.6f
        
        // Matikan koreksi pitch jika HP ditidurkan/mendatar (> 45°) untuk mencegah distorsi ekstrem saat scan meja/lantai
        val pitchCorrection = if (Math.abs(currentPitchDeg) < 45f) {
            (currentPitchDeg * correctionFactor).coerceIn(-25f, 25f)
        } else {
            0f
        }
        val rollCorrection = (currentRollDeg * correctionFactor).coerceIn(-25f, 25f)

        val graphicsCamera = android.graphics.Camera()
        val correctionMatrix = Matrix()

        graphicsCamera.save()
        // Putar balik berlawanan arah kemiringan untuk meluruskan perspektif
        if (pitchCorrection != 0f) {
            graphicsCamera.rotateX(-pitchCorrection) // Koreksi depan/belakang (mendongak/menunduk)
        }
        // Gunakan rotateZ untuk meluruskan kemiringan kiri/kanan (roll) secara 2D, bukan rotateY (3D warp) yang membuat gambar miring/peyang
        graphicsCamera.rotateZ(-rollCorrection)
        graphicsCamera.getMatrix(correctionMatrix)
        graphicsCamera.restore()

        // Pusatkan transformasi di tengah gambar agar tidak bergeser
        correctionMatrix.preTranslate(-bitmap.width / 2f, -bitmap.height / 2f)
        correctionMatrix.postTranslate(bitmap.width / 2f, bitmap.height / 2f)

        Log.d("PERSPECTIVE", "Koreksi diterapkan: pitch=${String.format("%.1f", pitchCorrection)}°, roll=${String.format("%.1f", rollCorrection)}°")

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, correctionMatrix, true)
    }

    private var lastInferenceTime = 0L

    private fun processImage(imageProxy: ImageProxy) {
        // 1. Jika halaman sedang ditutup atau AI belum siap, abaikan gambar
        if (isDestroyedState || !::interpreter.isInitialized || isGalleryOpen || isPopupShowing) {
            imageProxy.close() // Buang gambar, jangan diproses!
            return
        }
        
        // 2. Throttle inferensi sesuai interval agar responsif
        val currentTimeInference = System.currentTimeMillis()
        if (currentTimeInference - lastInferenceTime < INFERENCE_INTERVAL_MS) {
            imageProxy.close()
            return
        }
        lastInferenceTime = currentTimeInference

        try {
            val bitmap = imageProxy.toBitmap()
            // Potong gambar hanya pada area di dalam kotak panduan
            val croppedBitmap = cropToScannerFrame(bitmap, imageProxy.imageInfo.rotationDegrees)
            // Koreksi perspektif otomatis berdasarkan kemiringan HP
            val correctedBitmap = correctPerspective(croppedBitmap)
            val resizedBitmap = Bitmap.createScaledBitmap(correctedBitmap, IMG_SIZE, IMG_SIZE, true)

            // --- Tahap 1: Model gate (ukiran Toraja vs bukan) ---
            // Kalau jelas bukan motif Toraja, lewati Model 2 sepenuhnya (hemat komputasi)
            // dan tampilkan panduan setelah beruntun 2-3 detik (bukan sekali frame buruk).
            if (::detectUIManager.isInitialized) {
                val gate = detectUIManager.runGateOnly(resizedBitmap)
                if (!gate.isToraja) {
                    val currentTimeGate = System.currentTimeMillis()
                    if (notTorajaStableStartTime == 0L) notTorajaStableStartTime = currentTimeGate
                    if (!isPopupShowing && currentTimeGate - notTorajaStableStartTime >= NOT_TORAJA_WARNING_TIME_MS) {
                        notTorajaStableStartTime = 0L
                        runOnUiThread { if (!isDestroyedState) showNotTorajaMotifWarning() }
                    }
                    return // imageProxy ditutup otomatis oleh blok finally di akhir fungsi
                } else {
                    notTorajaStableStartTime = 0L
                }
            }

            val byteBuffer = ByteBuffer.allocateDirect(4 * IMG_SIZE * IMG_SIZE * 3)
            byteBuffer.order(ByteOrder.nativeOrder())
            val intValues = IntArray(IMG_SIZE * IMG_SIZE)
            resizedBitmap.getPixels(intValues, 0, IMG_SIZE, 0, 0, IMG_SIZE, IMG_SIZE)

            var pixel = 0
            for (i in 0 until IMG_SIZE) {
                for (j in 0 until IMG_SIZE) {
                    val valInt = intValues[pixel++]
                    
                    // Gunakan fungsi Color bawaan Android agar lebih stabil
                    val r = Color.red(valInt)
                    val g = Color.green(valInt)
                    val b = Color.blue(valInt)

                    // Model memiliki layer Rescaling internal. Kirim nilai piksel mentah [0.0f, 255.0f]
                    byteBuffer.putFloat(r.toFloat())
                    byteBuffer.putFloat(g.toFloat())
                    byteBuffer.putFloat(b.toFloat())
                }
            }

            // 🔥 INI KUNCINYA: Kembalikan kursor ke awal sebelum dibaca AI!
            byteBuffer.rewind()

            val output = Array(1) { FloatArray(labels.size) }

            // --- UBAH MENJADI SEPERTI INI ---
            synchronized(aiLock) {
                if (!isDestroyedState) {
                    // AI Menebak Gambar dengan aman
                    interpreter.run(byteBuffer, output)
                }
            }

            val rawProbabilities = output[0]
            
            // Log output mentah (sebelum smoothing & boost) untuk diagnostik
            val rawTopIndices = rawProbabilities.indices.sortedByDescending { rawProbabilities[it] }.take(3)
            val rawLogStr = rawTopIndices.joinToString { idx ->
                val label = labels[idx]
                val pct = String.format(java.util.Locale.US, "%.1f%%", rawProbabilities[idx] * 100)
                "$label: $pct"
            }
            Log.d("RAW_MODEL_FRAME", "Raw: $rawLogStr")
            
            // Memintas (bypass) orientation boost sementara untuk melihat hasil prediksi asli model
            // val probabilities = applyOrientationBoost(rawProbabilities)
            val probabilities = rawProbabilities

            // Hitung kualitas gambar (untuk debug & indikator UI)
            val brightness = calculateBrightness(resizedBitmap)
            val blurScore = calculateBlurScore(resizedBitmap)

            // Cache frame terbaru untuk Manual Force Debug (long-press textTop1)
            lastFrameBitmap = resizedBitmap.copy(Bitmap.Config.ARGB_8888, false)
            lastFrameProbabilities = probabilities.copyOf()
            lastFrameBrightness = brightness
            lastFrameBlurScore = blurScore

            // Cari top-1 dan top-2 mentah (sebelum smoothing) untuk debug
            var maxIndex = 0
            var maxProb = probabilities[0]
            var secondProb = 0f
            for (i in 1 until probabilities.size) {
                if (probabilities[i] > maxProb) {
                    secondProb = maxProb
                    maxProb = probabilities[i]
                    maxIndex = i
                } else if (probabilities[i] > secondProb) {
                    secondProb = probabilities[i]
                }
            }
            val margin = maxProb - secondProb
            val top1Label = if (maxIndex < labels.size) labels[maxIndex].trim() else "unknown"

            // Selalu simpan jika debug mode aktif (dengan interval 1000ms untuk mencegah spam/lag)
            if (debugMode) {
                val now = System.currentTimeMillis()
                if (now - lastDebugSaveTime >= 1000L) {
                    lastDebugSaveTime = now
                    saveEnhancedDebugCrop(resizedBitmap, probabilities, DebugReason.NORMAL_SAMPLE, brightness, blurScore)
                }
            }

            // Tampilkan ke Layar HP
            if (!isDestroyedState) {
                runOnUiThread {
                    if (isPopupShowing) return@runOnUiThread

                    // Peringatan Kualitas Kamera
                    if (blurScore < DEBUG_BLUR_THRESHOLD) {
                        textKameraWarning.visibility = View.VISIBLE
                        textKameraWarning.text = "⚠️ Pastikan motif tidak blur"
                    } else if (brightness < DEBUG_BRIGHTNESS_LOW) {
                        textKameraWarning.visibility = View.VISIBLE
                        textKameraWarning.text = "⚠️ Tambahkan cahaya"
                    } else if (brightness > DEBUG_BRIGHTNESS_HIGH) {
                        textKameraWarning.visibility = View.VISIBLE
                        textKameraWarning.text = "⚠️ Cahaya terlalu terang"
                    } else {
                        textKameraWarning.visibility = View.GONE
                    }

                    // --- Smoothing Probabilitas ---
                    predictionProbabilityHistory.addLast(probabilities)
                    if (predictionProbabilityHistory.size > 5) { // Dikurangi dari 10 menjadi 5 agar responsif
                        predictionProbabilityHistory.removeFirst()
                    }

                    val avgProbs = FloatArray(labels.size)
                    for (p in predictionProbabilityHistory) {
                        for (i in p.indices) {
                            avgProbs[i] += p[i]
                        }
                    }
                    val historySize = predictionProbabilityHistory.size.toFloat()
                    for (i in avgProbs.indices) {
                        avgProbs[i] /= historySize
                    }

                    // Cari Top 3 dari rata-rata probabilitas
                    val topIndices = avgProbs.indices.sortedByDescending { avgProbs[it] }.take(3)
                    val top1AvgProb = avgProbs[topIndices[0]]
                    val top2AvgProb = avgProbs[topIndices[1]]
                    val top3AvgProb = avgProbs[topIndices[2]]
                    val marginAvg = top1AvgProb - top2AvgProb
                    val top1Str = displayLabelName(labels[topIndices[0]])
                    
                    // --- LOGGING UNTUK DEBUGGING MURNI DARI MODEL ---
                    val rawTop1 = labels[topIndices[0]]
                    val rawTop2 = labels[topIndices[1]]
                    val rawTop3 = labels[topIndices[2]]
                    Log.d("RAW_MODEL_OUTPUT", "TOP-1: $rawTop1 (${top1AvgProb * 100}%)")
                    Log.d("RAW_MODEL_OUTPUT", "TOP-2: $rawTop2 (${top2AvgProb * 100}%)")
                    Log.d("RAW_MODEL_OUTPUT", "TOP-3: $rawTop3 (${top3AvgProb * 100}%)")

                    // --- Logika Auto-Popup & Stabilitas ---
                    val currentTime = System.currentTimeMillis()
                    val rawTop1Label = labels[topIndices[0]]
                    val rawTop2Label = labels[topIndices[1]]
                    
                    val isGoodQuality = blurScore >= MIN_BLUR_SCORE && brightness in MIN_BRIGHTNESS..MAX_BRIGHTNESS
                    
                    val isUnknownNow = rawTop1Label.contains("tidak_terdeteksi", ignoreCase = true)

                    if (!isGoodQuality || isUnknownNow) {
                        popupStableLabel = ""
                        popupStableStartTime = 0L
                    } else {
                        if (rawTop1Label == popupStableLabel) {
                            if (popupStableStartTime == 0L) popupStableStartTime = currentTime
                        } else {
                            popupStableLabel = rawTop1Label
                            popupStableStartTime = currentTime
                        }
                    }

                    // Lacak "tidak terdeteksi" beruntun -- kalau bertahan 2-3 detik, jeda pemindaian
                    // dan tampilkan panduan (bukan cuma diam tanpa penjelasan ke pengguna).
                    if (isUnknownNow) {
                        if (unknownStableStartTime == 0L) unknownStableStartTime = currentTime
                        if (!isPopupShowing && currentTime - unknownStableStartTime >= UNKNOWN_WARNING_TIME_MS) {
                            unknownStableStartTime = 0L
                            showLowConfidenceWarning()
                        }
                    } else {
                        unknownStableStartTime = 0L
                    }

                    val pairNow = setOf(normalizeLabel(rawTop1Label), normalizeLabel(rawTop2Label))
                    val isRiskyPair = RISKY_PAIRS.contains(pairNow)
                    val requiredMargin = if (isRiskyPair) MARGIN_RISKY_PAIR else MARGIN_POPUP
                    
                    val isConfidentPopup = top1AvgProb >= CONF_POPUP
                    val isSeparatedPopup = marginAvg >= requiredMargin
                    val isStableLongEnough = popupStableLabel.isNotEmpty() && (currentTime - popupStableStartTime >= STABLE_TIME_MS)
                    
                    val isSameLabelCooldown = (rawTop1Label == lastPopupLabel) && (currentTime - lastPopupTime < POPUP_COOLDOWN_MS)
                    val canPopupByCooldown = !isSameLabelCooldown
                    
                    // Guard: Jangan popup untuk unknown class
                    val isNotUnknown = !isUnknownLabel(rawTop1Label)
                    
                    val shouldAutoPopup = isNotUnknown && isConfidentPopup && isSeparatedPopup && isStableLongEnough && canPopupByCooldown && isGoodQuality
                    
                    if (shouldAutoPopup) {
                        lastPopupLabel = rawTop1Label
                        lastPopupTime = currentTime
                        // Reset timer agar tidak double popup
                        popupStableStartTime = currentTime 
                        showMotifInfo(rawTop1Label, isYakin = true, capturedBitmap = correctedBitmap)
                    }

                    // Logika Status Keyakinan
                    val statusText: String
                    val statusColor: Int
                    
                    // Check jika top1 adalah unknown class
                    val isTop1Unknown = isUnknownLabel(rawTop1Label)
                    
                    if (isTop1Unknown) {
                        statusText = "Belum dapat dipastikan"
                        statusColor = android.graphics.Color.parseColor("#FF9800") // Oranye
                    } else if (isRiskyPair && marginAvg < MARGIN_RISKY_PAIR) {
                        statusText = "Mirip kandidat lain, cek Top 3"
                        statusColor = android.graphics.Color.parseColor("#FF9800") // Oranye
                    } else if (top1AvgProb >= 0.75f && marginAvg >= 0.15f) {
                        statusText = "Yakin"
                        statusColor = android.graphics.Color.parseColor("#4CAF50") // Hijau
                    } else if (top1AvgProb >= 0.40f) {
                        statusText = "Kemungkinan motif"
                        statusColor = android.graphics.Color.parseColor("#FFEB3B") // Kuning
                    } else {
                        statusText = "Belum dapat dipastikan"
                        statusColor = android.graphics.Color.parseColor("#FF9800") // Oranye
                    }
                    
                    if (predictionProbabilityHistory.size >= 5) {
                        textStatusKeyakinan.text = statusText
                        textStatusKeyakinan.setTextColor(statusColor)

                        // Update panduan secara dinamis berdasarkan keyakinan
                        if (isTop1Unknown || statusText == "Belum dapat dipastikan") {
                            textGuideline.text = "Perbaiki posisi kamera, pencahayaan, atau coba mode crop"
                        } else {
                            textGuideline.text = "Posisikan satu motif ukiran di dalam kotak"
                        }

                        textTop1.text = "1. $top1Str ${String.format(java.util.Locale.US, "%.0f", top1AvgProb * 100)}%"
                        
                        textTop2.visibility = View.VISIBLE
                        val top2Str = displayLabelName(labels[topIndices[1]])
                        textTop2.text = "2. $top2Str ${String.format(java.util.Locale.US, "%.0f", top2AvgProb * 100)}%"

                        textTop3.visibility = View.VISIBLE
                        val top3Str = displayLabelName(labels[topIndices[2]])
                        textTop3.text = "3. $top3Str ${String.format(java.util.Locale.US, "%.0f", top3AvgProb * 100)}%"

                        // Tombol Detail Kandidat Utama
                        if (!isTop1Unknown && (statusText == "Kemungkinan motif" || statusText == "Yakin") && !top1Str.contains("TIDAK TERDETEKSI")) {
                            btnLihatDetail.visibility = View.VISIBLE
                            btnLihatDetail.setOnClickListener {
                                showMotifInfo(labels[topIndices[0]], isYakin = (statusText == "Yakin"), capturedBitmap = correctedBitmap)
                            }
                        } else {
                            btnLihatDetail.visibility = View.GONE
                        }
                    } else {
                        textStatusKeyakinan.text = "Menganalisis..."
                        textStatusKeyakinan.setTextColor(android.graphics.Color.WHITE)
                        textTop1.text = ""
                        textTop2.visibility = View.GONE
                        textTop3.visibility = View.GONE
                        btnLihatDetail.visibility = View.GONE
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AI_ERROR", "Proses dibatalkan karena halaman ditutup: ${e.message}")
        } finally {
            // Pastikan gambar selalu ditutup agar memori tidak bocor
            imageProxy.close()
        }
    }

    override fun onDestroy() {
        // 1. Beri tahu sistem bahwa halaman ditutup
        isDestroyedState = true

        // 2. Tutup Pop-up jika masih terbuka
        dialogInfo?.dismiss()

        // 3. Putuskan sambungan kamera terlebih dahulu
        try {
            val cameraProvider = ProcessCameraProvider.getInstance(this).get()
            cameraProvider.unbindAll()
        } catch (e: Exception) {
            Log.e("CAMERA", "Abaikan, kamera sudah tertutup.")
        }

        // 4. Matikan sensor perspektif
        if (::sensorManager.isInitialized) {
            sensorManager.unregisterListener(gravitySensorListener)
        }

        // 5. Matikan antrean kamera secara halus (ubah dari shutdownNow ke shutdown)
        cameraExecutor.shutdown()

        // 6. GUNAKAN GEMBOK SAAT MENUTUP AI (Agar tidak tabrakan dengan processImage)
        synchronized(aiLock) {
            try {
                if (::interpreter.isInitialized) {
                    interpreter.close()
                }
            } catch (e: Exception) {
                Log.e("AI", "Abaikan, AI sudah ditutup.")
            }

            Unit

        }

        super.onDestroy()
    }

    // --- TAMBAHKAN 2 FUNGSI INI DI BAGIAN BAWAH ---

    private fun showMotifInfo(motifName: String, isYakin: Boolean, capturedBitmap: Bitmap? = null) {
        // Resolusi data motif dgn prioritas: motif_catalog aktif -> SumberData.kt -> label mentah.
        val resolved = MotifCatalogResolver.resolve(motifName, motifCatalog)
        Log.d("TorajaBundle", "popup MotifInfo source = ${resolved.source}")

        // Guard: Jangan tampilkan popup untuk unknown class / show_popup=false di catalog aktif
        if (isUnknownLabel(motifName) || !resolved.showPopup) {
            Log.d("POPUP_GUARD", "Blocked popup for unknown class: $motifName")
            return
        }

        if (dialogInfo?.isShowing == true) return

        isPopupShowing = true

        // Prefix kategori dibuat sekali per tampilan popup, bukan diakumulasi ke teks lama,
        // sehingga tidak dobel walau showMotifInfo dipanggil berkali-kali.
        // Kalau category kosong/blank (mis. belum diisi admin di catalog), jangan tampilkan
        // kalimat "termasuk dalam kategori ." yang janggal -- prefix jadi kosong sepenuhnya.
        val prefixKategori = if (resolved.category.isBlank()) {
            ""
        } else {
            "${resolved.displayName} termasuk dalam kategori ${resolved.category}."
        }

        // Gabungkan prefix ke teks deskripsi tanpa menyisakan baris kosong di awal
        // kalau prefix-nya kosong.
        fun withKategoriPrefix(text: String): String =
            if (prefixKategori.isBlank()) text else "$prefixKategori\n\n$text"

        // Hanya simpan ke riwayat jika status keyakinannya YAKIN. Simpan LABEL mentah juga
        // (bukan cuma nama tampilan) supaya bisa di-resolve ulang kalau catalog berubah nanti.
        if (isYakin) {
            val timeStamp = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
            HistoryManager(this).saveHistory(resolved.displayName, timeStamp, motifName)
        }

        dialogInfo = Dialog(this)
        dialogInfo?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogInfo?.setContentView(R.layout.dialog_motif_info)
        dialogInfo?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialogInfo?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialogInfo?.setCancelable(false)

        val tvTitle = dialogInfo?.findViewById<TextView>(R.id.tvDialogTitle)
        val tvKategori = dialogInfo?.findViewById<TextView>(R.id.tvDialogKategori)
        val ivDialogMotif = dialogInfo?.findViewById<ImageView>(R.id.ivDialogMotif)
        val tvDeskripsi = dialogInfo?.findViewById<TextView>(R.id.tvDialogDeskripsi)
        val btnDetail = dialogInfo?.findViewById<Button>(R.id.btnDialogDetail)
        val btnTutup = dialogInfo?.findViewById<Button>(R.id.btnDialogTutup)

        val btnSpeak = dialogInfo?.findViewById<android.widget.ImageButton>(R.id.btnSpeak)

        // Tampilkan nama dan deskripsi SINGKAT di awal
        tvTitle?.text = resolved.displayName
        // Kategori kosong: sembunyikan TextView-nya, jangan tampilkan "Kategori: " tanpa isi.
        if (resolved.category.isBlank()) {
            tvKategori?.text = ""
            tvKategori?.visibility = View.GONE
        } else {
            tvKategori?.text = resolved.category
            tvKategori?.visibility = View.VISIBLE
        }
        if (capturedBitmap != null) {
            ivDialogMotif?.setImageBitmap(capturedBitmap)
            ivDialogMotif?.visibility = View.VISIBLE
        } else if (ivDialogMotif != null) {
            loadMotifImage(ivDialogMotif, resolved.imageUrl, resolved.imageRes)
            ivDialogMotif.visibility = View.VISIBLE
        }
        tvDeskripsi?.text = withKategoriPrefix(resolved.shortText)

        // --- TAMBAHKAN LOGIKA KLIK SPEAKER ---
        btnSpeak?.setOnClickListener {
            val textToRead = withKategoriPrefix(resolved.fullText)
            tts.speak(textToRead, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, null)
            Toast.makeText(this, "Membacakan Makna Filosofi...", Toast.LENGTH_SHORT).show()
        }

        // Jika tombol "Baca Lengkap" diklik: Ubah teksnya, lalu sembunyikan tombolnya!
        btnDetail?.setOnClickListener {
            tvDeskripsi?.text = withKategoriPrefix(resolved.fullText)
            btnDetail.visibility = View.GONE
        }

        btnTutup?.setOnClickListener {
            if (::tts.isInitialized) tts.stop() // Matikan suara
            dialogInfo?.dismiss() // Tutup popup
            isPopupShowing = false
            isImageAnalysisPaused = false
            popupStableLabel = ""
            popupStableStartTime = 0L
            predictionHistory.clear() // Bersihkan riwayat voting agar mulai segar kembali
            predictionProbabilityHistory.clear()

            // Nyalakan kamera kembali setelah membaca popup (jika sebelumnya pakai galeri)
            startCamera()
        }

        dialogInfo?.show()
    }

    /**
     * Pop-up peringatan generik (ikon alert + judul/pesan bisa disesuaikan) --
     * dipakai untuk kasus "motif kurang jelas" (Model 2 tidak yakin) maupun
     * "bukan ukiran Toraja" (Model 1 menolak), supaya pengguna selalu dapat
     * penjelasan, bukan cuma layar diam. Otomatis menjeda pemindaian (isPopupShowing)
     * dan mengaktifkan kamera kembali saat ditutup.
     */
    private fun showGuidanceDialog(title: String, message: String) {
        if (isPopupShowing) return
        isPopupShowing = true

        val warningDialog = Dialog(this)
        warningDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        warningDialog.setContentView(R.layout.dialog_motif_info)
        warningDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        warningDialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        warningDialog.setCancelable(false)

        val tvTitle = warningDialog.findViewById<TextView>(R.id.tvDialogTitle)
        val ivDialogMotif = warningDialog.findViewById<ImageView>(R.id.ivDialogMotif)
        val tvDeskripsi = warningDialog.findViewById<TextView>(R.id.tvDialogDeskripsi)
        val btnDetail = warningDialog.findViewById<Button>(R.id.btnDialogDetail)
        val btnTutup = warningDialog.findViewById<Button>(R.id.btnDialogTutup)
        val btnSpeak = warningDialog.findViewById<android.widget.ImageButton>(R.id.btnSpeak)

        // Tampilkan ikon peringatan (bukan foto motif) supaya dialog tetap ada
        // elemen visual yang jelas, bukan cuma teks polos.
        ivDialogMotif?.apply {
            setImageResource(R.drawable.ic_alert_warning)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            visibility = View.VISIBLE
        }
        btnDetail?.visibility = View.GONE
        btnSpeak?.visibility = View.GONE

        tvTitle?.text = title
        tvDeskripsi?.text = message

        btnTutup?.text = "Mengerti"
        btnTutup?.setOnClickListener {
            warningDialog.dismiss()
            isPopupShowing = false
            isImageAnalysisPaused = false
            popupStableLabel = ""
            popupStableStartTime = 0L
            unknownStableStartTime = 0L
            notTorajaStableStartTime = 0L
            predictionHistory.clear()
            predictionProbabilityHistory.clear()
            startCamera()
        }

        warningDialog.show()
    }

    /**
     * Pop-up peringatan saat Model 2 terlalu ragu (dataset_tidak_terdeteksi/confidence rendah).
     * Nama fungsi dipertahankan supaya pemanggil lama (jalur kamera live) tidak perlu diubah.
     */
    private fun showLowConfidenceWarning() {
        showGuidanceDialog(
            title = "\u26A0\uFE0F Motif dideteksi bukan ukiran toraja",
            message = "Motif dideteksi bukan ukiran Toraja. Pastikan objek yang dipindai adalah motif ukiran Toraja, " +
                "serta pastikan pencahayaan cukup dan posisikan motif tepat di tengah bingkai."
        )
    }

    /** Pop-up saat Model 1 (gate) menolak -- citra tidak dikenali sebagai ukiran Toraja sama sekali. */
    private fun showNotTorajaMotifWarning() {
        showGuidanceDialog(
            title = "\u26A0\uFE0F Motif Ukiran Toraja Tidak Terdeteksi",
            message = "Citra ini tidak terdeteksi sebagai ukiran Toraja. Pastikan yang dipindai " +
                "benar-benar motif ukiran Toraja, lalu coba lagi."
        )
    }

    private fun getMotifDescription(motifName: String): String {
        return when (motifName) {
            "pa_ara_dena" -> "Ara' atau Dada. Dena' atau Burung pipit. Ukiran ini menyerupai bulu-bulu dada pada bumng pipit."
            "pa_barana" -> "Barana' atau Beringin ; Pa' barana' adalah ukiran yang menyerupai ranting dan daun beringin. Beringin adalah pohon yang besar, tinggi dan rindang/rimbun sehingga semua yang ada dibawahnya merasakan kesejukan dan keamanan."
            "pa_baranae'(Passape_Bai)" -> "Passape atau Dipisahkan atau Bahagian Bai atau Babi Ukiran Pa' Baranae' atau Passapa Bai adalah ukiran yang menyerupai mulut babi antara rahang bawah dengan rahang atas."
            "pa_bare_allo" -> "Barre atau Berre' atau Terbit (matahari terbit) Barre atau Bulat atau Matahari Bulat Allo atau Matahari Ukiran yang menyerupai bulatan matahari dengan pancaran sinarnya bagaikan sinar matahari yang baru terbit dipagi hari. Jenis ukiran ini ditemukan pada bagian muka dan belakang rumah ad at To ra ja p ad a p ap an a tas berbentuk segi tiga (Para Longa)."
            "pa_batang_lau" -> "Batang atau Batang. Lau atau Labu. Ukiran ini menggambarkan batang labu, yang hanya satu batang induk, kemudian bercabang lalu dari cabang tumbuh rantingnya dan dari ranting bertunas menjadi cabang dan seterusnya hingga mencapai ratusan meter panjangnya."
            "pa_bombo_uai" -> "Pa' Bombo Uai adalah ukiran yang menyerupai binatang air (bombo uai). Bombo uai adalah binatang yang dapat menitih air dan dapat bergerak sangat cepat."
            "pa_bulintong_somba" -> "Pa' Bulintong Somba' adalah pengembangan dari ukiran. Pa' Bulintong."
            "pa_bulu_londong" -> "Bulu atau Bulu Londong atau Ayam Jantan. Ukiran ini menyerupai rumbai ayam jantan."
            "pa_dadu" -> "Dadu yaitu sejenis benda segi empat sama sisi yang biasa digunakan untu bermain judi. Permainan dadu di Toraja merupakan sejenis judi yang digemari oleh sebagian masyarakat."
            "pa_don_paria" -> "Daun artinya daun “Paria” artinya sayur paria. Kita maklumi bahwa paria ini terkenal dengan tanaman pahit."
            "pa_don_bolu" -> "Don artinya daun, Bolu artinya Sirih. Ukiran yang menyerupai daun sirih, melambangkan keramahtamahan, persaudaraan, dan tanda penghormatan."
            "pa_doti" -> "Doti atau Ilmu (hitam) ; Doti atau Salego (tedong salego) ; Doti atau Baik atau Cantik Langi' atau Langit. Ukiran berupa palang yang berjejer- jejer dan ditengah-tengah ada semacam bintang bersinar bagaikan bintang diatas langit."
            "pa_doti_pandin" -> "Doti atau Kerbau belang atau Cantik atau Ilmu hitam Pandin atau Pemuda ganteng Pa' Doti Pandin adalah pengembangan dari ukiran Pa' Doti Langi'."
            "pa_erong" -> "Erong bagi masyarakat Toraja adalah sejenis peti yang setiap waktu dapat dibuka menurut adat untuk menyimpan tulang belulang dari satu rumpun keluarga. Jadi erong adalah peti tempat mengumpulkan tulang-tulang orang mati dalam satu rumpun yang biasa disimpan digua-gua."
            "pa_gayang" -> "Gaang (gayang) adalah ukiran yang menyerupai keris emas. Gayang adalah keris yang bukannya digunakan sebagai senjata melainkan lebih bermakna sebagai harta kemuliaan yang dipusakai turun temurun."
            "pa_kadang_pao" -> "Kadang atau Kait atau Jolok Pao atau Mangga Pa' Kadang Pao adalah ukiran yang menyerupai alat penjolok (pengait) mangga adalah"
            "pa_kangkung" -> "Kangkung adalah kerbau yang dibungkus kepalanya. Ukiran ini menyerupai kepala kerbau yang dibungkus dengan kulit kayu, yaitu kerbau yang somba, kemudian tali di kepala kerbau dihubungkan ke rumah melalui dinding yang dinamakan rinding datu (dinding aling atas)."
            "pa_kapu_baka" -> "Kapu' atau Ikat ; Baka atau Bakul ; Kapu' Baka atau Pengikat bakul tempat menyimpan perhiasan dan harta kekayaan rumah. Pa' kapu' baka adalah ukiran yang menyerupai simpulan-simpulan penutup bakul."
            "pa_komba_kalua" -> "Nama ukiran ini terdiri dari kata- kata boko' artinya belakang, komba artinya gelang, dan kalua' artinya luas atau besar atau lebar. Jadi pa' komba kalua' yaitu perhiasan gelang dari emas dan berbentuk manik-manik yang tersusun rapi menurut arsitektur khas Toraja."
            "pa_limbongan" -> "Limbongan atau Nama orang atau Ne' Limbongan Limbong atau Tenang Limbong atau Penampungan ikan berupa lobang besar yang sengaja dibuat dalam petak sawah yang kadang-kadang tidak pernah kering airnya. Dahulu kala Ne' Limbongan adalah arsitektur Toraja dan seorang pengukir pada j amannya."
            "pa_lolo_tabang" -> "Lolo tabang terdiri dari kata lolo artinya pucuk dan tabang artinya lenjuang adalah sejenis tumbuhan di Toraja yang dapat digunakan sebagai obat. Ukiran ini menyempai pucuk daun lenjuang."
            "pa_manuk_londong" -> "Manuk atau Ayam ; Londong atau Jantan Pa' manuk londong adalah ukiran berupa avam jantan, biasanya terdapat pada bagian muka dan belakang rumah adat Toraja pada papan atas berbentuk segitiga (Para Longa). Biasanya ukiran ayam jantan diletakkan diatas Pa' Barre Allo."
            "pa_ne_limbongan" -> "Limbongan atau Nama orang atau Ne' Limbongan Limbong atau Tenang Limbong atau Penampungan ikan berupa lobang besar yang sengaja dibuat dalam petak sawah yang kadang-kadang tidak pernah kering airnya. Dahulu kala Ne' Limbongan adalah arsitektur Toraja dan seorang pengukir pada j amannya."
            "pa_papan_kandaure" -> "Papan atau Sebidang atau Sebilah atau Selembar papan Kandaure atau Perhiasan tradisional Toraja yang dibuat dari butiran manik-manik yang beraneka ragam warnanya. Kandaure terdiri •."
            "pa_pollo_gayang" -> "Pollo atau Pantat atau Ujung bawah Gaang Keris Emas. Pa' Polio' Gaang adalah ukiran yang menyerupai polio' gang (ujung sebelah bawah dari keris emas)."
            "pa_rangga_ulu" -> "Rangga atau Banyak Ulu atau Kepala; Dalam ungkapan Toraja ada ungkapan: - Rangga Inaa artinya orang pintar dan bijaksana. - Rangga Lila artinya orang pintar berbicara."
            "pa_re_po" -> "Re'po artinya menari lincah sambil melipat lutut dalam bentuk siku-siku."
            "pa_sekong_kandaure" -> "Sekong atau Lekuk yang menyudut membentuk garis siku-siku. Kandaure - Perhiasan (benda) tradisional Toraja yang sangat berharga"
            "pa_sussuk" -> "Pa’Sussu’ ; Sussu - Garis, Goresan. Pa' sussu' adalah ukiran yang berbentuk garis-garis sejajar tanpa variasi dan tidak diberi warna."
            "pa_talinga" -> "Talinga atau Telinga Ukiran yang menyerupai telinga. Telinga adalah salah satu alat indra pada tubuh manusia yang sangat penting, yaitu untuk mendengar."
            "pa_tanduk_rape" -> "Tanduk atau Tanduk ; Ra'pe atau Tanduk yang pangkalnya melendut kebawah dan ujungnya melengkung ke atas. Bagi kerbau, tanduk adalah merupakan perisai atau alat untuk melawan dalam rangka melindungi diri atau menyerang lawan."
            "pa_tangke_lumu" -> "Tangke atau Cabang atau Carang Lumu' atau Lumut. Ukiran ini menyerupai carang carang tumbuhan lumut yang hidup didalam air."
            "pa_tangki_pattung" -> "Tangki' atau Alat semacam paku yang biasanya ditanamkan disebuah tiang dengan, maksud supaya kuat berkaitan. Ditangki' atau Supaya kuat berpegangan Pattung atau Bambu besar."
            "pa_tedong" -> "## Tedong atau Kerbau Ukiran ini biasa dilukiskan pada papan besar teratas (indo' para) dan pada dinding-dinding penyanggah badan rumah (manangnga banua), Bagi masyarakat Toraja kerbau adalah hewan paling tinggi nilai dan statusnya, untuk itu bagi masyarakat Toraja kerbau dijadikan sebagai standart/ukuran nilai dari semua harta / aset kekayaan"
            "Pa_tedong_tumuru" -> "Tedong atau Kerbau Tumuru atau Menderum. Tumuru artinya berjalan tanpa menghiraukan keadaan sekeliling."
            "pa_tangke_lumu_ditoke" -> "Tangke atau Cabang atau Carang Lumu' atau Lumut Ditoke atau Ditanam atau Dikembangkan. Ukiran ini menyerupai carang-carang tumbuhan lumut yang sengaja dikembangkan sebagai simbol kesuburan yang terus dipelihara."
            "pa_ulu_gayang" -> "Ulu atau Kepala Gayang (gaang) atau Keris emas Pa' Ulu Gaang adalah ukiran yang menyerupai kepala (tangkai) keris emas. Jadi merupakan bagian dari pada keris emas (gaang)."





            else -> "Makna motif ini belum tersedia di dalam sistem."
        }
    }

    // Fungsi untuk mengubah file galeri (URI) menjadi Gambar (Bitmap)
    private fun uriToBitmap(uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.isMutableRequired = true
                }
            } else {
                val inputStream = contentResolver.openInputStream(uri)
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Fungsi AI khusus untuk gambar dari Galeri atau hasil jepretan kamera --
    // memakai pipeline dua tahap (Model 1: gate ukiran Toraja, Model 2: klasifikasi motif).
    private fun processStaticImage(bitmap: Bitmap) {
        cameraExecutor.execute {
            try {
                val result = synchronized(aiLock) {
                    if (isDestroyedState || !::detectUIManager.isInitialized) return@synchronized null
                    detectUIManager.runPipeline(bitmap)
                }

                runOnUiThread {
                    if (isDestroyedState) return@runOnUiThread
                    when (result) {
                        null -> Unit
                        is DetectUIManager.PipelineResult.NotTorajaMotif -> showNotTorajaMotifWarning()
                        is DetectUIManager.PipelineResult.NotDetected -> showLowConfidenceWarning()
                        is DetectUIManager.PipelineResult.Detected -> {
                            textStatusKeyakinan.text = "Yakin"
                            textStatusKeyakinan.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                            textTop1.text = "1. ${result.label.replace("_", " ")} " +
                                String.format(java.util.Locale.US, "%.1f", result.confidence * 100) + "%"
                            textTop2.visibility = View.GONE
                            textTop3.visibility = View.GONE
                            // showMotifInfo() menampilkan panel hasil (nama, kategori, deskripsi,
                            // makna filosofis, tombol Audio Guide) DAN menyimpan ke riwayat karena isYakin=true.
                            showMotifInfo(result.label, isYakin = true, capturedBitmap = bitmap)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AI_ERROR", "Gagal memproses gambar statis: ${e.message}")
            }
        }
    }

    private fun computeAHash(bitmap: Bitmap): Long {
        val scaled = Bitmap.createScaledBitmap(bitmap, 8, 8, true)
        val pixels = IntArray(64)
        scaled.getPixels(pixels, 0, 8, 0, 0, 8, 8)
        var sum = 0L
        for (i in 0 until 64) {
            val p = pixels[i]
            val gray = (Color.red(p) + Color.green(p) + Color.blue(p)) / 3
            pixels[i] = gray
            sum += gray
        }
        val avg = sum / 64
        var hash = 0L
        for (i in 0 until 64) {
            if (pixels[i] >= avg) {
                hash = hash or (1L shl i)
            }
        }
        return hash
    }

    private fun hammingDistance(hash1: Long, hash2: Long): Int {
        return java.lang.Long.bitCount(hash1 xor hash2)
    }

    private fun takePictureAndProcess() {
        val capture = imageCapture ?: return
        if (isImageAnalysisPaused) return

        isImageAnalysisPaused = true
        Toast.makeText(this, "Mengambil foto...", Toast.LENGTH_SHORT).show()

        capture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val rotation = image.imageInfo.rotationDegrees
                    val bitmap = image.toBitmap()
                    image.close()

                    if (bitmap != null) {
                        val cropped = cropToScannerFrame(bitmap, rotation)
                        processStaticImage(cropped)
                    } else {
                        Toast.makeText(this@MainActivity, "Gagal memproses gambar", Toast.LENGTH_SHORT).show()
                        isImageAnalysisPaused = false
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CAPTURE", "Photo capture failed: ${exception.message}", exception)
                    Toast.makeText(this@MainActivity, "Gagal ambil foto", Toast.LENGTH_SHORT).show()
                    isImageAnalysisPaused = false
                }
            }
        )
    }

    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean {
        if (::scaleGestureDetector.isInitialized && !isPopupShowing && !isGalleryOpen) {
            scaleGestureDetector.onTouchEvent(ev)
        }
        return super.dispatchTouchEvent(ev)
    }
}