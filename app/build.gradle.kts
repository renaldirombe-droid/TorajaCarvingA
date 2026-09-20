import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(keystorePropertiesFile.inputStream())
}

android {
    namespace = "com.example.torajacarvingai"
    compileSdk = 34
    androidResources {
        noCompress.add("tflite")
    }
    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.example.torajacarvingai"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    // Library untuk Kamera
    val camerax_version = "1.4.0"
    implementation("androidx.camera:camera-core:$camerax_version")
    implementation("androidx.camera:camera-camera2:$camerax_version")
    implementation("androidx.camera:camera-lifecycle:$camerax_version")
    implementation("androidx.camera:camera-view:$camerax_version")
    implementation("org.tensorflow:tensorflow-lite:2.17.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4") {
        exclude(group = "org.tensorflow", module = "tensorflow-lite")
        exclude(group = "org.tensorflow", module = "tensorflow-lite-api")
    }
    implementation("org.tensorflow:tensorflow-lite-task-vision:0.4.4") {
        exclude(group = "org.tensorflow", module = "tensorflow-lite")
        exclude(group = "org.tensorflow", module = "tensorflow-lite-api")
    }
    implementation("com.google.code.gson:gson:2.10.1")
    // Library untuk Pemotongan Gambar (Gallery Crop)
    implementation("com.vanniktech:android-image-cropper:4.7.0")
    // Library untuk load gambar motif dari image_url (motif baru dari online update)
    implementation("io.coil-kt:coil:2.6.0")
}