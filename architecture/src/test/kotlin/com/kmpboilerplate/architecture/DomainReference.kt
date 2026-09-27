package com.kmpboilerplate.architecture

import com.lemonappdev.konsist.api.declaration.KoFileDeclaration

object DomainReference {
    val typeNames: Set<String> by lazy {
        val domain = ProjectScope.inPackage(DomainCollaborator.DOMAIN)

        (
            domain.classes().map { declaration -> declaration.name } +
                domain.interfaces().map { declaration -> declaration.name } +
                domain.objects().map { declaration -> declaration.name } +
                domain.typeAliases.map { declaration -> declaration.name }
        ).toSet()
    }

    fun namedIn(
        code: String,
        file: KoFileDeclaration,
    ): Set<String> {
        val named = KotlinSources.identifiersOf(code)
        val domainImports = file.imports.filter { import -> DomainCollaborator.isDomain(import.name) }
        val imported =
            domainImports
                .filterNot { import -> import.isWildcard }
                .map { import -> import.alias?.name ?: import.name.substringAfterLast('.') }
                .toSet()
        val wholePackage = domainImports.any { import -> import.isWildcard }

        return named.filter { name -> name in imported || (wholePackage && name in typeNames) }.toSet() +
            KotlinSources.qualifiedNamesOf(code).filter(DomainCollaborator::isDomain)
    }
}
