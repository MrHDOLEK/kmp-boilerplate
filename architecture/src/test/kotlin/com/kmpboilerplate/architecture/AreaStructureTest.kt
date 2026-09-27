package com.kmpboilerplate.architecture

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AreaStructureTest {
    @Test
    fun `should split the four layers into the same areas`() {
        val areas = LAYERS.associateWith(::areasOf)
        val vocabulary = areas.values.flatten().toSet()

        assertTrue(vocabulary.isNotEmpty(), "No area folders found; the scope is misconfigured.")

        LAYERS.forEach { layer ->
            assertEquals(
                (vocabulary - ABSENT.getValue(layer)).sorted(),
                areas.getValue(layer).sorted(),
                "$layer is split into other areas than the rest. $ONE_NAME",
            )
        }
    }

    @Test
    fun `should leave no file at the root of a layer`() {
        val loose =
            ProjectScope.production.files
                .filter { file -> file.packagee?.name in ROOTS }
                .map { file -> file.path }

        assertTrue(loose.isEmpty(), "A file belongs to an area folder:\n${loose.joinToString(separator = "\n")}")
    }

    @Test
    fun `should hold only the declared vocabulary in domain shared`() {
        val folders =
            ProjectScope.production.files
                .mapNotNull { file -> file.packagee?.name }
                .filter { name -> name.startsWith("$SHARED_PACKAGE.") }
                .map { name -> name.removePrefix("$SHARED_PACKAGE.").substringBefore('.') }
                .toSet()

        assertEquals(
            DomainArea.SHARED_VOCABULARY,
            folders,
            "domain/shared holds only the declared vocabulary; a feature of its own is an area. $ONE_NAME",
        )
        assertTrue(
            ABSENT.getValue(DOMAIN).containsAll(DomainArea.SHARED_VOCABULARY),
            "A word of domain/shared is no area of the domain: declare it absent in ABSENT.",
        )
    }

    private fun areasOf(layer: String): List<String> =
        ProjectScope.production.files
            .mapNotNull { file -> file.packagee?.name }
            .filter { name -> name.startsWith("$layer.") }
            .map { name -> name.removePrefix("$layer.").substringBefore('.') }
            .filterNot { area -> layer == VIEW_MODEL && area == MAPPER_FOLDER }
            .filterNot { area -> layer == DOMAIN && area == SHARED_FOLDER }
            .distinct()

    private companion object {
        const val DOMAIN = DomainCollaborator.DOMAIN
        const val ACTION = "com.kmpboilerplate.application.action"
        const val VIEW_MODEL = "com.kmpboilerplate.application.viewmodel"
        const val MAPPER = "com.kmpboilerplate.application.viewmodel.mapper"

        const val MAPPER_FOLDER = "mapper"

        const val SHARED_FOLDER = "shared"

        const val SHARED_PACKAGE = "$DOMAIN.$SHARED_FOLDER"

        const val ONE_NAME = "One feature is one folder name across the domain, action, viewmodel and mapper."

        val LAYERS = listOf(DOMAIN, ACTION, VIEW_MODEL, MAPPER)

        val ROOTS = LAYERS + listOf("com.kmpboilerplate.application", "com.kmpboilerplate.infrastructure")

        val ABSENT =
            mapOf(
                DOMAIN to emptySet<String>(),
                ACTION to emptySet<String>(),
                VIEW_MODEL to emptySet<String>(),
                MAPPER to emptySet<String>(),
            )
    }
}
