package com.kmpboilerplate.architecture

import com.lemonappdev.konsist.api.declaration.KoFileDeclaration

object ImportRule {
    fun reaches(
        importName: String,
        packageName: String,
    ): Boolean = importName == packageName || importName.startsWith("$packageName.")

    fun aliasesIn(file: KoFileDeclaration): Map<String, String> =
        file.imports
            .mapNotNull { import -> import.alias?.let { alias -> alias.name to import.name.substringAfterLast('.') } }
            .toMap()
}
