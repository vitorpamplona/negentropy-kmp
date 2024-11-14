import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.vanniktech.mavenPublish)
}

group = "com.vitorpamplona.negentropy"
version = "1.0.0"

kotlin {
    jvm()
    androidTarget {
        publishLibraryVariants("release")
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    linuxX64()

    sourceSets {
        val commonMain by getting {
            dependencies {

            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
}

android {
    namespace = "com.vitorpamplona.negentropy"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

mavenPublishing {
    coordinates(artifactId =  "kmp-negentropy")

    pom {
        name = "Negentropy Library for Kotlin Multiplatform"
        description = "Negentropy library ported to Kotlin/Multiplatform for JVM, Android, iOS & Linux"
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

    // Configure publishing to Maven Central
    //publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    // Enable GPG signing for all publications
    signAllPublications()
}

afterEvaluate {
    tasks.register<Copy>("copyDebugUnitTestResources") {
        from("src/commonTest/resources")
        into("build/tmp/kotlin-classes/debugUnitTest")
    }

    tasks.findByName("copyDebugUnitTestResources")!!.mustRunAfter("processDebugUnitTestJavaRes")
    tasks.findByName("testDebugUnitTest")!!.dependsOn("copyDebugUnitTestResources")

    tasks.register<Copy>("copyReleaseUnitTestResources") {
        from("src/commonTest/resources")
        into("build/tmp/kotlin-classes/releaseUnitTest")
    }

    tasks.findByName("copyReleaseUnitTestResources")!!.mustRunAfter("processReleaseUnitTestJavaRes")
    tasks.findByName("testReleaseUnitTest")!!.dependsOn("copyReleaseUnitTestResources")

    tasks.register<Copy>("copyiOSTestResources") {
        from("src/commonTest/resources")
        into("build/bin/iosSimulatorArm64/debugTest/resources")
    }

    tasks.findByName("iosSimulatorArm64Test")!!.dependsOn("copyiOSTestResources")
}