plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

android {
    namespace = "com.xiaocydx.inputview.compat"
    kotlinOptions { jvmTarget = Versions.jvmTarget }
}

dependencies {
    compileOnly(project(":inputview"))
    api(CommonLibs.`insets-compat`)
    implementation(PublishLibs.`androidx-core`)
}