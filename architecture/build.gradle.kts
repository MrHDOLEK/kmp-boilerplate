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

    // The rules that walk the files themselves - scripts, and sources Konsist skips - read them relative to the
    // repository root, which a test JVM has no other way of finding.
    systemProperty("kmpboilerplate.rootDir", rootDir.absolutePath)

    // The rules parse the Kotlin sources of every module from disk, which Gradle cannot infer on its own;
    // declaring them keeps the gate up to date when nothing changed and reruns it when anything did. Konsist
    // reads a .kt file wherever it sits, not only under src/, and the suppression rules read the .kts scripts too.
    inputs
        .files(
            fileTree(rootDir) {
                include("**/*.kt", "**/*.kts")
                // A module's output is not a source; a build or target folder inside src is, and SourceLayoutTest
                // rejects it.
                exclude("build/**", "*/build/**", "**/.*/**", "**/node_modules/**")
            },
        ).withPropertyName("projectSources")
        .withPathSensitivity(PathSensitivity.RELATIVE)

    // The hooks run Gradle with --quiet; without this a broken rule reports only "1 test failed".
    testLogging {
        quiet {
            events(TestLogEvent.FAILED)
            exceptionFormat = TestExceptionFormat.FULL
            showStackTraces = false
        }
    }
}
