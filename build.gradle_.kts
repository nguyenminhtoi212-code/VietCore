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
        ndk {
            abiFilters.clear()
            abiFilters.add("arm64-v8a")
        }

        // --- ĐỒNG BỘ ĐA MÁY CHỦ NHÀ CUNG CẤP ---
        buildConfigField("String", "SERVER_MAIN", "\"https://vietcore.intelligence.gov\"")
        buildConfigField("String", "SERVER_IP_CHECK_A", "\"https://checkip.amazonaws.com\"")
        buildConfigField("String", "SERVER_IP_CHECK_B", "\"https://api.ipify.org\"")
        
        manifestPlaceholders["audioPermission"] = "android.permission.RECORD_AUDIO"
        manifestPlaceholders["modifyAudio"] = "android.permission.MODIFY_AUDIO_SETTINGS"
    }

    // --- FIX LỖI: LOẠI BỎ HOÀN TOÀN DEXOPTIONS (OBSOLETE) ---
    // Không khai báo dexOptions để trình biên dịch tự động tối ưu hóa.

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
            isMinifyEnabled = true 
            isShrinkResources = true
            
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

    // --- FIX LỖI: CHỈNH SỬA PICKFIRST SANG CẤU TRÚC MỚI ---
    packaging {
        resources {
            // Thay thế pickFirst cũ bằng cấu trúc pickFirsts.add() để fix lỗi cảnh báo
            pickFirsts.add("META-INF/library_release.kotlin_module")
            pickFirsts.add("play-services-basement.properties")

            // Loại bỏ các tệp nhạy cảm để hacker không tìm thấy dấu vết
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
            // Áp dụng pickFirsts cho các tệp .so nếu có xung đột
            pickFirsts.add("**/lib*")
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

    // Thư viện hệ thống Google Play Services (Tạo giấy phép thật)
    implementation("com.google.android.gms:play-services-basement:18.4.0")
    
    // Thư viện hệ thống cơ bản (Full Version)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)

    // --- AN NINH & ĐỒNG BỘ ĐA MÁY CHỦ ---
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    
    // Bảo vệ tính toàn vẹn và mã hóa
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.media:media:1.7.0")
    
    // Quản lý quyền tự động nâng cao
    implementation("com.guolindev.permissionx:permissionx:1.7.1")
}
