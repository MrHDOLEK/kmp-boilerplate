package com.kmpboilerplate.architecture

import com.lemonappdev.konsist.api.declaration.KoFileDeclaration

/**
 * Whether an import reaches into [packageName]. Konsist reports `import a.b.*` as `a.b`, with no
 * trailing dot, so a plain prefix check of `"a.b."` lets every wildcard import through.
 */
object ImportRule {
    fun reaches(
        importName: String,
        packageName: String,
    ): Boolean = importName == packageName || importName.startsWith("$packageName.")

    /**
     * What each import alias of [file] stands for: `import a.b.CatRepositoryInterface as Cats` maps `Cats`
     * to `CatRepositoryInterface`.
     */
    fun aliasesIn(file: KoFileDeclaration): Map<String, String> =
        file.imports
            .mapNotNull { import -> import.alias?.let { alias -> alias.name to import.name.substringAfterLast('.') } }
            .toMap()
}
