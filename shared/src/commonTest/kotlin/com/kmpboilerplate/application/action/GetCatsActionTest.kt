package com.kmpboilerplate.application.action

import com.kmpboilerplate.domain.entity.Cat
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetCatsActionTest {
    @Test
    fun `should return list of cats`() =
        runTest {
            // Arrange
            val cats =
                listOf(
                    Cat(id = "1", tags = listOf("cute")),
                    Cat(id = "2", tags = listOf("funny")),
                )
            val repository = FakeCatRepository(cats = cats)
            val action = GetCatsAction(repository)

            // Act
            val result = action()

            // Assert
            assertTrue(result.isSuccess)
            assertEquals(2, result.getOrNull()?.size)
        }

    @Test
    fun `should return empty list when no cats exist`() =
        runTest {
            // Arrange
            val repository = FakeCatRepository(cats = emptyList())
            val action = GetCatsAction(repository)

            // Act
            val result = action()

            // Assert
            assertTrue(result.isSuccess)
            assertEquals(0, result.getOrNull()?.size)
        }

    @Test
    fun `should return failure when repository throws`() =
        runTest {
            // Arrange
            val repository = FakeCatRepository(shouldThrow = true)
            val action = GetCatsAction(repository)

            // Act
            val result = action()

            // Assert
            assertTrue(result.isFailure)
            assertEquals("Repository error", result.exceptionOrNull()?.message)
        }

    @Test
    fun `should filter cats by tags`() =
        runTest {
            // Arrange
            val cats =
                listOf(
                    Cat(id = "1", tags = listOf("cute")),
                    Cat(id = "2", tags = listOf("funny")),
                    Cat(id = "3", tags = listOf("cute", "funny")),
                )
            val repository = FakeCatRepository(cats = cats)
            val action = GetCatsAction(repository)

            // Act
            val result = action(tags = listOf("funny"))

            // Assert
            assertTrue(result.isSuccess)
            assertEquals(2, result.getOrNull()?.size)
        }
}
