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

        // --- SIẾT CHẶT KIẾN TRÚC 64-BIT ---
        ndk {
            abiFilters.clear()
            abiFilters.add("arm64-v8a")
        }
    }

    // --- LOẠI BỎ DEXOPTIONS LỖI THỜI ĐỂ FIX CẢNH BÁO ---
    // Không sử dụng dexOptions {} vì Gradle hiện đại tự động tối ưu hóa Dexing.
    // Điều này giúp bản build sạch hoàn toàn, không để lại cảnh báo "obsolete".

    buildFeatures {
        viewBinding = true
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
            // Cơ chế siết chặt mã nguồn cao cấp
            isMinifyEnabled = true 
            isShrinkResources = true
            
            // Kích hoạt nén và làm rối mã nguồn sâu
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        
        debug {
            isMinifyEnabled = false
            // Giữ nguyên abiFilters cho bản Debug để đồng bộ
            ndk {
                abiFilters.add("arm64-v8a")
            }
        }
    }

    // --- CƠ CHẾ NGĂN CHẶN CAN THIỆP APK (INTEGRITY SHIELD) ---
    packaging {
        resources {
            // Loại bỏ các tệp metadata nhạy cảm
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "**/kotlin/**"
            excludes += "**/security-notice.txt"
        }
    }

    // Fix lỗi splits trùng lặp cấu hình
    splits {
        abi {
            isEnable = false
        }
    }
}

dependencies {
    // --- HỖ TRỢ JAVA DESUGARING ---
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // Thư viện hệ thống cơ bản
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)

    // --- HỆ THỐNG AN NINH & KẾT NỐI ĐỒNG BỘ 26.1.2 ---
    // Đồng bộ với SecurityClient, OmnisSecurity và DeviceSimulator
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
}
