package com.kmpboilerplate.architecture

object DomainArea {
    private const val PREFIX = "${DomainCollaborator.DOMAIN}."

    private const val SHARED = "shared"

    val SHARED_VOCABULARY = emptySet<String>()

    fun names(): Set<String> =
        ProjectScope
            .inPackage(DomainCollaborator.DOMAIN)
            .files
            .mapNotNull { file -> file.packagee?.name }
            .filter { name -> name.startsWith(PREFIX) }
            .mapNotNull { name -> areaOf(name.removePrefix(PREFIX)) }
            .toSet()

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
