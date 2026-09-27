package com.kmpboilerplate.architecture

import com.lemonappdev.konsist.api.declaration.KoTypeAliasDeclaration
import com.lemonappdev.konsist.api.verify.assertFalse
import kotlin.test.Test
import kotlin.test.assertTrue

class UserInterfaceBoundaryTest {
    @Test
    fun `should reach infrastructure from the compose module only through bootstrap`() {
        ProjectScope.inModule(COMPOSE_APP).imports.assertFalse(
            strict = true,
            additionalMessage = "From infrastructure, composeApp may import only config.bootstrap.",
        ) { import ->
            ImportRule.reaches(import.name, INFRASTRUCTURE) &&
                COMPOSE_ALLOWED.none { allowed -> ImportRule.reaches(import.name, allowed) }
        }
    }

    @Test
    fun `should keep the domain out of the compose module`() {
        ProjectScope.inModule(COMPOSE_APP).imports.assertFalse(
            strict = true,
            additionalMessage = "The UI reads the domain through an action in com.kmpboilerplate.application.",
        ) { import -> ImportRule.reaches(import.name, DOMAIN) }
    }

    @Test
    fun `should name no domain or infrastructure in the code of the compose module`() {
        QualifiedReference.assertNoneNamed(ProjectScope.inModule(COMPOSE_APP).files, BEYOND_ACTIONS) { name ->
            BEYOND_ACTIONS_PACKAGES.any { forbidden -> ImportRule.reaches(name, forbidden) } &&
                COMPOSE_ALLOWED.none { allowed -> ImportRule.reaches(name, allowed) }
        }
    }

    @Test
    fun `should alias no domain type in the application layer`() {
        val application = ProjectScope.inPackage(APPLICATION)
        val offenders =
            application.typeAliases
                .filter(::aliasesTheDomain)
                .map { alias -> "${alias.containingFile.projectPath.trimStart('/')}: ${alias.name}" }

        assertTrue(
            application.files.isNotEmpty() && DomainReference.typeNames.isNotEmpty(),
            "No application files or domain types found; the scope is misconfigured.",
        )
        assertTrue(
            offenders.isEmpty(),
            "The UI reads a ViewModel, never a domain entity under another name:\n" +
                offenders.joinToString(separator = "\n"),
        )
    }

    @Test
    fun `should name nothing from the domain in a view model`() {
        val viewModels =
            ProjectScope.inPackage(VIEW_MODEL).files.filterNot { file ->
                ImportRule.reaches(file.packagee?.name.orEmpty(), VIEW_MODEL_MAPPER)
            }

        QualifiedReference.assertNoneNamed(viewModels, CARRIES_NO_ENTITY) { name -> ImportRule.reaches(name, DOMAIN) }
    }

    private fun aliasesTheDomain(alias: KoTypeAliasDeclaration): Boolean =
        DomainReference.namedIn(KotlinSources.aliasedIn(alias.text), alias.containingFile).isNotEmpty()

    private companion object {
        const val COMPOSE_APP = "composeApp"
        const val INFRASTRUCTURE = "com.kmpboilerplate.infrastructure"
        const val DOMAIN = "com.kmpboilerplate.domain"
        const val APPLICATION = "com.kmpboilerplate.application"
        const val BOOTSTRAP = "com.kmpboilerplate.infrastructure.config.bootstrap"
        const val VIEW_MODEL = "com.kmpboilerplate.application.viewmodel"
        const val VIEW_MODEL_MAPPER = "$VIEW_MODEL.mapper"

        const val CARRIES_NO_ENTITY =
            "A ViewModel carries values the UI may read; a domain type inside it is a domain type in the UI."

        const val BEYOND_ACTIONS =
            "The UI reads the domain through an action in com.kmpboilerplate.application; from infrastructure it " +
                "names only config.bootstrap."

        val BEYOND_ACTIONS_PACKAGES = listOf(DOMAIN, INFRASTRUCTURE)

        val COMPOSE_ALLOWED = setOf(BOOTSTRAP)
    }
}
