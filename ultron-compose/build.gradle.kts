
import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    id("org.jetbrains.dokka")
    id("maven-publish")
    id("signing")
}

group = project.findProperty("GROUP")!!
version = project.findProperty("VERSION_NAME")!!

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
    }
    androidTarget {
        publishLibraryVariants("release")
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    jvm("desktop")
    
    // Отключаем нативные таргеты для JitPack
    if (!System.getenv("JITPACK").toBoolean()) {
        macosX64()
        macosArm64()
        iosX64()
        iosArm64()
        iosSimulatorArm64()
        @OptIn(ExperimentalWasmDsl::class)
        wasmJs(){
            browser()
            nodejs()
        }
        js(IR){
            browser()
            nodejs()
        }
    }

    @OptIn(ExperimentalComposeLibrary::class)
    sourceSets {
        applyDefaultHierarchyTemplate()
        val commonMain by getting {
            dependencies {
                api(project(":ultron-common"))
                implementation(kotlin("reflect"))
                api(libs.kotlin.test)
                api(compose.uiTest)
                implementation(libs.atomicfu)
            }
        }
        val androidMain by getting {
            dependencies {
                api(project(":ultron-common"))
                implementation(Libs.androidXRunner)
                api(Libs.composeUiTest)
            }
        }
        val shared by creating {
            dependsOn(commonMain)
        }
        jvmMain {
            dependsOn(shared)
            dependencies {
                api(kotlin("stdlib"))
            }
        }
        val desktopMain by getting {
            dependsOn(shared)
            dependsOn(jvmMain.get())
            dependencies {
                api(project(":ultron-common"))
                implementation(compose.uiTest)
            }
        }
        if (!System.getenv("JITPACK").toBoolean()) {
            val nativeMain by getting { dependsOn(shared) }

            val jsWasmMain by creating {
                dependsOn(shared)
            }
            val jsMain by getting {
                dependsOn(jsWasmMain)
                dependencies {
                    implementation(kotlin("stdlib-js"))
                }
            }
            val wasmJsMain by getting {
                dependsOn(jsWasmMain)
                dependencies {
                    implementation(kotlin("stdlib"))
                }
            }
        }
    }
}

android {
    compileSdk = 35
    namespace = "com.atiurin.ultron.compose"
    defaultConfig {
        minSdk = 21
        multiDexEnabled = true
    }
    compileOptions {
        targetCompatibility = JavaVersion.VERSION_17
        sourceCompatibility = JavaVersion.VERSION_17
    }
}

val dokkaOutputDir = buildDir.resolve("dokka")
tasks.dokkaHtml { outputDirectory.set(file(dokkaOutputDir)) }
val deleteDokkaOutputDir by tasks.register<Delete>("deleteDokkaOutputDirectory") { delete(dokkaOutputDir) }

val ultronComposeJavadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    dependsOn(deleteDokkaOutputDir, tasks.dokkaHtml)
    from(dokkaOutputDir)
}

publishing {
    publications {
        publications.withType<MavenPublication> {
            artifact(ultronComposeJavadocJar)

            pom {
                name.set("Ultron Compose")
                description.set("Android & Compose Multiplatform UI testing framework")
                url.set("https://github.com/open-tool/ultron")
                inceptionYear.set("2021")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                issueManagement {
                    system.set("GitHub Issues")
                    url.set("https://github.com/open-tool/ultron/issues")
                }

                developers {
                    developer {
                        id.set("alex-tiurin")
                        name.set("Aleksei Tiurin")
                        url.set("https://github.com/open-tool")
                    }
                }

                scm {
                    connection.set("scm:git@github.com:open-tool/ultron.git")
                    developerConnection.set("scm:git@github.com:open-tool/ultron.git")
                    url.set("https://github.com/open-tool/ultron")
                }
            }
        }
    }
}

tasks.withType<PublishToMavenRepository>().configureEach {
    if (System.getenv("JITPACK") != "true") {
        dependsOn(tasks.withType<Sign>())
        mustRunAfter(tasks.withType<Sign>())
    }
    dependsOn(ultronComposeJavadocJar)
    dependsOn(tasks.withType<Jar>())
}

tasks.withType<PublishToMavenLocal>().configureEach {
    if (System.getenv("JITPACK") != "true") {
        dependsOn("signKotlinMultiplatformPublication")
        dependsOn("signAndroidReleasePublication")
        dependsOn("signDesktopPublication")

        mustRunAfter("signKotlinMultiplatformPublication")
        mustRunAfter("signAndroidReleasePublication")
        mustRunAfter("signDesktopPublication")
    }
}
signing {
    if (System.getenv("JITPACK") != "true") {
        println("Signing lib...")
        useGpgCmd()
        sign(publishing.publications)
    }
}