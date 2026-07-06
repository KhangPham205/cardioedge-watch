plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.wearhelloapp"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.wearhelloapp"
        minSdk = 26
        targetSdk = 36
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
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.work.runtime)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation("com.google.android.gms:play-services-wearable:19.0.0")
    implementation(libs.material.v190)

    // MVVM: ViewModel + LiveData (Transformations.switchMap)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)

    // Room cho Java: dùng annotationProcessor
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)

    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    // Đã gỡ Gson: không có class nào sử dụng.
}