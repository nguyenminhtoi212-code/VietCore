plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    kotlin("plugin.serialization") version "1.9.22"
}

base {
    // Tên file APK đầu ra đồng bộ với phiên bản NextGen
    archivesName.set("VietCore_v26.1.13-Beta-14_Final_NextGen")
}

android {
    namespace = "com.example.myempty.vietcore"
    
    // Giữ nguyên lên đời Android mới nhất (Android 16 dự kiến hoặc các bản preview)
    compileSdk = 36 

    defaultConfig {
        applicationId = "com.example.myempty.vietcore"
        minSdk = 35 
        targetSdk = 36 
        
        versionCode = 5 
        versionName = "26.1.13-Beta 14 [NextGen]"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters.clear()
            abiFilters.add("arm64-v8a") 
        }

        // --- VietCore Server Configuration ---
        buildConfigField("String", "SERVER_IP_CHECK_A", "\"https://checkip.amazonaws.com\"")
        buildConfigField("String", "SERVER_IP_CHECK_B", "\"https://api.ipify.org\"")
        
        manifestPlaceholders["audioPermission"] = "android.permission.RECORD_AUDIO"
        manifestPlaceholders["modifyAudio"] = "android.permission.MODIFY_AUDIO_SETTINGS"
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true 
        buildConfig = true 
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget("17"))
            freeCompilerArgs.addAll("-Xjsr305=strict", "-Xopt-in=kotlin.RequiresOptIn")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true 
            isShrinkResources = true 
            
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // Lưu ý: Thường release không để debuggable = true trừ khi bạn đang test đặc biệt
            // Giữ nguyên theo yêu cầu của Tới
            isDebuggable = true 
            signingConfig = signingConfigs.getByName("debug") 
        }
        
        debug {
            // FIX: Tắt Minify ở Debug để build nhanh hơn
            isMinifyEnabled = true
            isShrinkResources = true
            
            ndk {
                abiFilters.clear()
                abiFilters.add("arm64-v8a")
            }
        }
    }

    packaging {
        resources {
            pickFirsts.add("**/kotlin/**")
            pickFirsts.add("**/org/**")
            
            excludes += "**/firebase-*.properties"
            excludes += "**/com.google.firebase*.properties"
            excludes += "**/google-services.json"
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE*"
            excludes += "META-INF/NOTICE*"
            excludes += "META-INF/*.kotlin_module"
        }
        
        jniLibs {
            // Bật toàn bộ chế độ đóng gói cũ để tương thích với các lá chắn bảo mật
            useLegacyPackaging = true
            pickFirsts.add("**/lib*")
        }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false 
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // --- Core Libraries ---
    implementation("junit:junit:4.13.2")
    implementation("org.json:json:20231013")
    implementation("org.jetbrains:annotations:24.0.1")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.22")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    // Google Play Services & Integrity (Xác thực tính toàn vẹn)
    implementation("com.google.android.gms:play-services-basement:18.4.0")
    implementation("com.google.android.gms:play-services-base:18.5.0")
    implementation("com.google.android.play:integrity:1.3.0") 

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    
    // UI & Material Design
    implementation("com.google.android.material:material:1.11.0")
    implementation(libs.androidx.constraintlayout)

    // Network & Data (Xử lý dữ liệu bảo mật)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    
    // Security & Core System
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.media:media:1.7.0")
    
    // Permission Management
    implementation("com.guolindev.permissionx:permissionx:1.7.1")

    // --- BỔ SUNG CHO TÍNH NĂNG KIỂM TRA PHẦN CỨNG & HIỆU NĂNG ---
    // Hỗ trợ kiểm tra thông tin thiết bị chi tiết
    implementation("androidx.window:window:1.2.0")
    // Quản lý tài nguyên hệ thống tốt hơn cho các tính năng nặng (Radar, Scan)
    implementation("androidx.concurrent:concurrent-futures-ktx:1.1.0")
}
