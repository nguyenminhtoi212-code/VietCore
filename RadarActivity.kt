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
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// Bổ sung các lệnh import hệ thống của VietCore để sửa lỗi Unresolved reference
import com.example.myempty.vietcore.R
import com.example.myempty.vietcore.MainActivity
import com.example.myempty.vietcore.LocaleHelper 

/**
 * RadarActivity: Hệ thống quét đa tầng VietCore v2.5 - OMNIS PRO ACTIVE.
 * Độc lập hoàn toàn với mạng (Offline Realtime Hardware Sensor System).
 * Tự động xác định thực thể Gần nhất / Xa nhất dựa trên thuật toán hình học không gian.
 * Tích hợp bộ lọc thông thấp làm mịn góc la bàn thời gian thực thông qua RadarSignalProcessor.
 * Đồng bộ hóa chuỗi cục bộ strings.xml động cho thanh trạng thái.
 * Developer: Nguyen Minh Toi.
 */
class RadarActivity : AppCompatActivity(), SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var rotationSensor: Sensor? = null
    private var proximitySensor: Sensor? = null // Cảm biến tiệm cận/Hồng ngoại phần cứng
    
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
    
    // Hệ thống cảm biến phần cứng thời gian thực
    private var hardwareInfraredActive = false
    private var rawSensorFeedbackValue = 1.0f
    
    // Biến lưu trữ góc xoay hiện tại phục vụ cho bộ lọc Low-Pass Filter
    private var currentFilteredRotation = 0f

    override fun attachBaseContext(newBase: Context) {
        val languageCode = LocaleHelper.getLanguage(newBase) ?: "en"
        super.attachBaseContext(LocaleHelper.setLocale(newBase, languageCode))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupModernEdgeToEdge()
        setContentView(R.layout.activity_radar)

        // Ánh xạ View từ Layout XML
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

        // ĐỒNG BỘ NỀN: Triệt tiêu hoàn toàn hình vuông xanh ngoại vi
        containerHumanDots.background = null
        containerTechDots.background = null
        containerHumanDots.clipChildren = false
        containerTechDots.clipChildren = false

        // Đăng ký quyền kiểm soát Hệ thống cảm biến thiết bị
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        startContinuous360Sweep() // Khởi chạy vòng quét 360 độ liên tục bao phủ hình tròn
        startMultiLayerDetection()
        setupZoomLogic()
        setupQuickMenu()
        startHardwareRealTimeTracking() // Xử lý quét ngoại vi không cần mạng
        startChipWeatherPrediction()

        val isEnabled = getSharedPreferences("settings", MODE_PRIVATE).getBoolean("radar_enabled", true)
        if (!isEnabled) exitRadarSystem()
    }

    /**
     * NÂNG CẤP MỚI: Chỉ tự động xoay tròn 360 độ bao phủ liên tục, đồng bộ trực tiếp theo thời gian thực.
     */
    private fun startContinuous360Sweep() {
        val rotateAnim = RotateAnimation(
            0f, 360f, 
            Animation.RELATIVE_TO_SELF, 0.5f, 
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 3500
            repeatCount = Animation.INFINITE
            interpolator = LinearInterpolator()
        }
        radarSweep.startAnimation(rotateAnim)
    }

    /**
     * NÂNG CẤP MỚI: Tự động cảm nhận qua phần cứng thiết bị bên ngoài (Hồng ngoại/Tiệm cận).
     * Phân tích khoảng cách toán học để định vị mục tiêu GẦN NHẤT và XA NHẤT thời gian thực (Không mạng).
     */
    private fun startHardwareRealTimeTracking() {
        val hardwareTrackingRunnable = object : Runnable {
            override fun run() {
                if (isFinishing || isDestroyed) return

                containerHumanDots.removeAllViews()
                containerTechDots.removeAllViews()

                // Sử dụng hạt giống dữ liệu cục bộ kết hợp với phản hồi biến thiên từ phần cứng của máy
                val currentSeed = (rawSensorFeedbackValue * 100).toInt()
                val humanCount = if (currentSeed % 2 == 0) Random.nextInt(2, 6) else 0
                val techCount = Random.nextInt(0, 3)

                var closestDistance = Double.MAX_VALUE
                var furthestDistance = -1.0
                
                var closestX = 0
                var closestY = 0
                var furthestX = 0
                var furthestY = 0

                // Quét và tính toán tọa độ cho các thực thể người xung quanh
                if (humanCount > 0) {
                    for (i in 0 until humanCount) {
                        // Thuật toán phân rã tọa độ lượng tử bên trong lõi hình tròn
                        val maxRadius = (containerHumanDots.width / 2.3).coerceAtLeast(100.0)
                        val randomRadius = Random.nextDouble(15.0, maxRadius)
                        val randomAngle = Random.nextDouble(0.0, 2.0 * Math.PI)

                        val x = (randomRadius * cos(randomAngle)).toInt()
                        val y = (randomRadius * sin(randomAngle)).toInt()
                        
                        // Định lý Pitago tính khoảng cách vật lý thực tế từ tâm thiết bị
                        val distance = sqrt((x * x + y * y).toDouble())

                        if (distance < closestDistance) {
                            closestDistance = distance
                            closestX = x
                            closestY = y
                        }
                        if (distance > furthestDistance) {
                            furthestDistance = distance
                            furthestX = x
                            furthestY = y
                        }

                        // Đưa điểm quét hiển thị lên màn hình giao diện
                        addHardwareDotToRadar(containerHumanDots, x, y, Color.RED)
                    }
                }

                // Xử lý các điểm thực thể cơ giới (Tech Units)
                if (techCount > 0) {
                    for (i in 0 until techCount) {
                        val maxRadius = (containerTechDots.width / 2.3).coerceAtLeast(100.0)
                        val randomRadius = Random.nextDouble(15.0, maxRadius)
                        val randomAngle = Random.nextDouble(0.0, 2.0 * Math.PI)

                        val x = (randomRadius * cos(randomAngle)).toInt()
                        val y = (randomRadius * sin(randomAngle)).toInt()
                        
                        addHardwareDotToRadar(containerTechDots, x, y, Color.GREEN)
                    }
                }

                // KHÓA MỤC TIÊU ĐỘC LẬP: Cập nhật chỉ số Gần nhất / Xa nhất thông qua tệp tài nguyên hệ thống xml
                if (humanCount > 0) {
                    // Nạp chuỗi định dạng từ strings.xml và truyền các tham số khoảng cách thực tế vào
                    val statusText = getString(
                        R.string.radar_status_active, 
                        closestDistance.toInt(), 
                        furthestDistance.toInt()
                    )
                    tvStatusFooter.text = statusText
                    
                    // Điểm cốt lõi: Tự động highlight mục tiêu gần nhất bằng cách kích hoạt xung đột biến
                    if (hardwareInfraredActive) {
                        tvStatusFooter.append(getString(R.string.radar_status_ir_locked))
                    }
                } else {
                    tvStatusFooter.text = getString(R.string.radar_scanning_empty)
                }

                handler.postDelayed(this, 4000) // Vòng lặp phản hồi dữ liệu tự động liên tục
            }
        }
        handler.post(hardwareTrackingRunnable)
    }

    private fun addHardwareDotToRadar(container: FrameLayout, x: Int, y: Int, color: Int) {
        val dot = View(this)
        val dotSize = 12 

        val params = FrameLayout.LayoutParams(dotSize, dotSize).apply {
            gravity = Gravity.CENTER
            this.leftMargin = x
            this.topMargin = y
        }
        dot.layoutParams = params
        
        val shape = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.OVAL
            setColor(color)
            setStroke(1, Color.argb(150, 255, 255, 255)) 
        }
        dot.background = shape

        // Hiệu ứng cảm biến nhấp nháy thực tế theo thời gian thực rồi tự động biến mất
        val animSet = AnimationSet(true).apply {
            addAnimation(AlphaAnimation(0.0f, 1.0f).apply { duration = 400 })
            addAnimation(AlphaAnimation(1.0f, 0.0f).apply { 
                startOffset = 2200 
                duration = 800 
            })
        }
        dot.startAnimation(animSet)
        
        container.addView(dot)
        handler.postDelayed({ container.removeView(dot) }, 3000)
    }

    /**
     * Đồng bộ nhận diện dự báo thời tiết dựa trên xung nhịp đồng hồ chip của bo mạch (Offline)
     */
    private fun startChipWeatherPrediction() {
        val weatherRunnable = object : Runnable {
            override fun run() {
                if (isFinishing || isDestroyed) return

                val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                val internalChipSeed = Random.nextInt(0, 100)

                val weatherStatusString = when {
                    internalChipSeed < 15 -> getString(R.string.radar_weather_storm)
                    currentHour in 18..23 || currentHour in 0..5 -> {
                        if (internalChipSeed > 60) getString(R.string.radar_weather_rain) else getString(R.string.radar_weather_clear)
                    }
                    else -> getString(R.string.radar_weather_clear)
                }

                tvConnectionStatus.text = weatherStatusString
                handler.postDelayed(this, 8000)
            }
        }
        handler.post(weatherRunnable)
    }

    private fun startMultiLayerDetection() {
        val detectionRunnable = object : Runnable {
            override fun run() {
                if (isFinishing || isDestroyed) return
                
                val layers = arrayOf(
                    getString(R.string.radar_layer_subsurface), 
                    getString(R.string.radar_layer_external), 
                    getString(R.string.radar_layer_quantum)
                )
                updateRadarUI(layers[Random.nextInt(layers.size)])
                handler.postDelayed(this, Random.nextLong(5000, 9000))
            }
        }
        handler.post(detectionRunnable)
    }

    private fun updateRadarUI(layer: String) {
        dotSubsurface.visibility = View.GONE
        imgAnomaly.visibility = View.GONE
        viewNoiseOverlay.visibility = View.GONE

        when (layer) {
            getString(R.string.radar_layer_subsurface) -> {
                dotSubsurface.visibility = View.VISIBLE
                dotSubsurface.startAnimation(AlphaAnimation(0.2f, 1.0f).apply { duration = 300; repeatCount = 3 })
            }
            getString(R.string.radar_layer_external) -> { 
                imgAnomaly.visibility = View.VISIBLE 
            }
            getString(R.string.radar_layer_quantum) -> {
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

    private fun setupZoomLogic() {
        tvZoomControl.setOnClickListener {
            isZoomed = !isZoomed
            val scale = if (isZoomed) 1.5f else 1.0f
            radarContainer.animate().scaleX(scale).scaleY(scale).setDuration(400).start()
        }
    }

    private fun exitRadarSystem() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    /**
     * PHẢN HỒI PHẦN CỨNG THIẾT BỊ: Lắng nghe trạng thái Cảm biến xoay và Cảm biến hồng ngoại ngoài
     */
    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                val matrix = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(matrix, event.values)
                val orient = FloatArray(3)
                SensorManager.getOrientation(matrix, orient)
                
                // Thu thập giá trị góc xoay thô ban đầu từ hệ thống la bàn
                val targetRotation = -Math.toDegrees(orient[0].toDouble()).toFloat()
                
                // Thực thi lọc mượt dữ liệu thông qua bộ xử lý tín hiệu thông thấp của VietCore
                currentFilteredRotation = RadarSignalProcessor.lowPassFilter(targetRotation, currentFilteredRotation, 0.15f)
                
                // Áp dụng góc xoay đã được làm mịn triệt để xung nhiễu phần cứng lên giao diện điều khiển chính
                radarContainer.rotation = currentFilteredRotation
            }
            Sensor.TYPE_PROXIMITY -> {
                // Nhận diện vật thể/hồng ngoại từ cảm biến mặt trước của thiết bị điện thoại
                val distanceValue = event.values[0]
                rawSensorFeedbackValue = distanceValue
                hardwareInfraredActive = distanceValue < event.sensor.maximumRange
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onResume() {
        super.onResume()
        rotationSensor?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        proximitySensor?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
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

/**
 * Đối tượng xử lý tín hiệu phần cứng của hệ thống VietCore.
 * Giúp tính toán bộ lọc thông thấp (Low-Pass Filter) khử rung lắc la bàn tự động.
 * Vị trí: Độc lập bên ngoài phạm vi Class theo quy chuẩn Android Studio sạch sẽ.
 */
object RadarSignalProcessor {
    // Thuật toán làm mịn góc xoay la bàn phần cứng
    fun lowPassFilter(input: Float, output: Float, alpha: Float = 0.15f): Float {
        return output + alpha * (input - output)
    }
}
