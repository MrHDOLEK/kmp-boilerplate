package com.kmpboilerplate.architecture

import com.lemonappdev.konsist.api.KoModifier
import com.lemonappdev.konsist.api.declaration.KoBaseDeclaration
import com.lemonappdev.konsist.api.declaration.KoClassDeclaration
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import com.lemonappdev.konsist.api.declaration.KoFunctionDeclaration
import com.lemonappdev.konsist.api.declaration.KoInterfaceDeclaration
import com.lemonappdev.konsist.api.declaration.KoObjectDeclaration
import com.lemonappdev.konsist.api.declaration.KoPropertyDeclaration
import com.lemonappdev.konsist.api.declaration.KoTypeAliasDeclaration
import com.lemonappdev.konsist.api.provider.KoNameProvider
import com.lemonappdev.konsist.api.verify.assertTrue
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Domain and application layers: an area folder says what a file is about, the
 * subfolder says what kind of thing it is. `enum/` holds enumerations and nothing else;
 * `valueObject/` holds the domain values that are not enumerations, so neither folder has to be
 * opened to know what is inside. The application layer has no `valueObject/`: everything there is a
 * ViewModel, and calling one a value object would borrow a word from the domain that it does not mean.
 *
 * The kind folders are a closed list, and both directions are held: every enumeration is in an
 * `enum/` folder and an `enum/` folder holds nothing else; `valueObject/` holds values and nothing
 * that is an enumeration, a collaborator or a comparator. A `model/` folder, or a `valueObject/` in
 * the application, is not a kind the standard names.
 *
 * The enumeration rules are not strict: the code declares no enumeration yet, and each holds from the first.
 */
class KindPackageTest {
    @Test
    fun `should keep every domain enumeration in an enum package`() {
        ProjectScope
            .inPackage(DomainCollaborator.DOMAIN)
            .classes(includeNested = true, includeLocal = false)
            .filter { declaration -> declaration.hasModifier(KoModifier.ENUM) }
            .assertTrue(
                strict = false,
                additionalMessage = "An enumeration belongs in its area's `enum` package, not nested in a fact.",
            ) { declaration -> packageOf(declaration).endsWith(".$ENUM") && !isNested(declaration) }
    }

    /**
     * A value is what a fact is made of: no enumeration (that is `enum/`), no service or contract (that
     * is the area folder), and no comparator — an ordering over a type is a rule about the area and
     * sits with the facts, like a `cat/CatAgeOrder.kt` would. `Comparable` stays: a value's own natural
     * order, as a date has, is part of the value.
     *
     * Plain assertions: a domain with no value object at all is one the standard allows, and a strict
     * assertion fails on an empty list.
     */
    @Test
    fun `should hold only values in the value object packages`() {
        val domain = ProjectScope.inPackage(DomainCollaborator.DOMAIN).files
        val offenders =
            domain
                .filter { file -> packageOf(file).endsWith(".$VALUE_OBJECT") }
                .filter(::holdsMoreThanValues)
                .map { file -> file.projectPath.trimStart('/') }

        assertTrue(domain.isNotEmpty(), "No domain files found; the scope is misconfigured.")
        assertTrue(offenders.isEmpty(), "$VALUES_ONLY\n${offenders.joinToString(separator = "\n")}")
    }

    @Test
    fun `should keep every view model enumeration in an enum package`() {
        ProjectScope
            .inPackage(VIEW_MODEL)
            .classes(includeNested = true, includeLocal = false)
            .filter { declaration -> declaration.hasModifier(KoModifier.ENUM) }
            .assertTrue(
                strict = false,
                additionalMessage = "An enumeration belongs in its area's `enum` package, not nested in a ViewModel.",
            ) { declaration -> packageOf(declaration).endsWith(".$ENUM") && !isNested(declaration) }
    }

    /**
     * The other direction: an `enum/` folder of the domain or the application holds nothing but
     * top-level enumerations — no data class, no sealed hierarchy, no ViewModel that merely sits there.
     */
    @Test
    fun `should hold nothing but enumerations in an enum package`() {
        ProjectScope.production.files
            .filter { file -> isLayered(file) && packageOf(file).endsWith(".$ENUM") }
            .assertTrue(
                strict = false,
                additionalMessage = "An `enum` package holds enumerations only; any other value goes to its area.",
            ) { file ->
                file
                    .declarations(includeNested = false, includeLocal = false)
                    .filter(::isDeclaration)
                    .all(::isEnum)
            }
    }

