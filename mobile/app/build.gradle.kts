
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

val supabaseUrl = localProperties.getProperty("SUPABASE_URL", "")
val supabasePublishableKey =
    localProperties.getProperty("SUPABASE_PUBLISHABLE_KEY", "")
// Địa chỉ FastAPI. Mặc định là bản đang chạy trên Render.
// Chạy backend trên máy tính + máy ảo: đặt API_BASE_URL=http://10.0.2.2:8000 trong local.properties.
val apiBaseUrl = localProperties.getProperty(
    "API_BASE_URL",
    "https://lost-found-api-ph8v.onrender.com"
)

android {
    namespace = "com.lostfound.app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.lostfound.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "SUPABASE_URL",
            "\"$supabaseUrl\""
        )

        buildConfigField(
            "String",
            "SUPABASE_PUBLISHABLE_KEY",
            "\"$supabasePublishableKey\""
        )

        buildConfigField(
            "String",
            "API_BASE_URL",
            "\"$apiBaseUrl\""
        )
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Phần B: điều hướng, ViewModel, icon, mạng, ảnh, vị trí
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.exifinterface)
    implementation(libs.okhttp)
    implementation(libs.coil.compose)
    implementation(libs.play.services.location)

    testImplementation(libs.junit)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
