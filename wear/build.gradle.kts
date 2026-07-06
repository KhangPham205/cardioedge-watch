import org.gradle.kotlin.dsl.implementation

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.wearhelloapp"
    compileSdk = 36

//    signingConfigs {
//        create("release") {
//            storeFile = file("D:\\keystore\\my-samsung-key.jks") // Đường dẫn tuyệt đối đến file .jks
//            storePassword = "your_store_password"
//            keyAlias = "your_key_alias"
//            keyPassword = "your_key_password"
//        }
//    }

    defaultConfig {
        applicationId = "com.example.wearhelloapp"
        minSdk = 30
        targetSdk = 30
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.wear)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Chỉ giữ core TFLite — code chỉ dùng org.tensorflow.lite.Interpreter.
    // Đã gỡ: tensorflow-lite-support, tensorflow-lite-metadata, select-tf-ops (không dùng),
    // và onnxruntime-android (hướng ONNX/SLM đã bỏ). Nếu inference báo thiếu Flex op,
    // thêm lại "org.tensorflow:tensorflow-lite-select-tf-ops:2.17.0" đúng version với core.
    implementation("org.tensorflow:tensorflow-lite:2.17.0")
    implementation("com.google.android.gms:play-services-wearable:19.0.0")
    implementation(files("libs/samsung-health-sensor-api-1.4.1.aar"))
}