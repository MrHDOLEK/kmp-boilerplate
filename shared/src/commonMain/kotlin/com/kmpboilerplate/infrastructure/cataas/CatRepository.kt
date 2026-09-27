package com.kmpboilerplate.infrastructure.cataas

import com.kmpboilerplate.domain.cat.Cat
import com.kmpboilerplate.domain.cat.CatRepositoryInterface
import com.kmpboilerplate.infrastructure.cataas.dto.CatDto
import com.kmpboilerplate.infrastructure.cataas.mapper.CatDtoMapper
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class CatRepository(
    private val client: HttpClient,
    private val mapper: CatDtoMapper,
) : CatRepositoryInterface {
    override suspend fun getRandomCat(): Cat =
        mapper.map(client.get("$CATAAS_BASE_URL/cat") { parameter("json", true) }.body<CatDto>())

    override suspend fun getCats(
        tags: List<String>,
        skip: Int,
        limit: Int,
    ): List<Cat> {
        val cats =
            client
                .get("$CATAAS_BASE_URL/api/cats") {
                    if (tags.isNotEmpty()) {
                        parameter("tags", tags.joinToString(","))
                    }
                    parameter("skip", skip)
                    parameter("limit", limit)
                }.body<List<CatDto>>()
        return mapper.mapCollection(cats)
    }

    override suspend fun getTags(): List<String> = client.get("$CATAAS_BASE_URL/api/tags").body()
}
