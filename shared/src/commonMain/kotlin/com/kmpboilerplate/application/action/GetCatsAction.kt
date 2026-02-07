package com.kmpboilerplate.application.action

import com.kmpboilerplate.domain.entity.Cat
import com.kmpboilerplate.domain.repository.CatRepositoryInterface

class GetCatsAction(
    private val catRepository: CatRepositoryInterface,
) {
    suspend operator fun invoke(
        tags: List<String>? = null,
        skip: Int = 0,
        limit: Int = 10,
    ): Result<List<Cat>> =
        try {
            Result.success(this.catRepository.getCats(tags, skip, limit))
        } catch (e: Exception) {
            Result.failure(e)
        }
}
