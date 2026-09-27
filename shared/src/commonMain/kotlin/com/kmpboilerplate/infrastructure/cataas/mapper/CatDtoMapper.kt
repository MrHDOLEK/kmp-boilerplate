package com.kmpboilerplate.infrastructure.cataas.mapper

import com.kmpboilerplate.domain.cat.Cat
import com.kmpboilerplate.infrastructure.cataas.CATAAS_BASE_URL
import com.kmpboilerplate.infrastructure.cataas.dto.CatDto

class CatDtoMapper {
    fun map(dto: CatDto): Cat =
        Cat(
            id = dto.id,
            tags = dto.tags,
            imageUrl = "$CATAAS_BASE_URL/cat/${dto.id}",
        )

    fun mapCollection(dtos: List<CatDto>): List<Cat> = dtos.map { dto -> map(dto) }
}
