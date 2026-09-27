package com.kmpboilerplate.architecture

import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import kotlin.test.assertTrue

object QualifiedReference {
    fun assertNoneNamed(
        files: List<KoFileDeclaration>,
        message: String,
        forbidden: (String) -> Boolean,
    ) {
        assertTrue(files.isNotEmpty(), "No files found; the scope is misconfigured.")

        val offenders =
            files.flatMap { file ->
                KotlinSources
                    .qualifiedNamesOf(file.text)
                    .filter(forbidden)
                    .distinct()
                    .map { name -> "${file.projectPath.trimStart('/')}: $name" }
            }

        assertTrue(offenders.isEmpty(), "$message\n${offenders.joinToString(separator = "\n")}")
    }
}
