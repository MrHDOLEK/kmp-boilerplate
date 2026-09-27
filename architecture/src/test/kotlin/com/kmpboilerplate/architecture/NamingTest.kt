package com.kmpboilerplate.architecture

import com.lemonappdev.konsist.api.KoModifier
import com.lemonappdev.konsist.api.declaration.KoClassDeclaration
import com.lemonappdev.konsist.api.declaration.KoInterfaceDeclaration
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import kotlin.test.Test

class NamingTest {
    @Test
    fun `should name every domain contract Interface and every persistence one RepositoryInterface`() {
        val contracts = ProjectScope.inPackage(DomainCollaborator.DOMAIN).interfaces().filterNot(::isSealed)

        contracts.assertTrue(strict = true, additionalMessage = CONTRACT_SAYS_SO) { contract ->
            contract.name.endsWith("Interface")
        }

        contracts
            .filter { contract -> contract.name.contains("Repository") }
            .assertTrue(strict = true, additionalMessage = PERSISTENCE_SAYS_SO) { contract ->
                contract.name.endsWith("RepositoryInterface")
            }
    }

    @Test
    fun `should give every domain class that takes a collaborator a service name`() {
        ProjectScope
            .inPackage(DomainCollaborator.DOMAIN)
            .classes(includeNested = false, includeLocal = false)
            .filter(::takesCollaborator)
            .assertTrue(strict = true, additionalMessage = SERVICE_SAYS_SO) { declaration ->
                DomainCollaborator.isService(declaration.name)
            }
    }

    @Test
    fun `should keep technology out of repository names`() {
        ProjectScope.production
            .classes()
            .filter { declaration -> declaration.name.endsWith("Repository") }
            .assertFalse(
                strict = true,
                additionalMessage = NO_TECHNOLOGY,
            ) { repository -> namesTechnology(repository.name) }

        ProjectScope.production
            .interfaces()
            .filter { declaration -> declaration.name.endsWith("RepositoryInterface") }
            .assertFalse(
                strict = true,
                additionalMessage = NO_TECHNOLOGY,
            ) { contract -> namesTechnology(contract.name) }
    }

    @Test
    fun `should name every use case Action and make it invocable`() {
        ProjectScope
            .inPackage("com.kmpboilerplate.application.action")
            .classes(includeNested = false, includeLocal = false)
            .filterNot { declaration -> declaration.hasPrivateModifier || declaration.hasInternalModifier }
            .assertTrue(strict = true) { action ->
                action.name.endsWith("Action") &&
                    action.functions().any { function -> function.name == "invoke" && function.hasOperatorModifier }
            }
    }

    private fun isSealed(contract: KoInterfaceDeclaration): Boolean = contract.hasModifier(KoModifier.SEALED)

    private fun takesCollaborator(declaration: KoClassDeclaration): Boolean {
        val aliases = ImportRule.aliasesIn(declaration.containingFile)

        return declaration.primaryConstructor
            ?.parameters
            .orEmpty()
            .any { parameter ->
                KotlinSources.identifiersOf(parameter.type.text).any { name ->
                    DomainCollaborator.standsFor(aliases[name] ?: name, DomainCollaborator::isCollaborator)
                }
            }
    }

    private fun namesTechnology(name: String): Boolean = TECHNOLOGIES.any { technology -> name.contains(technology) }

    private companion object {
        const val CONTRACT_SAYS_SO = "A domain contract is named for being one: it ends in Interface."

        const val PERSISTENCE_SAYS_SO =
            "Persistence keeps its full form — RepositoryInterface — so a port is never read as a store."

        const val NO_TECHNOLOGY = "The driver underneath is replaceable, the repository is not: no technology."

        val SERVICE_SAYS_SO =
            "A domain class that takes a contract, a port or a service is a service: name it " +
                DomainCollaborator.SERVICE_SUFFIXES.joinToString(separator = ", ") { suffix -> "*$suffix" } +
                " so the rules and the container see it."

        val TECHNOLOGIES = listOf("Sql", "InMemory", "Room", "Ktor", "Http")
    }
}
