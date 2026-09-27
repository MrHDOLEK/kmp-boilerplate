package com.kmpboilerplate.architecture

import com.lemonappdev.konsist.api.declaration.combined.KoClassAndObjectDeclaration
import kotlin.test.Test
import kotlin.test.assertTrue

class InfrastructureStructureTest {
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
