package com.kmpboilerplate.domain.cat

interface CatRepositoryInterface {
    suspend fun getRandomCat(): Cat

    suspend fun getCats(
        tags: List<String>,
        skip: Int,
        limit: Int,
    ): List<Cat>

    suspend fun getTags(): List<String>
}
