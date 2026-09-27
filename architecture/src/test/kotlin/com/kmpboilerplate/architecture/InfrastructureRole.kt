package com.kmpboilerplate.architecture

import com.lemonappdev.konsist.api.declaration.combined.KoClassAndObjectDeclaration

/**
 * What an infrastructure declaration is, read from what it does rather than from where it sits.
 *
 * A repository is chosen by role: whatever implements a domain `*RepositoryInterface`, and whatever
 * calls itself one, class or object, anywhere in infrastructure. Selecting by folder judged the
 * folder: a repository beside its adapter, such as `cataas/`, would be outside every repository rule,
 * and so would one written into the root of `infrastructure/`.
 */
object InfrastructureRole {
    const val INFRASTRUCTURE = "com.kmpboilerplate.infrastructure"

    const val REPOSITORIES = "$INFRASTRUCTURE.repository"

    /**
     * The adapters, the folders a wire format lives in with its client, its DTOs and its mapper. A new
     * wire format is added here in the same change as its folder.
     */
    val ADAPTERS = listOf("cataas").map { adapter -> "$INFRASTRUCTURE.$adapter" }

    fun repositories(): List<KoClassAndObjectDeclaration> =
        ProjectScope
            .inPackage(INFRASTRUCTURE)
            .classesAndObjects(includeNested = false, includeLocal = false)
            .filter { declaration ->
                declaration.name.endsWith("Repository") ||
                    contractsOf(declaration).any(DomainCollaborator::isRepository)
            }

    /**
     * Every name the supertypes of [declaration] stand for — written qualified or not, through an
     * import alias and through a domain typealias.
     *
     * Konsist gives a supertype as the text of its type reference, so a qualified name keeps its
     * package and a generic one its type arguments; both are cut back to the simple name.
     */
    fun contractsOf(declaration: KoClassAndObjectDeclaration): Set<String> {
        val aliases = ImportRule.aliasesIn(declaration.containingFile)

        return declaration
            .parents()
            .flatMap { parent ->
                val written =
                    parent.name
                        .substringBefore('<')
                        .substringAfterLast('.')
                        .trim()
                        .removeSurrounding("`")

                DomainCollaborator.namesBehind(aliases[written] ?: written)
            }.toSet()
    }
}
