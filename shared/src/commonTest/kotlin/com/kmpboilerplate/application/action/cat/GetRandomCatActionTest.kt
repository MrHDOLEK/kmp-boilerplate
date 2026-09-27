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

class GetRandomCatActionTest {
    @Test
    fun `should return a random cat as a view model`() =
        runTest {
            // Arrange
            val action = actionFor(FakeCatRepository(cats = listOf(cat(id = "random-1", tags = listOf("cute")))))

            // Act
            val result = action()

            // Assert
            assertEquals(
                CatViewModel(id = "random-1", imageUrl = "https://example.com/cat/random-1", tags = listOf("cute")),
                result.getOrThrow(),
            )
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

    private fun actionFor(repository: FakeCatRepository): GetRandomCatAction =
        GetRandomCatAction(CatService(repository), CatViewModelMapper())
}
