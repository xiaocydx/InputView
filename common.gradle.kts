import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension

apply<CommonPlugin>()

class CommonPlugin : Plugin<Project> {

    override fun apply(project: Project) = project.subprojects {
        afterEvaluate {
            when {
                plugins.hasPlugin("com.android.application") -> configureApplication()
                plugins.hasPlugin("com.android.library") -> configureLibrary()
            }
        }
    }

    private fun Project.configureApplication() {
        extensions.configure<BaseAppModuleExtension>("android") {
            compileSdk = COMPILE_SDK

            defaultConfig {
                minSdk = MIN_SDK
                targetSdk = TARGET_SDK
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
        }
    }

    private fun Project.configureLibrary() {
        extensions.configure<LibraryExtension>("android") {
            compileSdk = COMPILE_SDK

            defaultConfig {
                minSdk = MIN_SDK
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                consumerProguardFiles("consumer-rules.pro")
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
        }
    }

    private companion object {
        const val COMPILE_SDK = 33
        const val MIN_SDK = 21
        const val TARGET_SDK = 33
    }
}