package com.kmpboilerplate.application.action

import com.kmpboilerplate.domain.entity.Cat
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetRandomCatActionTest {
    @Test
    fun `should return random cat`() =
        runTest {
            // Arrange
            val cat = Cat(id = "random-1", tags = listOf("cute"))
            val repository = FakeCatRepository(cats = listOf(cat))
            val action = GetRandomCatAction(repository)

            // Act
            val result = action()

            // Assert
            assertTrue(result.isSuccess)
            assertEquals("random-1", result.getOrNull()?.id)
        }

    @Test
    fun `should return failure when repository throws`() =
        runTest {
            // Arrange
            val repository = FakeCatRepository(shouldThrow = true)
            val action = GetRandomCatAction(repository)

            // Act
            val result = action()

            // Assert
            assertTrue(result.isFailure)
            assertEquals("Repository error", result.exceptionOrNull()?.message)
        }
}
