package com.kmpboilerplate.architecture

/**
 * The first segments a dotted name may start with and still be a package: one the production code
 * imports from, or a standard one. `cat.tags` is then a property read and `io.ktor.client.HttpClient`
 * a library, so a rule reading the qualified names of the code tells the two apart.
 */
object PackageRoots {
    /**
     * Roots every production classpath carries whether or not a file imports from them — the
     * platforms' own, and `okhttp3`, which arrives with the Ktor engine and is never imported.
     */
    private val standard =
        setOf(
            "android",
            "androidx",
            "com",
            "dalvik",
            "io",
            "java",
            "javax",
            "jdk",
            "kotlin",
            "kotlinx",
            "okhttp3",
            "org",
            "platform",
            "sun",
        )

    /** The standard roots and every first segment the production imports start with, such as `coil3`. */
    val known: Set<String> by lazy {
        ProjectScope.production.imports
            .map { import -> import.name.substringBefore('.') }
            .toSet() + standard
    }
}
