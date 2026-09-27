package com.kmpboilerplate.architecture

import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import kotlin.test.assertTrue

/**
 * The text half of an import rule. Konsist judges what a file imports, and a fully qualified name
 * needs no import: `com.kmpboilerplate.infrastructure.cataas.CatRepository::class` in an action
 * passes every import rule there is. This reads the names the code spells instead — comments and
 * strings aside, so a constant such as `"com.kmpboilerplate.app.action.OPEN"` is text, not a
 * reference.
 */
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
