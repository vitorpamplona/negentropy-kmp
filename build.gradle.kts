plugins {
    alias(libs.plugins.kotlinMultiplatform) apply  false
    alias(libs.plugins.androidKotlinMultiplatformLibrary) apply false
    alias(libs.plugins.vanniktech.mavenPublish) apply false
    alias(libs.plugins.diffplugSpotless)
}

allprojects {
    apply(plugin = "com.diffplug.spotless")

    spotless {
        kotlin {
            target ("src/**/*.kt")

            ktlint()
            licenseHeaderFile(
                rootProject.file("spotless/copyright.kt"),
                "@file:|package|import|class|object|sealed|open|interface|abstract "
            )
        }
    }
}

subprojects {
    afterEvaluate {
        try {
            tasks.named("preBuild") {
                dependsOn("spotlessApply")
            }
        } catch (_: UnknownTaskException) {
            tasks.matching {
                it.name.startsWith("pre") && it.name.endsWith("Build")
            }.configureEach {
                dependsOn("spotlessApply")
            }
        }

    }
}

tasks.register<Copy>("installGitHook") {
    from("git-hooks/pre-commit")
    from("git-hooks/pre-push")
    into(".git/hooks")
    filePermissions { unix("rwxrwxrwx") }
}