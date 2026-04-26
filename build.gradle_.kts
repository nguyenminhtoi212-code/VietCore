plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.myempty.vietcore"
    compileSdk = 36 

    defaultConfig {
        applicationId = "com.example.myempty.vietcore"
        minSdk = 34
        targetSdk = 34
        versionCode = 1
        versionName = "26.1.2-Omnis-Pro"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // --- SIẾT CHẶT KIẾN TRÚC 64-BIT TUYỆT ĐỐI ---
        // Loại bỏ hoàn toàn hỗ trợ 32-bit để ngăn chặn các công cụ truy vết cũ
        ndk {
            abiFilters.clear()
            abiFilters.add("arm64-v8a")
        }

        // --- ĐỒNG BỘ ĐA MÁY CHỦ NHÀ CUNG CẤP (TRIPLE-SYNC) ---
        // Tích hợp đồng bộ IP và máy chủ quản lý cùng lúc
        buildConfigField("String", "SERVER_MAIN", "\"https://vietcore.intelligence.gov\"")
        buildConfigField("String", "SERVER_IP_CHECK_A", "\"https://checkip.amazonaws.com\"")
        buildConfigField("String", "SERVER_IP_CHECK_B", "\"https://api.ipify.org\"")
        
        // Placeholder cho Manifest - Tự động hóa quyền Audio hệ thống
        manifestPlaceholders["audioPermission"] = "android.permission.RECORD_AUDIO"
        manifestPlaceholders["modifyAudio"] = "android.permission.MODIFY_AUDIO_SETTINGS"
    }

    // --- FIX LỖI DSL: LOẠI BỎ DEXOPTIONS OBSOLETE ---
    // Gradle hiện đại tự động quản lý Dexing, xóa bỏ block này để build sạch 100%

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
            freeCompilerArgs.addAll("-Xjsr305=strict")
        }
    }

    buildTypes {
        release {
            // Cơ chế siết chặt mã nguồn cao cấp (Anti-Reverse Engineering)
            isMinifyEnabled = true 
            isShrinkResources = true
            
            // Proguard tối ưu hóa sâu, xóa sạch dấu vết hàm và biến dư thừa
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        
        debug {
            isMinifyEnabled = false
            ndk {
                abiFilters.add("arm64-v8a")
            }
        }
    }

    // --- HỆ THỐNG INTEGRITY SHIELD (XÓA SẠCH DẤU VẾT) ---
    packaging {
        resources {
            // Loại bỏ toàn bộ tệp Metadata nhạy cảm, không để lại "sợi tóc" nào
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "**/kotlin/**"
            excludes += "**/security-notice.txt"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE*"
            excludes += "META-INF/NOTICE*"
            excludes += "META-INF/*.kotlin_module"
        }
        
        jniLibs {
            useLegacyPackaging = false
        }
    }

    splits {
        abi {
            isEnable = false
        }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    // --- HỖ TRỢ JAVA DESUGARING CAO CẤP ---
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // Thư viện hệ thống cơ bản (Full Version)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)

    // --- HỆ THỐNG AN NINH & KẾT NỐI ĐỒNG BỘ ĐA MÁY CHỦ ---
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    
    // Bảo vệ tính toàn vẹn và mã hóa AES-256 cho dữ liệu máy chủ
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // Duy trì giám sát ngăn chặn ghi âm ngay cả khi chạy ngầm (Silent Monitor)
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    
    // Thư viện xử lý Audio chuyên sâu để nhận diện và vô hiệu hóa nghe lén
    implementation("androidx.media:media:1.7.0")
    
    // Hỗ trợ kiểm tra trạng thái mạng và IP nâng cao
    implementation("com.guolindev.permissionx:permissionx:1.7.1")
}
