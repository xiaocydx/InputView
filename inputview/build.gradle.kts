import org.jetbrains.kotlin.gradle.utils.extendsFrom

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

android {
    namespace = "com.xiaocydx.inputview"
    kotlinOptions { jvmTarget = Versions.jvmTarget }
    testOptions {
        unitTests { isIncludeAndroidResources = true }
    }
    configurations {
        testImplementation.extendsFrom(compileOnly)
        androidTestImplementation.extendsFrom(compileOnly)
    }
}

dependencies {
    implementation("androidx.fragment:fragment:1.1.0")
    api(Libs.insets)
    testImplementation(Libs.`androidx-appcompat`)
    testImplementation(Libs.`androidx-viewpager2`)
    testImplementation(Libs.truth)
    testImplementation(Libs.robolectric)
    testImplementation(Libs.mockk)
    testImplementation(Libs.`androidx-test-core`)
    testImplementation(Libs.junit)
}