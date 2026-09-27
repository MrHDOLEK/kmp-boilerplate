plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinJvm) apply false
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

    tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
        enabled = false
    }

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
    dependsOn(allprojects.map { project -> "${project.path.trimEnd(':')}:ktlintCheck" })
}

tasks.register("csFix") {
    description = "Auto-fix code style"
    group = "verification"
    dependsOn(allprojects.map { project -> "${project.path.trimEnd(':')}:ktlintFormat" })
}

dependencies {
    detektPlugins(libs.detekt.compose)
}

val composeRulesProbe = layout.buildDirectory.file("detekt-compose-canary/ComposeRulesProbe.kt")
val composeRulesProbeConsole = layout.buildDirectory.file("detekt-compose-canary/console.yml")

val writeComposeRulesProbe =
    tasks.register("writeComposeRulesProbe") {
        description = "Writes the probe the Compose rules canary lints, and the config that keeps it off the console."
        val probe = composeRulesProbe
        val console = composeRulesProbeConsole
        val source =
            """
            package canary

            import androidx.compose.material3.Text
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.LaunchedEffect
            import androidx.compose.ui.Modifier

            @Composable
            fun TwoRootEmitters(modifier: Modifier = Modifier) {
                Text("first", modifier)
                Text("second")
            }

            @Composable
            fun LambdaInEffect(onDone: () -> Unit) {
                LaunchedEffect(Unit) { onDone() }
            }

            @Composable
            fun PastTenseLambda(onSelected: () -> Unit) {
                Text("third")
            }
            """.trimIndent()

        val quiet = "console-reports:\n  active: false\n"

        inputs.property("source", source)
        inputs.property("quiet", quiet)
        outputs.file(probe)
        outputs.file(console)
        doLast {
            probe
                .get()
                .asFile
                .apply { parentFile.mkdirs() }
                .writeText(source)
            console.get().asFile.writeText(quiet)
        }
    }

val detektComposeCanary =
    tasks.register<io.gitlab.arturbosch.detekt.Detekt>("detektComposeCanary") {
        description = "Fails when detekt no longer loads the Compose rules."
        group = "verification"
        setSource(writeComposeRulesProbe)
        include("**/*.kt")
        config.setFrom("$rootDir/detekt.yml", composeRulesProbeConsole)
        buildUponDefaultConfig = true
        ignoreFailures = true

        val report = layout.buildDirectory.file("reports/detekt/compose-canary.xml")
        val expected = listOf("MultipleEmitters", "LambdaParameterInRestartableEffect", "ParameterNaming")

        reports.xml.required.set(true)
        reports.xml.outputLocation.set(report)
        reports.html.required.set(false)
        reports.txt.required.set(false)
        reports.sarif.required.set(false)
        reports.md.required.set(false)

        doLast {
            val file = report.get().asFile
            check(file.isFile) { "detekt wrote no report for the Compose rules canary at ${file.path}." }
            val text = file.readText()
            val missing = expected.filterNot { rule -> Regex("""\b$rule\b""").containsMatchIn(text) }
            check(missing.isEmpty()) {
                "The Compose rules did not load into detekt: $missing not reported on the probe. Pin composeRules " +
                    "in gradle/libs.versions.toml to a release whose jar registers " +
                    "io.gitlab.arturbosch.detekt.api.RuleSetProvider, and keep these rules active in detekt.yml."
            }
        }
    }

tasks.named("detekt") {
    dependsOn(detektComposeCanary)
}
