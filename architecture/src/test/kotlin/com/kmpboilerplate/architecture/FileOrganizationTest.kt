package com.kmpboilerplate.architecture

import com.lemonappdev.konsist.api.declaration.KoBaseDeclaration
import com.lemonappdev.konsist.api.declaration.KoClassDeclaration
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import com.lemonappdev.konsist.api.declaration.KoInterfaceDeclaration
import com.lemonappdev.konsist.api.declaration.KoObjectDeclaration
import com.lemonappdev.konsist.api.declaration.KoTypeAliasDeclaration
import com.lemonappdev.konsist.api.provider.KoNameProvider
import com.lemonappdev.konsist.api.provider.modifier.KoVisibilityModifierProvider
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * File organization: one class, interface, object or enum per file,
 * and the file is named after what it declares. detekt's `MatchingDeclarationName` is off
 * (detekt.yml) because it reads a file of top-level composables as nameless; this judges the type
 * and the name without judging the shape of a Compose file.
 *
 * What it deliberately does not judge, so that the exclusions are written down rather than
 * assumed:
 *  - `private` and `internal` declarations — a file's own helpers and the module's plumbing
 *    (`CatGrid` beside `CatScreen`, `VISIBLE_TAG_COUNT` in `TagBadge.kt`) are not a second subject;
 *  - constants in SCREAMING_SNAKE_CASE — `CATAAS_BASE_URL` in `CataasBaseUrl.kt` is a value;
 *  - `Container.kt` in `infrastructure/config`, and only that file, whose subject is a list: its Koin
 *    modules are read top to bottom as a single graph.
 *
 * "Named after" is containment either way, so the Compose and Kotlin idioms count:
 * `rememberSheetState` in `SheetState.kt`, `LocalPalette` in `Palette.kt`, `resultOf` in `ResultOf.kt`,
 * `main` in `Main.kt`. A platform suffix is not part of the name: `Platform.desktop.kt` declares
 * `Platform`.
 */
class FileOrganizationTest {
    @Test
    fun `should declare one type per file and name the file after it`() {
        val offenders =
            files().mapNotNull { file ->
                val types = typesOf(file).filter(::isPublic)

                when {
                    types.size > 1 -> "${file.path}: ${types.joinToString { type -> nameOf(type) }}"
                    types.size == 1 && nameOf(types.first()) != subjectOf(file) ->
                        "${file.path}: declares ${nameOf(types.first())}"

                    else -> null
                }
            }

        assertTrue(offenders.isEmpty(), "$ONE_TYPE\n${offenders.joinToString(separator = "\n")}")
    }

    @Test
    fun `should name every file after what it declares`() {
        val offenders =
            files()
                .filterNot(::isContainer)
                .flatMap { file ->
                    declarationsOf(file)
                        .filter(::isPublic)
                        .map(::nameOf)
                        .filterNot(::isConstant)
                        .filterNot { name -> namesTheSubject(name, subjectOf(file)) }
                        .map { name -> "${file.path}: $name" }
                }

        assertTrue(offenders.isEmpty(), "$NAMED_AFTER\n${offenders.joinToString(separator = "\n")}")
    }

    /**
     * Every production file. Generated sources need no filter: Konsist drops every build/ directory below
     * the project root, and SourceLayoutTest keeps a source from hiding in one. A filter on the absolute
     * path emptied this list whenever the checkout itself sat under a folder named build.
     */
    private fun files(): List<KoFileDeclaration> =
        ProjectScope.production.files.also { files ->
            assertTrue(files.isNotEmpty(), "No production files found; the scope is misconfigured.")
        }

    private fun declarationsOf(file: KoFileDeclaration): List<KoBaseDeclaration> =
        file.declarations(includeNested = false, includeLocal = false)

    private fun typesOf(file: KoFileDeclaration): List<KoBaseDeclaration> =
        declarationsOf(file).filter { declaration ->
            declaration is KoClassDeclaration ||
                declaration is KoInterfaceDeclaration ||
                declaration is KoObjectDeclaration ||
                declaration is KoTypeAliasDeclaration
        }

    private fun isPublic(declaration: KoBaseDeclaration): Boolean =
        (declaration as? KoVisibilityModifierProvider)?.hasPublicOrDefaultModifier == true

    private fun nameOf(declaration: KoBaseDeclaration): String = (declaration as? KoNameProvider)?.name.orEmpty()

    /** The one file whose subject is a list of Koin modules — not any file that happens to share its name. */
    private fun isContainer(file: KoFileDeclaration): Boolean =
        file.packagee?.name == CONTAINER_PACKAGE && subjectOf(file) == CONTAINER

    private fun subjectOf(file: KoFileDeclaration): String =
        file.name.removeSuffix(KOTLIN_EXTENSION).substringBefore('.')

    private fun isConstant(name: String): Boolean = CONSTANT.matches(name)

    private fun namesTheSubject(
        name: String,
        subject: String,
    ): Boolean = name.contains(subject, ignoreCase = true) || subject.contains(name, ignoreCase = true)

    private companion object {
        const val ONE_TYPE = "One class, interface, object or enum per file, and the file carries its name."

        const val NAMED_AFTER =
            "A file is named after what it declares. Rename the file, or move the declaration into " +
                "the file that already carries its name."

        const val KOTLIN_EXTENSION = ".kt"

        const val CONTAINER = "Container"

        const val CONTAINER_PACKAGE = "com.kmpboilerplate.infrastructure.config"

        val CONSTANT = Regex("[A-Z][A-Z0-9_]*")
    }
}
