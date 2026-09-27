package com.kmpboilerplate.architecture

import com.lemonappdev.konsist.api.architecture.KoArchitectureCreator.assertArchitecture
import com.lemonappdev.konsist.api.architecture.Layer
import com.lemonappdev.konsist.api.verify.assertFalse
import kotlin.test.Test

/**
 * Hexagonal architecture, simplified: dependencies point inwards, towards the domain.
 *
 * Each import rule has a text half. Konsist reads imports, and a fully qualified name needs none, so
 * the names the code spells are held to the same prohibitions — see [QualifiedReference].
 */
class LayerDependencyTest {
    /**
     * Written as prohibitions on purpose. Konsist's `dependsOn` — strict or not — only asserts that a
     * dependency exists and never forbids one, so `application.dependsOn(domain, strict = true)` would
     * still let application code import infrastructure; only `doesNotDependOn` and `dependsOnNothing`
     * reject an import.
     */
    @Test
    fun `should keep dependencies pointing towards the domain`() {
        ProjectScope.production.assertArchitecture {
            val domain = Layer("Domain", "$DOMAIN..")
            val application = Layer("Application", "$APPLICATION..")
            val infrastructure = Layer("Infrastructure", "$INFRASTRUCTURE..")
            val presentation = Layer("Presentation", "$PRESENTATION..")

            domain.dependsOnNothing()
            application.doesNotDependOn(infrastructure, presentation)
            infrastructure.doesNotDependOn(presentation)
        }
    }

    @Test
    fun `should build the domain from plain Kotlin only`() {
        ProjectScope.inPackage(DOMAIN).imports.assertFalse(
            strict = true,
            additionalMessage = "The domain is pure Kotlin: it may import only ${DOMAIN_ALLOWED.joinToString()}.",
        ) { import -> DOMAIN_ALLOWED.none { allowed -> ImportRule.reaches(import.name, allowed) } }
    }

    /**
     * A dotted name counts when it starts at a package root the project knows — see [PackageRoots] —
     * so `cat.tags` is a property read and `io.ktor.client.HttpClient` a framework.
     */
    @Test
    fun `should name no framework and no outer layer in the code of the domain`() {
        val roots = PackageRoots.known

        QualifiedReference.assertNoneNamed(
            ProjectScope.inPackage(DOMAIN).files,
            "The domain is pure Kotlin: it may name only ${DOMAIN_ALLOWED.joinToString()}.",
        ) { name ->
            name.substringBefore('.') in roots && DOMAIN_ALLOWED.none { allowed -> ImportRule.reaches(name, allowed) }
        }
    }

    @Test
    fun `should name no outer layer in the code of the application layer`() {
        QualifiedReference.assertNoneNamed(
            ProjectScope.inPackage(APPLICATION).files,
            "The application layer reaches neither infrastructure nor the UI.",
        ) { name -> OUTSIDE_APPLICATION.any { outer -> ImportRule.reaches(name, outer) } }
    }

    @Test
    fun `should name no user interface in the code of infrastructure`() {
        QualifiedReference.assertNoneNamed(
            ProjectScope.inPackage(INFRASTRUCTURE).files,
            "Infrastructure serves the application; it never reaches the UI.",
        ) { name -> ImportRule.reaches(name, PRESENTATION) }
    }

    private companion object {
        const val DOMAIN = "com.kmpboilerplate.domain"
        const val APPLICATION = "com.kmpboilerplate.application"
        const val INFRASTRUCTURE = "com.kmpboilerplate.infrastructure"
        const val PRESENTATION = "com.kmpboilerplate.app"

        val DOMAIN_ALLOWED = listOf("kotlin", "kotlinx.coroutines", "kotlinx.datetime", DOMAIN)

        val OUTSIDE_APPLICATION = listOf(INFRASTRUCTURE, PRESENTATION)
    }
}
