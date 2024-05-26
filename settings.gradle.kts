pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
rootProject.name = "InputView"
include(":app")
include(":inputview")
include(":inputview-compat")

include (":insets")
include (":insets-compat")
include (":insets-systembar")
project(":insets").projectDir = File("D:\\AndroidProjects\\Insets\\insets")
project(":insets-compat").projectDir = File("D:\\AndroidProjects\\Insets\\insets-compat")
project(":insets-systembar").projectDir = File("D:\\AndroidProjects\\Insets\\insets-systembar")