    /**
     * Where a file of the domain or the application may sit. Which words the areas are is
     * [AreaStructureTest]'s question, and so is a file at the root of a layer; this one asks only
     * which kind folders exist below an area.
     */
    @Test
    fun `should keep the domain and the application to the kind folders the standard names`() {
        ProjectScope.production.files
            .filter(::isLayered)
            .assertTrue(
                strict = true,
                additionalMessage =
                    "Below an area the domain has only `enum` and `valueObject`, " +
                        "a ViewModel area only `enum`.",
            ) { file -> ALLOWED_PACKAGES.any { allowed -> allowed.matches(packageOf(file)) } }
    }

    /** An enumeration, a type named as a collaborator, or a file that spells a comparator. */
    private fun holdsMoreThanValues(file: KoFileDeclaration): Boolean {
        val declarations = file.declarations(includeNested = true, includeLocal = false)
        val types = declarations.filter(::isType).mapNotNull { declaration -> (declaration as? KoNameProvider)?.name }

        return declarations.any(::isEnum) ||
            types.any(DomainCollaborator::isCollaborator) ||
            KotlinSources.identifiersOf(file.text).any { name -> name in ORDERINGS }
    }

    private fun isLayered(file: KoFileDeclaration): Boolean =
        LAYERS.any { layer -> ImportRule.reaches(packageOf(file), layer) }

    private fun packageOf(declaration: KoClassDeclaration): String = declaration.packagee?.name.orEmpty()

    private fun packageOf(file: KoFileDeclaration): String = file.packagee?.name.orEmpty()

    private fun isEnum(declaration: KoBaseDeclaration): Boolean =
        declaration is KoClassDeclaration && declaration.hasModifier(KoModifier.ENUM)

    private fun isNested(declaration: KoClassDeclaration): Boolean = !declaration.isTopLevel

    private fun isType(declaration: KoBaseDeclaration): Boolean =
        declaration is KoClassDeclaration || declaration is KoObjectDeclaration || declaration is KoInterfaceDeclaration

    private fun isDeclaration(declaration: KoBaseDeclaration): Boolean =
        isType(declaration) ||
            declaration is KoTypeAliasDeclaration ||
            declaration is KoFunctionDeclaration ||
            declaration is KoPropertyDeclaration

    private companion object {
        const val ENUM = "enum"
        const val VIEW_MODEL = "com.kmpboilerplate.application.viewmodel"
        const val VALUE_OBJECT = "valueObject"

        const val NAME = "[A-Za-z][A-Za-z0-9]*"

        const val VALUES_ONLY =
            "`valueObject` holds values: an enumeration goes to `enum`, a service, a contract or a " +
                "comparator to the area folder."

        val LAYERS = listOf(DomainCollaborator.DOMAIN, "com.kmpboilerplate.application")

        val ORDERINGS = setOf("Comparator", "compareBy", "compareByDescending")

        val ALLOWED_PACKAGES =
            listOf(
                // The root and an area of the domain, and `shared` (a file at a root: AreaStructureTest).
                Regex("""com\.kmpboilerplate\.domain(\.$NAME)?"""),
                Regex("""com\.kmpboilerplate\.domain\.(?!shared\.)$NAME\.(enum|valueObject)"""),
                // Which words may sit under `shared`: AreaStructureTest.
                Regex("""com\.kmpboilerplate\.domain\.shared\.$NAME(\.(enum|valueObject))?"""),
                Regex("""com\.kmpboilerplate\.application(\.(action|viewmodel))?"""),
                // An area of the actions or the ViewModels, and `viewmodel.mapper` itself.
                Regex("""com\.kmpboilerplate\.application\.(action|viewmodel)\.$NAME"""),
                Regex("""com\.kmpboilerplate\.application\.viewmodel\.(?!mapper\.)$NAME\.enum"""),
                Regex("""com\.kmpboilerplate\.application\.viewmodel\.mapper\.$NAME"""),
            )
    }
}
