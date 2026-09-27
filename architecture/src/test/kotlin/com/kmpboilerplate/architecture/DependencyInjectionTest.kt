package com.kmpboilerplate.architecture

import com.lemonappdev.konsist.api.KoModifier
import com.lemonappdev.konsist.api.declaration.KoClassDeclaration
import com.lemonappdev.konsist.api.verify.assertTrue
import kotlin.test.Test
import kotlin.test.assertTrue

class DependencyInjectionTest {
    @Test
    fun `should start every Koin module from the container`() {
        val reading = KoinGraph.reading

        assertTrue(reading.started.isNotEmpty(), "No Koin module is started; the reading is misconfigured.")
        assertTrue(
            reading.problems.isEmpty(),
            "Every module is declared as val x = module { } or fun x(): Module = module { } in " +
                "infrastructure/config and listed in container:\n${reading.problems.joinToString(separator = "\n")}",
        )
    }

    @Test
    fun `should register every action service mapper and repository in the container`() {
        val registrations = KoinGraph.registrationsIn(KoinGraph.ALL)

        ProjectScope
            .inModule("shared")
            .classes(includeNested = false, includeLocal = false)
            .filter { declaration -> isInjected(declaration) }
            .filterNot { declaration -> declaration.hasModifier(KoModifier.ABSTRACT, *NOT_INSTANTIATED) }
            .assertTrue(
                strict = true,
                additionalMessage =
                    "Register it in a module container starts (Container.kt or a platform module).",
            ) { declaration ->
                Regex(
                    "::${declaration.name}\\b|\\b${declaration.name}\\s*\\(",
                ).containsMatchIn(registrations)
            }
    }

    @Test
    fun `should register a class on both platforms unless only one platform declares it`() {
        val declaredIn =
            ProjectScope
                .inModule("shared")
                .classesAndObjects(includeNested = false, includeLocal = false)
                .groupBy { declaration -> declaration.name }
                .mapValues { (_, declarations) ->
                    declarations.map { declaration -> declaration.containingFile.sourceSetName }.toSet()
                }
        val common = KoinGraph.registeredNamesIn(listOf(KoinGraph.COMMON))
        val registered = KoinGraph.PLATFORMS.associateWith { platform -> KoinGraph.registeredNamesIn(listOf(platform)) }
        val offenders =
            KoinGraph.PLATFORMS.flatMap { platform ->
                val others = KoinGraph.PLATFORMS.filterNot { other -> other == platform }
                val elsewhere = others.flatMap { other -> registered.getValue(other) }.toSet()

                (registered.getValue(platform) - common - elsewhere)
                    .filter { name -> name in declaredIn && declaredIn.getValue(name) != setOf(platform) }
                    .map { name ->
                        "$name is registered on $platform only, but ${declaredIn.getValue(name)} declares it"
                    }
            }

        if (KoinGraph.reading.started.any { body -> body.sourceSet in KoinGraph.PLATFORMS }) {
            KoinGraph.PLATFORMS.forEach { platform ->
                assertTrue(
                    registered.getValue(platform).isNotEmpty(),
                    "Nothing is registered on $platform; the reading is misconfigured.",
                )
            }
        }
        assertTrue(
            offenders.isEmpty(),
            "Register it on every platform, or declare it in that platform's source set:\n" +
                offenders.joinToString(separator = "\n"),
        )
    }

    @Test
    fun `should bind every domain contract that has an implementation`() {
        val contracts =
            ProjectScope
                .inPackage(DomainCollaborator.DOMAIN)
                .interfaces()
                .filterNot { contract -> contract.hasModifier(KoModifier.SEALED) }
                .map { contract -> contract.name }
                .toSet()
        val implemented =
            ProjectScope
                .inPackage(InfrastructureRole.INFRASTRUCTURE)
                .classesAndObjects(includeNested = false, includeLocal = false)
                .flatMap(InfrastructureRole::contractsOf)
                .filter { name -> name in contracts }
                .toSet()
        val common = KoinGraph.registrationsIn(listOf(KoinGraph.COMMON))
        val unbound =
            implemented.filterNot { contract ->
                val binding = bindingOf(contract)

                binding.containsMatchIn(common) ||
                    KoinGraph.PLATFORMS.all { platform ->
                        binding.containsMatchIn(KoinGraph.registrationsIn(listOf(platform)))
                    }
            }

        assertTrue(implemented.isNotEmpty(), "No implemented domain contract found; the scope is misconfigured.")
        assertTrue(
            unbound.isEmpty(),
            "Bind the implementation to its contract (bind X::class or single<X>) in a module container " +
                "starts:\n${unbound.sorted().joinToString(separator = "\n")}",
        )
    }

