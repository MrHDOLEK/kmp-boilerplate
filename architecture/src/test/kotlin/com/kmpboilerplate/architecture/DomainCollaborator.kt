package com.kmpboilerplate.architecture

import com.lemonappdev.konsist.api.declaration.KoBaseDeclaration
import com.lemonappdev.konsist.api.declaration.KoImportDeclaration
import com.lemonappdev.konsist.api.provider.KoNameProvider
import com.lemonappdev.konsist.api.provider.modifier.KoVisibilityModifierProvider

/**
 * Whether a domain name is a collaborator — something Koin builds and wires — or a fact the feature
 * is about.
 *
 * The domain is packaged by feature, so an area such as `cat/` holds its entities, its value
 * objects, its services and its contracts side by side. The folder therefore no
 * longer says which of the two a file is, and the rules that used to read `domain/service`,
 * `domain/repository` and `domain/port` read the name instead. That is not a fallback: the name is
 * what a reader sees first: `*Service`, `*RepositoryInterface`, `*Interface`.
 */
object DomainCollaborator {
    const val DOMAIN = "com.kmpboilerplate.domain"

    /** Appended to the message of every import rule that reads [hidesCollaborator]. */
    const val WILDCARD =
        "A wildcard import of a domain package that declares one counts as reaching for it: " +
            "import the names you use."

    /** Named after what they do, not what they hold; every one of them is built by Koin. */
    val SERVICE_SUFFIXES = listOf("Service", "Factory", "Resolver", "Converter", "Mapper")

    fun isService(name: String): Boolean = SERVICE_SUFFIXES.any { suffix -> name.endsWith(suffix) }

    fun isRepository(name: String): Boolean = name.endsWith("RepositoryInterface")

    /** A port is a contract that is not persistence: a clock, a time zone, a location source. */
    fun isPort(name: String): Boolean = name.endsWith("Interface") && !isRepository(name)

    fun isCollaborator(name: String): Boolean = isService(name) || isRepository(name) || isPort(name)

    fun isDomain(importName: String): Boolean = ImportRule.reaches(importName, DOMAIN)

    fun subjectOf(fileName: String): String = fileName.removeSuffix(".kt").substringBefore('.')

    /**
     * What each domain typealias names on its right-hand side, comments and strings aside.
     *
     * `typealias Cats = CatRepositoryInterface` gives a contract a name no suffix betrays, so a rule
     * reading the name alone would take `Cats` for a fact.
     */
    private val aliases: Map<String, Set<String>> by lazy {
        ProjectScope.inPackage(DOMAIN).typeAliases.associate { alias ->
            alias.name to KotlinSources.identifiersOf(KotlinSources.aliasedIn(alias.text))
        }
    }

    /** [name] and every name it stands for through domain typealiases, directly or through another alias. */
    fun namesBehind(name: String): Set<String> {
        val seen = linkedSetOf<String>()
        val pending = ArrayDeque(listOf(name))

        while (pending.isNotEmpty()) {
            val next = pending.removeFirst()

            if (seen.add(next)) pending.addAll(aliases[next].orEmpty())
        }

        return seen
    }

    /**
     * Whether [name] is one [matches] accepts, or a domain typealias that stands for one — directly or
     * through another alias.
     */
    fun standsFor(
        name: String,
        matches: (String) -> Boolean,
    ): Boolean = namesBehind(name).any(matches)

    /** The names a wildcard import of each domain package brings in: what its files declare at top level. */
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

    /**
     * Whether [import] brings in a domain name [matches] accepts — or hides one behind a wildcard.
     *
     * An import is judged by everything it reaches, not by its last segment: a member import such as
     * `com.kmpboilerplate.domain.cat.CatService.Companion.PAGE_SIZE` reaches `CatService`, and a
     * typealias is judged by what it stands for. So does a wildcard of those members,
     * `CatService.Companion.*`.
     *
     * Konsist reports `import com.kmpboilerplate.domain.cat.*` as `com.kmpboilerplate.domain.cat`, so no
     * segment of a package wildcard names anything. `.editorconfig` and detekt both allow wildcards, so the import is
     * judged by what its package declares: `com.kmpboilerplate.domain.cat.enum.*` would bring in
     * enumerations and pass, `com.kmpboilerplate.domain.cat.*` brings in `CatRepositoryInterface` and
     * counts as reaching for it whether the file then uses it or not. The code half of each rule reads the names a file actually
     * spells.
     */
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
