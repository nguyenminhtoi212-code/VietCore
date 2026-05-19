package com.example.myempty.vietcore

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.animation.*
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * RadarActivity: Hệ thống quét đa tầng VietCore v2.0.
 * Fix: Triệt tiêu hình vuông xanh, khớp hoàn toàn vòng tròn radar.
 * Developer: Nguyen Minh Toi.
 */
class RadarActivity : AppCompatActivity(), SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var rotationSensor: Sensor? = null
    
    private lateinit var radarSweep: View
    private lateinit var radarContainer: View
    private lateinit var imgAnomaly: ImageView
    private lateinit var dotSubsurface: View
    private lateinit var viewNoiseOverlay: View
    private lateinit var tvZoomControl: TextView
    private lateinit var btnSettings: ImageView
    private lateinit var tvStatusFooter: TextView
    private lateinit var tvConnectionStatus: TextView
    
    private lateinit var containerHumanDots: FrameLayout
    private lateinit var containerTechDots: FrameLayout

    private var isZoomed = false
    private val handler = Handler(Looper.getMainLooper())

    override fun attachBaseContext(newBase: Context) {
        val languageCode = LocaleHelper.getLanguage(newBase) ?: "en"
        super.attachBaseContext(LocaleHelper.setLocale(newBase, languageCode))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupModernEdgeToEdge()
        setContentView(R.layout.activity_radar)

        // Ánh xạ View
        radarSweep = findViewById(R.id.view_radar_sweep)
        radarContainer = findViewById(R.id.radar_container)
        imgAnomaly = findViewById(R.id.img_anomaly)
        dotSubsurface = findViewById(R.id.dot_subsurface)
        viewNoiseOverlay = findViewById(R.id.view_noise_overlay)
        tvZoomControl = findViewById(R.id.tv_zoom_control)
        btnSettings = findViewById(R.id.btn_quick_settings)
        tvStatusFooter = findViewById(R.id.tv_status_footer)
        tvConnectionStatus = findViewById(R.id.tv_connection_status)
        
        containerHumanDots = findViewById(R.id.container_human_dots)
        containerTechDots = findViewById(R.id.container_tech_dots)

        // TRIỆT TIÊU HÌNH VUÔNG: Xóa nền của container để không ảnh hưởng nền đen
        containerHumanDots.background = null
        containerTechDots.background = null
        containerHumanDots.clipChildren = false
        containerTechDots.clipChildren = false

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        startRadarSweep()
        startMultiLayerDetection()
        setupZoomLogic()
        setupQuickMenu()
        startRealTimeEntityTracking()

        val isEnabled = getSharedPreferences("settings", MODE_PRIVATE).getBoolean("radar_enabled", true)
        if (!isEnabled) exitRadarSystem()
    }

    private fun startRealTimeEntityTracking() {
        val entityRunnable = object : Runnable {
            override fun run() {
                if (isFinishing || isDestroyed) return

                containerHumanDots.removeAllViews()
                containerTechDots.removeAllViews()

                val humanCount = if (Random.nextBoolean()) Random.nextInt(1, 5) else 0
                val techCount = if (Random.nextBoolean()) Random.nextInt(1, 3) else 0

                if (humanCount > 0) {
                    for (i in 0 until humanCount) {
                        addEntityDot(containerHumanDots, Color.RED)
                    }
                }

                if (techCount > 0) {
                    for (i in 0 until techCount) {
                        addEntityDot(containerTechDots, Color.GREEN)
                    }
                }

                tvStatusFooter.text = if (humanCount > 0 || techCount > 0) {
                    "> DETECTED: $humanCount HUMAN | $techCount TECH UNITS"
                } else {
                    "> SCANNING: NO ENTITIES IN RANGE"
                }

                handler.postDelayed(this, 3000)
            }
        }
        handler.post(entityRunnable)
    }

    private fun addEntityDot(container: FrameLayout, color: Int) {
        val dot = View(this)
        val dotSize = 12 // Kích thước tinh tế hơn
        
        // TOÁN HỌC RADAR: Đảm bảo chấm nằm trong vòng tròn
        val maxRadius = (container.width / 2.3).coerceAtLeast(100.0)
        val randomRadius = Random.nextDouble(10.0, maxRadius)
        val randomAngle = Random.nextDouble(0.0, 2.0 * Math.PI)

        val x = (randomRadius * cos(randomAngle)).toInt()
        val y = (randomRadius * sin(randomAngle)).toInt()

        val params = FrameLayout.LayoutParams(dotSize, dotSize).apply {
            gravity = Gravity.CENTER
            this.leftMargin = x
            this.topMargin = y
        }
        
        dot.layoutParams = params
        
        val shape = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.OVAL
            setColor(color)
            setStroke(1, Color.argb(100, 255, 255, 255)) // Viền mờ nhẹ
        }
        dot.background = shape

        dot.startAnimation(AlphaAnimation(0.3f, 1.0f).apply {
            duration = Random.nextLong(400, 800)
            repeatMode = Animation.REVERSE
            repeatCount = Animation.INFINITE
        })
        
        container.addView(dot)
    }

    private fun startMultiLayerDetection() {
        val detectionRunnable = object : Runnable {
            override fun run() {
                if (isFinishing || isDestroyed) return
                val layers = arrayOf("SUBSURFACE", "EXTERNAL", "QUANTUM")
                updateRadarUI(layers[Random.nextInt(layers.size)])
                handler.postDelayed(this, Random.nextLong(4000, 8000))
            }
        }
        handler.post(detectionRunnable)
    }

    private fun updateRadarUI(layer: String) {
        dotSubsurface.visibility = View.GONE
        imgAnomaly.visibility = View.GONE
        viewNoiseOverlay.visibility = View.GONE

        when (layer) {
            "SUBSURFACE" -> {
                dotSubsurface.visibility = View.VISIBLE
                dotSubsurface.startAnimation(AlphaAnimation(0.2f, 1.0f).apply { duration = 300; repeatCount = 5 })
            }
            "EXTERNAL" -> { imgAnomaly.visibility = View.VISIBLE }
            "QUANTUM" -> {
                imgAnomaly.visibility = View.VISIBLE
                viewNoiseOverlay.visibility = View.VISIBLE
            }
        }
    }

    private fun setupQuickMenu() {
        btnSettings.setOnClickListener {
            val options = arrayOf(
                getString(R.string.menu_deactivate), 
                getString(R.string.menu_info), 
                getString(R.string.menu_cancel)
            )
            MaterialAlertDialogBuilder(this, R.style.VietCore_Terminal_Dialog)
                .setTitle(getString(R.string.menu_title))
                .setItems(options) { _, which ->
                    if (which == 0) deactivateAndExit()
                }.show()
        }
    }

    private fun deactivateAndExit() {
        getSharedPreferences("settings", MODE_PRIVATE).edit().putBoolean("radar_enabled", false).apply()
        exitRadarSystem()
    }

    private fun setupModernEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    private fun startRadarSweep() {
        val rotate = RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f).apply {
            duration = 3000
            repeatCount = Animation.INFINITE
            interpolator = LinearInterpolator()
        }
        radarSweep.startAnimation(rotate)
    }

    private fun setupZoomLogic() {
        tvZoomControl.setOnClickListener {
            isZoomed = !isZoomed
            val scale = if (isZoomed) 1.5f else 1.0f
            radarContainer.animate().scaleX(scale).scaleY(scale).setDuration(400).start()
        }
    }

    private fun exitRadarSystem() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
            val matrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(matrix, event.values)
            val orient = FloatArray(3)
            SensorManager.getOrientation(matrix, orient)
            radarContainer.rotation = -Math.toDegrees(orient[0].toDouble()).toFloat()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onResume() {
        super.onResume()
        rotationSensor?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
        handler.removeCallbacksAndMessages(null) 
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
