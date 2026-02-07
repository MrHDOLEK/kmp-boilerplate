package com.kmpboilerplate.application.action

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetCatTagsActionTest {
    @Test
    fun `should return list of tags`() =
        runTest {
            // Arrange
            val tags = listOf("cute", "funny", "sleepy")
            val repository = FakeCatRepository(tags = tags)
            val action = GetCatTagsAction(repository)

            // Act
            val result = action()

            // Assert
            assertTrue(result.isSuccess)
            assertEquals(listOf("cute", "funny", "sleepy"), result.getOrNull())
        }

    @Test
    fun `should return empty list when no tags exist`() =
        runTest {
            // Arrange
            val repository = FakeCatRepository(tags = emptyList())
            val action = GetCatTagsAction(repository)

            // Act
            val result = action()

            // Assert
            assertTrue(result.isSuccess)
            assertEquals(emptyList(), result.getOrNull())
        }

    @Test
    fun `should return failure when repository throws`() =
        runTest {
            // Arrange
            val repository = FakeCatRepository(shouldThrow = true)
            val action = GetCatTagsAction(repository)

            // Act
            val result = action()

            // Assert
            assertTrue(result.isFailure)
            assertEquals("Repository error", result.exceptionOrNull()?.message)
        }
}
