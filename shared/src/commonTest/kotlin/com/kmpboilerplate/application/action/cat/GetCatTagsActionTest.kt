package com.kmpboilerplate.application.action.cat

import com.kmpboilerplate.domain.cat.CatService
import com.kmpboilerplate.domain.cat.FakeCatRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetCatTagsActionTest {
    @Test
    fun `should return the tags`() =
        runTest {
            // Arrange
            val action = GetCatTagsAction(CatService(FakeCatRepository(tags = listOf("cute", "funny", "sleepy"))))

            // Act
            val result = action()

            // Assert
            assertEquals(listOf("cute", "funny", "sleepy"), result.getOrThrow())
        }

    @Test
    fun `should return an empty list when no tags exist`() =
        runTest {
            // Arrange
            val action = GetCatTagsAction(CatService(FakeCatRepository(tags = emptyList())))

            // Act
            val result = action()

            // Assert
            assertEquals(emptyList(), result.getOrThrow())
        }

    @Test
    fun `should return a failure when the repository throws`() =
        runTest {
            // Arrange
            val action = GetCatTagsAction(CatService(FakeCatRepository(shouldThrow = true)))

            // Act
            val result = action()

            // Assert
            assertTrue(result.isFailure)
            assertEquals(FakeCatRepository.ERROR_MESSAGE, result.exceptionOrNull()?.message)
        }
}
