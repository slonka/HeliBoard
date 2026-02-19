plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    namespace = "helium314.keyboard.whisper"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
        ndk {
            abiFilters.clear()
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86_64"))
        }
        externalNativeBuild {
            cmake {
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
        // Match app's custom build types so variant resolution works
        create("nouserlib") {
            isMinifyEnabled = false
        }
        create("runTests") {
            isMinifyEnabled = false
        }
        create("debugNoMinify") {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }

    ndkVersion = "28.0.13004108"
    externalNativeBuild {
        cmake {
            path = file("src/main/jni/whisper/CMakeLists.txt")
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
