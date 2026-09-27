package com.kmpboilerplate.infrastructure.cataas.mapper

import com.kmpboilerplate.domain.cat.Cat
import com.kmpboilerplate.infrastructure.cataas.dto.CatDto
import kotlin.test.Test
import kotlin.test.assertEquals

class CatDtoMapperTest {
    @Test
    fun `should resolve the image url from the cat id`() {
        // Arrange
        val dto = CatDto(id = "abc123", tags = listOf("cute"))

        // Act
        val cat = CatDtoMapper().map(dto)

        // Assert
        assertEquals(
            Cat(
                id = "abc123",
                tags = listOf("cute"),
                imageUrl = "https://cataas.com/cat/abc123",
            ),
            cat,
        )
    }

    @Test
    fun `should map every cat of a collection`() {
        // Arrange
        val dtos = listOf(CatDto(id = "1"), CatDto(id = "2"))

        // Act
        val cats = CatDtoMapper().mapCollection(dtos)

        // Assert
        assertEquals(listOf("1", "2"), cats.map { cat -> cat.id })
    }
}
