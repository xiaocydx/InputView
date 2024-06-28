plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.xiaocydx.inputview.sample"
    defaultConfig { applicationId = "com.xiaocydx.inputview.sample" }
    kotlinOptions { jvmTarget = Versions.jvmTarget }
    buildFeatures { viewBinding = true }
}

dependencies {
    implementation(project(":inputview"))
    implementation(project(":inputview-compat"))
    implementation(Libs.cxrv)
    implementation(Libs.`cxrv-binding`)
    implementation(Libs.`cxrv-paging`)
    implementation(Libs.`cxrv-viewpager2`)
    implementation(Libs.insets)
    implementation(Libs.`insets-compat`)
    implementation(Libs.`insets-systembar`)
    implementation(Libs.`androidx-core-ktx`)
    implementation(Libs.`androidx-appcompat`)
    implementation(Libs.`androidx-fragment`)
    implementation(Libs.`androidx-fragment-ktx`)
    implementation(Libs.`androidx-recyclerview`)
    implementation(Libs.`androidx-constraintlayout`)
    implementation(Libs.material)
    implementation(Libs.glide)
}