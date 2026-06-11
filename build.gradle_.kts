import org.ajoberstar.grgit.Grgit

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    kotlin("plugin.serialization") version "1.9.22"
    id("com.google.devtools.ksp") version "1.9.22-1.0.17"
    // Plugin hỗ trợ Git trong Gradle
    id("org.ajoberstar.grgit") version "5.2.2"
}

base {
    archivesName.set("VietCore_v26.1.13-Beta-14_Final_NextGen")
}

android {
    namespace = "com.example.myempty.vietcore"
    compileSdk = 36 

    defaultConfig {
        applicationId = "com.example.myempty.vietcore"
        minSdk = 35 
        targetSdk = 36 
        
        versionCode = 5 
        versionName = "26.1.15-Beta 16"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters.clear()
            abiFilters.add("arm64-v8a") 
        }

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
            freeCompilerArgs.addAll(
                "-Xjsr305=strict", 
                "-Xopt-in=kotlin.RequiresOptIn",
                "-Xemit-jvm-type-annotations",
                "-Xno-call-assertions",
                "-Xno-param-assertions"
            )
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true 
            isShrinkResources = true 
            isDebuggable = false 
            
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug") 
        }
        
        debug {
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
            excludes += setOf(
                "**/firebase-*.properties",
                "**/com.google.firebase*.properties",
                "**/google-services.json",
                "/META-INF/{AL2.0,LGPL2.1}",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE*",
                "META-INF/NOTICE*",
                "META-INF/*.kotlin_module"
            )
        }
        jniLibs {
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

    // --- DỮ LIỆU API & CHUYỂN ĐỔI ---
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    
    // --- Networking ---
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // --- BỔ SUNG: DỊCH VỤ GITHUB API ---
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("org.kohsuke:github-api:1.318") 

    // Google Play Services & Integrity
    implementation("com.google.android.gms:play-services-basement:18.4.0")
    implementation("com.google.android.gms:play-services-base:18.5.0")
    implementation("com.google.android.play:integrity:1.3.0") 

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    
    // UI & Material Design
    implementation("com.google.android.material:material:1.11.0")
    implementation(libs.androidx.constraintlayout)

    // Async & Lifecycle
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    
    // Security & Core System
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.media:media:1.7.0")
    implementation("com.guolindev.permissionx:permissionx:1.7.1")
    implementation("androidx.biometric:biometric:1.2.0-alpha05")

    // Hardware & Utilities
    implementation("androidx.window:window:1.2.0")
    implementation("androidx.concurrent:concurrent-futures-ktx:1.1.0")
}

// Cấu hình kết nối kho lưu trữ GitHub
tasks.register("pushToGitHub") {
    group = "versioning"
    description = "Đẩy bản build mới lên GitHub Releases/Tags."
    doLast {
        val grgit = Grgit.open(mapOf("currentDir" to project.rootDir))
        println("VietCore Sync: Đang đẩy phiên bản ${android.defaultConfig.versionName} lên GitHub...")
        
        // Logic tự động tag phiên bản
        grgit.tag.add(mapOf("name" to "v${android.defaultConfig.versionName}"))
        grgit.push()
        println("VietCore Sync: Hoàn tất đẩy tag lên GitHub.")
    }
}
