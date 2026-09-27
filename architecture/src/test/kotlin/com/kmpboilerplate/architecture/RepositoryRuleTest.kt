package com.kmpboilerplate.architecture

import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import kotlin.test.Test

class RepositoryRuleTest {
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
