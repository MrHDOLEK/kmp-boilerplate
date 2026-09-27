package com.kmpboilerplate.architecture

import com.lemonappdev.konsist.api.declaration.combined.KoClassAndObjectDeclaration
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Infrastructure layer: DTOs sit in their adapter's `dto/` folder, mappers beside the
 * adapter they serve, and there is no top-level `infrastructure/dto/` or `infrastructure/mapper/`.
 */
class InfrastructureStructureTest {
    /**
     * A DTO is named `*Dto` or is `@Serializable`; a mapper is named `*Mapper`. Either one outside an
     * adapter's folder of its kind is reported, and so is any infrastructure file whose package has a
     * `dto` or `mapper` segment anywhere but under an adapter — `infrastructure.dto`,
     * `infrastructure.mapper` and `infrastructure.http.dto` included.
     *
     * A row in a `dto/` folder that carries neither the `Dto` suffix nor `@Serializable` is judged only by
     * the folder it is in.
     */
    @Test
    fun `should keep data transfer objects and mappers inside their adapter`() {
        val infrastructure = ProjectScope.inPackage(InfrastructureRole.INFRASTRUCTURE)
        val declarations = infrastructure.classesAndObjects(includeNested = false, includeLocal = false)
        val transferObjects = declarations.filter(::isTransferObject)
        val mappers = declarations.filter { declaration -> declaration.name.endsWith("Mapper") }
        val offenders =
            transferObjects.filterNot { declaration -> inAdapter(packageOf(declaration), DTO) }.map(::describe) +
                mappers.filterNot { declaration -> inAdapter(packageOf(declaration), MAPPER) }.map(::describe) +
                infrastructure.files
                    .filter { file -> hasFolderOutsideAdapter(file.packagee?.name.orEmpty()) }
                    .map { file -> "${file.projectPath.trimStart('/')}: ${file.packagee?.name}" }

        assertTrue(
            transferObjects.isNotEmpty() && mappers.isNotEmpty(),
            "No DTOs or mappers found; the scope is misconfigured.",
        )
        InfrastructureRole.ADAPTERS.forEach { adapter ->
            assertTrue(
                infrastructure.files.any { file -> ImportRule.reaches(file.packagee?.name.orEmpty(), adapter) },
                "The adapter $adapter holds no file; InfrastructureRole.ADAPTERS is out of date.",
            )
        }
        assertTrue(
            offenders.isEmpty(),
            "A DTO belongs in its adapter's dto/ folder and a mapper in its adapter's mapper/ folder, one " +
                "of ${InfrastructureRole.ADAPTERS.joinToString()}:\n${offenders.joinToString(separator = "\n")}",
        )
    }

    private fun isTransferObject(declaration: KoClassAndObjectDeclaration): Boolean =
        declaration.name.endsWith("Dto") ||
            declaration.annotations.any { annotation -> annotation.name.substringAfterLast('.') == SERIALIZABLE }

    private fun hasFolderOutsideAdapter(packageName: String): Boolean =
        FOLDERS.any { folder -> folder in packageName.split('.') && !inAdapter(packageName, folder) }

    private fun inAdapter(
        packageName: String,
        folder: String,
    ): Boolean = InfrastructureRole.ADAPTERS.any { adapter -> ImportRule.reaches(packageName, "$adapter.$folder") }

    private fun packageOf(declaration: KoClassAndObjectDeclaration): String = declaration.packagee?.name.orEmpty()

    private fun describe(declaration: KoClassAndObjectDeclaration): String =
        "${declaration.containingFile.projectPath.trimStart('/')}: ${declaration.name}"

    private companion object {
        const val DTO = "dto"

        const val MAPPER = "mapper"

        const val SERIALIZABLE = "Serializable"

        val FOLDERS = listOf(DTO, MAPPER)
    }
}
