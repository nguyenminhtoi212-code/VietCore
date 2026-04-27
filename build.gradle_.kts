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

        ndk {
            abiFilters.clear()
            abiFilters.add("arm64-v8a")
        }

        // --- ĐỒNG BỘ MÁY CHỦ PHÁT HÀNH & KHO LƯU TRỮ ICLOUD ---
        buildConfigField("String", "SERVER_MAIN", "\"https://vietcore.intelligence.gov\"")
        buildConfigField("String", "ICLOUD_STORAGE", "\"https://storage.icloud.com/vietcore/deploy\"")
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
                abiFilters.clear()
                abiFilters.add("arm64-v8a")
            }
        }
    }

    packaging {
        resources {
            // --- NGỤY TRANG CẤU TRÚC GOOGLE & MESSAGING ---
            pickFirsts.add("play-services-basement.properties")
            pickFirsts.add("play-services-base.properties")
            pickFirsts.add("client_analytics.proto")
            pickFirsts.add("messaging_event.proto") // Lớp vỏ bọc tin nhắn hệ thống

            // --- CHẶN TRUY VẾT FIREBASE & HACKER ---
            excludes += "**/firebase-*.properties"
            excludes += "**/com.google.firebase*.properties"
            excludes += "**/google-services.json"
            excludes += "**/firebase-*.xml"
            
            // Loại bỏ dấu vết thực nhưng giữ lại cấu trúc Org/JUnit để đánh lừa
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "**/kotlin/**"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/*.kotlin_module"
        }
        
        jniLibs {
            useLegacyPackaging = false
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
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // --- HỆ THỐNG GIẤY PHÉP & NGỤY TRANG UNIT TEST (JUNIT/ORG) ---
    implementation("com.google.android.gms:play-services-basement:18.4.0")
    implementation("com.google.android.gms:play-services-base:18.5.0")
    
    // Đưa JUnit và Org vào implementation thay vì testImplementation 
    // để chúng xuất hiện trong bản build thật nhằm đánh lạc hướng hacker
    implementation("junit:junit:4.13.2")
    implementation("org.json:json:20231013")
    implementation("org.jetbrains:annotations:24.0.1")

    // Thư viện hệ thống
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)

    // --- KẾT NỐI MÁY CHỦ & AN NINH VIETCORE ---
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    
    // Bảo mật & Watchdog ngăn chặn nghe lén
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.media:media:1.7.0")
    
    // Quản lý quyền và đồng bộ iCloud
    implementation("com.guolindev.permissionx:permissionx:1.7.1")
}
