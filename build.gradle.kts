plugins {
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinMultiplatform) apply  false
    alias(libs.plugins.vanniktech.mavenPublish) apply false
    alias(libs.plugins.diffplugSpotless)
    alias(libs.plugins.dokka)
}

allprojects {
    apply(plugin = "com.diffplug.spotless")
    apply(plugin = "org.jetbrains.dokka")

    spotless {
        kotlin {
            target ("src/**/*.kt")

            ktlint("1.3.1")
            licenseHeaderFile(
                rootProject.file("spotless/copyright.kt"),
                "package|import|class|object|sealed|open|interface|abstract "
            )
        }
    }
}

subprojects {
    afterEvaluate {
        tasks.named("preBuild") {
            dependsOn("spotlessApply")
        }
    }
}

tasks.register<Copy>("installGitHook") {
    from("git-hooks/pre-commit")
    from("git-hooks/pre-push")
    into(".git/hooks")
    fileMode = 0b111_111_111
}