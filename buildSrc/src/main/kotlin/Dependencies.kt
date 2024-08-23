object Versions {
    const val jvmTarget = "1.8"
}

object CommonLibs {
    const val `androidx-core` = "androidx.core:core:1.7.0"
    const val `androidx-core-ktx` = "androidx.core:core-ktx:1.7.0"
    const val `androidx-appcompat` = "androidx.appcompat:appcompat:1.6.1"
    const val `androidx-fragment` = "androidx.fragment:fragment:1.6.2"
    const val `androidx-fragment-ktx` = "androidx.fragment:fragment-ktx:1.6.2"
    const val `androidx-lifecycle-runtime-ktx` = "androidx.lifecycle:lifecycle-runtime-ktx:2.4.0"
    const val `androidx-transition` = "androidx.transition:transition:1.4.1"
    const val `androidx-viewpager2` = "androidx.viewpager2:viewpager2:1.0.0"
    const val `androidx-recyclerview` = "androidx.recyclerview:recyclerview:1.2.0"
    const val `androidx-drawerlayout` = "androidx.drawerlayout:drawerlayout:1.1.1"
    const val `androidx-constraintlayout` = "androidx.constraintlayout:constraintlayout:2.1.4"
    const val `androidx-swiperefreshlayout` = "androidx.swiperefreshlayout:swiperefreshlayout:1.1.0"
    const val material = "com.google.android.material:material:1.7.0"

    private const val cxrvVersion = "1.5.4"
    const val cxrv = "com.github.xiaocydx.CXRV:cxrv:${cxrvVersion}"
    const val `cxrv-binding` = "com.github.xiaocydx.CXRV:cxrv-binding:${cxrvVersion}"
    const val `cxrv-paging` = "com.github.xiaocydx.CXRV:cxrv-paging:${cxrvVersion}"
    const val `cxrv-animatable` = "com.github.xiaocydx.CXRV:cxrv-animatable:${cxrvVersion}"
    const val `cxrv-viewpager2` = "com.github.xiaocydx.CXRV:cxrv-viewpager2:${cxrvVersion}"

    private const val inputViewVersion = "1.3.1"
    const val inputview = "com.github.xiaocydx.InputView:inputview:${inputViewVersion}"
    const val `inputview-compat` = "com.github.xiaocydx.InputView:inputview-compat:${inputViewVersion}"
    const val `inputview-transform` = "com.github.xiaocydx.InputView:inputview-transform:${inputViewVersion}"

    private const val insetsVersion = "1.2.5"
    const val insets = "com.github.xiaocydx.Insets:insets:${insetsVersion}"
    const val `insets-compat` = "com.github.xiaocydx.Insets:insets-compat:${insetsVersion}"
    const val `insets-systembar` = "com.github.xiaocydx.Insets:insets-systembar:${insetsVersion}"

    const val coroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3"
    const val `coroutines-android` = "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3"

    const val retrofit = "com.squareup.retrofit2:retrofit:2.9.0"
    const val `retrofit-converter-gson` = "com.squareup.retrofit2:converter-gson:2.9.0"

    const val glide = "com.github.bumptech.glide:glide:4.14.2"
    const val leakcanary = "com.squareup.leakcanary:leakcanary-android:2.9.1"
}

object PublishLibs {
    const val `androidx-core` = "androidx.core:core:1.7.0"
    const val `androidx-appcompat` = "androidx.appcompat:appcompat:1.2.0"
    const val `androidx-lifecycle-common` = "androidx.lifecycle:lifecycle-common:2.1.0"
    const val `androidx-lifecycle-viewmodel` = "androidx.lifecycle:lifecycle-viewmodel:2.1.0"
    const val `androidx-viewpager2` = "androidx.viewpager2:viewpager2:1.0.0"
    const val `androidx-recyclerview` = "androidx.recyclerview:recyclerview:1.2.0"
    const val `androidx-fragment-old` = "androidx.fragment:fragment:1.1.0"
    const val `androidx-fragment-new` = "androidx.fragment:fragment:1.3.6"
    const val `androidx-transition` = "androidx.transition:transition:1.2.0"

    const val coroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4"
    const val `coroutines-android` = "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4"
    const val hiddenapibypass = "org.lsposed.hiddenapibypass:hiddenapibypass:4.3"

    const val `androidx-test-core` = "androidx.test:core:1.5.0"
    const val `androidx-junit` = "androidx.test.ext:junit:1.2.0"
    const val `androidx-espresso-core` = "androidx.test.espresso:espresso-core:3.6.0"
    const val junit = "junit:junit:4.13.2"
    const val truth = "com.google.truth:truth:1.1.3"
    const val mockk = "io.mockk:mockk:1.13.7"
    const val robolectric = "org.robolectric:robolectric:4.10.3"
}