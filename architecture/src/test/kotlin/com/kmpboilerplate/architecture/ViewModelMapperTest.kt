package com.kmpboilerplate.architecture

import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Application layer: a mapper maps; the service gathers and the action delegates.
 *
 * A port is not on that list. A mapper that reads a clock, a time zone or a location source is still
 * mapping — those answer what "now" and "here" are, not what to fetch.
 *
 * Each import rule has a code half, and the `suspend` rule reads the code alone: a qualified
 * constructor parameter, an `object` mapper and a top-level function are all written without an
 * import a rule could read.
 */
class ViewModelMapperTest {
    @Test
    fun `should keep view model mappers free of services and repositories`() {
        ProjectScope.inPackage(MAPPERS).imports.assertFalse(
            strict = true,
            additionalMessage = "$MAPS ${DomainCollaborator.WILDCARD}",
        ) { import ->
            DomainCollaborator.hidesCollaborator(import) { name ->
                DomainCollaborator.isService(name) || DomainCollaborator.isRepository(name)
            }
        }
    }

    /**
     * The code half of the rule above. It compares against the names the domain declares, because a
     * mapper legitimately takes another `*ViewModelMapper`, whose suffix alone reads as a service.
     */
    @Test
    fun `should name no service or repository in the code of a view model mapper`() {
        val domain = ProjectScope.inPackage(DomainCollaborator.DOMAIN)
        val services =
            domain
                .classesAndObjects()
                .map { declaration -> declaration.name }
                .filter(DomainCollaborator::isService)
        val contracts =
            domain
                .interfaces()
                .map { declaration -> declaration.name }
                .filter(DomainCollaborator::isRepository)
        val collaborators = (services + contracts).toSet()
        val files = ProjectScope.inPackage(MAPPERS).files
        val offenders =
            files.mapNotNull { file ->
                KotlinSources
                    .identifiersOf(file.text)
                    .filter { name -> DomainCollaborator.standsFor(name) { behind -> behind in collaborators } }
                    .takeIf { named -> named.isNotEmpty() }
                    ?.let { named -> "${file.projectPath.trimStart('/')}: ${named.sorted().joinToString()}" }
            }

        assertTrue(
            collaborators.isNotEmpty() && files.isNotEmpty(),
            "No domain collaborators or view model mappers found; the scope is misconfigured.",
        )
        assertTrue(offenders.isEmpty(), "$MAPS\n${offenders.joinToString(separator = "\n")}")
    }

    /**
     * Read from the text, so a `suspend` in an `object`, a top-level function, a companion or a
     * suspending lambda type counts as well as one in a class.
     */
    @Test
    fun `should keep view model mappers free of suspending functions`() {
        val files = ProjectScope.inPackage(MAPPERS).files
        val offenders =
            files
                .filter { file -> SUSPEND.containsMatchIn(KotlinSources.symbolsOf(file.text)) }
                .map { file -> file.projectPath.trimStart('/') }

        assertTrue(files.isNotEmpty(), "No view model mappers found; the scope is misconfigured.")
        assertTrue(offenders.isEmpty(), "$LOADS\n${offenders.joinToString(separator = "\n")}")
    }

    /**
     * A mapper reads no file, no network and no database: it imports Kotlin, kotlinx.datetime, the
     * domain it maps from and the application it maps to, and nothing else. Read from Konsist's
     * imports, so a wildcard of a one-segment package (`import okio.*`, reported as `okio`) counts
     * too — the qualified names of the code half below never see it.
     */
    @Test
    fun `should import only kotlin, kotlinx datetime, the domain and the application into a view model mapper`() {
        val imports = ProjectScope.inPackage(MAPPERS).imports

        imports.assertTrue(strict = true, additionalMessage = NO_INPUT_OUTPUT) { import ->
            MAPPER_ALLOWED.any { allowed -> ImportRule.reaches(import.name, allowed) }
        }
    }

    /**
     * The code half of the rule above: the same allow-list over every name written qualified. A
     * dotted name counts when it starts at a package root the project knows — see [PackageRoots].
     */
    @Test
    fun `should keep input and output libraries out of view model mappers`() {
        QualifiedReference.assertNoneNamed(ProjectScope.inPackage(MAPPERS).files, NO_INPUT_OUTPUT) { name ->
            name.substringBefore('.') in PackageRoots.known &&
                MAPPER_ALLOWED.none { allowed -> ImportRule.reaches(name, allowed) }
        }
    }

    private companion object {
        const val MAPPERS = "com.kmpboilerplate.application.viewmodel.mapper"

        const val MAPS = "A mapper maps the data it is given; a service gathers it and an action passes it in."

        const val LOADS = "A suspending mapper is a mapper that loads: move the load into the service."

        const val NO_INPUT_OUTPUT =
            "A mapper translates values; it may name only Kotlin, kotlinx.datetime, the domain and the application."

        val SUSPEND = Regex("""\bsuspend\b""")

        val MAPPER_ALLOWED =
            listOf("kotlin", "kotlinx.datetime", "com.kmpboilerplate.domain", "com.kmpboilerplate.application")
    }
}
