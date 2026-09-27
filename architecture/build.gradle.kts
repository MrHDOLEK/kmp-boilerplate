import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    alias(libs.plugins.kotlinJvm)
}

dependencies {
    testImplementation(libs.konsist)
    testImplementation(libs.kotlin.testJunit)
}

tasks.test {
    useJUnit()

    systemProperty("kmpboilerplate.rootDir", rootDir.absolutePath)

    inputs
        .files(
            fileTree(rootDir) {
                include("**/*.kt", "**/*.kts")
                exclude("build/**", "*/build/**", "**/.*/**", "**/node_modules/**")
            },
        ).withPropertyName("projectSources")
        .withPathSensitivity(PathSensitivity.RELATIVE)

    testLogging {
        quiet {
            events(TestLogEvent.FAILED)
            exceptionFormat = TestExceptionFormat.FULL
            showStackTraces = false
        }
    }
}
