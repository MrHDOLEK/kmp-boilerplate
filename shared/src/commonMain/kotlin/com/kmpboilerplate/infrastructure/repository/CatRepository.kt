package com.kmpboilerplate.infrastructure.repository

import com.kmpboilerplate.domain.entity.Cat
import com.kmpboilerplate.domain.repository.CatRepositoryInterface
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class CatRepository(
    private val client: HttpClient,
) : CatRepositoryInterface {
    companion object {
        private const val BASE_URL = "https://cataas.com"
    }

    override suspend fun getRandomCat(): Cat = client.get("$BASE_URL/cat?json=true").body()

    override suspend fun getCatById(id: String): Cat = client.get("$BASE_URL/cat/$id?json=true").body()

    override suspend fun getCats(
        tags: List<String>?,
        skip: Int,
        limit: Int,
    ): List<Cat> =
        client
            .get("$BASE_URL/api/cats") {
                tags?.let { parameter("tags", it.joinToString(",")) }
                parameter("skip", skip)
                parameter("limit", limit)
            }.body()

    override suspend fun getTags(): List<String> = client.get("$BASE_URL/api/tags").body()
}
