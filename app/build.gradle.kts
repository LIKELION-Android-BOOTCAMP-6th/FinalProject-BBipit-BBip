import java.util.Properties

val properties = Properties().apply {
    load(rootProject.file("local.properties").inputStream())
}
plugins {
    id("org.jetbrains.kotlin.kapt")

    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    alias(libs.plugins.hilt.android)
    kotlin("plugin.serialization") version "2.0.21"


}

android {
    namespace = "com.bbip.bbipit"
    compileSdk {
        version = release(36)
    }
    buildFeatures {
        buildConfig = true  // BuildConfig 활성화
    }

    defaultConfig {
        applicationId = "com.bbip.bbipit"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["MAP_KEY"] =
            properties["MAP_KEY"] ?: ""
        manifestPlaceholders["KAKAO_KEY"] =
            properties["KAKAO_KEY"] ?: ""

        buildConfigField("String", "KAKAO_KEY", "\"${properties["KAKAO_KEY"]}\"")
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    //파이어베이스 버전 관리자 bom
    implementation (platform("com.google.firebase:firebase-bom:34.10.0"))
    implementation("com.google.firebase:firebase-analytics")

    //파이어베이스 인증(이메일/구글)
    implementation("com.google.firebase:firebase-auth")
//    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)

    ///파이어스토어
    implementation("com.google.firebase:firebase-firestore")
    //fcm
    implementation("com.google.firebase:firebase-messaging")
    //storage
    implementation("com.google.firebase:firebase-storage")
    //functions
    implementation("com.google.firebase:firebase-functions")
//    realtime-database
//    implementation("com.google.firebase:firebase-database")

    //coil
    implementation(libs.coil)

    //kakao
    implementation(libs.kakao.user)
    implementation(libs.kakao.share)

    //google map
    implementation(libs.maps.compose)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location) //geofencing

    //hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation("androidx.compose.material:material-icons-extended")

    implementation(libs.compose.nav)
    implementation(libs.coroutine.core)
    implementation(libs.coroutine.android)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.serialization)

    implementation(libs.play.services.wearable)
    implementation(libs.kotlinx.coroutines.play.service)
}