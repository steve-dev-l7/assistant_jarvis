plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.kotlin.android)
}


android {
    namespace = "com.example.jarvis"
    compileSdk = 36


    defaultConfig {
        applicationId = "com.example.jarvis"
        minSdk = 26
        targetSdk = 35
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

    packaging {
        resources {
            excludes.add("META-INF/**") // Optional safety
            pickFirsts.add("**/*.so")
        }
        jniLibs {
            pickFirsts.add("**/libtensorflowlite_jni.so")
            pickFirsts.add("**/libtensorflowlite_runtime_jni.so")
            useLegacyPackaging = true // Namma 16KB fix ithukulla vanthuduchu!
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    androidResources {
        noCompress.add("task")
    }
}



dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.firestore)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.play.services.mlkit.language.id)
    implementation(libs.translate)
    implementation(libs.firebase.database)
    implementation(libs.play.services.auth)
    implementation(libs.biometric)
    implementation(libs.core)
    implementation(libs.play.services.location)
    implementation(libs.play.services.nearby)
    implementation(libs.lottie.v640)
    implementation(libs.localbroadcastmanager)
    //noinspection UseTomlInstead
    implementation("com.squareup.okhttp3:okhttp:5.3.2")
    //noinspection UseTomlInstead

    implementation(libs.tensorflow.lite)
    //noinspection UseTomlInstead

    implementation(libs.tensorflow.lite.support)

    // noinspection UseTomlInstead
    implementation("com.alphacephei:vosk-android:0.3.75")
    
    //noinspection UseTomlInstead

    implementation("com.google.mediapipe:tasks-genai:0.10.33")

}