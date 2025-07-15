pluginManagement {
    repositories {
        google()
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        gradlePluginPortal()
        mavenCentral()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}


//rootProject.name = "Ultron"
include(":sample-app")
include(":ultron-android")
include(":ultron-compose")
include(":ultron-allure")
include(":ultron-common")
include(":composeApp")
