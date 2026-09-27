package com.kmpboilerplate.application.action.cat

import com.kmpboilerplate.application.viewmodel.cat.CatViewModel
import com.kmpboilerplate.application.viewmodel.mapper.cat.CatViewModelMapper
import com.kmpboilerplate.domain.cat.CatService
import com.kmpboilerplate.domain.cat.FakeCatRepository
import com.kmpboilerplate.domain.cat.FakeCatRepository.Companion.cat
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetCatsActionTest {
    @Test
    fun `should return the cats as view models`() =
        runTest {
            // Arrange
            val action =
                actionFor(
                    FakeCatRepository(cats = listOf(cat(id = "1", tags = listOf("cute")), cat(id = "2"))),
                )

            // Act
            val result = action()

            // Assert
            assertEquals(
                listOf(
                    CatViewModel(id = "1", imageUrl = "https://example.com/cat/1", tags = listOf("cute")),
                    CatViewModel(id = "2", imageUrl = "https://example.com/cat/2", tags = emptyList()),
                ),
                result.getOrThrow(),
            )
        }

    @Test
    fun `should return an empty list when no cats exist`() =
        runTest {
            // Arrange
            val action = actionFor(FakeCatRepository(cats = emptyList()))

            // Act
            val result = action()

            // Assert
            assertEquals(emptyList(), result.getOrThrow())
        }

    @Test
    fun `should return only the cats of the selected tag`() =
        runTest {
            // Arrange
            val action =
                actionFor(
                    FakeCatRepository(
                        cats =
                            listOf(
                                cat(id = "1", tags = listOf("cute")),
                                cat(id = "2", tags = listOf("funny")),
                            ),
                    ),
                )

            // Act
            val result = action(tag = "funny")

            // Assert
            assertEquals(listOf("2"), result.getOrThrow().map { cat -> cat.id })
        }

    @Test
    fun `should return a failure when the repository throws`() =
        runTest {
            // Arrange
            val action = actionFor(FakeCatRepository(shouldThrow = true))

            // Act
            val result = action()

            // Assert
            assertTrue(result.isFailure)
            assertEquals(FakeCatRepository.ERROR_MESSAGE, result.exceptionOrNull()?.message)
        }

    private fun actionFor(repository: FakeCatRepository): GetCatsAction =
        GetCatsAction(CatService(repository), CatViewModelMapper())
}
