plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.xiaocydx.inputview.sample"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.xiaocydx.inputview.sample"
        minSdk = 21
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(project(":inputview"))
    implementation(project(":inputview-compat"))
    val version = "1.1.2"
    implementation("com.github.xiaocydx.Insets:insets:${version}")
    implementation("com.github.xiaocydx.Insets:insets-compat:${version}")
    implementation("com.github.xiaocydx.Insets:insets-systembar:${version}")
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.7.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.2.0")
    implementation("androidx.fragment:fragment:1.6.1")
    implementation("androidx.fragment:fragment-ktx:1.6.1")
}