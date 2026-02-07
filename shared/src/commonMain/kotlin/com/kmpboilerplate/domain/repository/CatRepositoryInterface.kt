package com.kmpboilerplate.domain.repository

import com.kmpboilerplate.domain.entity.Cat

interface CatRepositoryInterface {
    suspend fun getRandomCat(): Cat

    suspend fun getCatById(id: String): Cat

    suspend fun getCats(
        tags: List<String>? = null,
        skip: Int = 0,
        limit: Int = 10,
    ): List<Cat>

    suspend fun getTags(): List<String>
}
