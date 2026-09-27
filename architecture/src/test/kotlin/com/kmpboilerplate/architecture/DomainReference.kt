package com.kmpboilerplate.architecture

import com.lemonappdev.konsist.api.declaration.KoFileDeclaration

/**
 * Which names in a piece of code reach the domain, however the file spelled them: a domain import
 * under its own name or an import alias (`Cat as Kitten`), any domain type when the file imports
 * a domain package whole, or a qualified domain name that needs no import at all.
 */
object DomainReference {
    /** Every class, interface, object and typealias the domain declares, nested ones included. */
    val typeNames: Set<String> by lazy {
        val domain = ProjectScope.inPackage(DomainCollaborator.DOMAIN)

        (
            domain.classes().map { declaration -> declaration.name } +
                domain.interfaces().map { declaration -> declaration.name } +
                domain.objects().map { declaration -> declaration.name } +
                domain.typeAliases.map { declaration -> declaration.name }
        ).toSet()
    }

    /** The names in [code] that reach the domain through the imports of [file], or qualified. */
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
