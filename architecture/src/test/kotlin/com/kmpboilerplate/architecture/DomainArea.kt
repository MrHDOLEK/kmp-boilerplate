package com.kmpboilerplate.architecture

/**
 * The names the domain is split into, as a folder outside the domain may use them.
 *
 * `domain/shared` is not an area but the vocabulary every area speaks, so what sits under it counts
 * by its own name: a declared `domain/shared/geo` would make `geo` an area, the word the application
 * would use for `viewmodel/geo`. [AreaStructureTest] reads the same tree with the other question in
 * mind — are the four layers split alike — and there such a word is declared absent from the domain.
 * The two disagree about that one name deliberately: a repository of the device's position would have
 * a domain folder to belong to, it is just spelled `shared/geo`.
 *
 * No guard for an empty result. A scope that found nothing fails every caller's rule, which is what
 * a misconfigured scope should do.
 */
object DomainArea {
    private const val PREFIX = "${DomainCollaborator.DOMAIN}."

    /** `domain/shared` holds what no area owns; the folders of [SHARED_VOCABULARY] inside it are areas. */
    private const val SHARED = "shared"

    /**
     * The folders `domain/shared` may hold: vocabulary every area speaks and no area owns. A closed list,
     * which [AreaStructureTest] holds the tree to, so a feature of its own cannot become an area by
     * moving under `shared`; a new word is added here, and declared absent from the domain there, in
     * the same change.
     */
    val SHARED_VOCABULARY = emptySet<String>()

    fun names(): Set<String> =
        ProjectScope
            .inPackage(DomainCollaborator.DOMAIN)
            .files
            .mapNotNull { file -> file.packagee?.name }
            .filter { name -> name.startsWith(PREFIX) }
            .mapNotNull { name -> areaOf(name.removePrefix(PREFIX)) }
            .toSet()

    /**
     * `shared` is no area; `shared.<x>` is the area `x` only when `x` is declared vocabulary; anything
     * else is the area its first segment names.
     */
    private fun areaOf(rest: String): String? {
        val first = rest.substringBefore('.')
        val word = rest.removePrefix("$SHARED.").substringBefore('.')

        return when {
            rest == SHARED -> null
            first == SHARED -> word.takeIf { shared -> shared in SHARED_VOCABULARY }
            else -> first
        }
    }
}
