package com.kmpboilerplate.architecture

import com.lemonappdev.konsist.api.verify.assertFalse
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Domain layer: an entity is data plus the rules that need nothing but itself.
 *
 * The domain is packaged by feature, so an entity does not sit in a folder of its own: `Cat` and
 * `CatService` are neighbours. The name is what separates them — see [DomainCollaborator]. Being
 * neighbours, they share a package, and a neighbour needs no import: `Cat` can name
 * `CatRepositoryInterface` without a single import line. So the code of a fact is read as well as
 * its imports.
 */
class EntityRuleTest {
    @Test
    fun `should keep entities free of services repositories and ports`() {
        ProjectScope
            .inPackage(DomainCollaborator.DOMAIN)
            .files
            .filterNot { file -> DomainCollaborator.isCollaborator(DomainCollaborator.subjectOf(file.name)) }
            .flatMap { file -> file.imports }
            .assertFalse(strict = true, additionalMessage = "$MESSAGE ${DomainCollaborator.WILDCARD}") { import ->
                DomainCollaborator.hidesCollaborator(import, DomainCollaborator::isCollaborator)
            }
    }

    /**
     * The identifiers of a fact, import lines included and comments and strings aside, against every
     * collaborator the domain declares — so a neighbour in the same package, a member import and a
     * typealias that stands for a collaborator are all caught. A KDoc link to a service is prose.
     */
    @Test
    fun `should name no service repository or port in the code of an entity`() {
        val domain = ProjectScope.inPackage(DomainCollaborator.DOMAIN)
        val declared =
            domain.classes().map { declaration -> declaration.name } +
                domain.interfaces().map { declaration -> declaration.name } +
                domain.objects().map { declaration -> declaration.name }
        val collaborators = declared.filter(DomainCollaborator::isCollaborator).toSet()
        val facts =
            domain.files.filterNot { file ->
                DomainCollaborator.isCollaborator(DomainCollaborator.subjectOf(file.name))
            }
        val offenders =
            facts.mapNotNull { file ->
                KotlinSources
                    .identifiersOf(file.text)
                    .filter { name -> DomainCollaborator.standsFor(name) { candidate -> candidate in collaborators } }
                    .takeIf { names -> names.isNotEmpty() }
                    ?.let { names -> "${file.projectPath.trimStart('/')}: ${names.sorted().joinToString()}" }
            }

        assertTrue(
            collaborators.isNotEmpty() && facts.isNotEmpty(),
            "No collaborators or facts found; the scope is misconfigured.",
        )
        assertTrue(offenders.isEmpty(), "$MESSAGE\n${offenders.joinToString(separator = "\n")}")
    }

    private companion object {
        const val MESSAGE =
            "An entity carries the rules it can answer on its own. Reaching for a service, a " +
                "repository or a port means the rule needs collaborators and belongs to a service."
    }
}
