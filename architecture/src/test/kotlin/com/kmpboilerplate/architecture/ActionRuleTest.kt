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
import com.lemonappdev.konsist.api.provider.modifier.KoVisibilityModifierProvider
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

class ActionRuleTest {
    @Test
    fun `should keep repositories and ports out of actions`() {
        ProjectScope.inPackage(ACTIONS).imports.assertFalse(
            strict = true,
            additionalMessage = "$DELEGATES ${DomainCollaborator.WILDCARD}",
        ) { import -> DomainCollaborator.hidesCollaborator(import, ::isContract) }
    }

    @Test
    fun `should name no repository or port in the code of an action`() {
        val contracts =
            ProjectScope
                .inPackage(DomainCollaborator.DOMAIN)
                .interfaces()
                .filterNot { contract -> contract.hasModifier(KoModifier.SEALED) }
                .map { contract -> contract.name }
                .filter(::isContract)
                .toSet()
        val files = ProjectScope.inPackage(ACTIONS).files
        val offenders =
            files.mapNotNull { file ->
                KotlinSources
                    .identifiersOf(file.text)
                    .filter { name -> DomainCollaborator.standsFor(name) { behind -> behind in contracts } }
                    .takeIf { named -> named.isNotEmpty() }
                    ?.let { named -> "${pathOf(file)}: ${named.sorted().joinToString()}" }
            }

        assertTrue(
            contracts.isNotEmpty() && files.isNotEmpty(),
            "No domain contracts or actions found; the scope is misconfigured.",
        )
        assertTrue(offenders.isEmpty(), "$DELEGATES\n${offenders.joinToString(separator = "\n")}")
    }

    @Test
    fun `should return a result carrying no domain entity from every action`() {
        val invocations = invocations()

        if (invocations.none { invocation -> returnTypeOf(invocation).contains("ViewModel") }) {
            fail("No return type carries its type argument, so this rule would pass vacuously.")
        }

        invocations.assertTrue(strict = true, additionalMessage = RETURNS_VIEW_MODELS) { invocation ->
            val returned = returnTypeOf(invocation)

            isResult(returned, invocation.containingFile) &&
                DomainReference.namedIn(returned, invocation.containingFile).isEmpty()
        }
    }

    @Test
    fun `should build every result with resultOf so cancellation is not swallowed`() {
        ProjectScope.inPackage(ACTIONS).files.assertFalse(
            strict = true,
            additionalMessage =
                "runCatching swallows the CancellationException a cancelled screen throws; use resultOf.",
        ) { file -> RUN_CATCHING.containsMatchIn(KotlinSources.symbolsOf(file.text)) }
    }

    @Test
    fun `should wrap every invocation in resultOf and catch nothing by hand`() {
        val files = ProjectScope.inPackage(ACTIONS).files
        val invocations = invocations()
        val unwrapped =
            invocations
                .filterNot { invocation ->
                    invocation.containingFile.imports.any { import -> import.name == RESULT_OF } &&
                        CALLS_RESULT_OF.containsMatchIn(KotlinSources.symbolsOf(invocation.text))
                }.map { invocation -> "${pathOf(invocation.containingFile)}: invoke without $RESULT_OF" }
        val catching =
            files
                .filter { file -> CATCH.containsMatchIn(KotlinSources.symbolsOf(file.text)) }
                .map { file -> "${pathOf(file)}: catch" }
        val offenders = unwrapped + catching

        assertTrue(invocations.isNotEmpty(), "No action invocations found; the scope is misconfigured.")
        assertTrue(offenders.isEmpty(), "$WRAPPED\n${offenders.joinToString(separator = "\n")}")
    }

    @Test
    fun `should declare nothing in the action package but Action classes`() {
        val files = ProjectScope.inPackage(ACTIONS).files
        val offenders =
            files.flatMap { file ->
                file
                    .declarations(includeNested = false, includeLocal = false)
                    .filter(::isDeclaration)
                    .filterNot(::isHelper)
                    .filterNot(::isActionClass)
                    .map { declaration -> "${pathOf(file)}: ${(declaration as? KoNameProvider)?.name}" }
            }

        assertTrue(files.isNotEmpty(), "No action files found; the scope is misconfigured.")
        assertTrue(offenders.isEmpty(), "$ACTION_CLASSES\n${offenders.joinToString(separator = "\n")}")
    }

    private fun invocations(): List<KoFunctionDeclaration> =
        ProjectScope
            .inPackage(ACTIONS)
            .classes(includeNested = false, includeLocal = false)
            .filterNot { action -> action.hasPrivateModifier || action.hasInternalModifier }
            .flatMap { action -> action.functions() }
            .filter { function -> function.name == "invoke" }

    private fun isContract(name: String): Boolean =
        DomainCollaborator.isRepository(name) || DomainCollaborator.isPort(name)

    private fun returnTypeOf(function: KoFunctionDeclaration): String = function.returnType?.text.orEmpty()

    private fun isResult(
        returned: String,
        file: KoFileDeclaration,
    ): Boolean {
        val written = KotlinSources.symbolsOf(returned).replace(WHITESPACE, "").replace("`", "")
        val shadowed =
            file.imports.any { import ->
                (import.alias?.name ?: import.name.substringAfterLast('.')) == RESULT && import.name != KOTLIN_RESULT
            }

        return !shadowed && (written.startsWith("$RESULT<") || written.startsWith("$KOTLIN_RESULT<"))
    }

    private fun isDeclaration(declaration: KoBaseDeclaration): Boolean =
        declaration is KoClassDeclaration ||
            declaration is KoInterfaceDeclaration ||
            declaration is KoObjectDeclaration ||
            declaration is KoTypeAliasDeclaration ||
            declaration is KoFunctionDeclaration ||
            declaration is KoPropertyDeclaration

    private fun isHelper(declaration: KoBaseDeclaration): Boolean =
        (declaration as? KoVisibilityModifierProvider)?.let { visible ->
            visible.hasPrivateModifier || visible.hasInternalModifier
        } == true

    private fun isActionClass(declaration: KoBaseDeclaration): Boolean =
        declaration is KoClassDeclaration &&
            declaration.name.endsWith("Action") &&
            !declaration.hasModifier(KoModifier.ABSTRACT, *NOT_A_USE_CASE)

    private fun pathOf(file: KoFileDeclaration): String = file.projectPath.trimStart('/')

    private companion object {
        const val ACTIONS = "com.kmpboilerplate.application.action"
        const val RESULT = "Result"
        const val KOTLIN_RESULT = "kotlin.Result"
        const val RESULT_OF = "com.kmpboilerplate.domain.shared.resultOf"

        const val DELEGATES =
            "An action delegates to a domain service; a repository or a port is the service's to read."

        const val RETURNS_VIEW_MODELS =
            "An action returns Result<ViewModel>, spelled out: the UI never sees a domain entity."

        const val WRAPPED =
            "Build the result with resultOf, which rethrows cancellation; a hand-written catch swallows it."

        const val ACTION_CLASSES =
            "A use case is a class named *Action; anything else in application/action is a use case no rule reads."

        val WHITESPACE = Regex("""\s+""")
        val CALLS_RESULT_OF = Regex("""\bresultOf\b""")
        val RUN_CATCHING = Regex("""\brunCatching\b""")
        val CATCH = Regex("""\bcatch\b""")

        val NOT_A_USE_CASE =
            arrayOf(KoModifier.SEALED, KoModifier.ENUM, KoModifier.ANNOTATION, KoModifier.DATA, KoModifier.VALUE)
    }
}
