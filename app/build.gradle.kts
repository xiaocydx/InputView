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
    implementation(project(":inputview-overlay"))
    implementation(CommonLibs.cxrv)
    implementation(CommonLibs.`cxrv-binding`)
    implementation(CommonLibs.`cxrv-paging`)
    implementation(CommonLibs.`cxrv-viewpager2`)
    implementation(CommonLibs.insets)
    implementation(CommonLibs.`insets-compat`)
    implementation(CommonLibs.`insets-systembar`)
    implementation(CommonLibs.`androidx-core-ktx`)
    implementation(CommonLibs.`androidx-appcompat`)
    implementation(CommonLibs.`androidx-fragment`)
    implementation(CommonLibs.`androidx-fragment-ktx`)
    implementation(CommonLibs.`androidx-recyclerview`)
    implementation(CommonLibs.`androidx-constraintlayout`)
    implementation(CommonLibs.material)
    implementation(CommonLibs.glide)
}