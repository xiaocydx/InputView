import org.jetbrains.kotlin.gradle.utils.extendsFrom

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

android {
    namespace = "com.xiaocydx.inputview.transform"
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
    compileOnly(project(":inputview"))
    compileOnly(CommonLibs.`insets-systembar`)
    implementation(PublishLibs.`androidx-fragment-old`)
    implementation(PublishLibs.`androidx-transition`)
    testImplementation(PublishLibs.`androidx-appcompat`)
    testImplementation(PublishLibs.`androidx-viewpager2`)
    testImplementation(PublishLibs.`androidx-test-core`)
    testImplementation(PublishLibs.truth)
    testImplementation(PublishLibs.mockk)
    testImplementation(PublishLibs.junit)
    testImplementation(PublishLibs.robolectric)
}