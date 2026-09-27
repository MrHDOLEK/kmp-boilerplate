package com.kmpboilerplate.architecture

import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import com.lemonappdev.konsist.api.verify.assertTrue
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * UI architecture — layout, component, screen. A component takes values and callbacks: it never loads
 * data and never reaches into the container for a use case. The screen folder lists the app's pages and
 * nothing else — a helper that drifts in there is a component, a callback bundle or an action
 * bundle that has not been given its folder yet.
 *
 * The component rules read the code, not only the imports: a wildcard, an import alias and a fully
 * qualified name reach Koin or a use case as surely as an import does.
 */
class UserInterfaceStructureTest {
    /**
     * An application action, or an action bundle from `ui/action` that hands a screen its use cases —
     * `catScreenActions()` as a default parameter is a component injecting them itself.
     */
    @Test
    fun `should keep use cases out of components`() {
        QualifiedReference.assertNoneNamed(
            components(),
            "A component takes values and callbacks; the screen drives the actions.",
        ) { name -> USE_CASE_PACKAGES.any { forbidden -> ImportRule.reaches(name, forbidden) } }
    }

    /**
     * Any identifier that names Koin: `koinInject`, `getKoin`, `KoinComponent`, `koinViewModel`,
     * `KoinPlatform`, and the `koin` segment of every `org.koin` import or qualified name.
     */
    @Test
    fun `should keep dependency injection out of components`() {
        val files = components()
        val offenders =
            files.mapNotNull { file ->
                KotlinSources
                    .identifiersOf(file.text)
                    .filter { name -> KOIN.containsMatchIn(name) }
                    .takeIf { named -> named.isNotEmpty() }
                    ?.let { named -> "${file.projectPath.trimStart('/')}: ${named.sorted().joinToString()}" }
            }

        assertTrue(files.isNotEmpty(), "No components found; the scope is misconfigured.")
        assertTrue(
            offenders.isEmpty(),
            "A component never injects; the screen does, through koinInject.\n" +
                offenders.joinToString(separator = "\n"),
        )
    }

    /**
     * `ui/component` follows the areas of the application, so a cat's tile and the mapper behind it
     * are found under the same word; `common` holds what every area draws with. The areas are read
     * from `application/viewmodel`, the layer a component displays.
     */
    @Test
    fun `should file every component under an area of the application or common`() {
        val areas =
            ProjectScope
                .inPackage(VIEW_MODEL)
                .files
                .mapNotNull { file -> file.packagee?.name }
                .filter { name -> name.startsWith("$VIEW_MODEL.") }
                .map { name -> name.removePrefix("$VIEW_MODEL.").substringBefore('.') }
                .filterNot { area -> area == MAPPER_FOLDER }
                .toSet()
        val allowed = areas + COMMON

        assertTrue(areas.isNotEmpty(), "No ViewModel areas found; the scope is misconfigured.")

        components().assertTrue(
            strict = true,
            additionalMessage = "A component sits in the folder of its area, one of ${allowed.sorted()}.",
        ) { file ->
            val packageName = file.packagee?.name.orEmpty()

            packageName.startsWith("$COMPONENT.") &&
                packageName.removePrefix("$COMPONENT.").substringBefore('.') in allowed
        }
    }

    @Test
    fun `should hold only screens in the screen folder`() {
        ProjectScope.inPackage(SCREEN).files.assertTrue(
            strict = true,
            additionalMessage = "ui/screen lists the app's pages; the rest belongs in component, action or navigation.",
        ) { file -> file.name.removeSuffix(KOTLIN_EXTENSION).endsWith("Screen") }
    }

    private fun components(): List<KoFileDeclaration> = ProjectScope.inPackage(COMPONENT).files

    private companion object {
        const val COMPONENT = "com.kmpboilerplate.app.ui.component"
        const val SCREEN = "com.kmpboilerplate.app.ui.screen"
        const val VIEW_MODEL = "com.kmpboilerplate.application.viewmodel"
        const val KOTLIN_EXTENSION = ".kt"

        /** `viewmodel/mapper` is a layer of its own, not an area of `viewmodel`. */
        const val MAPPER_FOLDER = "mapper"

        /** The building blocks every area draws with: rows, tiles, banners, states. */
        const val COMMON = "common"

        val USE_CASE_PACKAGES = listOf("com.kmpboilerplate.application.action", "com.kmpboilerplate.app.ui.action")

        val KOIN = Regex("koin", RegexOption.IGNORE_CASE)
    }
}
