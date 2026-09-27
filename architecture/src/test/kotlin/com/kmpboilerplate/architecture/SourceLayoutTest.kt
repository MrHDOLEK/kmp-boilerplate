package com.kmpboilerplate.architecture

import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Project structure: two product modules, each with its own package roots, and every file where its
 * package says it is.
 *
 * Every other rule selects its files by package or by module, so a file outside that map is judged
 * by none of them: a file with no `package` line belongs to no layer, a `com.kmpboilerplate.common`
 * belongs to no layer either, a `com.kmpboilerplate.domain` file in composeApp reads the domain without
 * an import, and a third module has no boundary at all. detekt's InvalidPackageDeclaration says nothing about
 * a file without a package, and ktlint's `package-name` is off, so this closes the map instead.
 */
class SourceLayoutTest {
    @Test
    fun `should declare in every production file the package its folder names`() {
        val offenders =
            files().mapNotNull { file ->
                val path = pathOf(file)
                val marker = "/src/${file.sourceSetName}/kotlin/"
                val declared = file.packagee?.name

                when {
                    declared == null -> "$path: no package"
                    marker !in "/$path" -> "$path: outside a src/<source set>/kotlin folder"
                    declared != folderPackageOf(path, marker) -> "$path: declares $declared"
                    else -> null
                }
            }

        assertTrue(
            offenders.isEmpty(),
            "A file declares the package its folder names, under src/<source set>/kotlin:\n" +
                offenders.joinToString(separator = "\n"),
        )
    }

    @Test
    fun `should keep every module to its own package roots`() {
        val files = files()

        assertEquals(
            MODULE_ROOTS.keys,
            files.map { file -> file.moduleName }.toSet(),
            "Production code lives in shared and composeApp. A new module is added here, with " +
                "its package roots, in the same change as its first file.",
        )

        val offenders =
            files
                .filter { file ->
                    val packageName = file.packagee?.name.orEmpty()

                    MODULE_ROOTS[file.moduleName].orEmpty().none { root -> ImportRule.reaches(packageName, root) }
                }.map { file -> "${pathOf(file)}: ${file.packagee?.name}" }

        assertTrue(
            offenders.isEmpty(),
            "shared holds the domain, the application and infrastructure; composeApp holds " +
                "com.kmpboilerplate.app:\n" + offenders.joinToString(separator = "\n"),
        )
    }

    /**
     * Konsist drops every path with a `build/` or `target/` directory in it, source folders included,
     * and every path with a segment that starts with `buildsrc` in any case, a file name included —
     * `BuildSrcProbe.kt` as much as a `buildSrc/` folder. Only what sits under `src/` is judged here,
     * so a module's output and a root `buildSrc/` of build logic stay out of it.
     */
    @Test
    fun `should keep every Kotlin source where Konsist reads it`() {
        val hidden =
            SourceTree.kotlinFiles
                .map(SourceTree::pathOf)
                .filter { path ->
                    path.endsWith(".kt") && HIDDEN_FROM_KONSIST.any { pattern -> pattern.containsMatchIn(path) }
                }

        assertTrue(SourceTree.kotlinFiles.isNotEmpty(), "No Kotlin files found; the source tree is misconfigured.")
        assertTrue(
            hidden.isEmpty(),
            "Konsist skips a build/ or target/ directory and any file or folder whose name starts with " +
                "buildsrc, so no rule judges what sits there. Rename it:\n" + hidden.joinToString(separator = "\n"),
        )
    }

    private fun files(): List<KoFileDeclaration> =
        ProjectScope.production.files.also { files ->
            assertTrue(files.isNotEmpty(), "No production files found; the scope is misconfigured.")
        }

    private fun pathOf(file: KoFileDeclaration): String = file.projectPath.replace('\\', '/').trimStart('/')

    private fun folderPackageOf(
        path: String,
        marker: String,
    ): String = "/$path".substringAfter(marker).substringBeforeLast('/', "").replace('/', '.')

    private companion object {
        const val DOMAIN = "com.kmpboilerplate.domain"
        const val APPLICATION = "com.kmpboilerplate.application"
        const val INFRASTRUCTURE = "com.kmpboilerplate.infrastructure"
        const val APP = "com.kmpboilerplate.app"

        val MODULE_ROOTS =
            mapOf(
                "shared" to listOf(DOMAIN, APPLICATION, INFRASTRUCTURE),
                "composeApp" to listOf(APP),
            )

        val HIDDEN_FROM_KONSIST =
            listOf(
                Regex("""(^|/)src/(.+/)?(build|target)/"""),
                Regex("""(^|/)src/(.+/)?buildsrc""", RegexOption.IGNORE_CASE),
            )
    }
}