    @Test
    fun `should register every action as a factory`() {
        val actions =
            ProjectScope
                .inPackage(ACTIONS)
                .classes(includeNested = false, includeLocal = false)
                .filterNot { action -> action.hasPrivateModifier || action.hasInternalModifier }
                .map { action -> action.name }
        val offenders =
            actions.mapNotNull { action ->
                val kinds = KoinGraph.definitionKindsOf(action)

                when {
                    kinds.isEmpty() -> "$action is built by no started module"
                    kinds.any { kind -> kind != FACTORY } -> "$action is built by ${kinds.distinct().joinToString()}"
                    else -> null
                }
            }

        assertTrue(actions.isNotEmpty(), "No actions found; the scope is misconfigured.")
        assertTrue(
            offenders.isEmpty(),
            "Register an action with factoryOf(::X) or factory { X(...) } only:\n" +
                offenders.joinToString(separator = "\n"),
        )
    }

    @Test
    fun `should register every class that keeps state as a single`() {
        val stateful =
            ProjectScope.production
                .classes(includeNested = false, includeLocal = false)
                .filter { declaration -> keepsState(declaration) }
                .map { declaration -> declaration.name }
                .filter { name -> KoinGraph.definitionKindsOf(name).isNotEmpty() }
        val offenders =
            stateful.mapNotNull { name ->
                val kinds = KoinGraph.definitionKindsOf(name)

                if (kinds.all { kind ->
                        kind == SINGLE
                    }
                ) {
                    null
                } else {
                    "$name is built by ${kinds.distinct().joinToString()}"
                }
            }

        assertTrue(
            offenders.isEmpty(),
            "Register a class that keeps state with singleOf(::X) or single { X(...) } only:\n" +
                offenders.joinToString(separator = "\n"),
        )
    }

    private fun keepsState(declaration: KoClassDeclaration): Boolean =
        declaration.properties(includeNested = false).any { property ->
            property.isVar || MUTABLE_HOLDER.containsMatchIn(KotlinSources.symbolsOf(property.text))
        }

    private fun isInjected(declaration: KoClassDeclaration): Boolean {
        val packageName = declaration.packagee?.name.orEmpty()
        val inInjectedFolder =
            INJECTED_PACKAGES.any { injected -> ImportRule.reaches(packageName, injected) } ||
                InfrastructureRole.ADAPTERS.any { adapter -> ImportRule.reaches(packageName, "$adapter.mapper") }

        return (inInjectedFolder && declaration.hasPublicOrDefaultModifier) ||
            (
                ImportRule.reaches(packageName, InfrastructureRole.INFRASTRUCTURE) &&
                    InfrastructureRole.contractsOf(declaration).any(DomainCollaborator::isRepository)
            ) ||
            INJECTED_SUFFIXES.any { suffix -> declaration.name.endsWith(suffix) }
    }

    private fun bindingOf(contract: String): Regex =
        Regex(
            """\bbinds?\b[^\n]*\b$contract\s*::\s*class|\bbind\s*<\s*$contract\s*>|""" +
                """\b(?:single|factory|scoped)\s*<\s*$contract\s*>""",
        )

    private companion object {
        const val ACTIONS = "com.kmpboilerplate.application.action"
        const val FACTORY = "factory"
        const val SINGLE = "single"

        val MUTABLE_HOLDER = Regex("""\b(?:Mutable\w*|mutable\w*Of)\s*[(<]""")

        val INJECTED_PACKAGES =
            listOf(
                ACTIONS,
                "com.kmpboilerplate.application.viewmodel.mapper",
                InfrastructureRole.REPOSITORIES,
            )

        val INJECTED_SUFFIXES = listOf("Action", "Repository") + DomainCollaborator.SERVICE_SUFFIXES

        val NOT_INSTANTIATED =
            arrayOf(KoModifier.DATA, KoModifier.ENUM, KoModifier.SEALED, KoModifier.VALUE, KoModifier.PRIVATE)
    }
}
