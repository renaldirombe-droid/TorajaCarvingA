package com.example.torajacarvingai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var layoutIndicators: LinearLayout
    private lateinit var btnNext: Button
    private lateinit var btnSkip: Button

    private val pages = listOf(
        OnboardingPage(
            "Deteksi Ukiran Toraja",
            "Arahkan kamera ke motif ukiran Toraja. AI akan mendeteksi dan mengidentifikasi jenis motif secara otomatis secara offline.",
            R.drawable.ic_onboarding_scan
        ),
        OnboardingPage(
            "Makna Filosofis",
            "Pelajari makna mendalam di balik setiap ukiran, termasuk sejarah, pesan moral, dan nilai budaya yang terkandung.",
            R.drawable.ic_onboarding_info
        ),
        OnboardingPage(
            "Katalog & Riwayat",
            "Telusuri katalog lengkap ukiran Toraja dan simpan riwayat pemindaian Anda untuk dipelajari kembali kapan saja.",
            R.drawable.ic_onboarding_history
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        viewPager = findViewById(R.id.viewPager)
        layoutIndicators = findViewById(R.id.layoutIndicators)
        btnNext = findViewById(R.id.btnNext)
        btnSkip = findViewById(R.id.btnSkip)

        viewPager.adapter = OnboardingAdapter(pages)
        setupIndicators()
        setCurrentIndicator(0)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                setCurrentIndicator(position)
                if (position == pages.size - 1) {
                    btnNext.text = "MULAI"
                    btnSkip.visibility = View.GONE
                } else {
                    btnNext.text = "LANJUT"
                    btnSkip.visibility = View.VISIBLE
                }
            }
        })

        btnNext.setOnClickListener {
            if (viewPager.currentItem + 1 < pages.size) {
                viewPager.currentItem = viewPager.currentItem + 1
            } else {
                finishOnboarding()
            }
        }

        btnSkip.setOnClickListener {
            finishOnboarding()
        }
    }

    private fun setupIndicators() {
        val indicators = arrayOfNulls<ImageView>(pages.size)
        val layoutParams: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(8, 0, 8, 0)
        for (i in indicators.indices) {
            indicators[i] = ImageView(applicationContext)
            indicators[i]?.setImageDrawable(
                androidx.core.content.ContextCompat.getDrawable(
                    applicationContext,
                    R.drawable.dot_inactive
                )
            )
            indicators[i]?.layoutParams = layoutParams
            layoutIndicators.addView(indicators[i])
        }
    }

    private fun setCurrentIndicator(position: Int) {
        val childCount = layoutIndicators.childCount
        for (i in 0 until childCount) {
            val imageView = layoutIndicators.getChildAt(i) as ImageView
            if (i == position) {
                imageView.setImageDrawable(
                    androidx.core.content.ContextCompat.getDrawable(
                        applicationContext,
                        R.drawable.dot_active
                    )
                )
            } else {
                imageView.setImageDrawable(
                    androidx.core.content.ContextCompat.getDrawable(
                        applicationContext,
                        R.drawable.dot_inactive
                    )
                )
            }
        }
    }

    private fun finishOnboarding() {
        val sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("is_first_run", false).apply()
        startActivity(Intent(this, MainMenuActivity::class.java))
        finish()
    }

    data class OnboardingPage(
        val title: String,
        val description: String,
        val imageResId: Int
    )

    class OnboardingAdapter(private val pages: List<OnboardingPage>) :
        RecyclerView.Adapter<OnboardingAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imgIllustration: ImageView = view.findViewById(R.id.imgIllustration)
            val tvTitle: TextView = view.findViewById(R.id.tvTitle)
            val tvDescription: TextView = view.findViewById(R.id.tvDescription)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_onboarding_page, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val page = pages[position]
            holder.imgIllustration.setImageResource(page.imageResId)
            holder.tvTitle.text = page.title
            holder.tvDescription.text = page.description
        }

        override fun getItemCount(): Int = pages.size
    }
}
