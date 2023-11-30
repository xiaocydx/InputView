import org.jetbrains.kotlin.gradle.utils.extendsFrom

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.github.xiaocydx"
            afterEvaluate { from(components["release"]) }
        }
    }
}

android {
    namespace = "com.xiaocydx.inputview"
    compileSdk = 33

    defaultConfig {
        minSdk = 21
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    testOptions {
        unitTests { isIncludeAndroidResources = true }
    }
    configurations {
        testImplementation.extendsFrom(compileOnly)
        androidTestImplementation.extendsFrom(compileOnly)
    }
}

dependencies {
    compileOnly("androidx.appcompat:appcompat:1.6.1")
    testImplementation("androidx.viewpager2:viewpager2:1.0.0")
    testImplementation("com.google.truth:truth:1.1.3")
    testImplementation("org.robolectric:robolectric:4.10.3")
    testImplementation("io.mockk:mockk:1.13.7")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}