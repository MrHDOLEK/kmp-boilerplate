package com.kmpboilerplate.architecture

import com.lemonappdev.konsist.api.KoModifier
import com.lemonappdev.konsist.api.declaration.KoBaseDeclaration
import com.lemonappdev.konsist.api.provider.KoModuleProvider
import com.lemonappdev.konsist.api.provider.KoNameProvider
import com.lemonappdev.konsist.api.provider.KoPackageProvider
import com.lemonappdev.konsist.api.provider.KoSourceSetProvider
import com.lemonappdev.konsist.api.provider.modifier.KoModifierProvider
import kotlin.test.Test
import kotlin.test.assertTrue

class PlatformActualTest {
    @Test
    fun `should give every expect declaration an actual on android ios and desktop`() {
        val declarations = ProjectScope.production.declarations(includeNested = true)
        val expected = declarations.filter { declaration -> hasModifier(declaration, KoModifier.EXPECT) }
        val actuals =
            declarations
                .filter { declaration -> hasModifier(declaration, KoModifier.ACTUAL) }
                .groupBy(keySelector = ::keyOf, valueTransform = ::sourceSetOf)

        val missing =
            expected.flatMap { declaration ->
                val key = keyOf(declaration)
                val sourceSets = actuals[key].orEmpty().toSet()

                PLATFORMS
                    .filterNot { platform -> platform in sourceSets }
                    .map { platform -> "$key has no actual in $platform" }
            }

        assertTrue(missing.isEmpty(), missing.joinToString(separator = "\n"))
    }

    private fun hasModifier(
        declaration: KoBaseDeclaration,
        modifier: KoModifier,
    ): Boolean = (declaration as? KoModifierProvider)?.hasModifier(modifier) == true

    private fun keyOf(declaration: KoBaseDeclaration): String {
        val module = (declaration as KoModuleProvider).moduleName
        val packageName = (declaration as? KoPackageProvider)?.packagee?.name.orEmpty()
        val name = (declaration as KoNameProvider).name

        return "$module:$packageName.$name"
    }

    private fun sourceSetOf(declaration: KoBaseDeclaration): String = (declaration as KoSourceSetProvider).sourceSetName

    private companion object {
        val PLATFORMS = listOf("androidMain", "iosMain", "desktopMain")
    }
}
