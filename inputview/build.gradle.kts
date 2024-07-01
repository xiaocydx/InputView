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
    api(CommonLibs.insets)
    implementation(PublishLibs.`androidx-fragment-old`)
    testImplementation(PublishLibs.`androidx-appcompat`)
    testImplementation(PublishLibs.`androidx-viewpager2`)
    testImplementation(PublishLibs.`androidx-test-core`)
    testImplementation(PublishLibs.truth)
    testImplementation(PublishLibs.mockk)
    testImplementation(PublishLibs.junit)
    testImplementation(PublishLibs.robolectric)
}