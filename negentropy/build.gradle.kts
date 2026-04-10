@file:OptIn(ExperimentalWasmDsl::class)

import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.SourcesJar
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.vanniktech.mavenPublish)
}

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    android {
        namespace = "com.vitorpamplona.negentropy"
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }

        withHostTest {}

        withDeviceTest {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    js(IR) {
        browser {
            testTask {
                useMocha { timeout = "120s" }
            }
        }
        nodejs {
            testTask {
                useMocha { timeout = "120s" }
            }
        }
    }
    wasmJs {
        browser {
            testTask {
                useMocha { timeout = "120s" }
            }
        }
        nodejs {
            testTask {
                useMocha { timeout = "120s" }
            }
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    macosArm64()
    linuxX64()
    mingwX64()

    sourceSets {
        val commonMain by getting {
            dependencies { }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
}

mavenPublishing {
    // sources publishing is always enabled by the Kotlin Multiplatform plugin
    configure(
        KotlinMultiplatform(
            // whether to publish a sources jar
            sourcesJar = SourcesJar.Sources(),
        )
    )

    coordinates(
        groupId = "com.vitorpamplona.negentropy",
        artifactId = "kmp-negentropy",
        version = "1.0.2",
    )

    // Configure publishing to Maven Central
    publishToMavenCentral(automaticRelease = true)

    // Enable GPG signing for all publications
    signAllPublications()

    pom {
        name = "Negentropy Library for Kotlin Multiplatform"
        description = "Negentropy library ported to Kotlin/Multiplatform for JVM, JS, WasmJS, Android, iOS, macOS, Linux & Windows"
        inceptionYear = "2024"
        url = "https://github.com/vitorpamplona/kmp-negentropy/"
        licenses {
            license {
                name = "MIT License"
                url = "https://github.com/vitorpamplona/kmp-negentropy/blob/main/LICENSE"
            }
        }
        developers {
            developer {
                id = "vitorpamplona"
                name = "Vitor Pamplona"
                url = "http://vitorpamplona.com"
                email = "vitor@vitorpamplona.com"
            }
        }
        scm {
            url = "https://github.com/vitorpamplona/kmp-negentropy"
            connection = "https://github.com/vitorpamplona/kmp-negentropy.git"
        }
    }
}

afterEvaluate {
    tasks.register<Copy>("copyDebugUnitTestResources") {
        from("src/commonTest/resources")
        into("build/tmp/kotlin-classes/debugUnitTest")
    }

    tasks.findByName("copyDebugUnitTestResources")!!.mustRunAfter("processDebugUnitTestJavaRes")
    //tasks.findByName("testDebugUnitTest")!!.dependsOn("copyDebugUnitTestResources")

    tasks.register<Copy>("copyReleaseUnitTestResources") {
        from("src/commonTest/resources")
        into("build/tmp/kotlin-classes/releaseUnitTest")
    }

    tasks.findByName("copyReleaseUnitTestResources")!!.mustRunAfter("processReleaseUnitTestJavaRes")
    //tasks.findByName("testReleaseUnitTest")!!.dependsOn("copyReleaseUnitTestResources")

    listOf("iosSimulatorArm64", "iosX64", "iosArm64", "macosArm64", "macosX64").forEach { target ->
        val taskName = "copy${target.replaceFirstChar { it.uppercaseChar() }}TestResources"
        tasks.register<Copy>(taskName) {
            from("src/commonTest/resources")
            into("build/bin/$target/debugTest/resources")
        }
        tasks.findByName("${target}Test")?.dependsOn(taskName)
    }

}