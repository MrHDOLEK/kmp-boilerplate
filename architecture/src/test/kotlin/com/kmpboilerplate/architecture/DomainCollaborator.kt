package com.kmpboilerplate.architecture

import com.lemonappdev.konsist.api.declaration.KoBaseDeclaration
import com.lemonappdev.konsist.api.declaration.KoImportDeclaration
import com.lemonappdev.konsist.api.provider.KoNameProvider
import com.lemonappdev.konsist.api.provider.modifier.KoVisibilityModifierProvider

object DomainCollaborator {
    const val DOMAIN = "com.kmpboilerplate.domain"

    const val WILDCARD =
        "A wildcard import of a domain package that declares one counts as reaching for it: " +
            "import the names you use."

    val SERVICE_SUFFIXES = listOf("Service", "Factory", "Resolver", "Converter", "Mapper")

    fun isService(name: String): Boolean = SERVICE_SUFFIXES.any { suffix -> name.endsWith(suffix) }

    fun isRepository(name: String): Boolean = name.endsWith("RepositoryInterface")

    fun isPort(name: String): Boolean = name.endsWith("Interface") && !isRepository(name)

    fun isCollaborator(name: String): Boolean = isService(name) || isRepository(name) || isPort(name)

    fun isDomain(importName: String): Boolean = ImportRule.reaches(importName, DOMAIN)

    fun subjectOf(fileName: String): String = fileName.removeSuffix(".kt").substringBefore('.')

    private val aliases: Map<String, Set<String>> by lazy {
        ProjectScope.inPackage(DOMAIN).typeAliases.associate { alias ->
            alias.name to KotlinSources.identifiersOf(KotlinSources.aliasedIn(alias.text))
        }
    }

    fun namesBehind(name: String): Set<String> {
        val seen = linkedSetOf<String>()
        val pending = ArrayDeque(listOf(name))

        while (pending.isNotEmpty()) {
            val next = pending.removeFirst()

            if (seen.add(next)) pending.addAll(aliases[next].orEmpty())
        }

        return seen
    }

    fun standsFor(
        name: String,
        matches: (String) -> Boolean,
    ): Boolean = namesBehind(name).any(matches)

    private val declaredIn: Map<String, Set<String>> by lazy {
        ProjectScope
            .inPackage(DOMAIN)
            .files
            .groupBy { file -> file.packagee?.name.orEmpty() }
            .mapValues { (_, files) ->
                files
                    .flatMap { file -> file.declarations(includeNested = false, includeLocal = false) }
                    .filter(::isImportable)
                    .mapNotNull { declaration -> (declaration as? KoNameProvider)?.name }
                    .toSet()
            }
    }

    private fun isImportable(declaration: KoBaseDeclaration): Boolean =
        (declaration as? KoVisibilityModifierProvider)?.hasPrivateModifier != true

    fun hidesCollaborator(
        import: KoImportDeclaration,
        matches: (String) -> Boolean,
    ): Boolean =
        isDomain(import.name) &&
            (
                import.name.split('.').any { segment -> standsFor(segment, matches) } ||
                    (import.isWildcard && declaredIn[import.name].orEmpty().any { name -> standsFor(name, matches) })
            )
}
