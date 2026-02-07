package com.kmpboilerplate.domain.entity

import kotlin.test.Test
import kotlin.test.assertEquals

class CatTest {
    @Test
    fun `should generate image url from id when url is null`() {
        // Arrange
        val cat = Cat(id = "abc123")

        // Act
        val result = cat.imageUrl

        // Assert
        assertEquals("https://cataas.com/cat/abc123", result)
    }

    @Test
    fun `should use provided url when available`() {
        // Arrange
        val cat = Cat(id = "abc123", url = "https://example.com/cat.jpg")

        // Act
        val result = cat.imageUrl

        // Assert
        assertEquals("https://example.com/cat.jpg", result)
    }

    @Test
    fun `should return createdAt as created when available`() {
        // Arrange
        val cat = Cat(id = "1", createdAt = "2025-01-01")

        // Act
        val result = cat.created

        // Assert
        assertEquals("2025-01-01", result)
    }

    @Test
    fun `should fallback to createdAtAlt when createdAt is null`() {
        // Arrange
        val cat = Cat(id = "1", createdAtAlt = "2025-06-15")

        // Act
        val result = cat.created

        // Assert
        assertEquals("2025-06-15", result)
    }

    @Test
    fun `should return null when no created date is available`() {
        // Arrange
        val cat = Cat(id = "1")

        // Act
        val result = cat.created

        // Assert
        assertEquals(null, result)
    }
}
