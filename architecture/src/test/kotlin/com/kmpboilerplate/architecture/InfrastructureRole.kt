package com.kmpboilerplate.architecture

import com.lemonappdev.konsist.api.declaration.combined.KoClassAndObjectDeclaration

object InfrastructureRole {
    const val INFRASTRUCTURE = "com.kmpboilerplate.infrastructure"

    const val REPOSITORIES = "$INFRASTRUCTURE.repository"

    val ADAPTERS = listOf("cataas").map { adapter -> "$INFRASTRUCTURE.$adapter" }

    fun repositories(): List<KoClassAndObjectDeclaration> =
        ProjectScope
            .inPackage(INFRASTRUCTURE)
            .classesAndObjects(includeNested = false, includeLocal = false)
            .filter { declaration ->
                declaration.name.endsWith("Repository") ||
                    contractsOf(declaration).any(DomainCollaborator::isRepository)
            }

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
