plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

allprojects {
    plugins.withType<org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin> {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension> {
            jvmToolchain(24)
        }
    }
}

subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set("1.5.0")
        filter {
            exclude { element -> element.file.path.contains("/build/") }
        }
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom("$rootDir/detekt.yml")
    parallel = true
    source.setFrom(
        files(
            "composeApp/src/commonMain/kotlin",
            "composeApp/src/androidMain/kotlin",
            "composeApp/src/iosMain/kotlin",
            "composeApp/src/desktopMain/kotlin",
            "shared/src/commonMain/kotlin",
            "shared/src/androidMain/kotlin",
            "shared/src/iosMain/kotlin",
            "shared/src/desktopMain/kotlin",
        ),
    )
}

ktlint {
    version.set("1.5.0")
    filter {
        exclude("**/build/**")
    }
}

tasks.register("csCheck") {
    description = "Run code style check"
    group = "verification"
    dependsOn("ktlintCheck")
}

tasks.register("csFix") {
    description = "Auto-fix code style"
    group = "verification"
    dependsOn("ktlintFormat")
}

dependencies {
    detektPlugins(libs.detekt.compose)
}
