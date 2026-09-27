package com.kmpboilerplate.architecture

import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import kotlin.test.Test

/**
 * Infrastructure layer: a repository reads and writes; the decisions are the domain's.
 *
 * A repository is chosen by role — see [InfrastructureRole] — so the rules below hold for a
 * repository under `repository/<area>/` and for a wire-format one beside its adapter alike.
 */
class RepositoryRuleTest {
    /**
     * The identifiers of a repository's file are read, import lines included and comments and strings
     * aside, so a qualified name, a wildcard import followed by a bare name and an alias are all seen.
     *
     * Reading the clock is deciding when "now" is, and when is a rule: the service that decides to run
     * passes the moment in. `Clock` on its own covers `Clock.System`, `kotlin.time.Clock` and a member
     * import of either.
     */
    @Test
    fun `should keep business rules out of repositories`() {
        val services =
            ProjectScope
                .inPackage(DomainCollaborator.DOMAIN)
                .classesAndObjects()
                .map { declaration -> declaration.name }
                .filter(DomainCollaborator::isService)
                .toSet()

        check(services.isNotEmpty()) { "No domain services found; the scope is misconfigured." }

        InfrastructureRole.repositories().assertFalse(strict = true, additionalMessage = DECISIONS) { repository ->
            KotlinSources.identifiersOf(repository.containingFile.text).any { name ->
                DomainCollaborator.standsFor(name) { candidate -> candidate in services || candidate in CLOCKS }
            }
        }
    }

    /**
     * The folder names the feature, the same word the domain uses for the contract. A repository over
     * local storage sits under `repository/<area>/` of the contract it implements; one that speaks a wire
     * format sits beside its adapter. A repository that implements no contract belongs to no area, so
     * an adapter is the only place left for it.
     */
    @Test
    fun `should file every repository under the area of its contract or beside its adapter`() {
        val areas = DomainArea.names()
        val contractAreas =
            ProjectScope
                .inPackage(DomainCollaborator.DOMAIN)
                .interfaces()
                .filter { contract -> DomainCollaborator.isRepository(contract.name) }
                .associate { contract -> contract.name to areaOf(contract.packagee?.name.orEmpty()) }

        InfrastructureRole.repositories().assertTrue(
            strict = true,
            additionalMessage =
                "A repository belongs to the area of the contract it implements: move it into " +
                    "infrastructure/repository/<area>/, one of ${areas.sorted().joinToString()}, or beside " +
                    "its adapter, one of ${InfrastructureRole.ADAPTERS.joinToString()}.",
        ) { repository ->
            val packageName = repository.packagee?.name.orEmpty()
            val owned = InfrastructureRole.contractsOf(repository).mapNotNull { name -> contractAreas[name] }

            packageName in InfrastructureRole.ADAPTERS ||
                (owned.isNotEmpty() && owned.all { area -> area in areas && isFiledUnder(packageName, area) })
        }
    }

    /** The area of a domain package: `shared` holds no area of its own, the folders inside it do. */
    private fun areaOf(packageName: String): String =
        packageName
            .removePrefix("${DomainCollaborator.DOMAIN}.")
            .removePrefix("shared.")
            .substringBefore('.')

    private fun isFiledUnder(
        packageName: String,
        area: String,
    ): Boolean = ImportRule.reaches(packageName, "${InfrastructureRole.REPOSITORIES}.$area")

    private companion object {
        const val DECISIONS =
            "A repository answers a parametric query. Which page, which limit, which filter, what to " +
                "fall back on and what time it is are decisions: they belong to a service."

        val CLOCKS = setOf("ClockInterface", "SystemClock", "Clock")
    }
}
