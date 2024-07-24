plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.xiaocydx.inputview.transform"
    kotlinOptions { jvmTarget = Versions.jvmTarget }
}

dependencies {
    compileOnly(project(":inputview"))
    implementation(PublishLibs.`androidx-fragment-old`)
    implementation(PublishLibs.`androidx-transition`)
}